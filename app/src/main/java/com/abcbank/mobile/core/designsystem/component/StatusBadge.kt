package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/** Neutral for states like Pending or Scheduled; Warning and Danger for Disputed and Frozen. */
enum class BadgeTone { Neutral, Warning, Danger }

/**
 * `StatusBadge` from DESIGN_SYSTEM.md section 06: a Small label in a radius-full pill. The tone is
 * in the text colour and a matching outline, so it never relies on fill colour alone.
 */
@Composable
fun StatusBadge(text: String, modifier: Modifier = Modifier, tone: BadgeTone = BadgeTone.Neutral) {
    val colors = AbcTheme.colors
    val color = when (tone) {
        BadgeTone.Neutral -> colors.textMuted
        BadgeTone.Warning -> colors.warning
        BadgeTone.Danger -> colors.danger
    }
    val shape = AbcTheme.shapes.full
    Text(
        text = text,
        style = AbcTheme.typography.small,
        color = color,
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(AbcTheme.dimens.borderWidth, color, shape)
            .padding(horizontal = AbcTheme.spacing.space2, vertical = AbcTheme.spacing.space1)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun StatusBadgePreview() {
    AbcTheme {
        Row(
            modifier = Modifier.padding(AbcTheme.spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2)
        ) {
            StatusBadge("Pending")
            StatusBadge("Disputed", tone = BadgeTone.Warning)
            StatusBadge("Frozen", tone = BadgeTone.Danger)
        }
    }
}
