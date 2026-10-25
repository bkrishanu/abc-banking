package com.abcbank.mobile.feature.accounts

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.feature.accounts.domain.CreateReceiptUseCase
import com.abcbank.mobile.feature.accounts.domain.DateRange
import com.abcbank.mobile.feature.accounts.domain.DisputeMessages
import com.abcbank.mobile.feature.accounts.domain.DisputeResult
import com.abcbank.mobile.feature.accounts.domain.DisputeRules
import com.abcbank.mobile.feature.accounts.domain.DisputeTransactionUseCase
import com.abcbank.mobile.feature.accounts.domain.LedgerGroup
import com.abcbank.mobile.feature.accounts.domain.LedgerRules
import com.abcbank.mobile.feature.accounts.domain.ObserveLedgerUseCase
import com.abcbank.mobile.feature.accounts.domain.ObserveTransactionUseCase
import com.abcbank.mobile.feature.accounts.domain.ReceiptFile
import com.abcbank.mobile.feature.accounts.domain.ReceiptGenerator
import com.abcbank.mobile.feature.accounts.domain.TransactionDetail
import com.abcbank.mobile.feature.accounts.domain.TransactionFilter
import com.abcbank.mobile.feature.accounts.domain.TransactionsData
import com.abcbank.mobile.feature.accounts.domain.TransactionsRepository
import com.abcbank.mobile.feature.accounts.domain.TypeFilter
import com.abcbank.mobile.feature.accounts.ui.ReceiptStatus
import com.abcbank.mobile.feature.accounts.ui.TransactionDetailUiState
import com.abcbank.mobile.feature.accounts.ui.TransactionDetailViewModel
import com.abcbank.mobile.feature.accounts.ui.TransactionsUiState
import com.abcbank.mobile.feature.accounts.ui.TransactionsViewModel
import com.abcbank.mobile.testing.MainDispatcherRule
import com.abcbank.mobile.testing.ManualClock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val NOW: Instant = Instant.parse("2026-10-19T13:00:00Z")
private val TODAY: LocalDate = LocalDate.of(2026, 10, 19)
private val UTC = ZoneOffset.UTC

private fun daysAgo(days: Long, hour: Int = 12): Instant =
    TODAY.minusDays(days).atTime(hour, 0).toInstant(UTC)

private fun tx(
    id: String,
    signedCents: Long,
    at: Instant,
    merchant: String = id,
    category: TransactionCategory = TransactionCategory.SHOPPING,
    accountId: String = "checking"
) = Transaction(
    id = id,
    accountId = accountId,
    direction = if (signedCents < 0) TransactionDirection.DEBIT else TransactionDirection.CREDIT,
    amount = Money(kotlin.math.abs(signedCents)),
    merchant = merchant,
    category = category,
    reference = id,
    runningBalance = Money.ZERO,
    status = TransactionStatus.POSTED,
    disputed = false,
    createdAt = at,
    clearsAt = null
)

private val CHECKING =
    DepositAccount(
        "checking",
        AccountType.CHECKING,
        "Everyday Checking",
        "3144",
        Money(1),
        Money(1),
        null
    )
private val SAVINGS =
    DepositAccount(
        "savings",
        AccountType.SAVINGS,
        "Travel Savings",
        "7720",
        Money(1),
        Money(1),
        null
    )

private val SAMPLE = listOf(
    tx("boutique", -12_500, daysAgo(0), "Aster & Vine Boutique"),
    tx("payroll", 711_538, daysAgo(3), "Lumen Studio Payroll", TransactionCategory.INCOME),
    tx("coffee", -645, daysAgo(1), "Bluebird Coffee", TransactionCategory.DINING),
    tx(
        "interest",
        3_066,
        daysAgo(12),
        "Interest",
        TransactionCategory.INTEREST,
        accountId = "savings"
    ),
    tx("old", -9_000, daysAgo(45), "Fresh Market Co.", TransactionCategory.GROCERIES)
)

private class FakeTransactionsRepository : TransactionsRepository {
    val result = MutableStateFlow<DataResult<TransactionsData>?>(null)
    val detail = MutableStateFlow<DataResult<TransactionDetail?>?>(null)
    val requestedSince = mutableListOf<Instant?>()

    override fun observeTransactions(since: Instant?): Flow<DataResult<TransactionsData>> {
        requestedSince += since
        return result.filterNotNull()
    }

