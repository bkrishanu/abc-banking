package com.abcbank.mobile.feature.insights.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.feature.insights.domain.ObserveInsightsUseCase
import com.abcbank.mobile.feature.insights.domain.SavingsSummary
import com.abcbank.mobile.feature.insights.domain.SpendingPeriod
import com.abcbank.mobile.feature.insights.domain.SpendingSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

sealed interface InsightsUiState {
    data object Loading : InsightsUiState

    /** No spending in any period and no savings goal. */
    data object Empty : InsightsUiState

    data class Error(val error: DataError) : InsightsUiState

    data class Content(
        val period: SpendingPeriod,
        val spending: SpendingSummary,
        val savings: SavingsSummary?,
        /** The point the savings tooltip shows; the latest until one is tapped. */
        val selectedPoint: Int,
        val balancesHidden: Boolean,
        val isOffline: Boolean
    ) : InsightsUiState
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    observeInsights: ObserveInsightsUseCase,
    sessionManager: SessionManager
) : ViewModel() {
    private val period = MutableStateFlow(SpendingPeriod.LAST_30_DAYS)
    private val selectedPoint = MutableStateFlow<Int?>(null)
    private val attempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InsightsUiState> = combine(
        attempt.flatMapLatest { observeInsights() },
        period,
        selectedPoint,
        sessionManager.balancesHidden
    ) { result, selectedPeriod, point, hidden ->
        when (result) {
            is DataResult.Failure -> InsightsUiState.Error(result.error)
            is DataResult.Success -> {
                val overview = result.value
                val nothing =
                    overview.savings == null &&
                        overview.spending.values.all { !it.total.isPositive }
                if (nothing) {
                    InsightsUiState.Empty
                } else {
                    val savings = overview.savings
                    InsightsUiState.Content(
                        period = selectedPeriod,
                        spending = overview.spending.getValue(selectedPeriod),
                        savings = savings,
                        selectedPoint = point ?: savings?.points?.lastIndex ?: 0,
                        balancesHidden = hidden,
                        isOffline = overview.isFromCache
                    )
                }
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        InsightsUiState.Loading
    )

    fun onPeriodSelect(selected: SpendingPeriod) {
        period.value = selected
    }

    fun onPointSelect(index: Int) {
        selectedPoint.value = index
    }

    fun onRetry() {
        attempt.value++
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
