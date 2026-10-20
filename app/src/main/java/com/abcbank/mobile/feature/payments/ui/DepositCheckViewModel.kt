package com.abcbank.mobile.feature.payments.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.feature.payments.domain.AmountInput
import com.abcbank.mobile.feature.payments.domain.DepositData
import com.abcbank.mobile.feature.payments.domain.DepositError
import com.abcbank.mobile.feature.payments.domain.DepositResult
import com.abcbank.mobile.feature.payments.domain.DepositRules
import com.abcbank.mobile.feature.payments.domain.ObserveDepositDataUseCase
import com.abcbank.mobile.feature.payments.domain.PendingDeposit
import com.abcbank.mobile.feature.payments.domain.SubmitDepositUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DepositCheckUiState {
    data object Loading : DepositCheckUiState

    data class Error(val error: DataError) : DepositCheckUiState

    data class Content(
        val accounts: List<Account>,
        val account: Account?,
        val amountText: String,
        val amount: Money,
        val limit: Money,
        /** Shown inline once an amount is typed (MobileBuild.md 2.3: above the limit is blocked). */
        val amountError: DepositError?,
        val frontImage: Boolean,
        val backImage: Boolean,
        val submitted: PendingDeposit?,
        val submitting: Boolean,
        val failed: Boolean,
        val balancesHidden: Boolean,
        val isOffline: Boolean
    ) : DepositCheckUiState {
        val canConfirm: Boolean get() = account != null &&
            amount.isPositive &&
            amountError == null &&
            submitted == null
    }
}

private data class DepositDraft(
    val accountId: String? = null,
    val amount: AmountInput = AmountInput(),
    val front: Boolean = false,
    val back: Boolean = false
)

private data class DepositFlow(
    val submitted: PendingDeposit? = null,
    val submitting: Boolean = false,
    val failed: Boolean = false
)

/** Deposit check (MobileBuild.md 2.3, PRD feature 16). */
@HiltViewModel
class DepositCheckViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeDepositData: ObserveDepositDataUseCase,
    private val submitDeposit: SubmitDepositUseCase,
    sessionManager: SessionManager
) : ViewModel() {
    private val draft = MutableStateFlow(
        DepositDraft(
            accountId = savedStateHandle.get<String>(KEY_ACCOUNT),
            amount = AmountInput(
                savedStateHandle.get<String>(KEY_AMOUNT).orEmpty().filter {
                    it.isDigit() ||
                        it == AmountInput.DECIMAL
                }
            )
        )
    )
    private val flow = MutableStateFlow(DepositFlow())
    private val attempt = MutableStateFlow(0)
    private var latest: DepositCheckUiState.Content? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DepositCheckUiState> = combine(
        attempt.flatMapLatest { observeDepositData() },
        draft,
        flow,
        sessionManager.balancesHidden
    ) { result, d, f, hidden ->
        when (result) {
            is DataResult.Failure -> DepositCheckUiState.Error(result.error)
            is DataResult.Success -> toContent(result.value, d, f, hidden)
        }.also { latest = it as? DepositCheckUiState.Content }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        DepositCheckUiState.Loading
    )

    fun onSelectAccount(accountId: String) = edit { it.copy(accountId = accountId) }

    fun onKey(key: Char) = edit { it.copy(amount = it.amount.press(key)) }

    fun onDeleteKey() = edit { it.copy(amount = it.amount.delete()) }

    fun onToggleFront() = edit { it.copy(front = !it.front) }

    fun onToggleBack() = edit { it.copy(back = !it.back) }

    fun onConfirm() {
        val content = latest ?: return
        val account = content.account ?: return
        // The live flow state, so a quick double tap deposits once.
        if (flow.value.submitting || flow.value.submitted != null) return
        flow.update { it.copy(submitting = true, failed = false) }
        // The live draft, not the last emitted state, so the amount is exactly what was typed.
        val current = draft.value
        viewModelScope.launch {
            val result =
                submitDeposit(
                    account,
                    current.amount.money,
                    content.limit,
                    current.front,
                    current.back
                )
            when (result) {
                is DepositResult.Submitted -> flow.update {
                    it.copy(submitting = false, submitted = result.deposit)
                }
                else -> flow.update { it.copy(submitting = false, failed = true) }
            }
        }
    }

    fun onRetry() {
        attempt.value++
    }

    private fun edit(change: (DepositDraft) -> DepositDraft) {
        draft.update(change)
        savedStateHandle[KEY_ACCOUNT] = draft.value.accountId
        savedStateHandle[KEY_AMOUNT] = draft.value.amount.text
        flow.update { it.copy(failed = false) }
    }

    private fun toContent(
        data: DepositData,
        d: DepositDraft,
        f: DepositFlow,
        hidden: Boolean
    ): DepositCheckUiState.Content {
        val accounts = data.accounts.filter(DepositRules::canDepositInto).sortedBy {
            ORDER.indexOf(it.type)
        }
        val amount = d.amount.money
        return DepositCheckUiState.Content(
            accounts = accounts,
            account = accounts.firstOrNull { it.id == d.accountId } ?: accounts.firstOrNull(),
            amountText = d.amount.text,
            amount = amount,
            limit = data.limit,
            amountError = if (amount.isPositive) {
                DepositRules.validate(
                    amount,
                    data.limit
                )
            } else {
                null
            },
            frontImage = d.front,
            backImage = d.back,
            submitted = f.submitted,
            submitting = f.submitting,
            failed = f.failed,
            balancesHidden = hidden,
            isOffline = data.isFromCache
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val KEY_ACCOUNT = "depositAccount"
        const val KEY_AMOUNT = "depositAmount"
        val ORDER = listOf(AccountType.CHECKING, AccountType.SAVINGS)
    }
}
