package com.abcbank.mobile.feature.accounts.ui

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.feature.accounts.domain.LedgerGroup
import com.abcbank.mobile.feature.accounts.domain.TransactionFilter
import java.time.LocalDate
import java.time.LocalTime

sealed interface TransactionsUiState {
    data object Loading : TransactionsUiState

    data class Error(val error: DataError) : TransactionsUiState

    /** No transactions in the chosen period at all, before any filter. */
    data class Empty(val header: LedgerHeader) : TransactionsUiState

    data class Content(
        val header: LedgerHeader,
        val sections: List<LedgerSectionItem>,
        /** The period has transactions, but the filters hide every one. */
        val hiddenByFilters: Boolean,
        val balancesHidden: Boolean
    ) : TransactionsUiState
}

/** What the top of the ledger shows in every loaded state, so the filters stay usable. */
data class LedgerHeader(
    /** Set when the ledger was opened from one account. */
    val account: Account?,
    val filter: TransactionFilter,
    val isOffline: Boolean
)

data class LedgerSectionItem(val group: LedgerGroup, val rows: List<LedgerRow>)

data class LedgerRow(val transaction: Transaction, val date: LocalDate, val time: LocalTime)
