package com.abcbank.mobile.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/** One destination in the bottom bar. [label] is what TalkBack reads; the bar shows icons only. */
data class BottomBarItem(
    @DrawableRes val icon: Int,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

/**
 * How much room tab screens leave at the bottom so their content scrolls above the floating bar
 * (DESIGN_SYSTEM.md section 04, space-12 plus the bar height). Zero outside the tabs.
 */
val LocalBottomBarClearance = compositionLocalOf { 0.dp }

/**
 * `AbcBottomBar` from DESIGN_SYSTEM.md section 06: a floating black pill, 64dp tall, white 24dp
 * icons, and a 4dp white dot under the active destination. Place it 16dp from the screen sides and
 * 12dp above the navigation bar inset.
 */
@Composable
fun AbcBottomBar(items: List<BottomBarItem>, modifier: Modifier = Modifier) {
    val colors = AbcTheme.colors
    val dimens = AbcTheme.dimens
    val shape = AbcTheme.shapes.full
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimens.bottomBarHeight)
            .shadow(
                AbcTheme.elevation.floating,
                shape,
                ambientColor = colors.shadow,
                spotColor = colors.shadow
            )
            .clip(shape)
            .background(colors.primary)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(selected = item.selected, role = Role.Tab, onClick = item.onClick)
                    .semantics { contentDescription = item.label },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(dimens.iconLarge)
                    )
                    Spacer(Modifier.height(AbcTheme.spacing.space1))
                    Box(
                        Modifier
                            .size(dimens.bottomBarDot)
                            .clip(shape)
                            .background(if (item.selected) colors.onPrimary else Color.Transparent)
                    )
                }
            }
        }
    }
}

/** The clearance tab screens add at the bottom: space-12 plus the bar and its gap. */
@Composable
fun bottomBarClearance(): Dp = AbcTheme.spacing.space12 + AbcTheme.dimens.bottomBarHeight

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun AbcBottomBarPreview() {
    AbcTheme {
        AbcBottomBar(
            items = listOf(
                BottomBarItem(R.drawable.ic_house, "Home", selected = true, onClick = {}),
                BottomBarItem(R.drawable.ic_credit_card, "Cards", selected = false, onClick = {}),
                BottomBarItem(R.drawable.ic_user, "Profile", selected = false, onClick = {})
            ),
            modifier = Modifier.padding(AbcTheme.spacing.space4)
        )
    }
}
