package com.abcbank.mobile.core.data.seed

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.UserProfileDto
import com.abcbank.mobile.core.data.firebase.await
import com.abcbank.mobile.core.data.firebase.toDataError
import com.abcbank.mobile.core.data.firebase.toTimestamp
import com.abcbank.mobile.core.model.PersonaId
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Which persona `users/{uid}` was seeded with, as far as the user document says. */
data class StoredSeed(val personaId: PersonaId, val name: String, val seedVersion: Long)

/**
 * Writes a persona's demo data under `users/{uid}` (ARCHITECTURE.md section 04, Auth and session).
 * A seed deletes the whole subtree first; with Reset demo data, this is the only data deletion in
 * the app (DEMO_DATA.md section 01). The user document is deleted first and written last, so a
 * seed that stops halfway leaves no persona on record and the next login seeds again.
 */
@Singleton
class DemoDataSeeder @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val assets: SeedAssetReader,
    private val clock: Clock,
    // Read at seed time so day 0 follows the device's current time zone.
    private val zone: Provider<ZoneId>,
    private val dispatchers: DispatcherProvider
) {
    /** Null when the install has not been seeded, or its user document is unreadable. */
    suspend fun storedSeed(uid: String): DataResult<StoredSeed?> = firebaseCall {
        val snapshot = userDocument(uid).get().await()
        val data = snapshot.data ?: return@firebaseCall null
        val dto = UserProfileDto.fromMap(data)
        val persona = PersonaId.entries.firstOrNull { it.key == dto.personaId }
        val name = dto.name
        val version = dto.seedVersion
        if (persona == null || name.isNullOrBlank() || version == null) {
            null
        } else {
            StoredSeed(persona, name, version)
        }
    }

    suspend fun bundledSeedVersion(personaId: PersonaId): DataResult<Long> = firebaseCall {
        assets.persona(personaId).seedLong(SEED_VERSION)
    }

    suspend fun seed(uid: String, personaId: PersonaId): DataResult<Unit> {
        val finished = withTimeoutOrNull(SEED_TIMEOUT) {
            firebaseCall {
                val plan = withContext(dispatchers.default) {
                    SeedPlanBuilder(
                        persona = assets.persona(personaId),
                        catalog = assets.catalog(),
                        now = clock.now(),
                        zone = zone.get()
                    ).build()
                }
                deleteUserData(uid)
                write(uid, plan)
            }
        }
        // Firestore queues writes while offline instead of failing, so a stalled seed means offline.
        return finished ?: DataResult.Failure(DataError.OFFLINE)
    }

    private suspend fun deleteUserData(uid: String) {
        // Server reads fail straight away when offline, before anything is deleted.
        val documents = mutableListOf<DocumentReference>()
        for (collection in FirestorePaths.USER_SUBCOLLECTIONS) {
            val snapshot = firestore.collection(FirestorePaths.userCollection(uid, collection))
                .get(Source.SERVER).await()
            for (document in snapshot.documents) {
                documents += document.reference
                NESTED_COLLECTIONS[collection]?.let { nested ->
                    documents += document.reference.collection(nested)
                        .get(Source.SERVER).await().documents.map { it.reference }
                }
            }
        }
        val all = listOf(userDocument(uid)) + documents
        for (chunk in all.chunked(MAX_BATCH_WRITES)) {
            val batch = firestore.batch()
            chunk.forEach(batch::delete)
            batch.commit().await()
        }
    }

    private suspend fun write(uid: String, plan: SeedPlan) {
        val user = userDocument(uid)
        val writes = plan.documents.map { document ->
            val reference = document.path.chunked(2).fold(user) { parent, (collection, id) ->
                parent.collection(collection).document(id)
            }
            reference to document.fields
        } + (user to plan.profile)
        for (chunk in writes.chunked(MAX_BATCH_WRITES)) {
            val batch = firestore.batch()
            chunk.forEach { (reference, fields) -> batch.set(reference, fields.forFirestore()) }
            batch.commit().await()
        }
    }

    private fun userDocument(uid: String): DocumentReference =
        firestore.document(FirestorePaths.user(uid))

    private suspend fun <T> firebaseCall(block: suspend () -> T): DataResult<T> = try {
        DataResult.Success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (malformed: MalformedSeedException) {
        DataResult.Failure(DataError.MALFORMED_DATA)
    } catch (error: Exception) {
        DataResult.Failure(error.toDataError())
    }

    private companion object {
        const val SEED_VERSION = "seedVersion"
        const val MAX_BATCH_WRITES = 500
        val SEED_TIMEOUT = 45.seconds
        val NESTED_COLLECTIONS = mapOf(
            FirestorePaths.ACCOUNTS to FirestorePaths.HOLDINGS,
            FirestorePaths.CARDS to FirestorePaths.TRAVEL_NOTICES
        )

        /** Firestore stores dates as Timestamps; the plan uses Instants so it stays testable. */
        fun Map<String, Any>.forFirestore(): Map<String, Any> = mapValues { (_, value) ->
            value.forFirestore()
        }

        fun Any.forFirestore(): Any = when (this) {
            is Instant -> toTimestamp()
            is Map<*, *> -> entries.associate { (key, value) -> key to value?.forFirestore() }
            is List<*> -> map { it?.forFirestore() }
            else -> this
        }
    }
}
