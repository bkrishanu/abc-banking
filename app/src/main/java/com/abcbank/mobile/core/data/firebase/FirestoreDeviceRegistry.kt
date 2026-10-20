package com.abcbank.mobile.core.data.firebase

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.security.DeviceRegistration
import com.abcbank.mobile.core.security.DeviceRegistry
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * `users/{uid}/devices/{deviceId}`: publicKey (base64 EC P-256), model, registeredAt. Outside every
 * persona tree, because the key belongs to the install (ARCHITECTURE.md section 04).
 */
class FirestoreDeviceRegistry @Inject constructor(
    private val auth: AuthDataSource,
    private val firestore: FirebaseFirestore
) : DeviceRegistry {

    private suspend fun <T> withDevices(
        block: suspend (CollectionReference) -> DataResult<T>
    ): DataResult<T> = try {
        when (val uid = auth.signedInUid()) {
            is DataResult.Failure -> uid
            is DataResult.Success -> block(
                firestore.collection(
                    FirestorePaths.userCollection(uid.value, FirestorePaths.DEVICES)
                )
            )
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        DataResult.Failure(error.toDataError())
    }

    override suspend fun isRegistered(deviceId: String): DataResult<Boolean> =
        withDevices { devices ->
            DataResult.Success(devices.document(deviceId).get().await().exists())
        }

    override suspend fun register(registration: DeviceRegistration): DataResult<Unit> =
        withDevices { devices ->
            // Not awaited, like every write: applied to the cache now, uploaded when possible.
            devices.document(registration.deviceId).set(
                firestoreFields(
                    PUBLIC_KEY to registration.publicKey,
                    MODEL to registration.model,
                    REGISTERED_AT to registration.registeredAt.toTimestamp()
                )
            )
            DataResult.Success(Unit)
        }

    private companion object {
        const val PUBLIC_KEY = "publicKey"
        const val MODEL = "model"
        const val REGISTERED_AT = "registeredAt"
    }
}
