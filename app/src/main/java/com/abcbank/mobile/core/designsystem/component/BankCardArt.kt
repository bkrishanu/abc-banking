package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.BANK_CARD_ASPECT_RATIO
import com.abcbank.mobile.core.designsystem.theme.CardGradient
import kotlin.math.roundToInt

/** The fluid artwork on a card face (DESIGN_SYSTEM.md section 02). */
enum class CardArtPattern {
    /** Broad folded ribbons sweeping down the left side: card-peach, as in Sample Screens 1. */
    Ribbons,

    /** Layered waves across the lower half: card-sage and card-graphite. */
    Waves
}

/**
 * A bank card face: the gradient with fluid artwork drawn in Canvas from the card's colour tokens,
 * an optional chip, and an optional scrim fading up from the bottom so white text stays readable
 * (not needed on the light peach face, whose text is dark). BankCardView builds on this; the
 * Welcome screen uses it with the wordmark. Decorative: put text and descriptions in [content].
 */
@Composable
fun BankCardArt(
    gradient: CardGradient,
    modifier: Modifier = Modifier,
    pattern: CardArtPattern = CardArtPattern.Waves,
    showChip: Boolean = false,
    scrim: Boolean = true,
    growToFit: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val scrimColor = AbcTheme.colors.cardScrim
    Box(
        modifier = modifier
            .then(
                if (growToFit) {
                    Modifier.minAspectRatio(
                        BANK_CARD_ASPECT_RATIO
                    )
                } else {
                    Modifier.aspectRatio(BANK_CARD_ASPECT_RATIO)
                }
            )
            .clip(AbcTheme.shapes.lg),
        // When growing, content gets the card's height as its minimum, so it can fill the face.
        propagateMinConstraints = growToFit
    ) {
        // The artwork is softened like the fluid 3D art in the samples (Android 12+; older devices
        // show it crisp). The chip and scrim stay sharp on a layer above.
        Canvas(
            Modifier
                .matchParentSize()
                .blur(AbcTheme.dimens.cardArtBlur, BlurredEdgeTreatment.Rectangle)
        ) {
            when (pattern) {
                CardArtPattern.Ribbons -> drawRibbons(gradient)
                CardArtPattern.Waves -> drawWaves(gradient)
            }
        }
        Canvas(Modifier.matchParentSize()) {
            if (showChip) drawChip(gradient)
            if (scrim) {
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, scrimColor)))
            }
        }
        content()
    }
}

/**
 * At least the card's proportions, taller when the content needs it (a card face at 200% font
 * scale). The usual aspectRatio would clip that content instead.
 */
