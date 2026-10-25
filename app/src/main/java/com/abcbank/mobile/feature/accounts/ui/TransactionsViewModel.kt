package com.abcbank.mobile.feature.accounts.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.feature.accounts.domain.DateRange
import com.abcbank.mobile.feature.accounts.domain.Ledger
import com.abcbank.mobile.feature.accounts.domain.ObserveLedgerUseCase
import com.abcbank.mobile.feature.accounts.domain.TransactionFilter
import com.abcbank.mobile.feature.accounts.domain.TypeFilter
import dagger.hilt.android.lifecycle.HiltViewModel
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

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeLedger: ObserveLedgerUseCase,
    sessionManager: SessionManager,
    private val zone: Provider<ZoneId>
) : ViewModel() {

    // The navigation argument is external input: anything that is not a plain id means all accounts.
    private val accountId = savedStateHandle.get<String>(ARG_ACCOUNT_ID)?.takeIf(::isValidId)

    private val filter = MutableStateFlow(TransactionFilter(accountId = accountId))
    private val attempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionsUiState> = combine(
        attempt.flatMapLatest { observeLedger(filter) },
        sessionManager.balancesHidden
    ) { result, hidden ->
        when (result) {
            is DataResult.Failure -> TransactionsUiState.Error(result.error)
            is DataResult.Success -> toUiState(result.value, hidden)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        TransactionsUiState.Loading
    )

    fun onQueryChange(query: String) = filter.update {
        it.copy(query = query.take(MAX_QUERY_LENGTH))
    }

    fun onTypeSelect(type: TypeFilter) = filter.update { it.copy(type = type) }

    fun onRangeSelect(range: DateRange) = filter.update { it.copy(range = range) }

    fun onCategoryToggle(category: TransactionCategory) = filter.update {
        val categories = if (category in
            it.categories
        ) {
            it.categories - category
        } else {
            it.categories + category
        }
        it.copy(categories = categories)
    }

    fun onClearFilters() = filter.update { it.cleared() }

    fun onShowAllTime() = filter.update { it.copy(range = DateRange.ALL) }

    fun onRetry() {
        attempt.value++
    }

    private fun toUiState(ledger: Ledger, hidden: Boolean): TransactionsUiState {
        val header =
            LedgerHeader(ledger.account, ledger.filter, ledger.isOffline, ledger.typeCounts)
        if (ledger.sections.isEmpty() &&
            !ledger.hiddenByFilters
        ) {
            return TransactionsUiState.Empty(header)
        }
        val zone = zone.get()
        return TransactionsUiState.Content(
            header = header,
            sections = ledger.sections.map { section ->
                LedgerSectionItem(
                    group = section.group,
                    rows = section.transactions.map {
                        val local = it.createdAt.atZone(zone)
                        LedgerRow(it, local.toLocalDate(), local.toLocalTime())
                    }
                )
            },
            hiddenByFilters = ledger.hiddenByFilters,
            balancesHidden = hidden
        )
    }

    companion object {
        /** Navigation argument: the account to show, or absent for every account. */
        const val ARG_ACCOUNT_ID = "accountId"
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val MAX_QUERY_LENGTH = 50
        private const val MAX_ID_LENGTH = 64

        fun isValidId(id: String): Boolean =
            id.isNotBlank() && id.length <= MAX_ID_LENGTH && '/' !in id
    }
}
