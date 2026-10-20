package com.abcbank.mobile.core.security

import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.testing.FakeMpinAttemptStore
import com.abcbank.mobile.testing.ManualClock
import com.abcbank.mobile.testing.SchedulerClock
import java.time.Duration
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepUpManagerTest {
    private val clock = ManualClock()
    private val manager = StepUpManager(clock)
    private val reveal = StepUpAction.RevealCardDetails("main-card")

    @Test
    fun `transfers need step-up only above $1,000, the other actions always`() {
        assertFalse(manager.isRequired(StepUpAction.OutboundTransfer(Money.ofDollars(1_000))))
        assertTrue(manager.isRequired(StepUpAction.OutboundTransfer(Money(100_001))))
        assertTrue(manager.isRequired(StepUpAction.AddBeneficiary))
        assertTrue(manager.isRequired(StepUpAction.ChangeCardLimits))
        assertTrue(manager.isRequired(reveal))
    }

    @Test
    fun `a grant works once, for its own action only`() {
        val grant = manager.issue(reveal)

        assertFalse(manager.consume(grant, StepUpAction.RevealCardDetails("other-card")))
        assertTrue(manager.consume(manager.issue(reveal), reveal))
        assertTrue(manager.consume(grant, reveal))
        assertFalse(manager.consume(grant, reveal))
        assertFalse(manager.consume(null, reveal))
    }

    @Test
    fun `a grant expires after two minutes`() {
        val grant = manager.issue(StepUpAction.ChangeCardLimits)
        clock.now = clock.now.plus(StepUpManager.GRANT_LIFETIME).plus(Duration.ofSeconds(1))

        assertFalse(manager.consume(grant, StepUpAction.ChangeCardLimits))
    }

    @Test
    fun `small transfers pass without a grant`() {
        assertTrue(manager.consume(null, StepUpAction.OutboundTransfer(Money.ofDollars(50))))
    }
}

class StepUpControllerTest {
    private val store = FakeMpinAttemptStore()
    private val reveal = StepUpAction.RevealCardDetails("main-card")

    private fun TestScope.controller(): Pair<StepUpController, StepUpManager> {
        val clock = SchedulerClock(testScheduler)
        val manager = StepUpManager(clock)
        return StepUpController(MpinVerifier(store, clock), manager, clock, this) to manager
    }

    private fun StepUpController.enter(digits: String) = digits.forEach { onDigit(it.digitToInt()) }

    @Test
    fun `the correct MPIN closes the sheet and hands over a grant that works`() = runTest {
        val (controller, manager) = controller()
        var granted: StepUpGrant? = null

        controller.request(reveal) { granted = it }
        runCurrent()
        assertTrue(controller.state.value.isVisible)

        controller.enter("123456")
        runCurrent()

        assertFalse(controller.state.value.isVisible)
        assertTrue(manager.consume(granted, reveal))
    }

    @Test
    fun `a wrong MPIN keeps the sheet open with the attempts left`() = runTest {
        val (controller, _) = controller()
        var granted: StepUpGrant? = null
        controller.request(reveal) { granted = it }
        runCurrent()

        controller.enter("000000")
        runCurrent()

        assertEquals(StepUpMessage.WrongMpin(attemptsLeft = 4), controller.state.value.message)
        assertEquals(0, controller.state.value.entered)
        assertNull(granted)
    }

    @Test
    fun `the lockout is shared with login and counts down`() = runTest {
        val (controller, _) = controller()
        store.attempts = MpinAttempts(failedCount = 4)
        controller.request(reveal) {}
        runCurrent()

        controller.enter("000000")
        runCurrent()
        assertEquals(StepUpMessage.LockedOut(secondsLeft = 30), controller.state.value.message)
        assertFalse(controller.state.value.isPadEnabled)

        advanceTimeBy(30_000)
        runCurrent()
        assertNull(controller.state.value.message)
        assertTrue(controller.state.value.isPadEnabled)
        controller.cancel()
    }

    @Test
    fun `cancel closes without a grant, and a small transfer needs no sheet`() = runTest {
        val (controller, _) = controller()
        var granted: StepUpGrant? = null
        controller.request(reveal) { granted = it }
        controller.cancel()
        assertFalse(controller.state.value.isVisible)
        assertNull(granted)

        controller.request(StepUpAction.OutboundTransfer(Money.ofDollars(20))) { granted = it }
        assertFalse(controller.state.value.isVisible)
        assertNotNull(granted)
    }
}
