package com.abcbank.mobile.feature.payments

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.security.MpinVerifier
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.core.security.StepUpControllerFactory
import com.abcbank.mobile.core.security.StepUpManager
import com.abcbank.mobile.feature.payments.data.catalogBillers
import com.abcbank.mobile.feature.payments.domain.AddBillerUseCase
import com.abcbank.mobile.feature.payments.domain.Bill
import com.abcbank.mobile.feature.payments.domain.BillAccountError
import com.abcbank.mobile.feature.payments.domain.BillPaymentPlan
import com.abcbank.mobile.feature.payments.domain.BillResult
import com.abcbank.mobile.feature.payments.domain.BillRules
import com.abcbank.mobile.feature.payments.domain.BillStatus
import com.abcbank.mobile.feature.payments.domain.Biller
import com.abcbank.mobile.feature.payments.domain.BillerDirectory
import com.abcbank.mobile.feature.payments.domain.BillsData
import com.abcbank.mobile.feature.payments.domain.BillsRepository
import com.abcbank.mobile.feature.payments.domain.FetchBillUseCase
import com.abcbank.mobile.feature.payments.domain.FetchResult
import com.abcbank.mobile.feature.payments.domain.FetchedBill
import com.abcbank.mobile.feature.payments.domain.NotificationType
import com.abcbank.mobile.feature.payments.domain.ObserveBillsUseCase
import com.abcbank.mobile.feature.payments.domain.PayBillUseCase
import com.abcbank.mobile.feature.payments.domain.ScheduleBillUseCase
import com.abcbank.mobile.feature.payments.domain.SetAutoPayUseCase
import com.abcbank.mobile.feature.payments.ui.BillPayUiState
import com.abcbank.mobile.feature.payments.ui.BillPayViewModel
import com.abcbank.mobile.testing.FakeMpinAttemptStore
import com.abcbank.mobile.testing.MainDispatcherRule
import com.abcbank.mobile.testing.ManualClock
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val UTC = ZoneOffset.UTC
private val TODAY = LocalDate.of(2026, 10, 20)
private val NOW: Instant = TODAY.atTime(15, 0).toInstant(UTC)
private val VOLT = Biller("volt-mobile", "Volt Mobile", "Mobile")
private val COUNTY = Biller("fairview-county", "Fairview County Treasurer", "Property tax")
private val CHECKING =
    DepositAccount(
        "checking",
        AccountType.CHECKING,
        "Everyday Checking",
        "3144",
        Money(60_000),
        Money(60_000),
        null
    )

private fun bill(
    biller: Biller,
    cents: Long,
    dueIn: Long = 4,
    status: BillStatus = BillStatus.DUE
) = Bill(
    id = biller.id,
    billerId = biller.id,
    amountDue = Money(cents),
    dueDate = TODAY.plusDays(dueIn).atStartOfDay(UTC).toInstant(),
    autoPay = false,
    scheduledFor = null,
    status = status,
    paidAt = null,
    accountLast4 = null
)

class BillRulesTest {
    @Test
    fun `the bundled catalog has the eight documented billers`() {
        val catalog = Json.parseToJsonElement(
            File("src/main/assets/seed/catalog.json").readText()
        ).jsonObject
        val billers = catalogBillers(catalog)
        assertEquals(8, billers.size)
        assertEquals(COUNTY, billers.last())
    }

    @Test
    fun `fetching is stable per account number and inside the documented ranges`() {
        val first = BillRules.fetch(VOLT, "40021187", TODAY, UTC)
        assertEquals(first, BillRules.fetch(VOLT, "40021187", TODAY, UTC))
        assertEquals("1187", first.accountLast4)
        for (number in listOf("123456", "99887766", "12345678901234567", "555555")) {
            val fetched = BillRules.fetch(VOLT, number, TODAY, UTC)
            assertTrue(
                fetched.amountDue >= Money.ofDollars(20) &&
                    fetched.amountDue <= Money.ofDollars(250)
            )
            assertTrue(fetched.dueDay in TODAY.plusDays(7)..TODAY.plusDays(20))
            assertEquals(fetched.dueDay.atStartOfDay(UTC).toInstant(), fetched.dueDate)
        }
        assertEquals(BillAccountError.FORMAT, BillRules.validateAccountNumber("12345"))
        assertNull(BillRules.validateAccountNumber("123456"))
    }

    @Test
    fun `utility bills are Utilities and property tax and insurance are Housing`() {
        assertEquals(TransactionCategory.UTILITIES, BillRules.transactionCategory(VOLT))
        assertEquals(TransactionCategory.HOUSING, BillRules.transactionCategory(COUNTY))
        assertEquals(
            TransactionCategory.HOUSING,
            BillRules.transactionCategory(Biller("h", "Harbor Mutual Insurance", "Insurance"))
        )
    }

