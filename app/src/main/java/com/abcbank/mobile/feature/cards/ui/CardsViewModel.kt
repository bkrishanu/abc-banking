package com.abcbank.mobile.feature.cards.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.CardChannels
import com.abcbank.mobile.core.model.CardSecrets
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.core.security.StepUpAction
import com.abcbank.mobile.core.security.StepUpControllerFactory
import com.abcbank.mobile.core.security.StepUpState
import com.abcbank.mobile.feature.cards.domain.AddTravelNoticeUseCase
import com.abcbank.mobile.feature.cards.domain.CardRules
import com.abcbank.mobile.feature.cards.domain.CardsData
import com.abcbank.mobile.feature.cards.domain.LimitsResult
import com.abcbank.mobile.feature.cards.domain.ObserveCardsUseCase
import com.abcbank.mobile.feature.cards.domain.RevealCardDetailsUseCase
import com.abcbank.mobile.feature.cards.domain.SetCardFrozenUseCase
import com.abcbank.mobile.feature.cards.domain.StepUpResult
import com.abcbank.mobile.feature.cards.domain.TravelNoticeError
import com.abcbank.mobile.feature.cards.domain.TravelNoticeResult
import com.abcbank.mobile.feature.cards.domain.UpdateCardChannelsUseCase
import com.abcbank.mobile.feature.cards.domain.UpdateCardLimitsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything on the Cards tab that is not in Firestore. */
private data class LocalState(
    val selectedId: String? = null,
    val revealed: Pair<String, CardSecrets>? = null,
    val drafts: Map<String, LimitDraft> = emptyMap(),
    val walletAdded: Set<String> = emptySet(),
    val message: CardsMessage? = null,
    val noticeError: TravelNoticeError? = null,
    val noticesAdded: Int = 0
)

