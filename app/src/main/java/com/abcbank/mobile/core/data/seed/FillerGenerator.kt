package com.abcbank.mobile.core.data.seed

import java.time.LocalTime
import kotlin.random.Random

/** One category's merchant pool and per-item range in cents (DEMO_DATA.md section 07). */
internal data class FillerPool(val merchants: List<String>, val min: Long, val max: Long)

/** A utility bill the persona pays: the biller's name and the bill amount in cents. */
internal data class UtilityBill(val billerName: String, val amount: Long)

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
 * Generates the filler history described in DEMO_DATA.md section 01 (Filler rules): in each of the
 * three windows, every category's debits add up exactly to its target. Draws only from [random],
 * so the same seed always gives the same history.
 */
internal class FillerGenerator(private val random: Random) {

    fun generate(
        targets: Map<String, Long>,
        pools: Map<String, FillerPool>,
        utilityBills: List<UtilityBill>,
        explicit: List<ExplicitSpend>
    ): List<FillerItem> = WINDOWS.flatMapIndexed { index, window ->
        targets.flatMap { (category, target) ->
            val alreadySpent = explicit
                .filter { it.category == category && it.dayOffset in window.days }
                .sumOf { it.amount }
            val remaining = target - alreadySpent
            if (remaining < 0) {
                throw MalformedSeedException("$category explicit debits exceed the target")
            }
            when {
                remaining == 0L -> emptyList()
                category == UTILITIES -> utilities(remaining, utilityBills, window)
                category in pools ->
                    fromPool(category, remaining, pools.getValue(category), window)
                else -> repeatExplicit(category, remaining, explicit, monthsBack = index)
            }
        }
    }

    private fun fromPool(
        category: String,
        total: Long,
        pool: FillerPool,
        window: Window
    ): List<FillerItem> {
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
            item(category, pool.merchants.random(random), amount, window)
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

    private fun utilities(total: Long, bills: List<UtilityBill>, window: Window): List<FillerItem> {
        if (bills.isEmpty()) throw MalformedSeedException("Utilities target but no utility bills")
        val cycle = bills.shuffled(random)
        var left = total
        val items = mutableListOf<FillerItem>()
        while (left > 0) {
            val bill = cycle[items.size % cycle.size]
            val amount = if (bill.amount >= left) left else bill.amount
            items += item(UTILITIES, bill.billerName, amount, window)
            left -= amount
        }
        return items
    }

    /** Housing has no merchant pool: the persona's explicit payment repeats one month back per window. */
    private fun repeatExplicit(
        category: String,
        total: Long,
        explicit: List<ExplicitSpend>,
        monthsBack: Int
    ): List<FillerItem> {
        val latest = explicit.filter { it.category == category && it.dayOffset in WINDOWS[0].days }
        if (monthsBack == 0 || latest.sumOf { it.amount } != total) {
            throw MalformedSeedException("$category has no merchant pool to fill $total cents")
        }
        return latest.map {
            FillerItem(
                category = category,
                merchant = it.merchant,
                amount = it.amount,
                dayOffset = it.dayOffset - monthsBack * DAYS_PER_WINDOW,
                time = it.time ?: LocalTime.NOON
            )
        }
    }

    private fun item(category: String, merchant: String, amount: Long, window: Window) = FillerItem(
        category = category,
        merchant = merchant,
        amount = amount,
        dayOffset = random.nextInt(window.fillerDays.first, window.fillerDays.last + 1),
        time = FIRST_TIME.plusMinutes(random.nextLong(TIME_SPAN_MINUTES + 1))
    )

    /** [days] is what counts toward the targets; filler lands on [fillerDays] only. */
    private data class Window(val days: IntRange, val fillerDays: IntRange)

    companion object {
        const val UTILITIES = "UTILITIES"
        const val DAYS_PER_WINDOW = 30

        /** Day 0 and the 29 days before it, then two older windows back to day -90. */
        val LAST_30_DAYS: IntRange = -29..0

        private val WINDOWS = listOf(
            Window(days = LAST_30_DAYS, fillerDays = -29..-1),
            Window(days = -59..-30, fillerDays = -59..-30),
            Window(days = -90..-60, fillerDays = -90..-60)
        )
        private val FIRST_TIME: LocalTime = LocalTime.of(7, 0)
        private const val TIME_SPAN_MINUTES = 15 * 60L

        private fun ceilDiv(a: Long, b: Long): Long = (a + b - 1) / b
    }
}
