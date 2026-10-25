package com.abcbank.mobile.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.TermDepositAccount
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.feature.home.domain.HomeSummary
import com.abcbank.mobile.feature.home.domain.ObserveHomeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHome: ObserveHomeUseCase,
    private val sessionManager: SessionManager,
    private val clock: Clock,
    private val zone: Provider<ZoneId>
) : ViewModel() {

    // Bumped by Retry to start a fresh Firestore subscription.
    private val attempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        attempt.flatMapLatest { observeHome() },
        sessionManager.balancesHidden
    ) { result, hidden ->
        when (result) {
            is DataResult.Failure -> HomeUiState.Error(result.error)
            is DataResult.Success -> toUiState(result.value, hidden)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState.Loading)

    fun onToggleBalances() = sessionManager.toggleBalancesHidden()

    fun onRetry() {
        attempt.value++
    }

    private fun toUiState(summary: HomeSummary, hidden: Boolean): HomeUiState {
        if (summary.accounts.isEmpty()) {
            return HomeUiState.Empty(summary.personaId, summary.firstName)
        }
        val zone = zone.get()
        val today = clock.now().atZone(zone).toLocalDate()
        return HomeUiState.Content(
            personaId = summary.personaId,
            firstName = summary.firstName,
            unreadNotifications = summary.unreadNotifications,
            walletBalance = summary.walletBalance,
            walletName = summary.walletAccount?.nickname,
            walletLast4 = summary.walletAccount?.last4,
            walletIsLow = summary.walletIsLow,
            moneyIn = summary.moneyIn,
            moneyOut = summary.moneyOut,
            balancesHidden = hidden,
            accounts = summary.accounts.map { item ->
                val dueDate = (item.account as? CreditCardAccount)?.dueDate?.localDate(zone)
                HomeAccountItem(
                    account = item.account,
                    isLowBalance = item.isLowBalance,
                    dueDate = dueDate,
                    dueInDays = dueDate?.let { ChronoUnit.DAYS.between(today, it) }
                        ?.takeIf { it >= 0 },
                    maturesOn = (item.account as? TermDepositAccount)?.maturesAt?.localDate(zone)
                )
            },
            showNoInvestments = !summary.hasInvestments,
            latestTransactions = summary.latestTransactions.map { it.toItem(zone, today) },
            travelNotice = summary.activeTravelNotice?.let {
                HomeTravelNotice(
                    it.destination,
                    it.startDate.localDate(zone),
                    it.endDate.localDate(zone)
                )
            },
            isOffline = summary.isOffline
        )
    }

    private fun Transaction.toItem(zone: ZoneId, today: LocalDate): HomeTransactionItem {
        val local = createdAt.atZone(zone)
        val date = local.toLocalDate()
        val day = when (date) {
            today -> TransactionDay.TODAY
            today.minusDays(1) -> TransactionDay.YESTERDAY
            else -> TransactionDay.EARLIER
        }
        return HomeTransactionItem(this, day, date, local.toLocalTime())
    }

    private fun Instant.localDate(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
