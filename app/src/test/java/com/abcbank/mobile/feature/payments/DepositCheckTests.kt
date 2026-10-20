package com.abcbank.mobile.feature.payments

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.feature.payments.domain.ClearDueDepositsUseCase
import com.abcbank.mobile.feature.payments.domain.ClearedDeposit
import com.abcbank.mobile.feature.payments.domain.DepositData
import com.abcbank.mobile.feature.payments.domain.DepositError
import com.abcbank.mobile.feature.payments.domain.DepositRepository
import com.abcbank.mobile.feature.payments.domain.DepositResult
import com.abcbank.mobile.feature.payments.domain.DepositRules
import com.abcbank.mobile.feature.payments.domain.NotificationType
import com.abcbank.mobile.feature.payments.domain.ObserveDepositDataUseCase
import com.abcbank.mobile.feature.payments.domain.PendingDeposit
import com.abcbank.mobile.feature.payments.domain.PendingDeposits
import com.abcbank.mobile.feature.payments.domain.SubmitDepositUseCase
import com.abcbank.mobile.feature.payments.ui.DepositCheckUiState
import com.abcbank.mobile.feature.payments.ui.DepositCheckViewModel
import com.abcbank.mobile.testing.MainDispatcherRule
import com.abcbank.mobile.testing.ManualClock
import java.time.Duration
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
private val LIMIT = Money.ofDollars(10_000)
private val CHECKING =
    DepositAccount(
        "checking",
        AccountType.CHECKING,
        "Everyday Checking",
        "3144",
        Money(40_000),
        Money(40_000),
        null
    )
private val SAVINGS =
    DepositAccount(
        "savings",
        AccountType.SAVINGS,
        "Travel Savings",
        "7720",
        Money(100_000),
        Money(100_000),
        null
    )

private fun pending(id: String, cents: Long, clearsAt: Instant, accountId: String = "checking") =
    Transaction(
        id = id,
        accountId = accountId,
        direction = TransactionDirection.CREDIT,
        amount = Money(cents),
        merchant = DepositRules.MERCHANT,
        category = TransactionCategory.DEPOSIT,
        reference = id,
        runningBalance = Money(40_000),
        status = TransactionStatus.PENDING,
        disputed = false,
        createdAt = clearsAt.minus(DepositRules.CLEARING_TIME),
        clearsAt = clearsAt
    )

private class FakeDepositRepository : DepositRepository {
    val data = MutableStateFlow<DataResult<DepositData>?>(null)
    var pending = PendingDeposits(emptyList(), listOf(CHECKING, SAVINGS))
    val submitted = mutableListOf<PendingDeposit>()
    val cleared = mutableListOf<ClearedDeposit>()

    override fun observeDepositData(): Flow<DataResult<DepositData>> = data.filterNotNull()

    override suspend fun submitDeposit(deposit: PendingDeposit): DataResult<Unit> {
        submitted += deposit
        return DataResult.Success(Unit)
    }

    override suspend fun pendingDeposits(): DataResult<PendingDeposits> =
        DataResult.Success(pending)

    override suspend fun clearDeposit(cleared: ClearedDeposit): DataResult<Unit> {
        this.cleared += cleared
        // Posted deposits are no longer pending on the next read.
        pending =
            pending.copy(deposits = pending.deposits.filterNot { it.id == cleared.transactionId })
        return DataResult.Success(Unit)
    }
}

class DepositRulesTest {
    @Test
    fun `deposits are $1_00 up to the profile's limit, into checking or savings`() {
        assertEquals(DepositError.BELOW_MINIMUM, DepositRules.validate(Money(99), LIMIT))
        assertNull(DepositRules.validate(Money(100), LIMIT))
        assertNull(DepositRules.validate(LIMIT, LIMIT))
        assertEquals(DepositError.OVER_LIMIT, DepositRules.validate(LIMIT + Money(1), LIMIT))
        assertNull(DepositRules.validate(Money.ofDollars(100_000), Money.ofDollars(100_000)))
        assertTrue(DepositRules.canDepositInto(SAVINGS))
    }