private fun Modifier.minAspectRatio(ratio: Float): Modifier = layout { measurable, constraints ->
    val minHeight = (constraints.maxWidth / ratio).roundToInt().coerceAtMost(constraints.maxHeight)
    val placeable = measurable.measure(constraints.copy(minHeight = minHeight))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

// Shapes are laid out in fractions of the card so they scale with any card width.

private fun DrawScope.drawRibbons(g: CardGradient) {
    val w = size.width
    val h = size.height
    drawRect(
        Brush.linearGradient(
            listOf(g.highlight, g.start, g.base),
            start = Offset.Zero,
            end = Offset(w, h)
        )
    )
    // Back ribbon: a broad band from the top edge down to the left edge.
    val back = Path().apply {
        moveTo(0.30f * w, 0f)
        cubicTo(0.20f * w, 0.25f * h, 0.10f * w, 0.35f * h, 0f, 0.55f * h)
        lineTo(0f, h)
        cubicTo(0.25f * w, 0.78f * h, 0.46f * w, 0.36f * h, 0.64f * w, 0f)
        close()
    }
    drawPath(
        back,
        Brush.linearGradient(
            listOf(g.end, g.base, g.end),
            start = Offset(0.64f * w, 0f),
            end = Offset(0f, h)
        ),
        alpha = RIBBON_ALPHA
    )
    // Front fold: narrower, lit on top and shadowed below, so the ribbon reads as turning over.
    val fold = Path().apply {
        moveTo(0.42f * w, 0f)
        cubicTo(0.30f * w, 0.30f * h, 0.36f * w, 0.58f * h, 0.16f * w, h)
        lineTo(0.38f * w, h)
        cubicTo(0.56f * w, 0.62f * h, 0.48f * w, 0.30f * h, 0.58f * w, 0f)
        close()
    }
    drawPath(fold, Brush.verticalGradient(listOf(g.highlight, g.end, g.deep)), alpha = FOLD_ALPHA)
    // A lit edge where the fold turns.
    val edge = Path().apply {
        moveTo(0.58f * w, 0f)
        cubicTo(0.48f * w, 0.30f * h, 0.56f * w, 0.62f * h, 0.38f * w, h)
    }
    drawPath(edge, g.highlight, alpha = EDGE_ALPHA, style = Stroke(width = EDGE_WIDTH * w))
    // Soft glow at the top right, where the light comes from.
    drawRect(
        Brush.radialGradient(
            listOf(g.highlight, Color.Transparent),
            center = Offset(0.88f * w, 0.12f * h),
            radius = 0.6f * w
        ),
        alpha = GLOW_ALPHA
    )
}

private fun DrawScope.drawWaves(g: CardGradient) {
    val w = size.width
    val h = size.height
    drawRect(Brush.linearGradient(listOf(g.start, g.end), start = Offset.Zero, end = Offset(w, h)))
    // Each band sits lower and darker than the one behind it.
    WAVE_TOPS.forEachIndexed { index, top ->
        val y = top * h
        val wave = Path().apply {
            moveTo(0f, y)
            cubicTo(0.30f * w, y - 0.22f * h, 0.62f * w, y + 0.18f * h, w, y - 0.12f * h)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        val tone = if (index % 2 == 0) g.end else g.deep
        drawPath(
            wave,
            Brush.verticalGradient(listOf(tone, g.deep), startY = y, endY = h),
            alpha = WAVE_ALPHA
        )
        val crest = Path().apply {
            moveTo(0f, y)
            cubicTo(0.30f * w, y - 0.22f * h, 0.62f * w, y + 0.18f * h, w, y - 0.12f * h)
        }
        drawPath(crest, g.highlight, alpha = CREST_ALPHA, style = Stroke(width = CREST_WIDTH * w))
    }
}

/** An EMV chip tinted with the card's own tones, as on the sample's front card. */
private fun DrawScope.drawChip(g: CardGradient) {
    val w = size.width
    val origin = Offset(CHIP_LEFT * w, CHIP_TOP * size.height)
    val chip = Size(CHIP_WIDTH * w, CHIP_WIDTH * w * CHIP_ASPECT)
    val corner = CornerRadius(CHIP_CORNER * w)
    drawRoundRect(
        Brush.linearGradient(
            listOf(g.highlight, g.base),
            start = origin,
            end =
            origin + Offset(chip.width, chip.height)
        ),
        topLeft = origin,
        size = chip,
        cornerRadius = corner
    )
    val line = Stroke(width = CHIP_LINE * w)
    drawRoundRect(
        g.deep,
        topLeft = origin,
        size = chip,
        cornerRadius = corner,
        alpha = CHIP_LINE_ALPHA,
        style = line
    )
    // Contact pads: one horizontal and two vertical divisions.
    val midY = origin.y + chip.height / 2
    drawLine(
        g.deep,
        Offset(origin.x, midY),
        Offset(origin.x + chip.width, midY),
        CHIP_LINE * w,
        alpha = CHIP_LINE_ALPHA
    )
    for (fraction in listOf(1f / 3, 2f / 3)) {
        val x = origin.x + chip.width * fraction
        drawLine(
            g.deep,
            Offset(x, origin.y),
            Offset(x, origin.y + chip.height),
            CHIP_LINE * w,
            alpha = CHIP_LINE_ALPHA
        )
    }
}

private const val RIBBON_ALPHA = 0.85f
private const val FOLD_ALPHA = 0.9f
private const val EDGE_ALPHA = 0.7f
private const val EDGE_WIDTH = 0.012f
private const val GLOW_ALPHA = 0.55f
private val WAVE_TOPS = listOf(0.48f, 0.62f, 0.76f, 0.9f)
private const val WAVE_ALPHA = 0.35f
private const val CREST_ALPHA = 0.3f
private const val CREST_WIDTH = 0.006f
private const val CHIP_LEFT = 0.09f
private const val CHIP_TOP = 0.38f
private const val CHIP_WIDTH = 0.14f
private const val CHIP_ASPECT = 0.76f
private const val CHIP_CORNER = 0.02f
private const val CHIP_LINE = 0.004f
private const val CHIP_LINE_ALPHA = 0.45f

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun BankCardArtPreview() {
    AbcTheme {
        Column(
            modifier = Modifier.padding(AbcTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space4)
        ) {
            BankCardArt(
                gradient = AbcTheme.colors.cardPeach,
                pattern = CardArtPattern.Ribbons,
                showChip = true,
                scrim = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                AbcWordmark(
                    tone = WordmarkTone.OnLightCard,
                    style = AbcTheme.typography.h3,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(AbcTheme.spacing.space4)
                )
            }
            BankCardArt(gradient = AbcTheme.colors.cardSage, modifier = Modifier.fillMaxWidth()) {
                AbcWordmark(
                    tone = WordmarkTone.OnCard,
                    style = AbcTheme.typography.h3,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(AbcTheme.spacing.space4)
                )
            }
            BankCardArt(gradient = AbcTheme.colors.cardGraphite, modifier = Modifier.fillMaxWidth())
        }
    }
}
