package com.abcbank.mobile.feature.payments

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.Beneficiary
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Rate
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.security.MpinVerifier
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.core.security.StepUpAction
import com.abcbank.mobile.core.security.StepUpControllerFactory
import com.abcbank.mobile.core.security.StepUpGrant
import com.abcbank.mobile.core.security.StepUpManager
import com.abcbank.mobile.feature.payments.domain.AddPayeeUseCase
import com.abcbank.mobile.feature.payments.domain.AmountError
import com.abcbank.mobile.feature.payments.domain.AmountInput
import com.abcbank.mobile.feature.payments.domain.BankDirectory
import com.abcbank.mobile.feature.payments.domain.MoveMoneyUseCase
import com.abcbank.mobile.feature.payments.domain.NewPayee
import com.abcbank.mobile.feature.payments.domain.NotificationType
import com.abcbank.mobile.feature.payments.domain.ObservePaymentsDataUseCase
import com.abcbank.mobile.feature.payments.domain.PayeeError
import com.abcbank.mobile.feature.payments.domain.PayeeResult
import com.abcbank.mobile.feature.payments.domain.PaymentsData
import com.abcbank.mobile.feature.payments.domain.PaymentsRepository
import com.abcbank.mobile.feature.payments.domain.SendTransferUseCase
import com.abcbank.mobile.feature.payments.domain.TransferPlan
import com.abcbank.mobile.feature.payments.domain.TransferPlanner
import com.abcbank.mobile.feature.payments.domain.TransferResult
import com.abcbank.mobile.feature.payments.domain.TransferRules
import com.abcbank.mobile.feature.payments.domain.TransferSpeed
import com.abcbank.mobile.feature.payments.ui.TransferMode
import com.abcbank.mobile.feature.payments.ui.TransferUiState
import com.abcbank.mobile.feature.payments.ui.TransferViewModel
import com.abcbank.mobile.testing.FakeMpinAttemptStore
import com.abcbank.mobile.testing.MainDispatcherRule
import com.abcbank.mobile.testing.ManualClock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val NOW: Instant = Instant.parse("2026-10-20T15:00:00Z")

private val CHECKING = DepositAccount(
    id = "checking",
    type = AccountType.CHECKING,
    nickname = "Everyday Checking",
    last4 = "3144",
    available = Money(155_832_00),
    ledger = Money(156_207_00),
    apy = null
)
private val SAVINGS = DepositAccount(
    id = "savings",
    type = AccountType.SAVINGS,
    nickname = "Travel Savings",
    last4 = "7720",
    available = Money(12_032_00),
    ledger = Money(12_032_00),
    apy = null
)
private val CREDIT_LINE = LoanAccount(
    id = "loc",
    nickname = "Personal Line of Credit",
    last4 = "4410",
    creditLimit = Money.ofDollars(10_000),
    owed = Money.ofDollars(2_000),
    availableToDraw = Money.ofDollars(8_000),
    apr = Rate(1_199),
    principal = null,
    openedAt = null
)
private val MARK =
    Beneficiary(
        "mark-robinson",
        "Mark Robinson",
        "Mark R.",
        "Northfield Bank",
        "500000021",
        "1180",
        0
    )
private val OAKWOOD = Beneficiary(
    "oakwood-properties",
    "Oakwood Properties",
    "Oakwood Properties",
    "Summit National Bank",
    "500000050",
    "3305",
    null
)

private class FakePaymentsRepository : PaymentsRepository {
    val data = MutableStateFlow<DataResult<PaymentsData>?>(null)
    val plans = mutableListOf<TransferPlan>()
    val payees = mutableListOf<NewPayee>()

    override fun observePaymentsData(): Flow<DataResult<PaymentsData>> = data.filterNotNull()

    override suspend fun applyTransfer(plan: TransferPlan): DataResult<Unit> {
        plans += plan
        return DataResult.Success(Unit)
    }

    override suspend fun addPayee(payee: NewPayee): DataResult<Unit> {
        payees += payee
        return DataResult.Success(Unit)
    }
}

/** The catalog's first banks (DEMO_DATA.md section 07). */
private val BANKS = object : BankDirectory {
    override suspend fun bankName(routingNumber: String): String? =
        mapOf("500000018" to "ABC Bank", "500000021" to "Northfield Bank")[routingNumber]
}

