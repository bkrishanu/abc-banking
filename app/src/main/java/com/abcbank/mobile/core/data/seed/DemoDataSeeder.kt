package com.abcbank.mobile.core.data.seed

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.UserProfileDto
import com.abcbank.mobile.core.data.firebase.UserSummaryDto
import com.abcbank.mobile.core.data.firebase.await
import com.abcbank.mobile.core.data.firebase.toDataError
import com.abcbank.mobile.core.data.firebase.toTimestamp
import com.abcbank.mobile.core.model.PersonaId
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Writes a persona's demo data into its own tree, `users/{uid}/personas/{personaId}`
 * (ARCHITECTURE.md section 04). Logging in never deletes anything: another persona writes its own
 * tree once, and switching back is instant. Only Reset Demo Data deletes, and only its own tree.
 *
 * Writes go to Firestore's offline cache and upload in the background (the cache keeps pending
 * writes across restarts), so nobody waits for the server. The persona document, which carries
 * `seedVersion`, is in the last batch: when it exists, the rest of the tree does too.
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
    private val seedMutex = Mutex()

    // Seeds queued by this process; a cache read straight after queuing may not see them yet.
    private val queued = mutableSetOf<String>()

    /**
     * The `seedVersion` of [personaId]'s tree, or null when it has none. Reads the offline cache
     * first. Only when the cache has nothing and the data could exist on the server (an older
     * install) does it ask the server, for at most a few seconds; if that times out, it answers
     * null and the persona is written again, which beats leaving Home empty.
     */
    suspend fun storedSeedVersion(
        uid: String,
        personaId: PersonaId,
        mayExistOnServer: Boolean
    ): DataResult<Long?> = firebaseCall {
        val document = personaDocument(uid, personaId)
        val cached = document.cachedOrNull()
        when {
            cached?.exists() == true -> cached.seedVersion()
            !mayExistOnServer -> null
            else -> withTimeoutOrNull(SERVER_CHECK_TIMEOUT) {
                document.get(Source.SERVER).await()
            }?.takeIf { it.exists() }?.seedVersion()
        }
    }

    suspend fun bundledSeedVersion(personaId: PersonaId): DataResult<Long> = firebaseCall {
        assets.persona(personaId).seedLong(SEED_VERSION)
    }

    /** Queues [personaId]'s whole tree as local writes and returns without waiting for the server. */
    suspend fun seed(uid: String, personaId: PersonaId): DataResult<Unit> = firebaseCall {
        seedMutex.withLock {
            val key = "$uid/${personaId.key}"
            if (key in queued) return@withLock
            queueSeed(uid, personaId)
            queued += key
        }
    }

    /**
     * Profile > Reset Demo Data (PRD section 08): deletes everything in [personaId]'s tree, then
     * writes it again from the bundled file, so transfers, payees, bills, applications and alerts
     * made in the demo are gone. Other personas and the install's devices are untouched. Reads the
     * tree (server when online, cache otherwise); the writes are queued locally like the seed.
     */
    suspend fun reset(uid: String, personaId: PersonaId): DataResult<Unit> = firebaseCall {
        seedMutex.withLock {
            val root = personaDocument(uid, personaId)
            val doomed = mutableListOf<DocumentReference>()
            for (collection in PERSONA_COLLECTIONS) {
                for (document in root.collection(collection).get().await().documents) {
                    NESTED_COLLECTIONS[collection]?.let { nested ->
                        doomed +=
                            document.reference.collection(nested).get().await().documents.map {
                                it.reference
                            }
                    }
                    doomed += document.reference
                }
            }
            for (chunk in doomed.chunked(MAX_BATCH_WRITES)) {
                val batch = firestore.batch()
                chunk.forEach(batch::delete)
                // Not awaited, like the seed: queued in order before the new writes.
                batch.commit()
            }
            queueSeed(uid, personaId)
            queued += "$uid/${personaId.key}"
        }
    }

    private suspend fun queueSeed(uid: String, personaId: PersonaId) {
        val plan = withContext(dispatchers.default) {
            SeedPlanBuilder(
                persona = assets.persona(personaId),
                catalog = assets.catalog(),
                now = clock.now(),
                zone = zone.get()
            ).build()
        }
        queue(personaDocument(uid, personaId), plan)
    }

    /** Updates the install's summary document; queued locally like the seed writes. */
    fun recordLogin(uid: String, personaId: PersonaId, customerId: String) {
        firestore.document(FirestorePaths.user(uid))
            .set(UserSummaryDto.forLogin(personaId.key, customerId), SetOptions.merge())
    }

    private fun queue(root: DocumentReference, plan: SeedPlan) {
        val writes = plan.documents.map { document ->
            val reference = document.path.chunked(2).fold(root) { parent, (collection, id) ->
                parent.collection(collection).document(id)
            }
            reference to document.fields
        } + (root to plan.profile)
        for (chunk in writes.chunked(MAX_BATCH_WRITES)) {
            val batch = firestore.batch()
            chunk.forEach { (reference, fields) -> batch.set(reference, fields.forFirestore()) }
            // Not awaited on purpose: the write is applied to the cache now and uploads when it can.
            batch.commit()
        }
    }

    private suspend fun DocumentReference.cachedOrNull(): DocumentSnapshot? = try {
        get(Source.CACHE).await()
    } catch (notCached: FirebaseFirestoreException) {
        // UNAVAILABLE here means "not in the cache", not an error.
        if (notCached.code == FirebaseFirestoreException.Code.UNAVAILABLE) null else throw notCached
    }

    private fun DocumentSnapshot.seedVersion(): Long? =
        data?.let { UserProfileDto.fromMap(it).seedVersion }

    private fun personaDocument(uid: String, personaId: PersonaId): DocumentReference =
        firestore.document(FirestorePaths.persona(uid, personaId.key))

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

        /** Every collection in a persona's tree (ARCHITECTURE.md section 04), and those under them. */
        val PERSONA_COLLECTIONS = listOf(
            FirestorePaths.ACCOUNTS,
            FirestorePaths.TRANSACTIONS,
            FirestorePaths.BENEFICIARIES,
            FirestorePaths.CARDS,
            FirestorePaths.BILLS,
            FirestorePaths.SAVINGS_GOALS,
            FirestorePaths.APPLICATIONS,
            FirestorePaths.NOTIFICATIONS,
            FirestorePaths.SUPPORT_CASES
        )
        val NESTED_COLLECTIONS = mapOf(
            FirestorePaths.ACCOUNTS to FirestorePaths.HOLDINGS,
            FirestorePaths.CARDS to FirestorePaths.TRAVEL_NOTICES
        )
        const val MAX_BATCH_WRITES = 500
        val SERVER_CHECK_TIMEOUT = 5.seconds

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
