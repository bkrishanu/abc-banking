package com.abcbank.mobile.core.security

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.testing.ManualClock
import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeKeyStore(var key: ByteArray? = byteArrayOf(1, 2, 3)) : DeviceKeyStore {
    var calls = 0

    override fun publicKey(): ByteArray? {
        calls++
        return key
    }
}

private class FakeRegistry : DeviceRegistry {
    val stored = mutableMapOf<String, DeviceRegistration>()

    override suspend fun isRegistered(deviceId: String): DataResult<Boolean> = DataResult.Success(
        deviceId in stored
    )

    override suspend fun register(registration: DeviceRegistration): DataResult<Unit> {
        stored[registration.deviceId] = registration
        return DataResult.Success(Unit)
    }
}

class DeviceBindingTest {
    private val clock = ManualClock()

    @Test
    fun `first launch stores the public key once, under an id derived from it`() = runTest {
        val keys = FakeKeyStore()
        val registry = FakeRegistry()
        val bind = BindDeviceUseCase(keys, registry, clock)
        assertEquals(DataResult.Success(Unit), bind("Pixel 8"))
        val registration = registry.stored.values.single()
        assertEquals(BindDeviceUseCase.deviceId(byteArrayOf(1, 2, 3)), registration.deviceId)
        assertEquals(32, registration.deviceId.length)
        assertEquals(
            Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3)),
            registration.publicKey
        )
        assertEquals(clock.now, registration.registeredAt)

        clock.now = clock.now.plusSeconds(60)
        bind("Pixel 8")
        // Already registered: the first registration time is kept.
        assertEquals(registration, registry.stored.values.single())
    }

    @Test
    fun `a Keystore failure is reported, not thrown`() = runTest {
        val registry = FakeRegistry()
        val result = BindDeviceUseCase(FakeKeyStore(key = null), registry, clock)("Pixel 8")
        assertEquals(DataResult.Failure(DataError.UNKNOWN), result)
        assertTrue(registry.stored.isEmpty())
    }
}
