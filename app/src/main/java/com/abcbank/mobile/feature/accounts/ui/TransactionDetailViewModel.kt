package com.abcbank.mobile.feature.accounts.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.feature.accounts.domain.CreateReceiptUseCase
import com.abcbank.mobile.feature.accounts.domain.DisputeMessages
import com.abcbank.mobile.feature.accounts.domain.DisputeResult
import com.abcbank.mobile.feature.accounts.domain.DisputeRules
import com.abcbank.mobile.feature.accounts.domain.DisputeTransactionUseCase
import com.abcbank.mobile.feature.accounts.domain.ObserveTransactionUseCase
import com.abcbank.mobile.feature.accounts.domain.ReceiptFile
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TransactionDetailUiState {
    data object Loading : TransactionDetailUiState

    data class Error(val error: DataError) : TransactionDetailUiState

    /** No transaction with this id, for example a link from data since replaced. */
    data object Empty : TransactionDetailUiState

    data class Content(
        val transaction: Transaction,
        val account: Account?,
        val date: LocalDate,
        val time: LocalTime,
        val balancesHidden: Boolean,
        val isOffline: Boolean,
        /** From DisputeRules: posted charges that are not already disputed. */
        val canDispute: Boolean,
        val receipt: ReceiptStatus = ReceiptStatus.Idle,
        val disputeFailed: Boolean = false
    ) : TransactionDetailUiState
}

sealed interface ReceiptStatus {
    data object Idle : ReceiptStatus

    data object Creating : ReceiptStatus

    /** Ready to hand to the share sheet once; the screen reports back with onReceiptShared. */
    data class Ready(val file: ReceiptFile) : ReceiptStatus

    data object Failed : ReceiptStatus
}

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeTransaction: ObserveTransactionUseCase,
    private val createReceipt: CreateReceiptUseCase,
    private val disputeTransaction: DisputeTransactionUseCase,
    sessionManager: SessionManager,
    private val zone: Provider<ZoneId>
) : ViewModel() {

    // The navigation argument is external input; an invalid id shows the not-found state.
    private val transactionId = savedStateHandle.get<String>(ARG_TRANSACTION_ID)
        ?.takeIf(TransactionsViewModel::isValidId)

    private val attempt = MutableStateFlow(0)
    private val receipt = MutableStateFlow<ReceiptStatus>(ReceiptStatus.Idle)
    private val disputeFailed = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionDetailUiState> = combine(
        attempt.flatMapLatest {
            transactionId?.let(observeTransaction::invoke) ?: flowOf(DataResult.Success(null))
        },
        sessionManager.balancesHidden,
        receipt,
        disputeFailed
    ) { result, hidden, receiptStatus, failed ->
        when (result) {
            is DataResult.Failure -> TransactionDetailUiState.Error(result.error)
            is DataResult.Success -> result.value?.let { detail ->
                val local = detail.transaction.createdAt.atZone(zone.get())
                TransactionDetailUiState.Content(
                    transaction = detail.transaction,
                    account = detail.account,
                    date = local.toLocalDate(),
                    time = local.toLocalTime(),
                    balancesHidden = hidden,
                    isOffline = detail.isFromCache,
                    canDispute = DisputeRules.canDispute(detail.transaction),
                    receipt = receiptStatus,
                    disputeFailed = failed
                )
            } ?: TransactionDetailUiState.Empty
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        TransactionDetailUiState.Loading
    )

    fun onRetry() {
        attempt.value++
    }

    fun onDownloadReceipt() {
        val content = uiState.value as? TransactionDetailUiState.Content ?: return
        if (receipt.value == ReceiptStatus.Creating) return
        receipt.value = ReceiptStatus.Creating
        viewModelScope.launch {
            receipt.value =
                when (val result = createReceipt(content.transaction, content.account)) {
                    is DataResult.Success -> ReceiptStatus.Ready(result.value)
                    is DataResult.Failure -> ReceiptStatus.Failed
                }
        }
    }

    fun onReceiptShared() {
        receipt.value = ReceiptStatus.Idle
    }

    /** [messages] come from string resources; the case and the Disputed badge appear via the live data. */
    fun onDispute(messages: DisputeMessages) {
        val content = uiState.value as? TransactionDetailUiState.Content ?: return
        viewModelScope.launch {
            disputeFailed.value =
                disputeTransaction(content.transaction, messages) is DisputeResult.Failed
        }
    }

    companion object {
        /** Navigation argument: the transaction's document id. */
        const val ARG_TRANSACTION_ID = "transactionId"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
