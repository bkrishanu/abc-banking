package com.abcbank.mobile.feature.support.data

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.SupportCaseDto
import com.abcbank.mobile.core.data.firebase.await
import com.abcbank.mobile.core.data.firebase.snapshotFlow
import com.abcbank.mobile.core.data.firebase.toDataError
import com.abcbank.mobile.core.data.firebase.toTimestamp
import com.abcbank.mobile.core.data.seed.ActivePersonaSource
import com.abcbank.mobile.core.data.seed.MalformedSeedException
import com.abcbank.mobile.core.data.seed.SeedAssetReader
import com.abcbank.mobile.feature.support.domain.CaseMessage
import com.abcbank.mobile.feature.support.domain.CaseStatus
import com.abcbank.mobile.feature.support.domain.Faq
import com.abcbank.mobile.feature.support.domain.FaqSource
import com.abcbank.mobile.feature.support.domain.Party
import com.abcbank.mobile.feature.support.domain.SupportCase
import com.abcbank.mobile.feature.support.domain.SupportData
import com.abcbank.mobile.feature.support.domain.SupportRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * FAQs from Firestore's catalog/faqs (PRD feature 15). When that collection is empty or cannot be
 * read (not yet created in the console, or offline with nothing cached), the bundled catalog it is
 * created from answers instead, so the bot never goes silent in a demo.
 */
class FirestoreFaqSource @Inject constructor(
    private val auth: AuthDataSource,
    private val firestore: FirebaseFirestore,
    private val assets: SeedAssetReader
) : FaqSource {
    override suspend fun faqs(): DataResult<List<Faq>> {
        val remote = try {
            if (auth.signedInUid() is DataResult.Success) {
                firestore.collection(
                    FirestorePaths.CATALOG_FAQS
                ).get().await().documents.mapNotNull {
                    it.data.orEmpty().toFaq(it.id)
                }
            } else {
                emptyList()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            emptyList()
        }
        if (remote.isNotEmpty()) return DataResult.Success(remote)
        return try {
            val list =
                assets.catalog()[FAQS] as? JsonArray
                    ?: return DataResult.Failure(DataError.MALFORMED_DATA)
            DataResult.Success(list.mapNotNull { (it as? JsonObject)?.toFaq() })
        } catch (error: IOException) {
            DataResult.Failure(DataError.UNKNOWN)
        } catch (error: MalformedSeedException) {
            DataResult.Failure(DataError.MALFORMED_DATA)
        } catch (error: SerializationException) {
            DataResult.Failure(DataError.MALFORMED_DATA)
        }
    }

    private fun Map<String, Any?>.toFaq(id: String): Faq? = Faq(
        id = id,
        question = (this[QUESTION] as? String)?.takeIf(String::isNotBlank) ?: return null,
        keywords = (this[KEYWORDS] as? List<*>)?.filterIsInstance<String>() ?: return null,
        answer = (this[ANSWER] as? String)?.takeIf(String::isNotBlank) ?: return null
    )

    private fun JsonObject.toFaq(): Faq? {
        fun text(key: String) = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
        return Faq(
            id = text(ID) ?: return null,
            question = text(QUESTION) ?: return null,
            keywords =
            (this[KEYWORDS] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }
                ?: return null,
            answer = text(ANSWER) ?: return null
        )
    }

    private companion object {
        const val FAQS = "faqs"
        const val ID = "id"
        const val QUESTION = "question"
        const val KEYWORDS = "keywords"
        const val ANSWER = "answer"
    }
}

class FirestoreSupportRepository @Inject constructor(
    private val auth: AuthDataSource,
    private val activePersona: ActivePersonaSource,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : SupportRepository {

    private suspend fun cases(): DataResult<CollectionReference> {
        val uid = when (val result = auth.signedInUid()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> return result
        }
        val personaId =
            activePersona.activePersona()?.key ?: return DataResult.Failure(DataError.UNKNOWN)
        return DataResult.Success(
            firestore.collection(
                FirestorePaths.personaCollection(uid, personaId, FirestorePaths.SUPPORT_CASES)
            )
        )
    }

    override fun observeCases(): Flow<DataResult<SupportData>> = flow {
        val collection = when (val result = cases()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> {
                emit(result)
                return@flow
            }
        }
        emitAll(
            collection.snapshotFlow().map toData@{ snapshot ->
                val cases = snapshot.documents.map {
                    SupportCaseDto.fromMap(it.data.orEmpty()).toSupportCase(it.id)
                        ?: return@toData DataResult.Failure(DataError.MALFORMED_DATA)
                }
                DataResult.Success(SupportData(cases, snapshot.metadata.isFromCache))
            }
        )
    }
        .catch { error -> emit(DataResult.Failure(error.toDataError())) }
        .flowOn(dispatchers.default)

    override suspend fun openCase(case: SupportCase): DataResult<Unit> = try {
        when (val result = cases()) {
            is DataResult.Failure -> result
            is DataResult.Success -> {
                val dto = SupportCaseDto(
                    transactionId = case.transactionId,
                    topic = case.topic,
                    status = case.status.name,
                    createdAt = case.createdAt.toTimestamp(),
                    messages = case.messages.map {
                        SupportCaseDto.Message(it.from.name, it.body, it.sentAt.toTimestamp())
                    }
                )
                // Not awaited, like every write: applied to the cache now, uploaded when possible.
                result.value.document(case.id).set(dto.toMap())
                DataResult.Success(Unit)
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        DataResult.Failure(error.toDataError())
    }
}

/** Null when a field is missing or a status or sender is unknown. */
internal fun SupportCaseDto.toSupportCase(id: String): SupportCase? {
    fun Timestamp.instant(): Instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())
    return SupportCase(
        id = id,
        transactionId = transactionId?.takeIf(String::isNotBlank),
        topic = topic?.takeIf(String::isNotBlank) ?: return null,
        status = CaseStatus.entries.firstOrNull { it.name == status } ?: return null,
        createdAt = createdAt?.instant() ?: return null,
        messages = messages?.map { message ->
            CaseMessage(
                from = Party.entries.firstOrNull { it.name == message.from } ?: return null,
                body = message.body ?: return null,
                sentAt = message.sentAt?.instant() ?: return null
            )
        } ?: return null
    )
}
