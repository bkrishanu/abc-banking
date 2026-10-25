package com.abcbank.mobile.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * `QuickActionButton` from DESIGN_SYSTEM.md section 06 (Stitch Home: Send, Pay Bills, Deposit,
 * More): a 56dp round icon button with a Small label below. [emphasized] fills the circle with
 * primary for the main action; the rest are tonal. [offline] adds a small wifi-off badge as a cue;
 * the action itself stays available and the flow it opens deals with the connection.
 */
@Composable
fun QuickActionButton(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    offline: Boolean = false
) {
    val colors = AbcTheme.colors
    val dimens = AbcTheme.dimens
    val offlineLabel = stringResource(R.string.quick_action_offline)
    Column(
        // At least the minimum width, and never narrower than the label's longest word, so a
        // label wraps between words at large font scales.
        modifier = modifier
            .widthIn(min = dimens.quickActionMinWidth)
            .width(IntrinsicSize.Min)
            .clip(AbcTheme.shapes.md)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                if (offline) stateDescription = offlineLabel
            }
            .padding(vertical = AbcTheme.spacing.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2)
    ) {
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(dimens.quickActionCircle)
                    .clip(AbcTheme.shapes.full)
                    .background(if (emphasized) colors.primary else colors.surfaceMuted)
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = if (emphasized) colors.onPrimary else colors.text,
                    modifier = Modifier.size(dimens.iconLarge)
                )
            }
            if (offline) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = AbcTheme.spacing.space1, y = -AbcTheme.spacing.space1)
                        .size(dimens.offlineBadge)
                        .clip(AbcTheme.shapes.full)
                        .background(colors.accentTint)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wifi_off),
                        contentDescription = null,
                        tint = colors.accentDeep,
                        modifier = Modifier.size(dimens.iconSmall)
                    )
                }
            }
        }
        Text(
            text = label,
            style = AbcTheme.typography.small,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * A row of [QuickActionButton]s, spread across the width. At large font scales the buttons grow
 * with their labels and wrap onto a second row instead of squeezing the words.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionRow(modifier: Modifier = Modifier, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space3),
        content = content
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF8F5)
@Composable
private fun QuickActionButtonPreview() {
    AbcTheme {
        Column(
            Modifier.padding(AbcTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space4)
        ) {
            QuickActionRow {
                QuickActionButton("Send", R.drawable.ic_send, {}, emphasized = true)
                QuickActionButton("Pay Bills", R.drawable.ic_receipt, {})
                QuickActionButton("Deposit", R.drawable.ic_landmark, {})
                QuickActionButton("More", R.drawable.ic_layout_grid, {})
            }
            QuickActionRow {
                QuickActionButton("Send", R.drawable.ic_send, {}, emphasized = true, offline = true)
                QuickActionButton("Pay Bills", R.drawable.ic_receipt, {}, offline = true)
                QuickActionButton("Deposit", R.drawable.ic_landmark, {}, offline = true)
                QuickActionButton("More", R.drawable.ic_layout_grid, {})
            }
        }
    }
}
