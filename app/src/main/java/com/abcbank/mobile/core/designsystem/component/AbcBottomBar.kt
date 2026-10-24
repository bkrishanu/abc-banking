package com.abcbank.mobile.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/** One destination in the bottom bar: its icon and the label shown under it. */
data class BottomBarItem(
    @DrawableRes val icon: Int,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

/**
 * How much room tab screens leave at the bottom so their content scrolls above the bar and the
 * floating Transfer button (DESIGN_SYSTEM.md section 04). Zero outside the tabs.
 */
val LocalBottomBarClearance = compositionLocalOf { 0.dp }

/**
 * `AbcBottomBar` from DESIGN_SYSTEM.md section 06 (Stitch redesign, 2026-10-24): docked to the
 * bottom edge on a background-coloured surface with a hairline top border, four destinations with
 * a 24dp icon over a Label. The selected one is primary and semibold, the others textMuted.
 * It pads itself for the navigation bar inset.
 */
@Composable
fun AbcBottomBar(items: List<BottomBarItem>, modifier: Modifier = Modifier) {
    val colors = AbcTheme.colors
    val dimens = AbcTheme.dimens
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        HorizontalDivider(color = colors.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .heightIn(min = dimens.bottomBarHeight)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val tint = if (item.selected) colors.primary else colors.textMuted
                val weight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = dimens.bottomBarHeight)
                        .selectable(
                            selected = item.selected,
                            role = Role.Tab,
                            onClick = item.onClick
                        )
                        .semantics(mergeDescendants = true) {}
                        .padding(vertical = AbcTheme.spacing.space2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(dimens.iconLarge)
                    )
                    Text(
                        text = item.label,
                        style = AbcTheme.typography.label.copy(fontWeight = weight),
                        color = tint,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * The floating Transfer button over the bottom bar (DESIGN_SYSTEM.md section 06): a primary pill
 * with the `arrow-left-right` icon and "Transfer", opening Send Money. Centred, space-3 above the bar.
 */
@Composable
fun TransferButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AbcTheme.colors
    val dimens = AbcTheme.dimens
    val shape = AbcTheme.shapes.full
    Row(
        modifier = modifier
            .heightIn(min = dimens.buttonHeight)
            .shadow(
                AbcTheme.elevation.floating,
                shape,
                ambientColor = colors.shadow,
                spotColor = colors.shadow
            )
            .clip(shape)
            .background(colors.primary)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = AbcTheme.spacing.space6),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_left_right),
            contentDescription = null,
            tint = colors.onPrimary,
            modifier = Modifier.size(dimens.iconMedium)
        )
        Spacer(Modifier.width(AbcTheme.spacing.space2))
        Text(
            text = stringResource(R.string.tab_transfer),
            style = AbcTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onPrimary
        )
    }
}

/**
 * The clearance tab screens add at the bottom: the bar, the Transfer button above it and a
 * space-6 gap, so the last row of content can scroll clear of both.
 */
@Composable
fun bottomBarClearance(): Dp =
    AbcTheme.dimens.bottomBarHeight + AbcTheme.spacing.space3 + AbcTheme.dimens.buttonHeight +
        AbcTheme.spacing.space6

@Preview(showBackground = true, backgroundColor = 0xFFFAF8F5)
@Composable
private fun AbcBottomBarPreview() {
    AbcTheme {
        Box(contentAlignment = Alignment.BottomCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TransferButton(onClick = {})
                Spacer(Modifier.size(AbcTheme.spacing.space3))
                val tabs = listOf(
                    R.drawable.ic_house to "Home",
                    R.drawable.ic_credit_card to "Cards",
                    R.drawable.ic_chart_column to "Insights",
                    R.drawable.ic_user to "Profile"
                )
                AbcBottomBar(
                    items = tabs.mapIndexed { index, (icon, label) ->
                        BottomBarItem(icon, label, selected = index == 0, onClick = {})
                    }
                )
            }
        }
    }
}
