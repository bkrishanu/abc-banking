package com.abcbank.mobile.feature.insights

import app.cash.turbine.test
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.feature.insights.domain.GoalProgress
import com.abcbank.mobile.feature.insights.domain.InsightsData
import com.abcbank.mobile.feature.insights.domain.InsightsRepository
import com.abcbank.mobile.feature.insights.domain.InsightsRules
import com.abcbank.mobile.feature.insights.domain.ObserveInsightsUseCase
import com.abcbank.mobile.feature.insights.domain.SavingsGoal
import com.abcbank.mobile.feature.insights.domain.SpendingPeriod
import com.abcbank.mobile.feature.insights.ui.InsightsUiState
import com.abcbank.mobile.feature.insights.ui.InsightsViewModel
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

private val UTC = ZoneOffset.UTC
private val TODAY = LocalDate.of(2026, 10, 20)
private val NOW: Instant = TODAY.atTime(15, 0).toInstant(UTC)

private fun spend(
    id: String,
    category: TransactionCategory,
    cents: Long,
    daysAgo: Long,
    direction: TransactionDirection = TransactionDirection.DEBIT,
    status: TransactionStatus = TransactionStatus.POSTED,
    merchant: String = id
) = Transaction(
    id = id,
    accountId = "checking",
    direction = direction,
    amount = Money(cents),
    merchant = merchant,
    category = category,
    reference = id,
    runningBalance = Money.ZERO,
    status = status,
    disputed = false,
    createdAt = TODAY.minusDays(daysAgo).atTime(12, 0).toInstant(UTC),
    clearsAt = null
)

private val GOAL = SavingsGoal(
    id = "goal",
    name = "Summer in Lisbon",
    target = Money(400_000),
    saved = Money(365_000),
    accountId = "savings",
    startedAt = TODAY.minusDays(90).atTime(13, 0).toInstant(UTC),
    weeklyHistory = listOf(100_000L, 200_000, 365_000).map(::Money)
)

class InsightsRulesTest {
    @Test
    fun `transfers and money in are not spending, pending charges are`() {
        assertTrue(InsightsRules.isSpending(spend("a", TransactionCategory.DINING, 100, 0)))
        assertTrue(
            InsightsRules.isSpending(
                spend("b", TransactionCategory.HEALTH, 100, 0, status = TransactionStatus.PENDING)
            )
        )
        assertFalse(InsightsRules.isSpending(spend("c", TransactionCategory.TRANSFER, 100, 0)))
        assertFalse(InsightsRules.isSpending(spend("d", TransactionCategory.TRANSFER_OUT, 100, 0)))
        assertFalse(
            InsightsRules.isSpending(
                spend("e", TransactionCategory.INCOME, 100, 0, TransactionDirection.CREDIT)
            )
        )
    }

    @Test
    fun `periods cover the last 30 days, this month and all of last month`() {
        assertEquals(
            LocalDate.of(2026, 9, 21)..TODAY,
            InsightsRules.range(SpendingPeriod.LAST_30_DAYS, TODAY)
        )
        assertEquals(
            LocalDate.of(2026, 10, 1)..TODAY,
            InsightsRules.range(SpendingPeriod.THIS_MONTH, TODAY)
        )
        assertEquals(
            LocalDate.of(2026, 9, 1)..LocalDate.of(2026, 9, 30),
            InsightsRules.range(SpendingPeriod.LAST_MONTH, TODAY)
        )
        assertEquals(LocalDate.of(2026, 9, 1), InsightsRules.earliestDay(TODAY))
    }

    @Test
    fun `the top two categories show by name and the rest as Other`() {
        val transactions = listOf(
            spend("s1", TransactionCategory.SHOPPING, 50_000, 1),
            spend("s2", TransactionCategory.SHOPPING, 10_000, 29),
            spend("e", TransactionCategory.ENTERTAINMENT, 30_000, 2),
            spend("d", TransactionCategory.DINING, 6_000, 3),
            spend("g", TransactionCategory.GROCERIES, 4_000, 4),
            spend("old", TransactionCategory.TRAVEL, 99_000, 30),
            spend("t", TransactionCategory.TRANSFER, 99_000, 1)
        )
        val summary = InsightsRules.spending(
            transactions,
            Money(200_000),
            SpendingPeriod.LAST_30_DAYS,
            TODAY,
            UTC
        )
        assertEquals(Money(100_000), summary.total)
        assertEquals(50, summary.percentOfBudget)
        assertEquals(
            listOf(
                TransactionCategory.SHOPPING to 60,
                TransactionCategory.ENTERTAINMENT to 30,
                null to 10
            ),
            summary.slices.map { it.category to it.percentOfSpending }
        )
        val empty = InsightsRules.spending(
            emptyList(),
            Money(200_000),
            SpendingPeriod.THIS_MONTH,
            TODAY,
            UTC
        )
        assertEquals(Money.ZERO, empty.total)
        assertTrue(empty.slices.isEmpty())
    }

