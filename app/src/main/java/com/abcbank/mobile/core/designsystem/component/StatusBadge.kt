package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/** Neutral for states like Pending or Scheduled; Warning and Danger for Disputed and Frozen. */
enum class BadgeTone { Neutral, Warning, Danger }

/**
 * `StatusBadge` from DESIGN_SYSTEM.md section 06: a tone dot and a Small label in a radius-full
 * tinted pill. The label stays in text colour, because warning text on its own tint fails AA; the
 * fill is opaque so the badge stays readable on card art.
 */
@Composable
fun StatusBadge(text: String, modifier: Modifier = Modifier, tone: BadgeTone = BadgeTone.Neutral) {
    val colors = AbcTheme.colors
    val (fill, dot) = when (tone) {
        BadgeTone.Neutral -> colors.surfaceMuted to colors.textMuted
        BadgeTone.Warning -> colors.warningTint to colors.warning
        BadgeTone.Danger -> colors.dangerTint to colors.danger
    }
    Row(
        modifier = modifier
            .clip(AbcTheme.shapes.full)
            .background(fill)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = AbcTheme.spacing.space2, vertical = AbcTheme.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space1)
    ) {
        Box(
            Modifier
                .size(AbcTheme.dimens.statusDot)
                .clip(AbcTheme.shapes.full)
                .background(dot)
        )
        Text(text = text, style = AbcTheme.typography.small, color = colors.text)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF8F5)
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
