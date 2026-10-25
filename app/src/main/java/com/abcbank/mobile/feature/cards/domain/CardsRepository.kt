package com.abcbank.mobile.feature.cards.domain

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Card
import com.abcbank.mobile.core.model.CardChannels
import com.abcbank.mobile.core.model.CardSecrets
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TravelNotice
import java.time.Instant
import kotlinx.coroutines.flow.Flow

data class CardWithNotices(val card: Card, val notices: List<TravelNotice>)

data class CardsData(
    /** In stack order: the Main card first. */
    val cards: List<CardWithNotices>,
    val accounts: List<Account>,
    val isFromCache: Boolean,
    /** Every transaction since the requested instant, on any account, for Recent Activity. */
    val recentTransactions: List<Transaction> = emptyList()
)

/**
 * The active persona's cards. Writes apply to the offline cache at once and upload in the
 * background, like every other write; every control persists and survives a restart (PRD 10).
 */
interface CardsRepository {
    /** Live; [transactionsSince] bounds the transactions read for Recent Activity. */
    fun observeCards(transactionsSince: Instant): Flow<DataResult<CardsData>>

    /** The full number and CVV, read only when revealing (AGENTS.md section 05). */
    suspend fun loadSecrets(cardId: String): DataResult<CardSecrets>

    suspend fun setFrozen(cardId: String, frozen: Boolean): DataResult<Unit>

    suspend fun setChannels(cardId: String, channels: CardChannels): DataResult<Unit>

    suspend fun setLimits(cardId: String, atmDaily: Money, retailDaily: Money?): DataResult<Unit>

    suspend fun addTravelNotice(
        cardId: String,
        destination: String,
        start: Instant,
        end: Instant
    ): DataResult<Unit>
}
