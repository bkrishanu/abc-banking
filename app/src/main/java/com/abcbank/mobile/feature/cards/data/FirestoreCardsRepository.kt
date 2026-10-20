package com.abcbank.mobile.feature.cards.data

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.data.firebase.AccountDto
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.firebase.BeneficiaryDto
import com.abcbank.mobile.core.data.firebase.CardDto
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.TravelNoticeDto
import com.abcbank.mobile.core.data.firebase.await
import com.abcbank.mobile.core.data.firebase.snapshotFlow
import com.abcbank.mobile.core.data.firebase.toDataError
import com.abcbank.mobile.core.data.firebase.toTimestamp
import com.abcbank.mobile.core.data.seed.ActivePersonaSource
import com.abcbank.mobile.core.model.CardChannels
import com.abcbank.mobile.core.model.CardSecrets
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TravelNotice
import com.abcbank.mobile.feature.cards.domain.CardWithNotices
import com.abcbank.mobile.feature.cards.domain.CardsData
import com.abcbank.mobile.feature.cards.domain.CardsRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class FirestoreCardsRepository @Inject constructor(
    private val auth: AuthDataSource,
    private val activePersona: ActivePersonaSource,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : CardsRepository {

    /** The active persona's tree, or null when signed out of it. */
    private data class Tree(val uid: String, val personaId: String)

    private suspend fun tree(): DataResult<Tree> {
        val uid = when (val result = auth.signedInUid()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> return result
        }
        val personaId =
            activePersona.activePersona()?.key ?: return DataResult.Failure(DataError.UNKNOWN)
        return DataResult.Success(Tree(uid, personaId))
    }

    private fun Tree.collection(name: String) =
        firestore.collection(FirestorePaths.personaCollection(uid, personaId, name))

    private fun Tree.card(cardId: String) = collection(FirestorePaths.CARDS).document(cardId)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCards(): Flow<DataResult<CardsData>> = flow {
        val tree = when (val result = tree()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> {
                emit(result)
                return@flow
            }
        }
        val cardsAndAccounts = combine(
            tree.collection(FirestorePaths.CARDS).snapshotFlow(),
            tree.collection(FirestorePaths.ACCOUNTS).snapshotFlow(),
            tree.collection(FirestorePaths.BENEFICIARIES).snapshotFlow()
        ) { cards, accounts, beneficiaries -> Triple(cards, accounts, beneficiaries) }
        emitAll(
            cardsAndAccounts.flatMapLatest { (cardSnapshot, accountSnapshot, beneficiarySnapshot) ->
                val cards = cardSnapshot.documents.map {
                    CardDto.fromMap(it.data.orEmpty()).toCard(it.id)
                        ?: return@flatMapLatest flowOf(malformed())
                }.sortedBy { it.stackOrder }
                val accounts = accountSnapshot.documents.map {
                    AccountDto.fromMap(it.data.orEmpty()).toAccount(it.id)
                        ?: return@flatMapLatest flowOf(malformed())
                }
                val beneficiaries = beneficiarySnapshot.documents.map {
                    BeneficiaryDto.fromMap(it.data.orEmpty()).toBeneficiary(it.id)
                        ?: return@flatMapLatest flowOf(malformed())
                }
                val fromCache =
                    cardSnapshot.metadata.isFromCache || accountSnapshot.metadata.isFromCache
                if (cards.isEmpty()) {
                    return@flatMapLatest flowOf(
                        DataResult.Success(
                            CardsData(emptyList(), accounts, fromCache, beneficiaries)
                        )
                    )
                }
                // One small live query per card (at most three) for its travel notices.
                val notices = cards.map { card ->
                    firestore.collection(
                        FirestorePaths.travelNotices(tree.uid, tree.personaId, card.id)
                    )
                        .snapshotFlow()
                        .map { it.toNotices() }
                }
                combine(notices) { perCard ->
                    if (perCard.any { it == null }) return@combine malformed()
                    DataResult.Success(
                        CardsData(
                            cards = cards.mapIndexed { index, card ->
                                CardWithNotices(
                                    card,
                                    perCard[index].orEmpty().sortedBy {
                                        it.startDate
                                    }
                                )
                            },
                            accounts = accounts,
                            isFromCache = fromCache,
                            beneficiaries = beneficiaries
                        )
                    )
                }
            }
        )
    }
        .catch { error -> emit(DataResult.Failure(error.toDataError())) }
        .flowOn(dispatchers.default)

    override suspend fun loadSecrets(cardId: String): DataResult<CardSecrets> = call { tree ->
        val document = tree.card(cardId)
        // The cache first: the cards are already live on screen, so this is instant offline too.
        val snapshot = try {
            document.get(Source.CACHE).await()
        } catch (notCached: FirebaseFirestoreException) {
            document.get().await()
        }
        snapshot.data?.let { CardDto.fromMap(it).toCardSecrets() }
            ?.let { DataResult.Success(it) }
            ?: malformed()
    }

    override suspend fun setFrozen(cardId: String, frozen: Boolean) =
        update(cardId, mapOf(CardDto.FROZEN to frozen))

    override suspend fun setChannels(cardId: String, channels: CardChannels) = update(
        cardId,
        mapOf(
            CardDto.INTERNATIONAL to channels.international,
            CardDto.ONLINE to channels.online,
            CardDto.CONTACTLESS to channels.contactless
        )
    )

    override suspend fun setLimits(cardId: String, atmDaily: Money, retailDaily: Money?) = update(
        cardId,
        buildMap {
            put(CardDto.ATM_DAILY_LIMIT, atmDaily.cents)
            // A credit card has no retail limit of its own, so none is written.
            retailDaily?.let { put(CardDto.RETAIL_DAILY_LIMIT, it.cents) }
        }
    )

    override suspend fun addTravelNotice(
        cardId: String,
        destination: String,
        start: Instant,
        end: Instant
    ): DataResult<Unit> = call { tree ->
        val notice = TravelNoticeDto(destination, start.toTimestamp(), end.toTimestamp())
        // Not awaited, like every write: applied to the cache now, uploaded when possible.
        firestore.collection(FirestorePaths.travelNotices(tree.uid, tree.personaId, cardId))
            .document()
            .set(notice.toMap())
        DataResult.Success(Unit)
    }

    private suspend fun update(cardId: String, fields: Map<String, Any>): DataResult<Unit> =
        call { tree ->
            tree.card(cardId).update(fields)
            DataResult.Success(Unit)
        }

    private suspend fun <T> call(block: suspend (Tree) -> DataResult<T>): DataResult<T> = try {
        when (val result = tree()) {
            is DataResult.Success -> block(result.value)
            is DataResult.Failure -> result
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        DataResult.Failure(error.toDataError())
    }

    private fun QuerySnapshot.toNotices(): List<TravelNotice>? = documents.map {
        TravelNoticeDto.fromMap(it.data.orEmpty()).toTravelNotice(it.id) ?: return null
    }

    // A document that does not map is reported, never shown half-read (AGENTS.md section 04).
    private fun malformed(): DataResult.Failure = DataResult.Failure(DataError.MALFORMED_DATA)
}
