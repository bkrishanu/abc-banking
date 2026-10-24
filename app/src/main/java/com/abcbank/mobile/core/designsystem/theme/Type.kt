package com.abcbank.mobile.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.abcbank.mobile.R

/**
 * Plus Jakarta Sans, bundled under res/font as the variable font from google/fonts
 * (OFL, see docs/licenses/PlusJakartaSans-OFL.txt). Each weight is an instance of its 'wght' axis.
 */
val PlusJakartaSans = FontFamily(
    listOf(
        FontWeight.Light,
        FontWeight.Normal,
        FontWeight.Medium,
        FontWeight.SemiBold
    ).map { weight ->
        Font(
            R.font.plus_jakarta_sans,
            weight,
            variationSettings = FontVariation.Settings(weight, FontStyle.Normal)
        )
    }
)

/** Tabular figures so amounts line up in columns (DESIGN_SYSTEM.md section 03). */
const val TABULAR_FIGURES = "tnum"

/** Type scale from DESIGN_SYSTEM.md section 03. */
@Immutable
data class AbcTypography(
    val display: TextStyle,
    val balance: TextStyle,
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val body: TextStyle,
    val small: TextStyle,
    val label: TextStyle,
    val mono: TextStyle
)

internal val DefaultAbcTypography = AbcTypography(
    display = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Light,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.02).em
    ),
    balance = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Light,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = TABULAR_FIGURES
    ),
    h1 = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.01).em
    ),
    h2 = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    h3 = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    body = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    small = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    label = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    mono = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontFeatureSettings = TABULAR_FIGURES
    )
)
