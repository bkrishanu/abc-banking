package com.abcbank.mobile.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalAbcColors = staticCompositionLocalOf { LightAbcColors }
private val LocalAbcTypography = staticCompositionLocalOf { DefaultAbcTypography }
private val LocalAbcSpacing = staticCompositionLocalOf { DefaultAbcSpacing }
private val LocalAbcShapes = staticCompositionLocalOf { AbcShapes() }
private val LocalAbcElevation = staticCompositionLocalOf { AbcElevation() }

/**
 * App theme. Composables read tokens through [AbcTheme.colors] and friends.
 * Material 3 is mapped to the same tokens so its switches, sliders and sheets match.
 * Light only in v1 (PRD section 07); a dark palette would be swapped in here.
 */
@Composable
fun AbcTheme(content: @Composable () -> Unit) {
    val colors = LightAbcColors
    val typography = DefaultAbcTypography
    CompositionLocalProvider(
        LocalAbcColors provides colors,
        LocalAbcTypography provides typography,
        LocalAbcSpacing provides DefaultAbcSpacing,
        LocalAbcShapes provides AbcShapes(),
        LocalAbcElevation provides AbcElevation()
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = colors.primary,
                onPrimary = colors.onPrimary,
                secondary = colors.accent,
                onSecondary = colors.text,
                background = colors.background,
                onBackground = colors.text,
                surface = colors.surface,
                onSurface = colors.text,
                surfaceVariant = colors.surfaceMuted,
                onSurfaceVariant = colors.textMuted,
                outline = colors.border,
                error = colors.danger,
                onError = colors.onPrimary,
                scrim = colors.scrim
            ),
            typography = Typography(
                headlineMedium = typography.h1,
                titleLarge = typography.h2,
                titleMedium = typography.h3,
                bodyLarge = typography.body,
                bodyMedium = typography.body,
                bodySmall = typography.small,
                labelLarge = typography.body,
                labelMedium = typography.label,
                labelSmall = typography.label
            ),
            content = content
        )
    }
}

object AbcTheme {
    val colors: AbcColors
        @Composable @ReadOnlyComposable
        get() = LocalAbcColors.current

    val typography: AbcTypography
        @Composable @ReadOnlyComposable
        get() = LocalAbcTypography.current

    val spacing: AbcSpacing
        @Composable @ReadOnlyComposable
        get() = LocalAbcSpacing.current

    val shapes: AbcShapes
        @Composable @ReadOnlyComposable
        get() = LocalAbcShapes.current

    val elevation: AbcElevation
        @Composable @ReadOnlyComposable
        get() = LocalAbcElevation.current
}
