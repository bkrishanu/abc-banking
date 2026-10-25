package com.abcbank.mobile.feature.cards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.InitialsAvatar
import com.abcbank.mobile.core.designsystem.component.QuickActionButton
import com.abcbank.mobile.core.designsystem.component.QuickActionRow
import com.abcbank.mobile.core.designsystem.component.SectionHeader
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.Beneficiary

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
        title = stringResource(R.string.cards_quick_transfer),
        actionLabel = stringResource(R.string.action_view_all),
        onAction = navigation.onSend
    )
    ContentCard(padding = spacing.space3) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            state.quickPayees.forEach { payee ->
                QuickPayee(payee) { navigation.onQuickTransfer(payee.id) }
            }
            AddPayee(navigation.onAddPayee)
        }
    }
}

@Composable
private fun QuickPayee(payee: Beneficiary, onClick: () -> Unit) {
    val description = stringResource(R.string.cd_quick_transfer_to, payee.name)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = AbcTheme.dimens.avatarLarge, max = AbcTheme.dimens.avatarLarge)
            .clip(AbcTheme.shapes.md)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
            }
            .padding(vertical = AbcTheme.spacing.space1)
    ) {
        InitialsAvatar(payee.name, size = AbcTheme.dimens.iconCircle)
        Spacer(Modifier.height(AbcTheme.spacing.space1))
        Text(
            text = payee.shortName,
            style = AbcTheme.typography.small,
            color = AbcTheme.colors.text,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddPayee(onClick: () -> Unit) {
    val description = stringResource(R.string.cd_add_payee)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = AbcTheme.dimens.avatarLarge)
            .clip(AbcTheme.shapes.md)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
            }
            .padding(vertical = AbcTheme.spacing.space1)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AbcTheme.dimens.iconCircle)
                .clip(AbcTheme.shapes.full)
                .background(AbcTheme.colors.accent)
        ) {
            Icon(
                painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = AbcTheme.colors.text,
                modifier = Modifier.size(AbcTheme.dimens.iconLarge)
            )
        }
        Spacer(Modifier.height(AbcTheme.spacing.space1))
        Text(
            stringResource(R.string.cards_add_payee),
            style = AbcTheme.typography.small,
            color = AbcTheme.colors.text
        )
    }
}
