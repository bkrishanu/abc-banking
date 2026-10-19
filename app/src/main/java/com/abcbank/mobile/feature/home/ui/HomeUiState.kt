package com.abcbank.mobile.feature.home.ui

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.model.Transaction
import java.time.LocalDate
import java.time.LocalTime

sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** Signed in but no accounts: one line and Explore products (DESIGN_SYSTEM.md section 06). */
    data class Empty(val personaId: PersonaId, val firstName: String) : HomeUiState

    data class Error(val error: DataError) : HomeUiState

    data class Content(
        val personaId: PersonaId,
        val firstName: String,
        val unreadNotifications: Int,
        val walletBalance: Money,
        val moneyIn: Money,
        val moneyOut: Money,
        val balancesHidden: Boolean,
        val accounts: List<HomeAccountItem>,
        /** Shown when there is no investment account, as for Marcus (PRD feature 5). */
        val showNoInvestments: Boolean,
        val latestTransactions: List<HomeTransactionItem>,
        val isOffline: Boolean
    ) : HomeUiState
}

/** An account with its dates already in the device time zone. */
data class HomeAccountItem(
    val account: Account,
    val isLowBalance: Boolean,
    val dueDate: LocalDate? = null,
    val maturesOn: LocalDate? = null
)

enum class TransactionDay { TODAY, YESTERDAY, EARLIER }

data class HomeTransactionItem(
    val transaction: Transaction,
    val day: TransactionDay,
    val date: LocalDate,
    val time: LocalTime
)