    @Test
    fun `a due bill is due soon within 7 days or once overdue, never when paid or scheduled`() {
        fun soon(dueIn: Long, status: BillStatus = BillStatus.DUE) =
            BillRules.isDueSoon(bill(VOLT, 9_500, dueIn, status), TODAY.plusDays(dueIn), TODAY)

        assertEquals(4L, BillRules.daysUntil(TODAY.plusDays(4), TODAY))
        assertEquals(-2L, BillRules.daysUntil(TODAY.minusDays(2), TODAY))
        assertTrue(soon(0))
        assertTrue(soon(7))
        assertFalse(soon(8))
        assertTrue(soon(-2))
        assertFalse(soon(3, BillStatus.PAID))
        assertFalse(soon(3, BillStatus.SCHEDULED))
    }

    @Test
    fun `the total due adds up due and scheduled bills, not paid ones`() {
        val total = BillRules.totalDue(
            listOf(
                bill(VOLT, 9_500),
                bill(COUNTY, 1_875_000, status = BillStatus.SCHEDULED),
                bill(Biller("x", "Paid One", "Mobile"), 5_000, status = BillStatus.PAID)
            )
        )
        assertEquals(Money(1_884_500), total)
    }

    @Test
    fun `payments can be scheduled from tomorrow through the due date`() {
        val due = TODAY.plusDays(4)
        assertFalse(BillRules.canScheduleFor(TODAY, TODAY, due))
        assertTrue(BillRules.canScheduleFor(TODAY.plusDays(1), TODAY, due))
        assertTrue(BillRules.canScheduleFor(due, TODAY, due))
        assertFalse(BillRules.canScheduleFor(due.plusDays(1), TODAY, due))
    }

    @Test
    fun `paying debits the account, posts a bill paid alert and a low balance alert on crossing`() {
        val plan = BillRules.plan(CHECKING, bill(VOLT, 15_000), VOLT, "ref", NOW)
        assertEquals(Money(45_000), plan.transfer.accounts.single().available)
        val transaction = plan.transfer.transactions.single()
        assertEquals("Volt Mobile", transaction.merchant)
        assertEquals(TransactionCategory.UTILITIES, transaction.category)
        assertEquals(Money(45_000), transaction.runningBalance)
        assertEquals(
            listOf(NotificationType.BILL_PAID, NotificationType.LOW_BALANCE),
            plan.transfer.notifications.map { it.type }
        )
        assertEquals("volt-mobile", plan.billId)
    }
}

private class FakeBillsRepository : BillsRepository {
    val data = MutableStateFlow<DataResult<BillsData>?>(null)
    val calls = mutableListOf<String>()
    val payments = mutableListOf<BillPaymentPlan>()

    override fun observeBills(): Flow<DataResult<BillsData>> = data.filterNotNull()

    override suspend fun addBill(bill: FetchedBill): DataResult<Unit> {
        calls += "add ${bill.biller.id}"
        return DataResult.Success(Unit)
    }

    override suspend fun payBill(plan: BillPaymentPlan): DataResult<Unit> {
        payments += plan
        return DataResult.Success(Unit)
    }

    override suspend fun schedule(billId: String, date: Instant?): DataResult<Unit> {
        calls += "schedule $billId $date"
        return DataResult.Success(Unit)
    }

    override suspend fun setAutoPay(billId: String, autoPay: Boolean): DataResult<Unit> {
        calls += "autopay $billId $autoPay"
        return DataResult.Success(Unit)
    }
}

private val DIRECTORY = object : BillerDirectory {
    override suspend fun billers(): List<Biller> = listOf(VOLT, COUNTY)
}

class BillUseCasesTest {
    private val repository = FakeBillsRepository()
    private val clock = ManualClock(NOW)
    private val stepUp = StepUpManager(clock)

