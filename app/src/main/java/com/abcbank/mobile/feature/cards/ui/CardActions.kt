package com.abcbank.mobile.feature.cards.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.component.QuickActionButton
import com.abcbank.mobile.core.designsystem.component.QuickActionRow

/**
 * The row under the card stack (Stitch Cards): Details or Hide, Freeze or Unfreeze, Limits and
 * Wallet for the selected card. A filled circle marks the state that is on (shown, frozen, in
 * wallet). Freezing is confirmed by the screen; Limits scrolls to the limits section.
 */
@Composable
internal fun CardActions(
    state: CardsUiState.Content,
    onToggleDetails: () -> Unit,
    onFreezeToggle: () -> Unit,
    onLimits: () -> Unit,
    onAddToWallet: () -> Unit
) {
    val revealed = state.revealed != null
    val frozen = state.selected.card.frozen
    QuickActionRow {
        QuickActionButton(
            label = stringResource(
                if (revealed) R.string.card_action_hide else R.string.card_action_details
            ),
            icon = if (revealed) R.drawable.ic_eye_off else R.drawable.ic_eye,
            onClick = onToggleDetails,
            emphasized = revealed
        )
        QuickActionButton(
            label = stringResource(
                if (frozen) R.string.action_unfreeze else R.string.action_freeze
            ),
            icon = R.drawable.ic_snowflake,
            onClick = onFreezeToggle,
            emphasized = frozen
        )
        QuickActionButton(
            label = stringResource(R.string.card_action_limits),
            icon = R.drawable.ic_sliders_horizontal,
            onClick = onLimits
        )
        QuickActionButton(
            label = stringResource(
                if (state.walletAdded) {
                    R.string.card_action_in_wallet
                } else {
                    R.string.card_action_wallet
                }
            ),
            icon = R.drawable.ic_wallet,
            onClick = onAddToWallet,
            emphasized = state.walletAdded
        )
    }
}