class TransferRulesTest {
    @Test
    fun `instant fee is 1_5 percent rounded half up, between 25 cents and $15`() {
        assertEquals(Money(150), TransferRules.fee(Money.ofDollars(100), TransferSpeed.INSTANT))
        // $33.67 x 1.5% = 50.505 cents, which rounds up.
        assertEquals(Money(51), TransferRules.fee(Money(3_367), TransferSpeed.INSTANT))
        assertEquals(Money(25), TransferRules.fee(Money.ofDollars(10), TransferSpeed.INSTANT))
        assertEquals(
            Money.ofDollars(15),
            TransferRules.fee(Money.ofDollars(2_000), TransferSpeed.INSTANT)
        )
        assertEquals(Money.ZERO, TransferRules.fee(Money.ofDollars(2_000), TransferSpeed.STANDARD))
        assertEquals(Money.ZERO, TransferRules.fee(Money.ZERO, TransferSpeed.INSTANT))
    }

    @Test
    fun `a send must fit the available balance including the fee`() {
        val all = SAVINGS.available
        assertNull(TransferRules.validateSend(all, Money.ZERO, SAVINGS))
        assertEquals(
            AmountError.OVER_AVAILABLE,
            TransferRules.validateSend(all, Money(25), SAVINGS)
        )
        assertEquals(AmountError.ZERO, TransferRules.validateSend(Money.ZERO, Money.ZERO, SAVINGS))
    }

    @Test
    fun `moves need two accounts, fit the source, and cannot overpay a credit line`() {
        assertEquals(
            AmountError.SAME_ACCOUNT,
            TransferRules.validateMove(Money(100), CHECKING, CHECKING)
        )
        assertEquals(
            AmountError.OVER_OWED,
            TransferRules.validateMove(Money.ofDollars(2_001), CHECKING, CREDIT_LINE)
        )
        assertNull(TransferRules.validateMove(Money.ofDollars(2_000), CHECKING, CREDIT_LINE))
        assertEquals(
            AmountError.OVER_AVAILABLE,
            TransferRules.validateMove(Money.ofDollars(8_001), CREDIT_LINE, SAVINGS)
        )
        assertNull(TransferRules.validateMove(Money.ofDollars(8_000), CREDIT_LINE, SAVINGS))
    }

    @Test
    fun `only checking and savings send, and credit lines with a limit can move`() {
        assertTrue(TransferRules.canSendFrom(CHECKING))
        assertFalse(TransferRules.canSendFrom(CREDIT_LINE))
        assertTrue(TransferRules.canMoveBetween(CREDIT_LINE))
        assertFalse(TransferRules.canMoveBetween(CREDIT_LINE.copy(creditLimit = null)))
    }

    @Test
    fun `routing numbers pass the ABA checksum`() {
        assertTrue(TransferRules.passesAbaChecksum("500000021"))
        assertTrue(TransferRules.passesAbaChecksum("500000018"))
        assertFalse(TransferRules.passesAbaChecksum("500000022"))
        assertFalse(TransferRules.passesAbaChecksum("50000002"))
    }

    @Test
    fun `payee checks run in order and numbers ending 0000 fail verification`() {
        fun check(
            name: String = "Jordan Ellis",
            routing: String = "500000021",
            account: String = "40021187",
            bank: String? = "Northfield Bank"
        ) = TransferRules.validatePayee(name, routing, account, bank)
        assertNull(check())
        assertEquals(PayeeError.NAME_MISSING, check(name = "  "))
        assertEquals(PayeeError.NAME_TOO_LONG, check(name = "x".repeat(41)))
        assertEquals(PayeeError.ROUTING_FORMAT, check(routing = "5000"))
        assertEquals(PayeeError.ROUTING_CHECKSUM, check(routing = "500000022"))
        assertEquals(PayeeError.BANK_NOT_FOUND, check(bank = null))
        assertEquals(PayeeError.ACCOUNT_FORMAT, check(account = "12345"))
        assertEquals(PayeeError.ACCOUNT_NOT_VERIFIED, check(account = "12340000"))
    }

    @Test
    fun `short names keep the first name and last initial`() {
        assertEquals("Jordan E.", TransferRules.shortName("  Jordan  Ellis "))
        assertEquals("Cher", TransferRules.shortName("Cher"))
    }
}

class AmountInputTest {
    private fun type(keys: String) = keys.fold(AmountInput()) { input, key -> input.press(key) }

    @Test
    fun `typing builds cents with at most two decimals and seven whole digits`() {
        assertEquals(Money(1_250), type("12.5").money)
        assertEquals("12.50", type("12.505").text)
        assertEquals("0.", type(".").text)
        assertEquals("5", type("05").text)
        assertEquals("1.5", type("1..5").text)
        assertEquals("9999999", type("99999999").text)
        assertEquals(Money.ZERO, AmountInput().money)
        assertEquals("1", type("12.").delete().delete().text)
        assertEquals("1", type("12").delete().text)
    }
}