@HiltViewModel
class CardsViewModel @Inject constructor(
    observeCards: ObserveCardsUseCase,
    private val revealCardDetails: RevealCardDetailsUseCase,
    private val setCardFrozen: SetCardFrozenUseCase,
    private val updateChannels: UpdateCardChannelsUseCase,
    private val updateLimits: UpdateCardLimitsUseCase,
    private val addTravelNotice: AddTravelNoticeUseCase,
    sessionManager: SessionManager,
    stepUpFactory: StepUpControllerFactory,
    private val clock: Clock,
    private val zone: Provider<ZoneId>
) : ViewModel() {

    private val stepUp = stepUpFactory.create(viewModelScope)
    private val local = MutableStateFlow(LocalState())
    private val attempt = MutableStateFlow(0)
    private var latest: CardsUiState.Content? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CardsUiState> = combine(
        attempt.flatMapLatest { observeCards() },
        local,
        sessionManager.balancesHidden,
        stepUp.state
    ) { result, localState, hidden, stepUpState ->
        when (result) {
            is DataResult.Failure -> CardsUiState.Error(result.error)
            is DataResult.Success -> toUiState(result.value, localState, hidden, stepUpState)
        }.also { latest = it as? CardsUiState.Content }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CardsUiState.Loading)

    fun onSelectCard(cardId: String) = local.update {
        it.copy(selectedId = cardId, revealed = null, message = null)
    }

    /** Show needs step-up for this card; hide is immediate. */
    fun onToggleDetails() {
        val selected = latest?.selected?.card ?: return
        if (local.value.revealed != null) {
            onHideDetails()
            return
        }
        stepUp.request(StepUpAction.RevealCardDetails(selected.id)) { grant ->
            viewModelScope.launch {
                when (val result = revealCardDetails(selected.id, grant)) {
                    is StepUpResult.Done -> local.update {
                        it.copy(
                            revealed =
                            selected.id to result.value
                        )
                    }
                    StepUpResult.StepUpRequired, is StepUpResult.Failed ->
                        local.update { it.copy(message = CardsMessage.ACTION_FAILED) }
                }
            }
        }
    }

    /** Leaving the screen or the app going to the background wipes the revealed details. */
    fun onHideDetails() = local.update { it.copy(revealed = null) }

    /** Freezing is confirmed by the screen first; unfreezing is not destructive. */
    fun onSetFrozen(frozen: Boolean) {
        val card = latest?.selected?.card ?: return
        viewModelScope.launch { report(setCardFrozen(card.id, frozen)) }
    }

    fun onChannelsChange(channels: CardChannels) {
        val card = latest?.selected?.card ?: return
        viewModelScope.launch { report(updateChannels(card.id, channels)) }
    }

    fun onAtmLimitChange(value: Money) = editDraft { it.copy(atm = value) }

    fun onRetailLimitChange(value: Money) = editDraft { it.copy(retail = value) }

    fun onSaveLimits() {
        val content = latest ?: return
        val card = content.selected.card
        val draft = content.limits
        stepUp.request(StepUpAction.ChangeCardLimits) { grant ->
            viewModelScope.launch {
                val result = updateLimits(card, draft.atm, draft.retail, grant)
                local.update {
                    it.copy(
                        drafts = if (result ==
                            LimitsResult.Saved
                        ) {
                            it.drafts - card.id
                        } else {
                            it.drafts
                        },
                        message = if (result ==
                            LimitsResult.Saved
                        ) {
                            CardsMessage.LIMITS_SAVED
                        } else {
                            CardsMessage.ACTION_FAILED
                        }
                    )
                }
            }
        }
    }

    fun onAddTravelNotice(destination: String, start: LocalDate, end: LocalDate) {
        val card = latest?.selected?.card ?: return
        viewModelScope.launch {
            when (val result = addTravelNotice(card.id, destination, start, end)) {
                TravelNoticeResult.Added -> local.update {
                    it.copy(
                        noticeError = null,
                        noticesAdded =
                        it.noticesAdded + 1
                    )
                }
                is TravelNoticeResult.Invalid -> local.update {
                    it.copy(noticeError = result.error)
                }
                is TravelNoticeResult.Failed -> local.update {
                    it.copy(message = CardsMessage.ACTION_FAILED)
                }
            }
        }
    }

    fun onTravelSheetClosed() = local.update { it.copy(noticeError = null) }

    /** Google Wallet is simulated (PRD section 07): a success state, nothing is tokenised. */
    fun onAddToWallet() {
        val card = latest?.selected?.card ?: return
        local.update { it.copy(walletAdded = it.walletAdded + card.id) }
    }

    fun onMessageShown() = local.update { it.copy(message = null) }

    fun onStepUpDigit(digit: Int) = stepUp.onDigit(digit)

    fun onStepUpDelete() = stepUp.onDelete()

    fun onStepUpCancel() = stepUp.cancel()

    fun onRetry() {
        attempt.value++
    }

    private fun editDraft(change: (LimitDraft) -> LimitDraft) {
        val content = latest ?: return
        val id = content.selected.card.id
        local.update {
            it.copy(drafts = it.drafts + (id to change(content.limits)), message = null)
        }
    }

    private fun report(result: DataResult<Unit>) {
        if (result is DataResult.Failure) {
            local.update {
                it.copy(message = CardsMessage.ACTION_FAILED)
            }
        }
    }

    private fun toUiState(
        data: CardsData,
        state: LocalState,
        hidden: Boolean,
        stepUpState: StepUpState
    ): CardsUiState {
        if (data.cards.isEmpty()) return CardsUiState.Empty
        val zone = zone.get()
        val today = clock.now().atZone(zone).toLocalDate()
        val accounts = data.accounts.associateBy { it.id }
        val items = data.cards.map { (card, notices) ->
            val account = accounts[card.accountId]
            CardItem(
                card = card,
                balance = account?.let(::available) ?: Money.ZERO,
                isCredit = card.limits.retailMax == null,
                notices = notices.map {
                    NoticeItem(
                        id = it.id,
                        destination = it.destination,
                        start = it.startDate.atZone(zone).toLocalDate(),
                        end = it.endDate.atZone(zone).toLocalDate(),
                        active = CardRules.isActive(it, today, zone)
                    )
                }
            )
        }
        val selected = items.firstOrNull { it.card.id == state.selectedId } ?: items.first()
        val saved = LimitDraft(selected.card.limits.atmDaily, selected.card.limits.retailDaily)
        val limits = state.drafts[selected.card.id] ?: saved
        return CardsUiState.Content(
            cards = items,
            selected = selected,
            revealed = state.revealed?.takeIf { it.first == selected.card.id }?.second,
            limits = limits,
            limitsChanged = limits != saved,
            walletAdded = selected.card.id in state.walletAdded,
            balancesHidden = hidden,
            isOffline = data.isFromCache,
            stepUp = stepUpState,
            message = state.message,
            noticeError = state.noticeError,
            noticesAdded = state.noticesAdded
        )
    }

    /** What the card can spend: the debit card's account balance, the credit card's available credit. */
    private fun available(account: Account): Money? = when (account) {
        is DepositAccount -> account.available
        is CreditCardAccount -> account.availableCredit
        is LoanAccount -> account.availableToDraw
        else -> null
    }

    override fun onCleared() {
        stepUp.cancel()
        local.update { it.copy(revealed = null) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
