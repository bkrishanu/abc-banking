package com.abcbank.mobile.feature.accounts.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.feature.accounts.domain.ObserveTransactionUseCase
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
        val isOffline: Boolean
    ) : TransactionDetailUiState
}

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeTransaction: ObserveTransactionUseCase,
    sessionManager: SessionManager,
    private val zone: Provider<ZoneId>
) : ViewModel() {

    // The navigation argument is external input; an invalid id shows the not-found state.
    private val transactionId = savedStateHandle.get<String>(ARG_TRANSACTION_ID)
        ?.takeIf(TransactionsViewModel::isValidId)

    private val attempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionDetailUiState> = combine(
        attempt.flatMapLatest {
            transactionId?.let(observeTransaction::invoke) ?: flowOf(DataResult.Success(null))
        },
        sessionManager.balancesHidden
    ) { result, hidden ->
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
                    isOffline = detail.isFromCache
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

    companion object {
        /** Navigation argument: the transaction's document id. */
        const val ARG_TRANSACTION_ID = "transactionId"
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
