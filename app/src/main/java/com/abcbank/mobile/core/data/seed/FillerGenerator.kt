package com.abcbank.mobile.core.data.seed

import java.time.LocalTime
import kotlin.random.Random

/** One category's merchant pool and per-item range in cents (DEMO_DATA.md section 07). */
internal data class FillerPool(val merchants: List<String>, val min: Long, val max: Long)

/** A monthly utility bill: who it is from, the amount in cents, and when the current one is due. */
internal data class UtilityBill(
    val billerName: String,
    val amount: Long,
    val dueDayOffset: Int,
    val autoPay: Boolean
)

/** An explicit spending debit from the persona file, as the generator needs to see it. */
internal data class ExplicitSpend(
    val category: String,
    val merchant: String,
    val amount: Long,
    val dayOffset: Int,
    val time: LocalTime?
)

/** A generated posted debit on the Main card's account. [amount] is cents. */
internal data class FillerItem(
    val category: String,
    val merchant: String,
    val amount: Long,
    val dayOffset: Int,
    val time: LocalTime
)

/**
 * Generates the spending history described in DEMO_DATA.md section 01 (Filler rules): in each
 * 30-day window, every category's debits add up exactly to its target. Bills (Utilities, and
 * Housing, which has no merchant pool) are paid once a month and repeat in older windows; other
 * categories are drawn from their merchant pool. Draws only from [random], so the same seed always
 * gives the same history.
 */
internal class FillerGenerator(private val random: Random) {

    fun generate(
        targets: Map<String, Long>,
        pools: Map<String, FillerPool>,
        utilityBills: List<UtilityBill>,
        explicit: List<ExplicitSpend>
    ): List<FillerItem> {
        val latestBills = targets.flatMap { (category, target) ->
            if (category in
                pools
            ) {
                emptyList()
            } else {
                latestBills(category, target, utilityBills, explicit)
            }
        }
        val latestWindow =
            latestBills.filter { it.isGenerated } + targets.flatMap { (category, target) ->
                pools[category]?.let { pool ->
                    val remaining = target - spent(explicit, category, WINDOWS[0].days)
                    fromPool(category, remaining, pool, WINDOWS[0])
                }.orEmpty()
            }
        val olderWindows = WINDOWS.drop(1).flatMapIndexed { index, window ->
            val monthsBack = index + 1
            val bills = latestBills.map {
                it.item.copy(dayOffset = it.item.dayOffset - monthsBack * DAYS_PER_WINDOW)
            }
            val discretionary = targets.flatMap { (category, target) ->
                pools[category]?.let { fromPool(category, target, it, window) }.orEmpty()
            }
            bills + discretionary.map { it.item }
        }
        return latestWindow.map { it.item } + olderWindows
    }

    /**
     * The latest month's payments for a bill category: the explicit ones plus, for Utilities, one
     * payment for each bill not paid explicitly, made 30 days before the current bill is due.
     */
    private fun latestBills(
        category: String,
        target: Long,
        utilityBills: List<UtilityBill>,
        explicit: List<ExplicitSpend>
    ): List<PlannedItem> {
        val window = WINDOWS[0]
        val paid = explicit.filter { it.category == category && it.dayOffset in window.days }
        val explicitPayments = paid.map {
            PlannedItem(
                FillerItem(
                    category,
                    it.merchant,
                    it.amount,
                    it.dayOffset,
                    it.time ?: LocalTime.NOON
                ),
                isGenerated = false
            )
        }
        val generated = if (category != UTILITIES) {
            emptyList()
        } else {
            val paidBillers = paid.map { it.merchant }.toSet()
            utilityBills.filter { it.billerName !in paidBillers }.map { bill ->
                val day = (bill.dueDayOffset - DAYS_PER_WINDOW)
                    .coerceIn(window.fillerDays.first, window.fillerDays.last)
                PlannedItem(
                    FillerItem(
                        category = category,
                        merchant = bill.billerName,
                        amount = bill.amount,
                        dayOffset = day,
                        time = if (bill.autoPay) AUTOPAY_TIME else randomTime()
                    ),
                    isGenerated = true
                )
            }
        }
        val payments = explicitPayments + generated
        if (payments.sumOf { it.item.amount } != target) {
            throw MalformedSeedException("$category target is not one month of its bills")
        }
        return payments
    }

    private fun fromPool(
        category: String,
        total: Long,
        pool: FillerPool,
        window: Window
    ): List<PlannedItem> {
        if (total < 0) throw MalformedSeedException("$category explicit debits exceed the target")
        if (total == 0L) return emptyList()
        val count = itemCount(total, pool)
        var left = total
        return List(count) { index ->
            val itemsAfter = (count - index - 1).toLong()
            // Keep enough room so whatever is left can still be split within the range.
            val low = maxOf(pool.min, left - itemsAfter * pool.max)
            val high = minOf(pool.max, left - itemsAfter * pool.min)
            val amount = when {
                itemsAfter == 0L -> left
                low >= high -> low
                else -> random.nextLong(low, high + 1)
            }
            left -= amount
            val item = FillerItem(
                category = category,
                merchant = pool.merchants.random(random),
                amount = amount,
                dayOffset = random.nextInt(window.fillerDays.first, window.fillerDays.last + 1),
                time = randomTime()
            )
            PlannedItem(item, isGenerated = true)
        }
    }

    /** As many items as the total needs at the range's midpoint, but never more or fewer than fit. */
    private fun itemCount(total: Long, pool: FillerPool): Int {
        if (total < pool.min) return 1
        val fewest = ceilDiv(total, pool.max)
        val most = total / pool.min
        val typical = Math.round(total.toDouble() / ((pool.min + pool.max) / 2))
        return typical.coerceIn(fewest, most).coerceAtLeast(1).toInt()
    }

    private fun randomTime(): LocalTime = FIRST_TIME.plusMinutes(
        random.nextLong(
            TIME_SPAN_MINUTES + 1
        )
    )

    private fun spent(explicit: List<ExplicitSpend>, category: String, days: IntRange): Long =
        explicit.filter { it.category == category && it.dayOffset in days }.sumOf { it.amount }

    /** [isGenerated] is false for the persona file's own transactions, which are not added again. */
    private data class PlannedItem(val item: FillerItem, val isGenerated: Boolean)

    /** [days] is what counts toward the targets; filler lands on [fillerDays] only. */
    private data class Window(val days: IntRange, val fillerDays: IntRange)

    companion object {
        const val UTILITIES = "UTILITIES"
        const val DAYS_PER_WINDOW = 30

        /** Day 0 and the 29 days before it. */
        val LAST_30_DAYS: IntRange = -29..0

        /** How far back older windows sit, in days; recurring items repeat at these shifts. */
        val OLDER_WINDOW_SHIFTS: List<Int> = listOf(30, 60, 90)

        // Filler stays off the last two days so their explicit items decide the latest balances,
        // such as the moment Marcus's checking crosses below $500. The oldest window runs to day
        // -122 so the three calendar months before the seed month are always covered.
        private val WINDOWS = listOf(
            Window(days = LAST_30_DAYS, fillerDays = -29..-3),
            Window(days = -59..-30, fillerDays = -59..-30),
            Window(days = -89..-60, fillerDays = -89..-60),
            Window(days = -122..-90, fillerDays = -122..-90)
        )
        private val FIRST_TIME: LocalTime = LocalTime.of(7, 0)
        private val AUTOPAY_TIME: LocalTime = LocalTime.of(6, 0)
        private const val TIME_SPAN_MINUTES = 15 * 60L

        private fun ceilDiv(a: Long, b: Long): Long = (a + b - 1) / b
    }
}
