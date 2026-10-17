package com.abcbank.mobile.testing

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.security.MpinAttemptStore
import com.abcbank.mobile.core.security.MpinAttempts
import com.abcbank.mobile.core.security.RememberedCustomerStore
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

val TEST_START: Instant = Instant.parse("2026-10-17T10:00:00Z")

/** A clock that moves with the coroutine test scheduler's virtual time. */
class SchedulerClock(private val scheduler: TestCoroutineScheduler) : Clock {
    override fun now(): Instant = TEST_START.plusMillis(scheduler.currentTime)
}

/** A clock the test moves by hand. */
class ManualClock(var now: Instant = TEST_START) : Clock {
    override fun now(): Instant = now
}

class FakeMpinAttemptStore(var attempts: MpinAttempts = MpinAttempts()) : MpinAttemptStore {
    override suspend fun readMpinAttempts(): MpinAttempts = attempts

    override suspend fun writeMpinAttempts(attempts: MpinAttempts) {
        this.attempts = attempts
    }
}

class FakeRememberedCustomerStore : RememberedCustomerStore {
    var remembered: CustomerId? = null

    override suspend fun rememberCustomer(customerId: CustomerId) {
        remembered = customerId
    }

    override suspend fun rememberedCustomer(): CustomerId? = remembered
}

class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) :
    TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
