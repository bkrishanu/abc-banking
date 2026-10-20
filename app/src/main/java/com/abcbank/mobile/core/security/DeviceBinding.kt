package com.abcbank.mobile.core.security

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import javax.inject.Inject

/** The install's hardware-backed key pair (Android Keystore); only the public half leaves it. */
interface DeviceKeyStore {
    /**
     * Creates the key on first call, then returns the same public key (X.509, DER); null when the
     * Keystore fails, so binding is retried on the next launch instead of crashing it.
     */
    fun publicKey(): ByteArray?
}

/** One registered install: `users/{uid}/devices/{deviceId}` (ARCHITECTURE.md section 04). */
data class DeviceRegistration(
    val deviceId: String,
    val publicKey: String,
    val model: String,
    val registeredAt: Instant
)

interface DeviceRegistry {
    suspend fun isRegistered(deviceId: String): DataResult<Boolean>

    suspend fun register(registration: DeviceRegistration): DataResult<Unit>
}

/**
 * Device binding (MobileBuild.md 1.1, PRD feature 3): makes sure the install has its Keystore key
 * and that the public key is stored once under the user's devices. The device id is derived from
 * the public key, so it is stable for the install and needs no storage of its own.
 */
class BindDeviceUseCase @Inject constructor(
    private val keyStore: DeviceKeyStore,
    private val registry: DeviceRegistry,
    private val clock: Clock
) {
    suspend operator fun invoke(model: String): DataResult<Unit> {
        val key = keyStore.publicKey() ?: return DataResult.Failure(DataError.UNKNOWN)
        val deviceId = deviceId(key)
        return when (val registered = registry.isRegistered(deviceId)) {
            is DataResult.Failure -> registered
            is DataResult.Success -> if (registered.value) {
                DataResult.Success(Unit)
            } else {
                registry.register(
                    DeviceRegistration(
                        deviceId,
                        Base64.getEncoder().encodeToString(key),
                        model,
                        clock.now()
                    )
                )
            }
        }
    }

    companion object {
        private const val ID_BYTES = 16

        /** The first 16 bytes of the public key's SHA-256, in hex: 32 characters. */
        fun deviceId(publicKey: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(publicKey).take(ID_BYTES).joinToString("") {
                "%02x".format(it)
            }
    }
}