    override fun observeTransaction(transactionId: String): Flow<DataResult<TransactionDetail?>> =
        detail.filterNotNull()

    val disputes = mutableListOf<Pair<String, DisputeMessages>>()
    var disputeResult: DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun disputeTransaction(
        transaction: Transaction,
        messages: DisputeMessages
    ): DataResult<Unit> {
        disputes += transaction.id to messages
        return disputeResult
    }
}

private class FakeReceiptGenerator : ReceiptGenerator {
    var result: DataResult<ReceiptFile> = DataResult.Success(ReceiptFile("content://receipt"))

    override suspend fun createReceipt(transaction: Transaction, account: Account?) = result
}

private val MESSAGES = DisputeMessages("topic", "customer", "bank")

class LedgerRulesTest {
    @Test
    fun `date ranges start at midnight, today included`() {
        assertEquals(
            Instant.parse("2026-09-20T00:00:00Z"),
            LedgerRules.since(DateRange.LAST_30_DAYS, TODAY, UTC)
        )
        assertEquals(
            Instant.parse("2026-07-22T00:00:00Z"),
            LedgerRules.since(DateRange.LAST_90_DAYS, TODAY, UTC)
        )
        assertNull(LedgerRules.since(DateRange.ALL, TODAY, UTC))
    }

    @Test
    fun `type, category, search and account filters all combine`() {
        fun ids(filter: TransactionFilter) = LedgerRules.apply(SAMPLE, filter).map { it.id }

        assertEquals(
            listOf("boutique", "coffee", "old"),
            ids(TransactionFilter(type = TypeFilter.SPENDINGS))
        )
        assertEquals(
            listOf("payroll", "interest"),
            ids(TransactionFilter(type = TypeFilter.EARNINGS))
        )
        assertEquals(
            listOf("coffee"),
            ids(
                TransactionFilter(
                    type = TypeFilter.SPENDINGS,
                    categories = setOf(TransactionCategory.DINING)
                )
            )
        )
        assertEquals(listOf("boutique"), ids(TransactionFilter(query = "  VINE ")))
        assertEquals(listOf("interest"), ids(TransactionFilter(accountId = "savings")))
        assertEquals(
            emptyList<String>(),
            ids(TransactionFilter(accountId = "savings", type = TypeFilter.SPENDINGS))
        )
    }

    @Test
    fun `type counts follow the other filters but ignore the selected type`() {
        val all = LedgerRules.typeCounts(SAMPLE, TransactionFilter(type = TypeFilter.EARNINGS))
        assertEquals(5, all[TypeFilter.ALL])
        assertEquals(3, all[TypeFilter.SPENDINGS])
        assertEquals(2, all[TypeFilter.EARNINGS])

        val searched = LedgerRules.typeCounts(SAMPLE, TransactionFilter(query = "pay"))
        assertEquals(1, searched[TypeFilter.ALL])
        assertEquals(0, searched[TypeFilter.SPENDINGS])
        assertEquals(1, searched[TypeFilter.EARNINGS])
    }

    @Test
    fun `groups are today, yesterday, last 30 days and earlier, newest first`() {
        val sections = LedgerRules.group(SAMPLE, TODAY, UTC)

        assertEquals(
            listOf(
                LedgerGroup.TODAY,
                LedgerGroup.YESTERDAY,
                LedgerGroup.LAST_30_DAYS,
                LedgerGroup.EARLIER
            ),
            sections.map { it.group }
        )
        assertEquals(listOf("payroll", "interest"), sections[2].transactions.map { it.id })
        assertEquals(listOf("old"), sections[3].transactions.map { it.id })
    }

    @Test
    fun `day 29 is still last 30 days and day 30 is earlier`() {
        val sections = LedgerRules.group(
            listOf(tx("d29", -1, daysAgo(29)), tx("d30", -1, daysAgo(30))),
            TODAY,
            UTC
        )

        assertEquals(
            listOf(LedgerGroup.LAST_30_DAYS, LedgerGroup.EARLIER),
            sections.map {
                it.group
            }
        )
    }

    @Test
    fun `active filter count ignores search and the defaults`() {
        assertEquals(0, TransactionFilter(query = "x").activeCount)
        assertEquals(
            3,
            TransactionFilter(
                range = DateRange.ALL,
                type = TypeFilter.EARNINGS,
                categories = setOf(TransactionCategory.INCOME)
            ).activeCount
        )
        assertEquals(
            TransactionFilter(accountId = "a"),
            TransactionFilter(accountId = "a", query = "x").cleared()
        )
    }
}

class TransactionsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTransactionsRepository()
    private val session = SessionManager()

    private fun viewModel(accountId: String? = null) = TransactionsViewModel(
        savedStateHandle = SavedStateHandle(
            listOfNotNull(
                accountId?.let {
                    "accountId" to it
                }
            ).toMap()
        ),
        observeLedger = ObserveLedgerUseCase(repository, ManualClock(NOW)) { UTC },
        sessionManager = session,
        zone = { UTC }
    )

    private fun data(transactions: List<Transaction> = SAMPLE) = DataResult.Success(
        TransactionsData(transactions, listOf(CHECKING, SAVINGS), isFromCache = false)
    )

    @Test
    fun `shows grouped content, filters on the device, and reads again only for a new range`() =
        runTest {
            val viewModel = viewModel()
            viewModel.uiState.test {
                assertEquals(TransactionsUiState.Loading, awaitItem())
                repository.result.value = data()
                val content = awaitItem() as TransactionsUiState.Content
                assertEquals(4, content.sections.size)

                viewModel.onTypeSelect(TypeFilter.EARNINGS)
                val earnings = awaitItem() as TransactionsUiState.Content
                assertEquals(
                    listOf("payroll", "interest"),
                    earnings.sections.flatMap { s ->
                        s.rows.map { it.transaction.id }
                    }
                )
                assertEquals(1, repository.requestedSince.size)

                viewModel.onRangeSelect(DateRange.ALL)
                skipItems(1)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(
                listOf(Instant.parse("2026-09-20T00:00:00Z"), null),
                repository.requestedSince
            )
        }

    @Test
    fun `filters that hide everything offer Clear Filters, which restores the list`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            skipItems(1)
            repository.result.value = data()
            skipItems(1)

            viewModel.onQueryChange("no such merchant")
            assertTrue((awaitItem() as TransactionsUiState.Content).hiddenByFilters)

            viewModel.onClearFilters()
            val restored = awaitItem() as TransactionsUiState.Content
            assertEquals(false, restored.hiddenByFilters)
            assertEquals("", restored.header.filter.query)
        }
    }

    @Test
    fun `opened from an account, it shows only that account and its name`() = runTest {
        val viewModel = viewModel(accountId = "savings")
        viewModel.uiState.test {
            skipItems(1)
            repository.result.value = data()
            val content = awaitItem() as TransactionsUiState.Content
            assertEquals("Travel Savings", content.header.account?.nickname)
            assertEquals(
                listOf("interest"),
                content.sections.flatMap { s ->
                    s.rows.map { it.transaction.id }
                }
            )
        }
    }

    @Test
    fun `an invalid account argument falls back to every account`() = runTest {
        val viewModel = viewModel(accountId = "../users/other")
        viewModel.uiState.test {
            skipItems(1)
            repository.result.value = data()
            assertNull((awaitItem() as TransactionsUiState.Content).header.account)
        }
    }

    @Test
    fun `no transactions in the period is the empty state, and errors pass through`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            skipItems(1)
            repository.result.value = data(emptyList())
            assertTrue(awaitItem() is TransactionsUiState.Empty)

            repository.result.value = DataResult.Failure(DataError.OFFLINE)
            assertEquals(TransactionsUiState.Error(DataError.OFFLINE), awaitItem())
        }
    }
}

class DisputeTests {
    private val repository = FakeTransactionsRepository()
    private val dispute = DisputeTransactionUseCase(repository)

    @Test
    fun `only posted charges that are not transfers or already disputed can be disputed`() {
        val charge = tx("shop", -12_500, daysAgo(2))
        assertTrue(DisputeRules.canDispute(charge))
        assertFalse(
            DisputeRules.canDispute(
                tx("pay", 711_538, daysAgo(2), category = TransactionCategory.INCOME)
            )
        )
        assertFalse(DisputeRules.canDispute(charge.copy(status = TransactionStatus.PENDING)))
        assertFalse(
            DisputeRules.canDispute(charge.copy(category = TransactionCategory.TRANSFER_OUT))
        )
        assertFalse(DisputeRules.canDispute(charge.copy(category = TransactionCategory.TRANSFER)))
        assertFalse(DisputeRules.canDispute(charge.copy(disputed = true)))
    }

