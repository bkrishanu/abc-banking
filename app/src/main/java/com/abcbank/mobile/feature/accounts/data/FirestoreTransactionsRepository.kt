package com.abcbank.mobile.feature.accounts.data

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.data.firebase.AccountDto
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.SupportCaseDto
import com.abcbank.mobile.core.data.firebase.TransactionDto
import com.abcbank.mobile.core.data.firebase.snapshotFlow
import com.abcbank.mobile.core.data.firebase.toDataError
import com.abcbank.mobile.core.data.firebase.toTimestamp
import com.abcbank.mobile.core.data.seed.ActivePersonaSource
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.feature.accounts.domain.DisputeMessages
import com.abcbank.mobile.feature.accounts.domain.TransactionDetail
import com.abcbank.mobile.feature.accounts.domain.TransactionsData
import com.abcbank.mobile.feature.accounts.domain.TransactionsRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class FirestoreTransactionsRepository @Inject constructor(
    private val auth: AuthDataSource,
    private val clock: Clock,
    private val activePersona: ActivePersonaSource,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : TransactionsRepository {

    override fun observeTransactions(since: Instant?): Flow<DataResult<TransactionsData>> =
        personaFlow { collection ->
            // Filtered and ordered by createdAt only, so no composite index is needed.
            val base = collection(FirestorePaths.TRANSACTIONS)
            val transactions = (
                since?.let {
                    base.whereGreaterThanOrEqualTo(TransactionDto.CREATED_AT, it.toTimestamp())
                }
                    ?: base
                ).orderBy(TransactionDto.CREATED_AT, Query.Direction.DESCENDING)
            combine(
                transactions.snapshotFlow(),
                collection(FirestorePaths.ACCOUNTS).snapshotFlow()
            ) { transactionSnapshot, accountSnapshot ->
                val accounts = accountSnapshot.toAccounts() ?: return@combine malformed()
                val items = transactionSnapshot.documents.map {
                    TransactionDto.fromMap(it.data.orEmpty()).toTransaction(it.id)
                        ?: return@combine malformed()
                }
                DataResult.Success(
                    TransactionsData(
                        transactions = items,
                        accounts = accounts,
                        isFromCache = transactionSnapshot.metadata.isFromCache ||
                            accountSnapshot.metadata.isFromCache
                    )
                )
            }
        }

    override fun observeTransaction(transactionId: String): Flow<DataResult<TransactionDetail?>> =
        personaFlow { collection ->
            combine(
                collection(FirestorePaths.TRANSACTIONS).document(transactionId).snapshotFlow(),
                collection(FirestorePaths.ACCOUNTS).snapshotFlow()
            ) { transactionSnapshot, accountSnapshot ->
                val data = transactionSnapshot.data ?: return@combine DataResult.Success(null)
                val transaction = TransactionDto.fromMap(data).toTransaction(transactionSnapshot.id)
                    ?: return@combine malformed()
                val accounts = accountSnapshot.toAccounts() ?: return@combine malformed()
                DataResult.Success(
                    TransactionDetail(
                        transaction = transaction,
                        account = accounts.firstOrNull { it.id == transaction.accountId },
                        isFromCache = transactionSnapshot.metadata.isFromCache
                    )
                )
            }
        }

    override suspend fun disputeTransaction(
        transaction: Transaction,
        messages: DisputeMessages
    ): DataResult<Unit> = try {
        val uid = when (val result = auth.signedInUid()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> return result
        }
        val personaId = activePersona.activePersona()?.key
            ?: return DataResult.Failure(DataError.UNKNOWN)
        fun collection(name: String) =
            firestore.collection(FirestorePaths.personaCollection(uid, personaId, name))
        val now = clock.now().toTimestamp()
        // Same shape as the seeded dispute (DEMO_DATA.md section 03, Support cases).
        val supportCase = mapOf(
            SupportCaseDto.TRANSACTION_ID to transaction.id,
            SupportCaseDto.TOPIC to messages.topic,
            SupportCaseDto.STATUS to SupportCaseDto.IN_REVIEW,
            SupportCaseDto.CREATED_AT to now,
            SupportCaseDto.MESSAGES to listOf(
                mapOf(
                    SupportCaseDto.FROM to SupportCaseDto.CUSTOMER,
                    SupportCaseDto.BODY to messages.customerMessage,
                    SupportCaseDto.SENT_AT to now
                ),
                mapOf(
                    SupportCaseDto.FROM to SupportCaseDto.BANK,
                    SupportCaseDto.BODY to messages.bankReply,
                    SupportCaseDto.SENT_AT to now
                )
            )
        )
        firestore.batch()
            .set(
                collection(FirestorePaths.SUPPORT_CASES).document("dispute-${transaction.id}"),
                supportCase
            )
            .update(
                collection(FirestorePaths.TRANSACTIONS).document(transaction.id),
                TransactionDto.DISPUTED,
                true
            )
            // Not awaited, like every write: applied to the cache now, uploaded when possible.
            .commit()
        DataResult.Success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        DataResult.Failure(error.toDataError())
    }

    /** Signs in, finds the active persona's tree, then hands its collections to [block]. */
    private fun <T> personaFlow(
        block: (collection: (String) -> CollectionReference) -> Flow<DataResult<T>>
    ): Flow<DataResult<T>> = flow {
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
        emitAll(
            block { name ->
                firestore.collection(FirestorePaths.personaCollection(uid, personaId, name))
            }
        )
    }
        .catch { error -> emit(DataResult.Failure(error.toDataError())) }
        .flowOn(dispatchers.default)

    private fun QuerySnapshot.toAccounts(): List<Account>? = documents.map {
        AccountDto.fromMap(it.data.orEmpty()).toAccount(it.id) ?: return null
    }

    // A document that does not map is reported, never shown half-read (AGENTS.md section 04).
    private fun malformed(): DataResult.Failure = DataResult.Failure(DataError.MALFORMED_DATA)
}
