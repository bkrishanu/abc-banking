package com.abcbank.mobile.core.common

import java.time.Instant
import javax.inject.Inject

/** Injected wherever time matters (lockouts, session timeouts) so tests control it. */
fun interface Clock {
    fun now(): Instant
}

class SystemWallClock @Inject constructor() : Clock {
    override fun now(): Instant = Instant.now()
}
