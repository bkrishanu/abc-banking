package com.abcbank.mobile.core.common

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormatterTest {
    @Test
    fun `dates and times read the same whatever the device locale`() {
        assertEquals("Nov 6", DateFormatter.shortDate(LocalDate.of(2026, 11, 6)))
        assertEquals("Feb 16, 2027", DateFormatter.fullDate(LocalDate.of(2027, 2, 16)))
        assertEquals("3:14 pm", DateFormatter.time(LocalTime.of(15, 14)))
        assertEquals("12:05 am", DateFormatter.time(LocalTime.of(0, 5)))
    }
}