class TransferPlannerTest {
    @Test
    fun `a send debits the amount and fee, with running balances and the alert`() {
        val plan = TransferPlanner.send(
            source = SAVINGS,
            beneficiary = MARK,
            amount = Money.ofDollars(100),
            fee = Money(150),
            memo = "Dinner",
            category = TransactionCategory.DINING,
            reference = "ref",
            idPrefix = "transfer-ref",
            now = NOW
        )
        val update = plan.accounts.single()
        assertEquals(SAVINGS.available - Money(10_150), update.available)
        assertEquals(SAVINGS.ledger - Money(10_150), update.ledger)
        val (main, fee) = plan.transactions
        assertEquals("transfer-ref-main", plan.mainTransactionId)
        assertEquals(Money.ofDollars(100), main.amount)
        assertEquals("Mark Robinson", main.merchant)
        assertEquals("Dinner", main.memo)
        assertEquals(SAVINGS.ledger - Money.ofDollars(100), main.runningBalance)
        assertEquals(TransferPlanner.INSTANT_FEE_MERCHANT, fee.merchant)
        assertEquals("ref-fee", fee.reference)
        assertEquals(update.ledger, fee.runningBalance)
        assertTrue(fee.createdAt.isAfter(main.createdAt))
        assertEquals(listOf(NotificationType.MONEY_SENT), plan.notifications.map { it.type })
        assertEquals(listOf("10000", "Mark Robinson"), plan.notifications.single().args)
    }

    @Test
    fun `a standard send has no fee leg, and crossing below $500 adds one low balance alert`() {
        val source = SAVINGS.copy(available = Money.ofDollars(600), ledger = Money.ofDollars(600))
        val plan = TransferPlanner.send(
            source, MARK,
            Money.ofDollars(
                200
            ),
            Money.ZERO, null, TransactionCategory.TRANSFER_OUT, "r", "t", NOW
        )
        assertEquals(1, plan.transactions.size)
        assertEquals(
            listOf(NotificationType.MONEY_SENT, NotificationType.LOW_BALANCE),
            plan.notifications.map { it.type }
        )
        // Already low: no second alert.
        val again = TransferPlanner.send(
            SAVINGS.copy(available = Money.ofDollars(400), ledger = Money.ofDollars(400)),
            MARK,
            Money.ofDollars(10),
            Money.ZERO,
            null,
            TransactionCategory.TRANSFER_OUT,
            "r",
            "t",
            NOW
        )
        assertEquals(listOf(NotificationType.MONEY_SENT), again.notifications.map { it.type })
    }

    @Test
    fun `paying a credit line lowers what is owed and credits both legs`() {
        val plan = TransferPlanner.move(
            CHECKING,
            CREDIT_LINE,
            Money.ofDollars(500),
            "ref",
            "move-ref",
            NOW
        )
        val (out, into) = plan.transactions
        assertEquals(TransactionDirection.DEBIT, out.direction)
        assertEquals("To Personal Line of Credit ••4410", out.merchant)
        assertEquals("From Everyday Checking ••3144", into.merchant)
        assertEquals(Money.ofDollars(1_500), into.runningBalance)
        val loan = plan.accounts.first { it.accountId == "loc" }
        assertEquals(Money.ofDollars(8_500), loan.available)
        assertEquals(Money.ofDollars(1_500), loan.ledger)
        assertEquals(NotificationType.MONEY_MOVED, plan.notifications.first().type)
    }

    @Test
    fun `drawing from a credit line raises what is owed`() {
        val plan = TransferPlanner.move(
            CREDIT_LINE,
            SAVINGS,
            Money.ofDollars(1_000),
            "ref",
            "move-ref",
            NOW
        )
        val loan = plan.accounts.first { it.accountId == "loc" }
        assertEquals(Money.ofDollars(7_000), loan.available)
        assertEquals(Money.ofDollars(3_000), loan.ledger)
        val savings = plan.accounts.first { it.accountId == "savings" }
        assertEquals(SAVINGS.available + Money.ofDollars(1_000), savings.available)
    }
}

class TransferUseCasesTest {
    private val repository = FakePaymentsRepository()
    private val clock = ManualClock(NOW)
    private val stepUp = StepUpManager(clock)

