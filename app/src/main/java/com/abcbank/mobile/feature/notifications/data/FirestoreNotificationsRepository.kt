package com.abcbank.mobile.feature.notifications.data

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.NotificationDto
import com.abcbank.mobile.core.data.firebase.snapshotFlow
import com.abcbank.mobile.core.data.firebase.toDataError
import com.abcbank.mobile.core.data.seed.ActivePersonaSource
import com.abcbank.mobile.feature.notifications.domain.Notification
import com.abcbank.mobile.feature.notifications.domain.NotificationKind
import com.abcbank.mobile.feature.notifications.domain.NotificationsData
import com.abcbank.mobile.feature.notifications.domain.NotificationsRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class FirestoreNotificationsRepository @Inject constructor(
    private val auth: AuthDataSource,
    private val activePersona: ActivePersonaSource,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : NotificationsRepository {

    private suspend fun collection(): DataResult<CollectionReference> {
        val uid = when (val result = auth.signedInUid()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> return result
        }
        val personaId =
            activePersona.activePersona()?.key ?: return DataResult.Failure(DataError.UNKNOWN)
        return DataResult.Success(
            firestore.collection(
                FirestorePaths.personaCollection(uid, personaId, FirestorePaths.NOTIFICATIONS)
            )
        )
    }

    override fun observeNotifications(): Flow<DataResult<NotificationsData>> = flow {
        val notifications = when (val result = collection()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> {
                emit(result)
                return@flow
            }
        }
        emitAll(
            notifications.snapshotFlow().map toData@{ snapshot ->
                val items = snapshot.documents.map {
                    NotificationDto.fromMap(it.data.orEmpty()).toNotification(it.id)
                        ?: return@toData DataResult.Failure(DataError.MALFORMED_DATA)
                }
                DataResult.Success(NotificationsData(items, snapshot.metadata.isFromCache))
            }
        )
    }
        .catch { error -> emit(DataResult.Failure(error.toDataError())) }
        .flowOn(dispatchers.default)

    override suspend fun markRead(ids: List<String>): DataResult<Unit> = try {
        when (val result = collection()) {
            is DataResult.Failure -> result
            is DataResult.Success -> {
                val batch = firestore.batch()
                ids.forEach { batch.update(result.value.document(it), NotificationDto.READ, true) }
                // Not awaited, like every write: applied to the cache now, uploaded when possible.
                batch.commit()
                DataResult.Success(Unit)
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        DataResult.Failure(error.toDataError())
    }
}

/** Null when a field is missing or the kind is unknown. */
internal fun NotificationDto.toNotification(id: String): Notification? = Notification(
    id = id,
    kind = NotificationKind.entries.firstOrNull { it.name == kind } ?: return null,
    title = title?.takeIf(String::isNotBlank) ?: return null,
    body = body ?: return null,
    read = read ?: false,
    createdAt =
    createdAt?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) } ?: return null
)
