package com.abcbank.mobile.core.security

import com.abcbank.mobile.testing.FakeMpinAttemptStore
import com.abcbank.mobile.testing.ManualClock
import com.abcbank.mobile.testing.TEST_START
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MpinVerifierTest {
    private val store = FakeMpinAttemptStore()
    private val clock = ManualClock()
    private val verifier = MpinVerifier(store, clock)

    private val correct get() = "123456".toCharArray()
    private val wrong get() = "654321".toCharArray()

    @Test
    fun `correct MPIN is accepted and clears earlier failures`() = runTest {
        verifier.verify(wrong)
        verifier.verify(wrong)

        assertEquals(MpinCheck.Correct, verifier.verify(correct))
        assertEquals(MpinAttempts(), store.attempts)
    }

    @Test
    fun `each wrong MPIN reports the attempts left`() = runTest {
        val left = (1..4).map { (verifier.verify(wrong) as MpinCheck.Wrong).attemptsLeft }

        assertEquals(listOf(4, 3, 2, 1), left)
    }

    @Test
    fun `fifth wrong MPIN locks entry for 30 seconds`() = runTest {
        repeat(4) { verifier.verify(wrong) }

        val result = verifier.verify(wrong)

        assertEquals(MpinCheck.LockedOut(TEST_START.plusSeconds(30)), result)
        assertEquals(TEST_START.plusSeconds(30), verifier.lockedUntil())
    }

    @Test
    fun `while locked even the correct MPIN is refused`() = runTest {
        repeat(5) { verifier.verify(wrong) }
        clock.now = TEST_START.plusSeconds(29)

        assertEquals(MpinCheck.LockedOut(TEST_START.plusSeconds(30)), verifier.verify(correct))
    }

    @Test
    fun `after the lockout the count starts again`() = runTest {
        repeat(5) { verifier.verify(wrong) }
        clock.now = TEST_START.plusSeconds(30)

        assertNull(verifier.lockedUntil())
        assertEquals(MpinCheck.Wrong(attemptsLeft = 4), verifier.verify(wrong))
        assertEquals(MpinCheck.Correct, verifier.verify(correct))
    }

    @Test
    fun `lockout survives an app restart`() = runTest {
        repeat(5) { verifier.verify(wrong) }

        val afterRestart = MpinVerifier(store, clock)

        assertEquals(MpinCheck.LockedOut(TEST_START.plusSeconds(30)), afterRestart.verify(correct))
    }

    @Test
    fun `an MPIN of the wrong length counts as wrong`() = runTest {
        assertEquals(MpinCheck.Wrong(attemptsLeft = 4), verifier.verify("12345".toCharArray()))
        assertEquals(MpinCheck.Wrong(attemptsLeft = 3), verifier.verify("1234567".toCharArray()))
    }
}
