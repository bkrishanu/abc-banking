package com.abcbank.mobile.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Colour tokens from DESIGN_SYSTEM.md section 02. The only place raw colour values may appear. */
@Immutable
data class AbcColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val border: Color,
    val text: Color,
    val textMuted: Color,
    val primary: Color,
    val primaryPressed: Color,
    val onPrimary: Color,
    val accent: Color,
    val accentDeep: Color,
    val accentTint: Color,
    val positive: Color,
    val positiveText: Color,
    val positiveTint: Color,
    val warning: Color,
    val warningTint: Color,
    val danger: Color,
    val dangerTint: Color,
    val scrim: Color,
    val shadow: Color,
    val cardScrim: Color,
    val cardPeach: CardGradient,
    val cardSage: CardGradient,
    val cardGraphite: CardGradient
)

/** Disabled controls render at 40% alpha (DESIGN_SYSTEM.md section 06). */
const val DISABLED_ALPHA = 0.4f

/**
 * Bank card face gradient, drawn top-left to bottom-right. The fluid artwork on the card face
 * (DESIGN_SYSTEM.md section 02) uses tones derived from the two stops, so every card art colour
 * still comes from these tokens.
 */
private fun Color.grey(): Color {
    val level = LUMA_RED * red + LUMA_GREEN * green + LUMA_BLUE * blue
    return Color(level, level, level, alpha)
}

// Rec. 709 luma weights, so a grey card keeps the contrast its text was checked against.
private const val LUMA_RED = 0.2126f
private const val LUMA_GREEN = 0.7152f
private const val LUMA_BLUE = 0.0722f

@Immutable
data class CardGradient(val start: Color, val end: Color) {
    /** Light sheen on the ribbon folds. */
    val highlight: Color get() = lerp(start, Color.White, HIGHLIGHT_MIX)

    /** The body of the card behind the ribbons. */
    val base: Color get() = lerp(start, end, BASE_MIX)

    /** Shadowed side of a fold. */
    val deep: Color get() = lerp(end, Color.Black, DEEP_MIX)

    /** The same face in greys of the same luma, for a frozen card (DESIGN_SYSTEM.md section 06). */
    fun desaturated(): CardGradient = CardGradient(start.grey(), end.grey())

    private companion object {
        const val HIGHLIGHT_MIX = 0.55f
        const val BASE_MIX = 0.18f
        const val DEEP_MIX = 0.18f
    }
}

internal val LightAbcColors = AbcColors(
    background = Color(0xFFFAF8F5),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEFEEEB),
    border = Color(0xFFE5DFD7),
    text = Color(0xFF142423),
    textMuted = Color(0xFF5C6665),
    primary = Color(0xFF0E3E3D),
    primaryPressed = Color(0xFF185352),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFE07A5F),
    accentDeep = Color(0xFF9A442D),
    accentTint = Color(0xFFF7E9E3),
    positive = Color(0xFF059669),
    positiveText = Color(0xFF047857),
    positiveTint = Color(0xFFE1F2ED),
    warning = Color(0xFFB45309),
    warningTint = Color(0xFFF6EAE1),
    danger = Color(0xFFBA1A1A),
    dangerTint = Color(0xFFF7E4E4),
    scrim = Color(0x66142423),
    shadow = Color(0x1A0E3E3D),
    cardScrim = Color(0x33000000),
    cardPeach = CardGradient(start = Color(0xFFF4C3AE), end = Color(0xFFE07A5F)),
    cardSage = CardGradient(start = Color(0xFF3A6664), end = Color(0xFF0E3E3D)),
    cardGraphite = CardGradient(start = Color(0xFF3A3A3C), end = Color(0xFF142423))
)
