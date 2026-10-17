package com.abcbank.mobile.core.model

import java.util.Locale

/**
 * A login customer ID: exactly 8 ASCII letters or digits, stored upper case (DEMO_DATA.md section 02).
 * Only [parse] creates one, so every instance is valid.
 */
@JvmInline
value class CustomerId private constructor(val value: String) {
    companion object {
        const val LENGTH = 8

        /** Trims, upper-cases and validates [raw]; null if it is not 8 letters or digits. */
        fun parse(raw: String): CustomerId? {
            val normalised = raw.trim().uppercase(Locale.ROOT)
            return if (normalised.length == LENGTH && normalised.all(::isAsciiLetterOrDigit)) {
                CustomerId(normalised)
            } else {
                null
            }
        }

        fun isAsciiLetterOrDigit(char: Char): Boolean =
            char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9'
    }
}
