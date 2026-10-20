package com.abcbank.mobile.core.common

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The only place dates and times become text. English (US) always, like [MoneyFormatter]: the app
 * has no localisation (PRD section 07).
 */
object DateFormatter {
    private val SHORT_DATE = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    private val FULL_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

    /** "Nov 6". */
    fun shortDate(date: LocalDate): String = date.format(SHORT_DATE)

    /** "Feb 17, 2027". */
    fun fullDate(date: LocalDate): String = date.format(FULL_DATE)

    /** "September 2026", for statements. */
    fun monthYear(month: YearMonth): String = month.format(MONTH_YEAR)

    /** "3:14 pm", lower case as in the reference screens. */
    fun time(time: LocalTime): String = time.format(TIME).lowercase(Locale.US)
}