    @Test
    fun `a deposit is pending for 30 seconds, then credits the account with an alert`() {
        val deposit = DepositRules.pending(
            CHECKING,
            Money(25_000),
            "ref",
            NOW,
            front = true,
            back = false
        )
        assertEquals(NOW.plus(Duration.ofSeconds(30)), deposit.clearsAt)
        assertEquals(CHECKING.ledger, deposit.runningBalance)
        val transaction = pending("deposit-ref", 25_000, deposit.clearsAt)
        assertFalse(DepositRules.isDue(transaction, NOW.plusSeconds(29)))
        assertTrue(DepositRules.isDue(transaction, NOW.plusSeconds(30)))
        assertFalse(
            DepositRules.isDue(
                transaction.copy(status = TransactionStatus.POSTED),
                NOW.plusSeconds(60)
            )
        )
        val cleared = DepositRules.clear(transaction, CHECKING, NOW.plusSeconds(30))
        assertEquals(Money(65_000), cleared.account.available)
        assertEquals(Money(65_000), cleared.runningBalance)
        assertEquals(NotificationType.DEPOSIT_CLEARED, cleared.notification.type)
        assertEquals(listOf("25000", "Everyday Checking"), cleared.notification.args)
    }
}

class DepositUseCasesTest {
    private val repository = FakeDepositRepository()
    private val clock = ManualClock(NOW)

    @Test
    fun `a valid deposit is written and schedules clearing`() = runTest {
        var triggered = 0
        val submit = SubmitDepositUseCase(repository, { triggered++ }, clock)
        assertEquals(
            DepositResult.Invalid(DepositError.OVER_LIMIT),
            submit(
                CHECKING,
                LIMIT + Money(1),
                LIMIT,
                false,
                false
            )
        )
        assertEquals(0, triggered)
        assertTrue(submit(CHECKING, Money(5_000), LIMIT, false, false) is DepositResult.Submitted)
        assertEquals(1, triggered)
        assertEquals(1, repository.submitted.size)
    }

    @Test
    fun `due deposits clear once, building on each other, and the next due time is returned`() =
        runTest {
            repository.pending = PendingDeposits(
                listOf(
                    pending("a", 10_000, NOW.minusSeconds(5)),
                    pending("b", 20_000, NOW.minusSeconds(1)),
                    pending("later", 5_000, NOW.plusSeconds(20)),
                    pending("savings", 1_000, NOW.plusSeconds(10), accountId = "savings")
                ),
                listOf(CHECKING, SAVINGS)
            )
            val clear = ClearDueDepositsUseCase(repository, clock)
            assertEquals(NOW.plusSeconds(10), clear())
            assertEquals(listOf("a", "b"), repository.cleared.map { it.transactionId })
            assertEquals(Money(50_000), repository.cleared[0].account.ledger)
            assertEquals(Money(70_000), repository.cleared[1].account.ledger)
            // Run again straight away: nothing is credited twice.
            clear()
            assertEquals(2, repository.cleared.size)
            clock.now = NOW.plusSeconds(25)
            assertNull(clear())
            assertEquals(4, repository.cleared.size)
        }
}

class DepositCheckViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDepositRepository()

    private fun viewModel() = DepositCheckViewModel(
        savedStateHandle = SavedStateHandle(),
        observeDepositData = ObserveDepositDataUseCase(repository),
        submitDeposit = SubmitDepositUseCase(repository, {}, ManualClock(NOW)),
        sessionManager = SessionManager()
    )

    @Test
    fun `over the limit is blocked inline and a double tap deposits once`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(DepositCheckUiState.Loading, awaitItem())
            repository.data.value =
                DataResult.Success(DepositData(LIMIT, listOf(SAVINGS, CHECKING), false))
            val content = awaitItem() as DepositCheckUiState.Content
            assertEquals("checking", content.account?.id)
            "10000.01".forEach(viewModel::onKey)
            testScheduler.runCurrent()
            val over = expectMostRecentItem() as DepositCheckUiState.Content
            assertEquals(DepositError.OVER_LIMIT, over.amountError)
            assertFalse(over.canConfirm)
            repeat(4) { viewModel.onDeleteKey() }
            viewModel.onToggleFront()
            viewModel.onConfirm()
            viewModel.onConfirm()
            testScheduler.runCurrent()
            val done = expectMostRecentItem() as DepositCheckUiState.Content
            assertNotNull(done.submitted)
            assertEquals(1, repository.submitted.size)
            assertEquals(Money.ofDollars(1_000), repository.submitted.single().amount)
            assertTrue(repository.submitted.single().frontImage)
        }
    }
}
