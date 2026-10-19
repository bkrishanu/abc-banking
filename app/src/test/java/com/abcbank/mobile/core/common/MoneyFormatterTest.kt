package com.abcbank.mobile.core.common

import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Rate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {
    @Test
    fun `formats with thousands separators and two decimals`() {
        assertEquals("$155,832.00", MoneyFormatter.format(Money(15_583_200)))
        assertEquals("$2,412,560.80", MoneyFormatter.format(Money(241_256_080)))
        assertEquals("$999.99", MoneyFormatter.format(Money(99_999)))
        assertEquals("$0.05", MoneyFormatter.format(Money(5)))
        assertEquals("$0.00", MoneyFormatter.format(Money.ZERO))
    }

    @Test
    fun `negative amounts put the sign before the dollar sign`() {
        assertEquals("-$125.00", MoneyFormatter.format(Money(-12_500)))
        assertEquals("-$1,000.00", MoneyFormatter.format(Money(-100_000)))
    }

    @Test
    fun `signed format marks incoming amounts with a plus`() {
        assertEquals("+$38.60", MoneyFormatter.formatSigned(Money(3_860)))
        assertEquals("-$125.00", MoneyFormatter.formatSigned(Money(-12_500)))
        assertEquals("$0.00", MoneyFormatter.formatSigned(Money.ZERO))
    }

    @Test
    fun `parts split dollars from cents`() {
        assertEquals(MoneyParts("$155,832", ".00"), MoneyFormatter.parts(Money(15_583_200)))
        assertEquals(MoneyParts("-$6", ".45"), MoneyFormatter.parts(Money(-645)))
    }

    @Test
    fun `device locale does not change the format`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("$12,032.00", MoneyFormatter.format(Money(1_203_200)))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `rates show two decimals and a percent sign`() {
        assertEquals("3.10%", MoneyFormatter.formatRate(Rate(310)))
        assertEquals("21.24%", MoneyFormatter.formatRate(Rate(2_124)))
        assertEquals("0.05%", MoneyFormatter.formatRate(Rate(5)))
    }
}
