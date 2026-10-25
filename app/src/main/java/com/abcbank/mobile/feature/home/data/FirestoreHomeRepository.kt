package com.abcbank.mobile.feature.home.data

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.data.firebase.AccountDto
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.firebase.BeneficiaryDto
import com.abcbank.mobile.core.data.firebase.CardDto
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.NotificationDto
import com.abcbank.mobile.core.data.firebase.TransactionDto
import com.abcbank.mobile.core.data.firebase.TravelNoticeDto
import com.abcbank.mobile.core.data.firebase.UserProfileDto
import com.abcbank.mobile.core.data.firebase.snapshotFlow
import com.abcbank.mobile.core.data.firebase.toDataError
import com.abcbank.mobile.core.data.firebase.toTimestamp
import com.abcbank.mobile.core.data.seed.ActivePersonaSource
import com.abcbank.mobile.core.model.TravelNotice
import com.abcbank.mobile.feature.home.domain.HomeData
import com.abcbank.mobile.feature.home.domain.HomeRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class FirestoreHomeRepository @Inject constructor(
    private val auth: AuthDataSource,
    private val activePersona: ActivePersonaSource,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : HomeRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
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

        val snapshots = combine(
            persona.snapshotFlow(),
            collection(FirestorePaths.ACCOUNTS).snapshotFlow(),
            collection(FirestorePaths.CARDS).snapshotFlow(),
            transactions.snapshotFlow(),
            unread.snapshotFlow()
        ) { personaDoc, accounts, cards, recent, unreadDocs ->
            Snapshots(personaDoc, accounts, cards, recent, unreadDocs)
        }.combine(collection(FirestorePaths.BENEFICIARIES).snapshotFlow()) { base, payees ->
            base.copy(beneficiaries = payees)
        }
        emitAll(
            snapshots.flatMapLatest { base ->
                // One small live query per card (at most three) for its travel notices, as on Cards.
                val notices = base.cards.documents.map { card ->
                    firestore.collection(FirestorePaths.travelNotices(uid, personaId, card.id))
                        .snapshotFlow()
                        .map { it.toNotices() }
                }
                if (notices.isEmpty()) {
                    flowOf(toHomeData(base, emptyList()))
                } else {
                    combine(notices) { perCard ->
                        if (perCard.any { it == null }) {
                            malformed()
                        } else {
                            toHomeData(base, perCard.flatMap { it.orEmpty() })
                        }
                    }
                }
            }.filterNotNull()
        )
    }
        .catch { error -> emit(DataResult.Failure(error.toDataError())) }
        .flowOn(dispatchers.default)

    private data class Snapshots(
        val persona: DocumentSnapshot,
        val accounts: QuerySnapshot,
        val cards: QuerySnapshot,
        val transactions: QuerySnapshot,
        val unread: QuerySnapshot,
        val beneficiaries: QuerySnapshot? = null
    )

    /** Null while the persona's tree is still being written after login: Home stays loading. */
    private fun toHomeData(
        snapshots: Snapshots,
        travelNotices: List<TravelNotice>
    ): DataResult<HomeData>? {
        val (persona, accounts, cards, transactions, unread, beneficiaries) = snapshots
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
            beneficiaries = beneficiaries?.documents.orEmpty().map {
                BeneficiaryDto.fromMap(it.data.orEmpty()).toBeneficiary(it.id) ?: return malformed()
            },
            travelNotices = travelNotices,
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

    private fun QuerySnapshot.toNotices(): List<TravelNotice>? = documents.map {
        TravelNoticeDto.fromMap(it.data.orEmpty()).toTravelNotice(it.id) ?: return null
    }

    // A document that does not map is reported, never shown half-read (AGENTS.md section 04).
    private fun malformed(): DataResult<HomeData> = DataResult.Failure(DataError.MALFORMED_DATA)
}
