package com.abcbank.mobile.feature.documents.data

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.data.firebase.AccountDto
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.TransactionDto
import com.abcbank.mobile.core.data.firebase.UserProfileDto
import com.abcbank.mobile.core.data.firebase.snapshotFlow
import com.abcbank.mobile.core.data.firebase.toDataError
import com.abcbank.mobile.core.data.firebase.toTimestamp
import com.abcbank.mobile.core.data.seed.ActivePersonaSource
import com.abcbank.mobile.core.data.seed.MalformedSeedException
import com.abcbank.mobile.core.data.seed.SeedAssetReader
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.feature.documents.domain.DocumentsData
import com.abcbank.mobile.feature.documents.domain.DocumentsRepository
import com.abcbank.mobile.feature.documents.domain.DocumentsSetup
import com.abcbank.mobile.feature.documents.domain.DocumentsSetupSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

class FirestoreDocumentsRepository @Inject constructor(
    private val auth: AuthDataSource,
    private val activePersona: ActivePersonaSource,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : DocumentsRepository {

    override fun observeDocumentsData(since: Instant): Flow<DataResult<DocumentsData>> = flow {
        val uid = when (val result = auth.signedInUid()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> {
                emit(result)
                return@flow
            }
        }
        val personaId = activePersona.activePersona()?.key ?: run {
            emit(DataResult.Failure(DataError.UNKNOWN))
            return@flow
        }
        fun collection(name: String) =
            firestore.collection(FirestorePaths.personaCollection(uid, personaId, name))

        // A single-field filter, so no composite index is needed.
        val transactions = collection(FirestorePaths.TRANSACTIONS)
            .whereGreaterThanOrEqualTo(TransactionDto.CREATED_AT, since.toTimestamp())
        emitAll(
            combine(
                firestore.document(FirestorePaths.persona(uid, personaId)).snapshotFlow(),
                collection(FirestorePaths.ACCOUNTS).snapshotFlow(),
                transactions.snapshotFlow(),
                ::toData
            ).filterNotNull()
        )
    }
        .catch { error -> emit(DataResult.Failure(error.toDataError())) }
        .flowOn(dispatchers.default)

    /** Null while the persona's tree is still being written after login. */
    private fun toData(
        persona: DocumentSnapshot,
        accounts: QuerySnapshot,
        transactions: QuerySnapshot
    ): DataResult<DocumentsData>? {
        val fields = persona.data ?: return null
        return DataResult.Success(
            DocumentsData(
                profile = UserProfileDto.fromMap(fields).toUserProfile() ?: return malformed(),
                accounts = accounts.documents.map {
                    AccountDto.fromMap(it.data.orEmpty()).toAccount(it.id) ?: return malformed()
                },
                transactions = transactions.documents.map {
                    TransactionDto.fromMap(it.data.orEmpty()).toTransaction(it.id)
                        ?: return malformed()
                },
                isFromCache =
                persona.metadata.isFromCache ||
                    accounts.metadata.isFromCache ||
                    transactions.metadata.isFromCache
            )
        )
    }

    // A document that does not map is reported, never shown half-read (AGENTS.md section 04).
    private fun malformed(): DataResult<DocumentsData> =
        DataResult.Failure(DataError.MALFORMED_DATA)
}

/**
 * The active persona's `documents` block from its bundled file (DEMO_DATA.md section 08). It never
 * changes, so it is not copied into Firestore.
 */
class AssetDocumentsSetup @Inject constructor(
    private val activePersona: ActivePersonaSource,
    private val assets: SeedAssetReader
) : DocumentsSetupSource {
    override suspend fun setup(): DocumentsSetup? {
        val personaId = activePersona.activePersona() ?: return null
        val persona = try {
            assets.persona(personaId)
        } catch (error: IOException) {
            return null
        } catch (error: MalformedSeedException) {
            return null
        } catch (error: SerializationException) {
            return null
        }
        val block = persona[DOCUMENTS] as? JsonObject ?: return null
        val ids =
            (block[STATEMENT_ACCOUNT_IDS] as? JsonArray)?.mapNotNull {
                (it as? JsonPrimitive)?.content
            }
                ?: return null
        val interest = (block[INTEREST_1099] as? JsonPrimitive)?.longOrNull?.takeIf {
            it > 0
        }?.let(::Money)
        return DocumentsSetup(ids, interest)
    }

    private companion object {
        const val DOCUMENTS = "documents"
        const val STATEMENT_ACCOUNT_IDS = "statementAccountIds"
        const val INTEREST_1099 = "interest1099"
    }
}
