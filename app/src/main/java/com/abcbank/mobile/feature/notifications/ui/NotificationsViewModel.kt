package com.abcbank.mobile.feature.notifications.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.feature.notifications.domain.DatedNotification
import com.abcbank.mobile.feature.notifications.domain.MarkNotificationsReadUseCase
import com.abcbank.mobile.feature.notifications.domain.NotificationKind
import com.abcbank.mobile.feature.notifications.domain.ObserveNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState

    data class Error(val error: DataError) : NotificationsUiState

    /** [items] is empty when the selected kind has nothing yet; the screen shows its empty line. */
    data class Content(
        val kind: NotificationKind,
        val items: List<DatedNotification>,
        val unread: Map<NotificationKind, Int>,
        val isOffline: Boolean
    ) : NotificationsUiState {
        val hasUnread: Boolean get() = (unread[kind] ?: 0) > 0
    }
}

/** The notification center (MobileBuild.md 6, PRD feature 13). */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    observeNotifications: ObserveNotificationsUseCase,
    private val markRead: MarkNotificationsReadUseCase
) : ViewModel() {
    private val kind = MutableStateFlow(NotificationKind.TRANSACTIONAL)
    private val attempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NotificationsUiState> = combine(
        attempt.flatMapLatest { observeNotifications() },
        kind
    ) { result, selected ->
        when (result) {
            is DataResult.Failure -> NotificationsUiState.Error(result.error)
            is DataResult.Success -> NotificationsUiState.Content(
                kind = selected,
                items = result.value.byKind[selected].orEmpty(),
                unread = result.value.unread,
                isOffline = result.value.isFromCache
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        NotificationsUiState.Loading
    )

    fun onKindSelect(selected: NotificationKind) {
        kind.value = selected
    }

    /** Opening an alert marks it read. */
    fun onOpen(item: DatedNotification) {
        viewModelScope.launch { markRead(listOf(item.notification)) }
    }

    fun onMarkAllRead() {
        val content = uiState.value as? NotificationsUiState.Content ?: return
        viewModelScope.launch { markRead(content.items.map { it.notification }) }
    }

    fun onRetry() {
        attempt.value++
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
