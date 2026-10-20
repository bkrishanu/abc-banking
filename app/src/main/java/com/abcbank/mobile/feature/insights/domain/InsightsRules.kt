package com.abcbank.mobile.feature.insights.domain

import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import java.time.LocalDate
import java.time.ZoneId

/**
 * Insights maths (PRD feature 11): computed from the customer's own transactions and goals,
 * never hard-coded. Pure, so every rule is unit-tested.
 */
object InsightsRules {
    /** Money moved to people or own accounts is not spending; tagged P2P payments are. */
    private val NOT_SPENDING = setOf(TransactionCategory.TRANSFER, TransactionCategory.TRANSFER_OUT)
    private const val NAMED_SLICES = 2
    private const val PERCENT = 100L
    private const val DAYS_IN_WINDOW = 30L
    private const val DAYS_PER_WEEK = 7L
    private const val ALMOST_PERCENT = 75
    private const val HALFWAY_PERCENT = 50

    /** Debits, posted or pending, that are not transfers (DEMO_DATA.md section 01). */
    fun isSpending(transaction: Transaction): Boolean =
        transaction.direction == TransactionDirection.DEBIT && transaction.category !in NOT_SPENDING

    /** First and last local day of [period]; Last 30 Days includes today. */
    fun range(period: SpendingPeriod, today: LocalDate): ClosedRange<LocalDate> = when (period) {
        SpendingPeriod.LAST_30_DAYS -> today.minusDays(DAYS_IN_WINDOW - 1)..today
        SpendingPeriod.THIS_MONTH -> today.withDayOfMonth(1)..today
        SpendingPeriod.LAST_MONTH -> today.minusMonths(1).withDayOfMonth(1).let { start ->
            start..start.withDayOfMonth(start.lengthOfMonth())
        }
    }

    /** The earliest day any period reaches back to, so one query serves them all. */
    fun earliestDay(today: LocalDate): LocalDate =
        SpendingPeriod.entries.minOf { range(it, today).start }

    /**
     * Spending in [period] by category: the two largest categories, then everything else as
     * Other (DEMO_DATA.md: top two plus Other). Ties go to the category listed first.
     */
    fun spending(
        transactions: List<Transaction>,
        budget: Money,
        period: SpendingPeriod,
        today: LocalDate,
        zone: ZoneId
    ): SpendingSummary {
        val range = range(period, today)
        val byCategory = transactions
            .filter { isSpending(it) && it.createdAt.atZone(zone).toLocalDate() in range }
            .groupBy { it.category }
            .mapValues { (_, items) -> Money(items.sumOf { it.amount.cents }) }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<TransactionCategory, Money>> {
                    it.value
                }.thenBy { it.key.ordinal }
            )
        val total = Money(byCategory.sumOf { it.value.cents })
        val named = byCategory.take(NAMED_SLICES).map {
            SpendingSlice(it.key, it.value, percent(it.value, total))
        }
        val rest = Money(byCategory.drop(NAMED_SLICES).sumOf { it.value.cents })
        val slices = if (rest.isPositive) {
            named + SpendingSlice(
                null,
                rest,
                percent(rest, total)
            )
        } else {
            named
        }
        return SpendingSummary(
            period = period,
            start = range.start,
            end = range.endInclusive,
            total = total,
            budget = budget,
            percentOfBudget = percent(total, budget),
            slices = slices
        )
    }

    /** One point per week from the goal's start; the average line is the mean of the points. */
    fun savings(goal: SavingsGoal, zone: ZoneId): SavingsSummary {
        val start = goal.startedAt.atZone(zone).toLocalDate()
        val points = goal.weeklyHistory.mapIndexed { week, saved ->
            SavingsPoint(start.plusDays(week * DAYS_PER_WEEK), saved)
        }
        val average = if (points.isEmpty()) {
            Money.ZERO
        } else {
            Money(
                points.sumOf { it.saved.cents } / points.size
            )
        }
        return SavingsSummary(goal, points, average, progress(goal.saved, goal.target))
    }

    fun progress(saved: Money, target: Money): GoalProgress {
        val percent = percent(saved, target)
        return when {
            target.isPositive && saved >= target -> GoalProgress.REACHED
            percent >= ALMOST_PERCENT -> GoalProgress.ALMOST
            percent >= HALFWAY_PERCENT -> GoalProgress.HALFWAY
            else -> GoalProgress.STARTED
        }
    }

    /** Whole percent, rounded half up; 0 when [whole] is zero. */
    fun percent(part: Money, whole: Money): Int {
        if (!whole.isPositive) return 0
        return ((part.cents * PERCENT * 2 + whole.cents) / (whole.cents * 2)).toInt()
    }
}