    @Test
    fun `a charge is disputed with the given texts, anything else is refused without a write`() =
        runTest {
            assertEquals(
                DisputeResult.Submitted,
                dispute(tx("shop", -12_500, daysAgo(2)), MESSAGES)
            )
            assertEquals(listOf("shop" to MESSAGES), repository.disputes)

            assertEquals(
                DisputeResult.NotDisputable,
                dispute(
                    tx("pay", 711_538, daysAgo(2), category = TransactionCategory.INCOME),
                    MESSAGES
                )
            )
            assertEquals(1, repository.disputes.size)

            repository.disputeResult = DataResult.Failure(DataError.PERMISSION_DENIED)
            assertEquals(
                DisputeResult.Failed(DataError.PERMISSION_DENIED),
                dispute(tx("other", -500, daysAgo(2)), MESSAGES)
            )
        }
}

class TransactionDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTransactionsRepository()
    private val receipts = FakeReceiptGenerator()

    private fun viewModel(id: String?) = TransactionDetailViewModel(
        savedStateHandle = SavedStateHandle(
            listOfNotNull(
                id?.let {
                    "transactionId" to it
                }
            ).toMap()
        ),
        observeTransaction = ObserveTransactionUseCase(repository),
        createReceipt = CreateReceiptUseCase(receipts),
        disputeTransaction = DisputeTransactionUseCase(repository),
        sessionManager = SessionManager().apply { toggleBalancesHidden() },
        zone = { UTC }
    )

    @Test
    fun `shows the transaction in local time with the session's masking`() = runTest {
        val viewModel = viewModel("payroll")
        viewModel.uiState.test {
            assertEquals(TransactionDetailUiState.Loading, awaitItem())
            repository.detail.value =
                DataResult.Success(TransactionDetail(SAMPLE[1], CHECKING, isFromCache = false))
            val content = awaitItem() as TransactionDetailUiState.Content
            assertEquals(TODAY.minusDays(3), content.date)
            assertEquals(CHECKING, content.account)
            assertTrue(content.balancesHidden)
        }
    }

    @Test
    fun `a missing or invalid id is the not-found state`() = runTest {
        viewModel("a/b").uiState.test {
            skipItems(1)
            assertEquals(TransactionDetailUiState.Empty, awaitItem())
        }
        val viewModel = viewModel("gone")
        viewModel.uiState.test {
            skipItems(1)
            repository.detail.value = DataResult.Success(null)
            assertEquals(TransactionDetailUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `download receipt goes creating, ready, then idle once shared`() = runTest {
        val viewModel = viewModel("boutique")
        viewModel.uiState.test {
            skipItems(1)
            repository.detail.value =
                DataResult.Success(TransactionDetail(SAMPLE[0], CHECKING, isFromCache = false))
            val content = awaitItem() as TransactionDetailUiState.Content
            assertTrue(content.canDispute)

            viewModel.onDownloadReceipt()
            assertEquals(
                ReceiptStatus.Creating,
                (awaitItem() as TransactionDetailUiState.Content).receipt
            )
            assertEquals(
                ReceiptStatus.Ready(ReceiptFile("content://receipt")),
                (awaitItem() as TransactionDetailUiState.Content).receipt
            )

            viewModel.onReceiptShared()
            assertEquals(
                ReceiptStatus.Idle,
                (awaitItem() as TransactionDetailUiState.Content).receipt
            )
        }
    }

    @Test
    fun `a receipt failure and a dispute failure are shown`() = runTest {
        receipts.result = DataResult.Failure(DataError.UNKNOWN)
        repository.disputeResult = DataResult.Failure(DataError.UNKNOWN)
        val viewModel = viewModel("boutique")
        viewModel.uiState.test {
            skipItems(1)
            repository.detail.value =
                DataResult.Success(TransactionDetail(SAMPLE[0], CHECKING, isFromCache = false))
            skipItems(1)

            viewModel.onDownloadReceipt()
            skipItems(1)
            assertEquals(
                ReceiptStatus.Failed,
                (awaitItem() as TransactionDetailUiState.Content).receipt
            )

            viewModel.onDispute(MESSAGES)
            assertTrue((awaitItem() as TransactionDetailUiState.Content).disputeFailed)
            assertEquals(listOf("boutique" to MESSAGES), repository.disputes)
        }
    }
}
