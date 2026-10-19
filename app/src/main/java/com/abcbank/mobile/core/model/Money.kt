package com.abcbank.mobile.core.model

/**
 * A USD amount in cents (ARCHITECTURE.md section 04). Never a Double. Arithmetic throws on
 * overflow instead of wrapping, so a bad value fails loudly rather than showing a wrong balance.
 * Format only through MoneyFormatter.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {
    val isZero: Boolean get() = cents == 0L
    val isPositive: Boolean get() = cents > 0L
    val isNegative: Boolean get() = cents < 0L

    operator fun plus(other: Money): Money = Money(Math.addExact(cents, other.cents))

    operator fun minus(other: Money): Money = Money(Math.subtractExact(cents, other.cents))

    operator fun unaryMinus(): Money = Money(Math.negateExact(cents))

    fun absoluteValue(): Money = if (isNegative) -this else this

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    companion object {
        val ZERO = Money(0L)

        fun ofDollars(dollars: Long): Money = Money(Math.multiplyExact(dollars, CENTS_PER_DOLLAR))

        const val CENTS_PER_DOLLAR = 100L
    }
}
