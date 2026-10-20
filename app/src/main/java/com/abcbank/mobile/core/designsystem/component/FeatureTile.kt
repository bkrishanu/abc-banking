package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * `FeatureTile` from DESIGN_SYSTEM.md section 06 (Sample 2, Features carousel): 140dp wide, at
 * least 120dp tall, radius-lg, surface, an illustration at the top (a 48dp tinted circle with a
 * Lucide icon) and the Body title at the bottom. The whole tile is one button.
 */
@Composable
fun FeatureTile(
    title: String,
    icon: Int,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(TILE_WIDTH)
            .heightIn(min = TILE_HEIGHT)
            .clip(AbcTheme.shapes.lg)
            .background(AbcTheme.colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(AbcTheme.spacing.space3),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(ILLUSTRATION)
                .clip(AbcTheme.shapes.full)
                .background(tint)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = AbcTheme.colors.text,
                modifier = Modifier.size(AbcTheme.dimens.iconLarge)
            )
        }
        Spacer(Modifier.size(AbcTheme.spacing.space2))
        Text(title, style = AbcTheme.typography.body, color = AbcTheme.colors.text)
    }
}

private val TILE_WIDTH = 140.dp
private val TILE_HEIGHT = 120.dp
private val ILLUSTRATION = 48.dp

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun FeatureTilePreview() {
    AbcTheme {
        Row(
            Modifier.padding(AbcTheme.spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space3)
        ) {
            val colors = AbcTheme.colors
            FeatureTile("Instant Bank Transfers", R.drawable.ic_arrow_up_right, colors.accent, {})
            FeatureTile("Spending Analytics", R.drawable.ic_chart_column, colors.positive, {})
        }
    }
}
