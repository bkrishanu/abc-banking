package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

enum class WordmarkTone {
    /** Dots in terracotta, "abc" in primary, "bank" in text colour. Welcome screen. */
    Brand,

    /** All white, for dark card art (sage, graphite). */
    OnCard,

    /** All text colour, for the light card-peach face, as in Sample Screens 1. */
    OnLightCard
}

/** The "abcbank" wordmark with the dotted mark (DESIGN_SYSTEM.md sections 01 and 06). */
@Composable
fun AbcWordmark(
    modifier: Modifier = Modifier,
    tone: WordmarkTone = WordmarkTone.Brand,
    style: TextStyle = AbcTheme.typography.h2
) {
    val colors = AbcTheme.colors
    // The dots are a graphic, so terracotta is fine there; "abc" is text and stays in primary.
    val markColor = when (tone) {
        WordmarkTone.Brand -> colors.accent
        WordmarkTone.OnCard -> colors.onPrimary
        WordmarkTone.OnLightCard -> colors.text
    }
    val abcColor = when (tone) {
        WordmarkTone.Brand -> colors.primary
        else -> markColor
    }
    val bankColor = when (tone) {
        WordmarkTone.Brand, WordmarkTone.OnLightCard -> colors.text
        WordmarkTone.OnCard -> colors.onPrimary
    }
    val description = stringResource(R.string.cd_abc_bank_logo)
    val abc = stringResource(R.string.wordmark_abc)
    val bank = stringResource(R.string.wordmark_bank)
    // The mark scales with the text so it follows the user's font scale.
    val markSize = with(LocalDensity.current) { (style.fontSize * MARK_TO_TEXT_RATIO).toDp() }

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2)
    ) {
        AbcMark(color = markColor, modifier = Modifier.size(markSize))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = abcColor)) { append(abc) }
                withStyle(SpanStyle(color = bankColor)) { append(bank) }
            },
            style = style
        )
    }
}

/**
 * The dotted mark: rounded squares on the diagonals c + r = 3, 5, 7 of a 6 x 6 grid, growing and
 * darkening towards the top right. Same geometry as the launcher icon (DESIGN_SYSTEM.md section 07).
 */
@Composable
private fun AbcMark(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cell = size.minDimension / MARK_GRID
        MarkDots.forEach { dot ->
            val side = dot.sizeFraction * cell
            val centre = Offset((dot.column + 0.5f) * cell, (dot.row + 0.5f) * cell)
            drawRoundRect(
                color = color.copy(alpha = color.alpha * dot.alpha),
                topLeft = Offset(centre.x - side / 2, centre.y - side / 2),
                size = Size(side, side),
                cornerRadius = CornerRadius(side * MARK_CORNER_FRACTION)
            )
        }
    }
}

private class MarkDot(val column: Int, val row: Int, val sizeFraction: Float, val alpha: Float)

private const val MARK_GRID = 6
private const val MARK_CORNER_FRACTION = 0.28f
private const val MARK_TO_TEXT_RATIO = 1.2f
private val MARK_DIAGONALS = setOf(3, 5, 7)

// Keyed by column minus row: the top-right dots are largest and fully opaque.
private val SIZE_BY_OFFSET = mapOf(
    5 to 0.92f,
    3 to 0.82f,
    1 to 0.64f,
    -1 to 0.48f,
    -3 to 0.34f,
    -5 to 0.24f
)
private val ALPHA_BY_OFFSET = mapOf(
    5 to 1f,
    3 to 1f,
    1 to 0.92f,
    -1 to 0.78f,
    -3 to 0.62f,
    -5 to 0.48f
)

private val MarkDots: List<MarkDot> = buildList {
    for (row in 0 until MARK_GRID) {
        for (column in 0 until MARK_GRID) {
            if (column + row in MARK_DIAGONALS) {
                val offset = column - row
                add(
                    MarkDot(
                        column,
                        row,
                        SIZE_BY_OFFSET.getValue(offset),
                        ALPHA_BY_OFFSET.getValue(offset)
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun AbcWordmarkPreview() {
    AbcTheme {
        Column(modifier = Modifier.padding(AbcTheme.spacing.space4)) {
            AbcWordmark()
        }
    }
}
