package com.abcbank.mobile.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.abcbank.mobile.core.common.ApplicationScope
import com.abcbank.mobile.core.common.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Wires [SessionLock] to the app (ARCHITECTURE.md section 04): process foreground and background
 * from ProcessLifecycleOwner, and an idle timer that runs while the app is in the foreground with
 * an active session. Touches reach [SessionLock.onUserInteraction] from MainActivity.
 */
@Singleton
class SessionLockObserver @Inject constructor(
    private val sessionLock: SessionLock,
    private val clock: Clock,
    @param:ApplicationScope private val scope: CoroutineScope
) : DefaultLifecycleObserver {
    private var idleTimer: Job? = null

    /** Called once from the Application. */
    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        sessionLock.onForeground()
        idleTimer?.cancel()
        // Restarts whenever the session changes, so logging in or unlocking starts a fresh wait.
        idleTimer = scope.launch {
            sessionLock.state.collectLatest {
                while (true) {
                    val deadline = sessionLock.checkIdle() ?: break
                    delay(
                        Duration.between(clock.now(), deadline).toMillis().coerceAtLeast(0) +
                            SETTLE_MS
                    )
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        idleTimer?.cancel()
        sessionLock.onBackground()
    }

    private companion object {
        // A moment past the deadline, so a check never lands a millisecond early.
        const val SETTLE_MS = 250L
    }
}
