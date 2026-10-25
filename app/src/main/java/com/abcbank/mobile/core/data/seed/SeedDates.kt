package com.abcbank.mobile.core.data.seed

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Turns the seed files' relative dates into real instants (DEMO_DATA.md section 01), so every
 * persona looks current on the day it is seeded. Day 0 is the seed day in the device time zone.
 */
internal class SeedDates(private val now: Instant, private val zone: ZoneId) {
    private val seedDay: LocalDate = now.atZone(zone).toLocalDate()

    fun resolve(date: SeedDate): Instant {
        val time = date.time ?: LocalTime.MIDNIGHT
        val day = seedDay.plusDays(date.dayOffset.toLong())
        val instant = day.atTime(time).atZone(zone).toInstant()
        if (date.dayOffset != 0 || !instant.isAfter(now)) return instant
        // A later-today item moves to 10 minutes before now. Items that were later in the day
        // stay later, by up to 24 seconds, so the list keeps its documented order.
        val endOfDay = seedDay.plusDays(1).atStartOfDay(zone).toInstant()
        val keepOrder = Duration.between(instant, endOfDay).seconds / SECONDS_PER_HOUR
        val shifted = now.minus(FUTURE_TODAY_SHIFT).minusSeconds(keepOrder)
        val startOfDay = seedDay.atStartOfDay(zone).toInstant()
        if (!shifted.isBefore(startOfDay)) return shifted
        // Seeded in the first minutes after midnight: 10 minutes back would be yesterday, so the
        // items share the time since midnight instead, in the same order, still on the seed day.
        val sinceMidnight = Duration.between(startOfDay, now)
        return now.minus(sinceMidnight.multipliedBy(keepOrder + 1).dividedBy(ORDER_SLOTS))
    }

    /** "Oct 29" for day +n. English only: the app has no localisation (PRD section 07). */
    fun shortDate(dayOffset: Int): String = seedDay.plusDays(dayOffset.toLong()).format(SHORT_DATE)

    /** Replaces `{date:+n}` tokens in notification text with [shortDate]. */
    fun replaceDateTokens(text: String): String =
        DATE_TOKEN.replace(text) { match -> shortDate(match.groupValues[1].toInt()) }

    private companion object {
        val FUTURE_TODAY_SHIFT: Duration = Duration.ofMinutes(10)
        const val SECONDS_PER_HOUR = 3_600L

        // keepOrder runs from 0 to 24, so 26 slots keep every item after midnight.
        const val ORDER_SLOTS = 26L
        val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
        val DATE_TOKEN = Regex("""\{date:([+-]?\d+)\}""")
    }
}
