package com.abcbank.mobile.core.security

import app.cash.turbine.test
import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.feature.auth.data.CatalogPersonaDirectory
import com.abcbank.mobile.feature.auth.domain.GetLockIdentityUseCase
import com.abcbank.mobile.feature.auth.domain.LockIdentity
import com.abcbank.mobile.feature.auth.domain.PersonaNames
import com.abcbank.mobile.feature.auth.ui.LockUiState
import com.abcbank.mobile.feature.auth.ui.LockViewModel
import com.abcbank.mobile.testing.FakeMpinAttemptStore
import com.abcbank.mobile.testing.FakeRememberedCustomerStore
import com.abcbank.mobile.testing.MainDispatcherRule
import com.abcbank.mobile.testing.ManualClock
import java.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SessionLockTest {
    private val clock = ManualClock()
    private val lock = SessionLock(clock)

    @Test
    fun `nothing locks before login or after log out`() {
        assertEquals(SessionState.LOGGED_OUT, lock.state.value)
        lock.onBackground()
        clock.now = clock.now.plus(Duration.ofHours(1))
        lock.onForeground()
        assertNull(lock.checkIdle())
        assertEquals(SessionState.LOGGED_OUT, lock.state.value)
    }

    @Test
    fun `30 minutes without a touch in the foreground locks, and touches push it back`() {
        lock.onLoggedIn()
        assertEquals(clock.now.plus(Duration.ofMinutes(30)), lock.checkIdle())
        clock.now = clock.now.plus(Duration.ofMinutes(20))
        lock.onUserInteraction()
        clock.now = clock.now.plus(Duration.ofMinutes(20))
        assertEquals(clock.now.plus(Duration.ofMinutes(10)), lock.checkIdle())
        clock.now = clock.now.plus(Duration.ofMinutes(10))
        assertNull(lock.checkIdle())
        assertEquals(SessionState.LOCKED, lock.state.value)
    }

    @Test
    fun `300 seconds in the background locks, a shorter absence does not`() {
        lock.onLoggedIn()
        lock.onBackground()
        clock.now = clock.now.plusSeconds(299)
        lock.onForeground()
        assertEquals(SessionState.ACTIVE, lock.state.value)
        // The time away counted as activity, so the idle clock restarted.
        assertEquals(clock.now.plus(Duration.ofMinutes(30)), lock.checkIdle())
        lock.onBackground()
        clock.now = clock.now.plusSeconds(300)
        lock.onForeground()
        assertEquals(SessionState.LOCKED, lock.state.value)
    }

    @Test
    fun `unlocking returns to the session, and log out ends it`() {
        lock.onLoggedIn()
        lock.onBackground()
        clock.now = clock.now.plus(Duration.ofMinutes(10))
        lock.onForeground()
        lock.onUnlocked()
        assertEquals(SessionState.ACTIVE, lock.state.value)
        lock.onLoggedOut()
        lock.onUnlocked()
        assertEquals(SessionState.LOGGED_OUT, lock.state.value)
    }
}

class LockViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = ManualClock()
    private val sessionLock = SessionLock(clock)
    private val store = FakeRememberedCustomerStore().apply {
        remembered =
            CustomerId.parse("SAMJOH01")
    }
    private val names = object : PersonaNames {
        override suspend fun firstName(personaId: PersonaId): String = "Samanta"
    }
    private val sessionManager = SessionManager()

    private fun viewModel() = LockViewModel(
        getLockIdentity = GetLockIdentityUseCase(store, CatalogPersonaDirectory(), names),
        sessionLock = sessionLock,
        logOut = LogOutUseCase(store, sessionManager, sessionLock),
        verifier = MpinVerifier(FakeMpinAttemptStore(), clock),
        clock = clock
    )

    private fun lockNow() {
        sessionLock.onLoggedIn()
        sessionLock.onBackground()
        clock.now = clock.now.plus(Duration.ofMinutes(6))
        sessionLock.onForeground()
    }

    @Test
    fun `greets the remembered customer and unlocks with the MPIN`() = runTest {
        lockNow()
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(LockUiState.Loading, awaitItem())
            viewModel.onShown()
            testScheduler.runCurrent()
            val content = expectMostRecentItem() as LockUiState.Content
            assertEquals(LockIdentity(PersonaId.SAMANTA, "Samanta"), content.identity)
            "111111".forEach { viewModel.onDigit(it.digitToInt()) }
            testScheduler.runCurrent()
            val wrong = expectMostRecentItem() as LockUiState.Content
            assertTrue(wrong.pad.message is StepUpMessage.WrongMpin)
            assertEquals(SessionState.LOCKED, sessionLock.state.value)
            "123456".forEach { viewModel.onDigit(it.digitToInt()) }
            testScheduler.runCurrent()
            assertEquals(SessionState.ACTIVE, sessionLock.state.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Not you logs out so another customer ID can log in`() = runTest {
        lockNow()
        val viewModel = viewModel()
        var loggedOut = false
        viewModel.onShown()
        viewModel.onNotYou { loggedOut = true }
        testScheduler.runCurrent()
        assertTrue(loggedOut)
        assertNull(store.remembered)
        assertEquals(SessionState.LOGGED_OUT, sessionLock.state.value)
    }
}
