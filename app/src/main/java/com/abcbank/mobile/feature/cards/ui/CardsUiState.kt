package com.abcbank.mobile.feature.cards.ui

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.model.Beneficiary
import com.abcbank.mobile.core.model.Card
import com.abcbank.mobile.core.model.CardSecrets
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.security.StepUpState
import com.abcbank.mobile.feature.cards.domain.TravelNoticeError
import java.time.LocalDate

sealed interface CardsUiState {
    data object Loading : CardsUiState

    /** The persona has no cards. */
    data object Empty : CardsUiState

    data class Error(val error: DataError) : CardsUiState

    data class Content(
        val cards: List<CardItem>,
        val selected: CardItem,
        /** The selected card's number and CVV while revealed; held in memory only. */
        val revealed: CardSecrets?,
        val limits: LimitDraft,
        val limitsChanged: Boolean,
        val walletAdded: Boolean,
        val balancesHidden: Boolean,
        val isOffline: Boolean,
        val stepUp: StepUpState,
        val message: CardsMessage?,
        val noticeError: TravelNoticeError?,
        /** Bumped each time a notice is added, so the screen can close its sheet. */
        val noticesAdded: Int,
        /** Recent payees for Quick Transfer (Sample 1). */
        val quickPayees: List<Beneficiary> = emptyList()
    ) : CardsUiState
}

/** A card with what the tab shows about it. [balance] is the linked account's available money. */
data class CardItem(
    val card: Card,
    val balance: Money,
    val isCredit: Boolean,
    val notices: List<NoticeItem>
)

data class NoticeItem(
    val id: String,
    val destination: String,
    val start: LocalDate,
    val end: LocalDate,
    val active: Boolean
)

/** Daily limits as shown on the sliders; [retail] is null for credit cards. */
data class LimitDraft(val atm: Money, val retail: Money?)

enum class CardsMessage { LIMITS_SAVED, ACTION_FAILED }
