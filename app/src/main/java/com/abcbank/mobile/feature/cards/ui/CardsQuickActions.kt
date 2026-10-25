package com.abcbank.mobile.feature.cards.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.component.QuickActionButton
import com.abcbank.mobile.core.designsystem.component.QuickActionRow
import com.abcbank.mobile.core.designsystem.component.QuickTransferRow
import com.abcbank.mobile.core.designsystem.component.SectionHeader
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * The row under the card stack: Send, Deposit and More quick actions, then Quick Transfer with the
 * recent payees (tap one to send them money) and an Add button for a new payee.
 */
@Composable
internal fun CardsQuickActions(state: CardsUiState.Content, navigation: CardsNavigation) {
    val spacing = AbcTheme.spacing
    QuickActionRow {
        QuickActionButton(
            label = stringResource(R.string.quick_action_send),
            icon = R.drawable.ic_send,
            onClick = navigation.onSend,
            emphasized = true,
            offline = state.isOffline
        )
        QuickActionButton(
            label = stringResource(R.string.quick_action_deposit),
            icon = R.drawable.ic_landmark,
            onClick = navigation.onDeposit,
            offline = state.isOffline
        )
        QuickActionButton(
            label = stringResource(R.string.quick_action_more),
            icon = R.drawable.ic_layout_grid,
            onClick = navigation.onMore
        )
    }
    Spacer(Modifier.height(spacing.space4))
    SectionHeader(
        title = stringResource(R.string.quick_transfer),
        actionLabel = stringResource(R.string.action_view_all),
        onAction = navigation.onSend
    )
    QuickTransferRow(
        payees = state.quickPayees,
        onPayee = navigation.onQuickTransfer,
        onAddPayee = navigation.onAddPayee
    )
}
