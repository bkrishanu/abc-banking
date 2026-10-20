package com.abcbank.mobile.core.security

import com.abcbank.mobile.core.common.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SessionState { LOGGED_OUT, ACTIVE, LOCKED }

/**
 * The session lifecycle (MobileBuild.md 1.1, PRD feature 2): after login the session is active;
 * it locks after 30 minutes without touch input in the foreground or 300 seconds in the
 * background, and the MPIN unlocks it. Pure state with an injected clock; the app wires the
 * lifecycle and timers to it ([SessionLockObserver]). In memory only, so a new process starts
 * logged out.
 */
@Singleton
class SessionLock @Inject constructor(private val clock: Clock) {
    private val _state = MutableStateFlow(SessionState.LOGGED_OUT)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var lastInteraction: Instant = clock.now()
    private var backgroundedAt: Instant? = null

    fun onLoggedIn() {
        lastInteraction = clock.now()
        backgroundedAt = null
        _state.value = SessionState.ACTIVE
    }

    fun onLoggedOut() {
        backgroundedAt = null
        _state.value = SessionState.LOGGED_OUT
    }

    fun onUnlocked() {
        if (_state.value != SessionState.LOCKED) return
        lastInteraction = clock.now()
        _state.value = SessionState.ACTIVE
    }

    fun onUserInteraction() {
        lastInteraction = clock.now()
    }

    fun onBackground() {
        backgroundedAt = clock.now()
    }

    /**
     * Locks when the app was away for 300 seconds or more. A shorter absence counts as activity:
     * time in the background is not foreground idle time.
     */
    fun onForeground() {
        val since = backgroundedAt ?: return
        backgroundedAt = null
        if (Duration.between(since, clock.now()) >=
            BACKGROUND_TIMEOUT
        ) {
            lock()
        } else {
            lastInteraction = clock.now()
        }
    }

    /**
     * Locks when the foreground has been idle for 30 minutes, and otherwise returns when to check
     * again; null when there is nothing to watch (not active).
     */
    fun checkIdle(): Instant? {
        if (_state.value != SessionState.ACTIVE) return null
        val deadline = lastInteraction.plus(IDLE_TIMEOUT)
        if (!clock.now().isBefore(deadline)) {
            lock()
            return null
        }
        return deadline
    }

    private fun lock() {
        if (_state.value == SessionState.ACTIVE) _state.value = SessionState.LOCKED
    }

    companion object {
        val IDLE_TIMEOUT: Duration = Duration.ofMinutes(30)
        val BACKGROUND_TIMEOUT: Duration = Duration.ofSeconds(300)
    }
}
