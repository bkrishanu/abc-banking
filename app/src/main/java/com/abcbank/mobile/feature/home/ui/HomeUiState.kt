package com.abcbank.mobile.feature.home.ui

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Beneficiary
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
        /** "Everyday Checking" and its last 4 under the balance; null without a wallet account. */
        val walletName: String?,
        val walletLast4: String?,
        val walletIsLow: Boolean,
        val moneyIn: Money,
        val moneyOut: Money,
        val balancesHidden: Boolean,
        val accounts: List<HomeAccountItem>,
        /** Shown when there is no investment account, as for Marcus (PRD feature 5). */
        val showNoInvestments: Boolean,
        val latestTransactions: List<HomeTransactionItem>,
        val quickPayees: List<Beneficiary>,
        val travelNotice: HomeTravelNotice?,
        val isOffline: Boolean
    ) : HomeUiState
}

/** An account with its dates already in the device time zone. */
data class HomeAccountItem(
    val account: Account,
    val isLowBalance: Boolean,
    val dueDate: LocalDate? = null,
    /** Days from today to [dueDate]; null when there is none or it has passed. */
    val dueInDays: Long? = null,
    val maturesOn: LocalDate? = null
)

/** The active travel notice, with its days in the device time zone. */
data class HomeTravelNotice(val destination: String, val start: LocalDate, val end: LocalDate)

enum class TransactionDay { TODAY, YESTERDAY, EARLIER }

data class HomeTransactionItem(
    val transaction: Transaction,
    val day: TransactionDay,
    val date: LocalDate,
    val time: LocalTime
)
