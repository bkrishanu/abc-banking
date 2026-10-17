package com.abcbank.mobile.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

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
    val positive: Color,
    val positiveText: Color,
    val warning: Color,
    val danger: Color,
    val scrim: Color,
    val shadow: Color,
    val cardScrim: Color,
    val cardPeach: CardGradient,
    val cardSage: CardGradient,
    val cardGraphite: CardGradient
)

/** Disabled controls render at 40% alpha (DESIGN_SYSTEM.md section 06). */
const val DISABLED_ALPHA = 0.4f

/** Bank card face gradient, drawn top-left to bottom-right. */
@Immutable
data class CardGradient(val start: Color, val end: Color)

internal val LightAbcColors = AbcColors(
    background = Color(0xFFF2F2F2),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEDEDED),
    border = Color(0xFFE6E6E6),
    text = Color(0xFF111111),
    textMuted = Color(0xFF6B6B70),
    primary = Color(0xFF111111),
    primaryPressed = Color(0xFF2A2A2A),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFFF9A3C),
    accentDeep = Color(0xFFE2773A),
    positive = Color(0xFF6CC04A),
    positiveText = Color(0xFF237A3B),
    warning = Color(0xFFB45309),
    danger = Color(0xFFD93025),
    scrim = Color(0x66000000),
    shadow = Color(0x1A000000),
    cardScrim = Color(0x33000000),
    cardPeach = CardGradient(start = Color(0xFFF7DCC8), end = Color(0xFFF08A3C)),
    cardSage = CardGradient(start = Color(0xFFD3DEC2), end = Color(0xFF6E9A5E)),
    cardGraphite = CardGradient(start = Color(0xFF3A3A3C), end = Color(0xFF111111))
)
