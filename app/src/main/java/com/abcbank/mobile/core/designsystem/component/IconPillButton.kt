package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * `IconPillButton` from DESIGN_SYSTEM.md section 06 (Sample 1: Deposit, Send, More): a primary
 * pill with a white icon in a white-outlined square and a Small 500 label. Drawn 40dp tall inside
 * a 48dp touch target.
 */
@Composable
fun IconPillButton(label: String, icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AbcTheme.colors
    val spacing = AbcTheme.spacing
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = AbcTheme.dimens.touchTarget)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = PILL_HEIGHT)
                .clip(AbcTheme.shapes.full)
                .background(colors.primary)
                .padding(start = spacing.space2, end = spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(ICON_BOX)
                    .border(AbcTheme.dimens.borderWidth, colors.onPrimary, AbcTheme.shapes.sm)
            ) {
                Icon(
                    painterResource(icon),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(AbcTheme.dimens.iconSmall)
                )
            }
            Text(
                label,
                style = AbcTheme.typography.small.copy(fontWeight = FontWeight.Medium),
                color = colors.onPrimary
            )
        }
    }
}

private val PILL_HEIGHT = 40.dp
private val ICON_BOX = 24.dp

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun IconPillButtonPreview() {
    AbcTheme {
        Row(
            Modifier.padding(AbcTheme.spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2)
        ) {
            IconPillButton("Deposit", R.drawable.ic_plus, {})
            IconPillButton("Send", R.drawable.ic_arrow_up, {})
            IconPillButton("More", R.drawable.ic_layout_grid, {})
        }
    }
}
