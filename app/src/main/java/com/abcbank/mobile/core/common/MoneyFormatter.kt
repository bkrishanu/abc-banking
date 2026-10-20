package com.abcbank.mobile.core.common

import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Rate

/**
 * The only place amounts and rates become text. USD and en-US grouping always (PRD section 07: no
 * localisation, USD only), so the device locale never changes how money reads.
 */
object MoneyFormatter {
    /** "$1,234.56", "-$125.00". */
    fun format(money: Money): String {
        val parts = parts(money)
        return parts.whole + parts.fraction
    }

    /** "$12,480" for charts and summaries: rounded to the nearest dollar, half up. */
    fun formatWhole(money: Money): String {
        val rounded =
            (money.absoluteValue().cents + Money.CENTS_PER_DOLLAR / 2) / Money.CENTS_PER_DOLLAR
        val sign = if (money.isNegative && rounded > 0) "-" else ""
        return "$sign$${groupThousands(rounded)}"
    }

    /** "+$38.60" for incoming, "-$125.00" for outgoing, "$0.00" for zero. */
    fun formatSigned(money: Money): String =
        if (money.isPositive) "+${format(money)}" else format(money)

    /** Split for `BalanceText`, which draws the cents smaller: "$155,832" and ".00". */
    fun parts(money: Money): MoneyParts {
        // Throws for Long.MIN_VALUE instead of silently formatting a wrong number.
        val absolute = money.absoluteValue().cents
        val dollars = absolute / Money.CENTS_PER_DOLLAR
        val cents = absolute % Money.CENTS_PER_DOLLAR
        val sign = if (money.isNegative) "-" else ""
        return MoneyParts(
            whole = "$sign$${groupThousands(dollars)}",
            fraction = ".${cents.toString().padStart(2, '0')}"
        )
    }

    /** "3.10%". */
    fun formatRate(rate: Rate): String {
        val whole = rate.basisPoints / Rate.BASIS_POINTS_PER_PERCENT
        val hundredths = rate.basisPoints % Rate.BASIS_POINTS_PER_PERCENT
        return "$whole.${hundredths.toString().padStart(2, '0')}%"
    }

    private fun groupThousands(value: Long): String =
        value.toString().reversed().chunked(3).joinToString(",").reversed()
}

data class MoneyParts(val whole: String, val fraction: String)
