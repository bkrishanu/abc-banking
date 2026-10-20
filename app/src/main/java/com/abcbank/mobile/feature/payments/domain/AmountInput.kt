package com.abcbank.mobile.feature.payments.domain

import com.abcbank.mobile.core.model.Money

/**
 * What the amount keypad has typed, kept as text so "12." and "12.5" show while typing. At most
 * two decimals and seven whole digits ($9,999,999.99), no leading zeros.
 */
@JvmInline
value class AmountInput(val text: String = "") {
    fun press(key: Char): AmountInput = when {
        key == DECIMAL -> if (DECIMAL in
            text
        ) {
            this
        } else {
            AmountInput(if (text.isEmpty()) "0." else "$text.")
        }
        key !in '0'..'9' -> this
        DECIMAL in text -> if (text.substringAfter(DECIMAL).length >=
            MAX_DECIMALS
        ) {
            this
        } else {
            AmountInput(text + key)
        }
        text == "0" -> AmountInput(key.toString())
        text.length >= MAX_WHOLE_DIGITS -> this
        else -> AmountInput(text + key)
    }

    fun delete(): AmountInput = AmountInput(text.dropLast(1))

    /** The typed amount in cents; "12.5" is $12.50. */
    val money: Money
        get() {
            if (text.isEmpty()) return Money.ZERO
            val whole = text.substringBefore(DECIMAL).ifEmpty { "0" }.toLong()
            val fraction = text.substringAfter(
                DECIMAL,
                ""
            ).padEnd(MAX_DECIMALS, '0').take(MAX_DECIMALS).toLong()
            return Money(whole * Money.CENTS_PER_DOLLAR + fraction)
        }

    companion object {
        const val DECIMAL = '.'
        private const val MAX_DECIMALS = 2
        private const val MAX_WHOLE_DIGITS = 7
    }
}