    @Test
    fun `sends up to $1,000 need no step-up, above it a grant for that amount`() = runTest {
        val send = SendTransferUseCase(repository, stepUp, clock)
        suspend fun sendAmount(amount: Money, grant: StepUpGrant?) = send(
            CHECKING,
            MARK,
            amount,
            TransferSpeed.STANDARD,
            " ",
            TransactionCategory.TRANSFER_OUT,
            grant
        )

        val small = sendAmount(Money.ofDollars(1_000), null)
        assertTrue(small is TransferResult.Sent)
        assertNull(repository.plans.single().transactions.single().memo)

        assertEquals(TransferResult.StepUpRequired, sendAmount(Money.ofDollars(1_200), null))
        assertEquals(1, repository.plans.size)

        val controller = StepUpControllerFactory(
            MpinVerifier(FakeMpinAttemptStore(), clock),
            stepUp,
            clock
        ).create(this)
        var grant: StepUpGrant? = null
        controller.request(StepUpAction.OutboundTransfer(Money.ofDollars(1_200))) { grant = it }
        testScheduler.runCurrent()
        "123456".forEach { controller.onDigit(it.digitToInt()) }
        testScheduler.runCurrent()
        assertNotNull(grant)
        // A grant for one amount does not approve another, and works once.
        assertEquals(TransferResult.StepUpRequired, sendAmount(Money.ofDollars(1_300), grant))
        assertTrue(sendAmount(Money.ofDollars(1_200), grant) is TransferResult.Sent)
        assertEquals(TransferResult.StepUpRequired, sendAmount(Money.ofDollars(1_200), grant))
    }

    @Test
    fun `invalid sends and moves write nothing`() = runTest {
        val send = SendTransferUseCase(repository, stepUp, clock)
        assertEquals(
            TransferResult.Invalid(AmountError.OVER_AVAILABLE),
            send(
                SAVINGS,
                MARK,
                SAVINGS.available,
                TransferSpeed.INSTANT,
                null,
                TransactionCategory.TRANSFER_OUT,
                null
            )
        )
        val move = MoveMoneyUseCase(repository, clock)
        assertEquals(
            TransferResult.Invalid(AmountError.OVER_OWED),
            move(CHECKING, CREDIT_LINE, Money.ofDollars(3_000))
        )
        assertTrue(repository.plans.isEmpty())
        assertTrue(move(CHECKING, SAVINGS, Money.ofDollars(50)) is TransferResult.Sent)
    }

    @Test
    fun `payees are checked before step-up and saved after it`() = runTest {
        val add = AddPayeeUseCase(repository, BANKS, stepUp)
        assertEquals(PayeeError.BANK_NOT_FOUND, add.check("Jordan Ellis", "500000034", "40021187"))
        assertEquals(
            PayeeResult.Invalid(PayeeError.ACCOUNT_NOT_VERIFIED),
            add("Jordan Ellis", "500000021", "40020000", null)
        )
        assertEquals(PayeeResult.StepUpRequired, add("Jordan Ellis", "500000021", "40021187", null))
        assertTrue(repository.payees.isEmpty())
        assertEquals("Northfield Bank", add.bankName("500000021"))
    }
}

class TransferViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePaymentsRepository()
    private val clock = ManualClock(NOW)
    private val stepUp = StepUpManager(clock)
    private val savedState = SavedStateHandle()

    private fun viewModel() = TransferViewModel(
        savedStateHandle = savedState,
        observePaymentsData = ObservePaymentsDataUseCase(repository),
        sendTransfer = SendTransferUseCase(repository, stepUp, clock),
        moveMoney = MoveMoneyUseCase(repository, clock),
        addPayee = AddPayeeUseCase(repository, BANKS, stepUp),
        sessionManager = SessionManager(),
        stepUpFactory = StepUpControllerFactory(
            MpinVerifier(FakeMpinAttemptStore(), clock),
            stepUp,
            clock
        )
    )

    private fun load() {
        repository.data.value = DataResult.Success(
            PaymentsData(
                listOf(SAVINGS, CREDIT_LINE, CHECKING),
                listOf(OAKWOOD, MARK),
                isFromCache = false
            )
        )
    }

    private suspend fun ReceiveTurbine<TransferUiState>.content(): TransferUiState.Content =
        awaitItem() as TransferUiState.Content

    private fun TransferViewModel.type(keys: String) = keys.forEach { onKey(it) }

    @Test
    fun `payees list recent first and sends leave from checking first`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(TransferUiState.Loading, awaitItem())
            load()
            val content = content()
            assertEquals(
                listOf("mark-robinson", "oakwood-properties"),
                content.beneficiaries.map {
                    it.id
                }
            )
            assertEquals(listOf("checking", "savings"), content.sources.map { it.id })
            assertEquals("checking", content.source?.id)
            assertFalse(content.canContinueFromAmount)
        }
    }

    @Test
    fun `amounts over the available balance are blocked inline`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            load()
            awaitItem()
            viewModel.onMoveBetweenAccounts()
            viewModel.onSelectSource("savings")
            viewModel.type("12032.01")
            testScheduler.runCurrent()
            val content = expectMostRecentItem() as TransferUiState.Content
            assertEquals(TransferMode.MOVE, content.mode)
            assertEquals(AmountError.OVER_AVAILABLE, content.amountError)
            assertFalse(content.canContinueFromAmount)
            viewModel.onDeleteKey()
            viewModel.onDeleteKey()
            testScheduler.runCurrent()
            val fixed = expectMostRecentItem() as TransferUiState.Content
            assertNull(fixed.amountError)
            assertTrue(fixed.canContinueFromAmount)
        }
    }

    @Test
    fun `a send over $1,000 waits for the MPIN, then shows the receipt`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            load()
            awaitItem()
            viewModel.onSendTo("mark-robinson")
            viewModel.type("1500")
            viewModel.onSpeedSelect(TransferSpeed.INSTANT)
            testScheduler.runCurrent()
            assertEquals(
                Money.ofDollars(15),
                (expectMostRecentItem() as TransferUiState.Content).fee
            )

            viewModel.onConfirm()
            testScheduler.runCurrent()
            assertTrue((expectMostRecentItem() as TransferUiState.Content).stepUp.isVisible)
            assertTrue(repository.plans.isEmpty())

            "123456".forEach { viewModel.onStepUpDigit(it.digitToInt()) }
            testScheduler.runCurrent()
            val receipt = (expectMostRecentItem() as TransferUiState.Content).receipt
            assertNotNull(receipt)
            val plan = repository.plans.single()
            assertEquals(plan.reference, receipt?.reference)
            assertEquals(plan.mainTransactionId, receipt?.transactionId)
            assertEquals("Mark Robinson", receipt?.recipient)

            // A second confirm on the receipt does nothing.
            viewModel.onConfirm()
            testScheduler.runCurrent()
            assertEquals(1, repository.plans.size)
        }
    }

    @Test
    fun `a double tap on Confirm moves the money once`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            load()
            awaitItem()
            viewModel.onMoveBetweenAccounts()
            viewModel.type("50")
            testScheduler.runCurrent()
            expectMostRecentItem()
            viewModel.onConfirm()
            viewModel.onConfirm()
            testScheduler.runCurrent()
            assertNotNull((expectMostRecentItem() as TransferUiState.Content).receipt)
            assertEquals(1, repository.plans.size)
        }
    }

    @Test
    fun `a payee chosen before the flow opens is used only if it is saved`() = runTest {
        savedState["beneficiaryId"] = "mark-robinson"
        viewModel().uiState.test {
            awaitItem()
            load()
            assertEquals("mark-robinson", (awaitItem() as TransferUiState.Content).beneficiary?.id)
        }
        val unknown = TransferViewModel(
            savedStateHandle = SavedStateHandle(mapOf("beneficiaryId" to "nobody")),
            observePaymentsData = ObservePaymentsDataUseCase(repository),
            sendTransfer = SendTransferUseCase(repository, stepUp, clock),
            moveMoney = MoveMoneyUseCase(repository, clock),
            addPayee = AddPayeeUseCase(repository, BANKS, stepUp),
            sessionManager = SessionManager(),
            stepUpFactory = StepUpControllerFactory(
                MpinVerifier(FakeMpinAttemptStore(), clock),
                stepUp,
                clock
            )
        )
        unknown.uiState.test {
            awaitItem()
            "50".forEach { unknown.onKey(it) }
            testScheduler.runCurrent()
            val content = expectMostRecentItem() as TransferUiState.Content
            assertNull(content.beneficiary)
            assertFalse(content.canContinueFromAmount)
        }
    }

    @Test
    fun `the draft survives process death`() = runTest {
        viewModel().apply {
            onSendTo("mark-robinson")
            type("42.5")
            onMemoChange("Lunch")
        }
        val restored = viewModel()
        restored.uiState.test {
            awaitItem()
            load()
            val content = content()
            assertEquals("mark-robinson", content.beneficiary?.id)
            assertEquals(Money(4_250), content.amount)
            assertEquals("Lunch", content.memo)
        }
    }
}
