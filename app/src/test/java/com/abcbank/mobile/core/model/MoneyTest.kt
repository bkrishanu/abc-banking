package com.abcbank.mobile.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test
    fun `adds and subtracts in cents`() {
        assertEquals(Money(15_583_200), Money(15_620_700) - Money(37_500))
        assertEquals(Money(10), Money(3) + Money(7))
    }

    @Test
    fun `dollars convert to cents`() {
        assertEquals(Money(100_000), Money.ofDollars(1_000))
    }

    @Test
    fun `negation and absolute value`() {
        assertEquals(Money(-12_500), -Money(12_500))
        assertEquals(Money(12_500), Money(-12_500).absoluteValue())
        assertTrue(Money(-1).isNegative)
        assertTrue(Money.ZERO.isZero)
    }

    @Test
    fun `compares by amount`() {
        assertTrue(Money(100_001) > Money.ofDollars(1_000))
        assertEquals(
            listOf(Money(1), Money(2), Money(3)),
            listOf(Money(3), Money(1), Money(2)).sorted()
        )
    }

    @Test(expected = ArithmeticException::class)
    fun `overflow throws instead of wrapping`() {
        Money(Long.MAX_VALUE) + Money(1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a rate cannot be negative`() {
        Rate(-1)
    }
}