    @Test
    fun `top categories are the four biggest with their two biggest merchants`() {
        val transactions = listOf(
            spend("a", TransactionCategory.SHOPPING, 30_000, 1, merchant = "Northwind"),
            spend("b", TransactionCategory.SHOPPING, 12_500, 2, merchant = "Aster & Vine"),
            spend("c", TransactionCategory.SHOPPING, 20_000, 3, merchant = "Aster & Vine"),
            spend("d", TransactionCategory.SHOPPING, 5_000, 4, merchant = "Corner Shop"),
            spend("e", TransactionCategory.DINING, 20_000, 1, merchant = "Olive & Ember"),
            spend("f", TransactionCategory.TRAVEL, 15_000, 1, merchant = "SkyWays"),
            spend("g", TransactionCategory.HEALTH, 10_000, 1, merchant = "Tony's Gym"),
            spend("h", TransactionCategory.UTILITIES, 2_500, 1, merchant = "Volt Mobile")
        )
        val top = InsightsRules.spending(
            transactions,
            Money(100_000),
            SpendingPeriod.LAST_30_DAYS,
            TODAY,
            UTC
        ).topCategories

        assertEquals(
            listOf(
                TransactionCategory.SHOPPING,
                TransactionCategory.DINING,
                TransactionCategory.TRAVEL,
                TransactionCategory.HEALTH
            ),
            top.map { it.category }
        )
        assertEquals(listOf("Aster & Vine", "Northwind"), top.first().merchants)
        assertEquals(Money(67_500), top.first().amount)
        assertEquals(59, top.first().percentOfSpending)
    }

    @Test
    fun `remaining is what is left of the budget, negative when over`() {
        fun remaining(spent: Long) = InsightsRules.spending(
            listOf(spend("a", TransactionCategory.SHOPPING, spent, 1)),
            Money(100_000),
            SpendingPeriod.LAST_30_DAYS,
            TODAY,
            UTC
        ).remaining

        assertEquals(Money(40_000), remaining(60_000))
        assertEquals(Money(-25_000), remaining(125_000))
    }

    @Test
    fun `percentages round half up and never divide by zero`() {
        assertEquals(66, InsightsRules.percent(Money(1_248_000), Money(1_890_000)))
        assertEquals(1, InsightsRules.percent(Money(5), Money(1_000)))
        assertEquals(0, InsightsRules.percent(Money(4), Money(1_000)))
        assertEquals(0, InsightsRules.percent(Money(100), Money.ZERO))
        assertEquals(150, InsightsRules.percent(Money(150), Money(100)))
    }

    @Test
    fun `savings points are a week apart and progress follows the target`() {
        val summary = InsightsRules.savings(GOAL, UTC)
        assertEquals(
            listOf(TODAY.minusDays(90), TODAY.minusDays(83), TODAY.minusDays(76)),
            summary.points.map {
                it.date
            }
        )
        assertEquals(Money(221_666), summary.average)
        assertEquals(GoalProgress.ALMOST, summary.progress)
        assertEquals(GoalProgress.REACHED, InsightsRules.progress(Money(400_000), Money(400_000)))
        assertEquals(
            GoalProgress.HALFWAY,
            InsightsRules.progress(Money(1_430_000), Money(2_000_000))
        )
        assertEquals(GoalProgress.STARTED, InsightsRules.progress(Money(125_000), Money(300_000)))
    }

    @Test
    fun `whole-dollar amounts round half up`() {
        assertEquals("$12,480", MoneyFormatter.formatWhole(Money(1_248_000)))
        assertEquals("$1,860", MoneyFormatter.formatWhole(Money(185_950)))
        assertEquals("$1,859", MoneyFormatter.formatWhole(Money(185_949)))
        assertEquals("-$3", MoneyFormatter.formatWhole(Money(-250)))
        assertEquals("$0", MoneyFormatter.formatWhole(Money(-49)))
    }
}

private class FakeInsightsRepository : InsightsRepository {
    val data = MutableStateFlow<DataResult<InsightsData>?>(null)
    var since: Instant? = null

    override fun observeInsights(since: Instant): Flow<DataResult<InsightsData>> {
        this.since = since
        return data.filterNotNull()
    }
}

class InsightsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeInsightsRepository()
    private val session = SessionManager()

    private fun viewModel() = InsightsViewModel(
        observeInsights = ObserveInsightsUseCase(repository, ManualClock(NOW)) { UTC },
        sessionManager = session
    )

    private fun load(transactions: List<Transaction>, goals: List<SavingsGoal> = listOf(GOAL)) {
        repository.data.value =
            DataResult.Success(
                InsightsData(Money(200_000), transactions, goals, isFromCache = false)
            )
    }

    @Test
    fun `reads from the first day of last month and shows the last 30 days first`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(InsightsUiState.Loading, awaitItem())
            load(
                listOf(
                    spend("a", TransactionCategory.DINING, 10_000, 1),
                    spend("b", TransactionCategory.TRAVEL, 5_000, 25)
                )
            )
            val content = awaitItem() as InsightsUiState.Content
            assertEquals(LocalDate.of(2026, 9, 1).atStartOfDay(UTC).toInstant(), repository.since)
            assertEquals(SpendingPeriod.LAST_30_DAYS, content.period)
            assertEquals(Money(15_000), content.spending.total)
            assertEquals(2, content.selectedPoint)

            viewModel.onPeriodSelect(SpendingPeriod.THIS_MONTH)
            testScheduler.runCurrent()
            assertEquals(
                Money(10_000),
                (expectMostRecentItem() as InsightsUiState.Content).spending.total
            )

            viewModel.onPointSelect(0)
            session.toggleBalancesHidden()
            testScheduler.runCurrent()
            val changed = expectMostRecentItem() as InsightsUiState.Content
            assertEquals(0, changed.selectedPoint)
            assertTrue(changed.balancesHidden)
        }
    }

    @Test
    fun `no spending and no goal is empty, and failures are errors`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            load(emptyList(), emptyList())
            assertEquals(InsightsUiState.Empty, awaitItem())
            repository.data.value = DataResult.Failure(DataError.OFFLINE)
            assertEquals(InsightsUiState.Error(DataError.OFFLINE), awaitItem())
        }
    }

    @Test
    fun `a customer without a goal still sees spending`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            load(listOf(spend("a", TransactionCategory.DINING, 10_000, 1)), emptyList())
            assertNull((awaitItem() as InsightsUiState.Content).savings)
        }
    }
}
