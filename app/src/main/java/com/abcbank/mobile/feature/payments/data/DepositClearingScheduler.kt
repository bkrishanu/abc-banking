package com.abcbank.mobile.feature.payments.data

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.abcbank.mobile.core.common.ApplicationScope
import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.feature.payments.domain.ClearDueDepositsUseCase
import com.abcbank.mobile.feature.payments.domain.DepositClearingTrigger
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Runs deposit clearing (ARCHITECTURE.md section 04, deposit check clearing) in the application
 * scope: when the app comes to the foreground, after each new deposit, and then on a timer for the
 * next pending one while the app stays open. In the background nothing runs; deposits that came
 * due meanwhile clear on the next return.
 */
@Singleton
class DepositClearingScheduler @Inject constructor(
    private val clearDueDeposits: ClearDueDepositsUseCase,
    private val clock: Clock,
    @param:ApplicationScope private val scope: CoroutineScope
) : DepositClearingTrigger,
    DefaultLifecycleObserver {
    private var job: Job? = null
    private var foreground = false

    /** Called once from the Application. */
    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        foreground = true
        run()
    }

    override fun onStop(owner: LifecycleOwner) {
        foreground = false
        job?.cancel()
    }

    override fun onDepositSubmitted() {
        if (foreground) run()
    }

    private fun run() {
        job?.cancel()
        job = scope.launch {
            while (true) {
                val next = clearDueDeposits() ?: break
                val wait = Duration.between(clock.now(), next).toMillis().coerceAtLeast(0)
                delay(wait + SETTLE_MS)
            }
        }
    }

    private companion object {
        // A moment past the due time, so the deposit is never read a millisecond early.
        const val SETTLE_MS = 250L
    }
}
