package com.abcbank.mobile.core.security

import com.abcbank.mobile.core.common.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface MpinCheck {
    data object Correct : MpinCheck

    data class Wrong(val attemptsLeft: Int) : MpinCheck

    data class LockedOut(val until: Instant) : MpinCheck
}

/**
 * Checks the MPIN for login, the lock screen and step-up, which share one attempt count
 * (PRD section 08). After [MAX_ATTEMPTS] wrong entries, entry is locked for [LOCKOUT], then the
 * count starts again.
 */
@Singleton
class MpinVerifier @Inject constructor(
    private val store: MpinAttemptStore,
    private val clock: Clock
) {
    private val mutex = Mutex()

    /** The end of the current lockout, or null when entry is allowed. */
    suspend fun lockedUntil(): Instant? = store.readMpinAttempts().lockedUntil?.takeIf {
        it.isAfter(clock.now())
    }

    /** Does not keep [mpin]; the caller clears it. */
    suspend fun verify(mpin: CharArray): MpinCheck = mutex.withLock {
        val now = clock.now()
        var attempts = store.readMpinAttempts()
        val lockedUntil = attempts.lockedUntil
        if (lockedUntil != null) {
            if (lockedUntil.isAfter(now)) return MpinCheck.LockedOut(lockedUntil)
            attempts = MpinAttempts()
        }

        if (matchesDemoMpin(mpin)) {
            store.writeMpinAttempts(MpinAttempts())
            return MpinCheck.Correct
        }

        val failed = attempts.failedCount + 1
        if (failed >= MAX_ATTEMPTS) {
            val until = now.plus(LOCKOUT)
            store.writeMpinAttempts(MpinAttempts(failedCount = 0, lockedUntil = until))
            return MpinCheck.LockedOut(until)
        }
        store.writeMpinAttempts(MpinAttempts(failedCount = failed))
        MpinCheck.Wrong(attemptsLeft = MAX_ATTEMPTS - failed)
    }

    // Compares every digit whatever the result, so timing does not reveal how many are right.
    private fun matchesDemoMpin(mpin: CharArray): Boolean {
        if (mpin.size != DEMO_MPIN.length) return false
        var difference = 0
        for (index in mpin.indices) {
            difference = difference or (mpin[index].code xor DEMO_MPIN[index].code)
        }
        return difference == 0
    }

    companion object {
        const val LENGTH = 6
        const val MAX_ATTEMPTS = 5
        val LOCKOUT: Duration = Duration.ofSeconds(30)

        // Hardcoded for every persona by design (MobileBuild.md 1.1, ARCHITECTURE.md section 06).
        // It is a demo lock, not a credential: do not move it to Firestore or make it configurable.
        private const val DEMO_MPIN = "123456"
    }
}
