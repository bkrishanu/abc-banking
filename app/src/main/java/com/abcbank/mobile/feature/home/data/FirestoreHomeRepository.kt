package com.abcbank.mobile.feature.home.data

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.data.firebase.AccountDto
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.firebase.CardDto
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.NotificationDto
import com.abcbank.mobile.core.data.firebase.TransactionDto
import com.abcbank.mobile.core.data.firebase.UserProfileDto
import com.abcbank.mobile.core.data.firebase.snapshotFlow
import com.abcbank.mobile.core.data.firebase.toDataError
import com.abcbank.mobile.core.data.firebase.toTimestamp
import com.abcbank.mobile.core.data.seed.ActivePersonaSource
import com.abcbank.mobile.feature.home.domain.HomeData
import com.abcbank.mobile.feature.home.domain.HomeRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class FirestoreHomeRepository @Inject constructor(
    private val auth: AuthDataSource,
    private val activePersona: ActivePersonaSource,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : HomeRepository {

    override fun observeHome(transactionsSince: Instant): Flow<DataResult<HomeData>> = flow {
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
        val persona = firestore.document(FirestorePaths.persona(uid, personaId))
        fun collection(name: String) =
            firestore.collection(FirestorePaths.personaCollection(uid, personaId, name))

        // Single-field filters only, so no composite index is needed.
        val transactions = collection(FirestorePaths.TRANSACTIONS)
            .whereGreaterThanOrEqualTo(TransactionDto.CREATED_AT, transactionsSince.toTimestamp())
            .orderBy(TransactionDto.CREATED_AT, Query.Direction.DESCENDING)
        val unread = collection(
            FirestorePaths.NOTIFICATIONS
        ).whereEqualTo(NotificationDto.READ, false)

        emitAll(
            combine(
                persona.snapshotFlow(),
                collection(FirestorePaths.ACCOUNTS).snapshotFlow(),
                collection(FirestorePaths.CARDS).snapshotFlow(),
                transactions.snapshotFlow(),
                unread.snapshotFlow(),
                ::toHomeData
            ).filterNotNull()
        )
    }
        .catch { error -> emit(DataResult.Failure(error.toDataError())) }
        .flowOn(dispatchers.default)

    /** Null while the persona's tree is still being written after login: Home stays loading. */
    private fun toHomeData(
        persona: DocumentSnapshot,
        accounts: QuerySnapshot,
        cards: QuerySnapshot,
        transactions: QuerySnapshot,
        unread: QuerySnapshot
    ): DataResult<HomeData>? {
        val profileFields = persona.data ?: return null
        val data = HomeData(
            profile = UserProfileDto.fromMap(profileFields).toUserProfile() ?: return malformed(),
            accounts = accounts.documents.map {
                AccountDto.fromMap(it.data.orEmpty()).toAccount(it.id) ?: return malformed()
            },
            cards = cards.documents.map {
                CardDto.fromMap(it.data.orEmpty()).toCard(it.id) ?: return malformed()
            },
            recentTransactions = transactions.documents.map {
                TransactionDto.fromMap(it.data.orEmpty()).toTransaction(it.id) ?: return malformed()
            },
            unreadNotifications = unread.size(),
            isFromCache = listOf(
                persona.metadata,
                accounts.metadata,
                cards.metadata,
                transactions.metadata
            )
                .any { it.isFromCache }
        )
        return DataResult.Success(data)
    }

    // A document that does not map is reported, never shown half-read (AGENTS.md section 04).
    private fun malformed(): DataResult<HomeData> = DataResult.Failure(DataError.MALFORMED_DATA)
}