    @Test
    fun `bills over $1,000 and new billers need step-up, smaller bills do not`() = runTest {
        val pay = PayBillUseCase(repository, stepUp, clock)
        val rich = CHECKING.copy(
            available = Money.ofDollars(10_000),
            ledger = Money.ofDollars(10_000)
        )
        val observe = ObserveBillsUseCase(repository, DIRECTORY, clock) { UTC }
        repository.data.value =
            DataResult.Success(
                BillsData(listOf(bill(VOLT, 9_500), bill(COUNTY, 342_000)), listOf(rich), false)
            )
        observe().test {
            val (volt, county) = (awaitItem() as DataResult.Success).value.bills
            assertTrue(pay(volt, rich, null) is BillResult.Paid)
            assertEquals(BillResult.StepUpRequired, pay(county, rich, null))
            assertEquals(1, repository.payments.size)
        }
        val fetched = BillRules.fetch(VOLT, "40021187", TODAY, UTC)
        assertEquals(BillResult.StepUpRequired, AddBillerUseCase(repository, stepUp)(fetched, null))
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `paid bills, short balances and out of range dates are refused`() = runTest {
        val observe = ObserveBillsUseCase(repository, DIRECTORY, clock) { UTC }
        repository.data.value = DataResult.Success(
            BillsData(
                listOf(bill(VOLT, 9_500, status = BillStatus.PAID), bill(COUNTY, 342_000)),
                listOf(CHECKING),
                false
            )
        )
        observe().test {
            val bills = (awaitItem() as DataResult.Success).value.bills
            val county = bills.first { it.biller == COUNTY }
            val volt = bills.first { it.biller == VOLT }
            // Unpaid first, paid last.
            assertEquals(COUNTY, bills.first().biller)
            val pay = PayBillUseCase(repository, stepUp, clock)
            assertEquals(BillResult.Invalid, pay(volt, CHECKING, null))
            assertEquals(BillResult.Invalid, pay(county, CHECKING, null))
            val schedule = ScheduleBillUseCase(repository, clock) { UTC }
            assertEquals(BillResult.Invalid, schedule(county.bill, TODAY))
            assertEquals(BillResult.Done, schedule(county.bill, TODAY.plusDays(2)))
            assertEquals(BillResult.Done, schedule(county.bill, null))
            assertEquals(BillResult.Done, SetAutoPayUseCase(repository)(county.bill, true))
            assertEquals(
                BillResult.Done,
                SetAutoPayUseCase(repository)(county.bill.copy(autoPay = true), true)
            )
        }
        assertEquals(3, repository.calls.size)
    }

    @Test
    fun `fetch checks the account number first`() {
        val fetch = FetchBillUseCase(clock) { UTC }
        assertEquals(FetchResult.Invalid(BillAccountError.FORMAT), fetch(VOLT, "123"))
        assertTrue(fetch(VOLT, "40021187") is FetchResult.Fetched)
    }
}

class BillPayViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBillsRepository()
    private val clock = ManualClock(NOW)
    private val stepUp = StepUpManager(clock)

    private fun viewModel() = BillPayViewModel(
        savedStateHandle = SavedStateHandle(),
        observeBills = ObserveBillsUseCase(repository, DIRECTORY, clock) { UTC },
        fetchBill = FetchBillUseCase(clock) { UTC },
        addBiller = AddBillerUseCase(repository, stepUp),
        payBill = PayBillUseCase(repository, stepUp, clock),
        scheduleBill = ScheduleBillUseCase(repository, clock) { UTC },
        setAutoPay = SetAutoPayUseCase(repository),
        sessionManager = SessionManager(),
        stepUpFactory = StepUpControllerFactory(
            MpinVerifier(FakeMpinAttemptStore(), clock),
            stepUp,
            clock
        )
    )

    @Test
    fun `a double tap on Pay pays once and shows the confirmation`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            repository.data.value =
                DataResult.Success(BillsData(listOf(bill(VOLT, 9_500)), listOf(CHECKING), false))
            awaitItem()
            viewModel.onSelectBill("volt-mobile")
            testScheduler.runCurrent()
            assertTrue((expectMostRecentItem() as BillPayUiState.Content).canPay)
            viewModel.onPay()
            viewModel.onPay()
            testScheduler.runCurrent()
            assertNotNull((expectMostRecentItem() as BillPayUiState.Content).paid)
            assertEquals(1, repository.payments.size)
        }
    }

    @Test
    fun `adding a biller fetches the bill, then waits for the MPIN`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            repository.data.value =
                DataResult.Success(BillsData(emptyList(), listOf(CHECKING), false))
            awaitItem()
            viewModel.onStartAddBiller()
            viewModel.onSearch("tax")
            testScheduler.runCurrent()
            assertEquals(
                listOf(COUNTY),
                (expectMostRecentItem() as BillPayUiState.Content).directory
            )
            viewModel.onChooseBiller(COUNTY)
            viewModel.onAccountNumberChange("12-34")
            viewModel.onFetchBill()
            testScheduler.runCurrent()
            assertEquals(
                BillAccountError.FORMAT,
                (expectMostRecentItem() as BillPayUiState.Content).form.error
            )
            viewModel.onAccountNumberChange("40021187")
            viewModel.onFetchBill()
            viewModel.onAddBiller()
            testScheduler.runCurrent()
            val waiting = expectMostRecentItem() as BillPayUiState.Content
            assertNotNull(waiting.form.fetched)
            assertTrue(waiting.stepUp.isVisible)
            "123456".forEach { viewModel.onStepUpDigit(it.digitToInt()) }
            testScheduler.runCurrent()
            assertEquals(listOf("add fairview-county"), repository.calls)
            assertEquals(1, (expectMostRecentItem() as BillPayUiState.Content).form.added)
        }
    }
}
