package com.abcbank.mobile.feature.insights.domain

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** A savings goal (DEMO_DATA.md, savings goal per persona): saved so far, one point a week. */
data class SavingsGoal(
    val id: String,
    val name: String,
    val target: Money,
    val saved: Money,
    val accountId: String,
    val startedAt: Instant,
    /** Saved-so-far at the end of each week since [startedAt], oldest first. */
    val weeklyHistory: List<Money>
)

data class InsightsData(
    val monthlyBudget: Money,
    val transactions: List<Transaction>,
    val goals: List<SavingsGoal>,
    val isFromCache: Boolean
)

interface InsightsRepository {
    /** The budget, transactions created at or after [since], and savings goals. */
    fun observeInsights(since: Instant): Flow<DataResult<InsightsData>>
}

/** The donut's periods. Last 30 Days matches the documented targets (DEMO_DATA.md). */
enum class SpendingPeriod { LAST_30_DAYS, THIS_MONTH, LAST_MONTH }

/** One donut segment: a category, or null for Other. */
data class SpendingSlice(
    val category: TransactionCategory?,
    val amount: Money,
    /** Share of all spending in the period, rounded to whole percent. */
    val percentOfSpending: Int
)

data class SpendingSummary(
    val period: SpendingPeriod,
    val start: LocalDate,
    val end: LocalDate,
    val total: Money,
    val budget: Money,
    /** Total as a share of the budget, rounded; can pass 100. */
    val percentOfBudget: Int,
    val slices: List<SpendingSlice>
)

enum class GoalProgress { REACHED, ALMOST, HALFWAY, STARTED }

data class SavingsPoint(val date: LocalDate, val saved: Money)

data class SavingsSummary(
    val goal: SavingsGoal,
    val points: List<SavingsPoint>,
    val average: Money,
    val progress: GoalProgress
)
