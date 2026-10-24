package com.abcbank.mobile.feature.notifications.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.feature.notifications.domain.ObserveUnreadCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The bell shared by the tabs. Loads nothing the user waits on, so it has no Empty or Error
 * state: while loading or on failure the bell shows without a dot.
 */
sealed interface TabBellUiState {
    data object Loading : TabBellUiState

    data class Content(val unreadNotifications: Int) : TabBellUiState
}

/** Feeds the unread dot on every tab's top bar (MainTabs). */
@HiltViewModel
class TabBellViewModel @Inject constructor(observeUnreadCount: ObserveUnreadCountUseCase) :
    ViewModel() {
    val uiState: StateFlow<TabBellUiState> = observeUnreadCount()
        .map<Int, TabBellUiState> { TabBellUiState.Content(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            TabBellUiState.Loading
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
