package com.abcbank.mobile.core.security

import java.time.Instant

/** Failed MPIN entries and any active lockout, persisted so restarting the app does not clear them. */
data class MpinAttempts(val failedCount: Int = 0, val lockedUntil: Instant? = null)

interface MpinAttemptStore {
    suspend fun readMpinAttempts(): MpinAttempts

    suspend fun writeMpinAttempts(attempts: MpinAttempts)
}
