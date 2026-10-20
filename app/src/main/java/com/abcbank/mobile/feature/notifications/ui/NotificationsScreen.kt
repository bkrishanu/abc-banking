package com.abcbank.mobile.feature.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.FilterChipRow
import com.abcbank.mobile.core.designsystem.component.FlowScaffold
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.SectionHeader
import com.abcbank.mobile.core.designsystem.component.SkeletonBlock
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.notifications.domain.DatedNotification
import com.abcbank.mobile.feature.notifications.domain.NotificationDay
import com.abcbank.mobile.feature.notifications.domain.NotificationKind

@Composable
fun NotificationsRoute(onBack: () -> Unit, viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NotificationsScreen(
        state = state,
        onBack = onBack,
        onKindSelect = viewModel::onKindSelect,
        onOpen = viewModel::onOpen,
        onMarkAllRead = viewModel::onMarkAllRead,
        onRetry = viewModel::onRetry
    )
}

private val KINDS = listOf(NotificationKind.TRANSACTIONAL, NotificationKind.PROMOTIONAL)

/**
 * The notification center (MobileBuild.md 6): Transactional and Promotional, newest first, unread
 * marked with a dot. Opening one marks it read; Mark All as Read clears the selected kind.
 */
@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onKindSelect: (NotificationKind) -> Unit,
    onOpen: (DatedNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = AbcTheme.spacing
    FlowScaffold(
        step = null,
        totalSteps = null,
        title = stringResource(R.string.notifications_title),
        onBack = onBack,
        modifier = modifier
    ) {
        when (state) {
            NotificationsUiState.Loading -> Column(
                verticalArrangement = Arrangement.spacedBy(spacing.space3)
            ) {
                repeat(PLACEHOLDER_ROWS) {
                    SkeletonBlock(
                        Modifier
                            .fillMaxWidth()
                            .height(AbcTheme.dimens.textFieldHeight),
                        shape = AbcTheme.shapes.lg
                    )
                }
            }
            is NotificationsUiState.Error -> ErrorState(
                message = stringResource(
                    if (state.error ==
                        DataError.OFFLINE
                    ) {
                        R.string.state_offline_no_data
                    } else {
                        R.string.notifications_error
                    }
                ),
                onRetry = onRetry
            )
            is NotificationsUiState.Content -> {
                if (state.isOffline) {
                    OfflineBanner()
                    Spacer(Modifier.height(spacing.space4))
                }
                FilterChipRow(
                    options = KINDS.map { kindLabel(it, state.unread[it] ?: 0) },
                    selectedIndex = KINDS.indexOf(state.kind),
                    onSelect = { onKindSelect(KINDS[it]) }
                )
                Spacer(Modifier.height(spacing.space2))
                if (state.items.isEmpty()) {
                    Spacer(Modifier.height(spacing.space4))
                    Text(
                        text = stringResource(
                            if (state.kind ==
                                NotificationKind.TRANSACTIONAL
                            ) {
                                R.string.notifications_empty_transactional
                            } else {
                                R.string.notifications_empty_promotional
                            }
                        ),
                        style = AbcTheme.typography.body,
                        color = AbcTheme.colors.textMuted
                    )
                    return@FlowScaffold
                }
                SectionHeader(
                    title = stringResource(
                        if (state.kind ==
                            NotificationKind.TRANSACTIONAL
                        ) {
                            R.string.notifications_alerts
                        } else {
                            R.string.notifications_offers
                        }
                    ),
                    actionLabel = if (state.hasUnread) {
                        stringResource(
                            R.string.action_mark_all_read
                        )
                    } else {
                        null
                    },
                    onAction = onMarkAllRead
                )
                ContentCard(padding = spacing.space2) {
                    state.items.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = AbcTheme.colors.border)
                        NotificationRow(item, onOpen)
                    }
                }
            }
        }
    }
}

private const val PLACEHOLDER_ROWS = 4

@Composable
private fun NotificationRow(item: DatedNotification, onOpen: (DatedNotification) -> Unit) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    val notification = item.notification
    val unreadText = stringResource(R.string.cd_unread)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AbcTheme.shapes.md)
            .clickable(role = Role.Button) { onOpen(item) }
            .padding(spacing.space3)
            .semantics(mergeDescendants = true) {
                if (!notification.read) stateDescription = unreadText
            },
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .padding(top = spacing.space2)
                .size(AbcTheme.dimens.unreadDot)
                .clip(AbcTheme.shapes.full)
                .background(if (notification.read) colors.surface else colors.accent)
        )
        Spacer(Modifier.width(spacing.space3))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
            Text(
                text = notification.title,
                style = AbcTheme.typography.body.copy(
                    fontWeight = if (notification.read) FontWeight.Normal else FontWeight.SemiBold
                ),
                color = colors.text
            )
            Text(notification.body, style = AbcTheme.typography.small, color = colors.textMuted)
            Text(timeLabel(item), style = AbcTheme.typography.small, color = colors.textMuted)
        }
    }
}

@Composable
private fun timeLabel(item: DatedNotification): String {
    val time = DateFormatter.time(item.time)
    return when (item.day) {
        NotificationDay.TODAY -> stringResource(R.string.notifications_today, time)
        NotificationDay.YESTERDAY -> stringResource(R.string.notifications_yesterday, time)
        NotificationDay.EARLIER -> DateFormatter.shortDate(item.date)
    }
}

@Composable
private fun kindLabel(kind: NotificationKind, unread: Int): String {
    val name = stringResource(
        if (kind ==
            NotificationKind.TRANSACTIONAL
        ) {
            R.string.notifications_transactional
        } else {
            R.string.notifications_promotional
        }
    )
    return if (unread >
        0
    ) {
        stringResource(R.string.notifications_kind_unread, name, unread)
    } else {
        name
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun NotificationsPreview() {
    AbcTheme {
        NotificationsScreen(
            state = NotificationsPreviewData.samanta(),
            onBack = {},
            onKindSelect = {},
            onOpen = {},
            onMarkAllRead = {},
            onRetry = {}
        )
    }
}
