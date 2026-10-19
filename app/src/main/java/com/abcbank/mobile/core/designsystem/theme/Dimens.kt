package com.abcbank.mobile.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Component sizes from DESIGN_SYSTEM.md sections 06 to 08. Composables never hard-code a size. */
@Immutable
data class AbcDimens(
    val touchTarget: Dp = 48.dp,
    val borderWidth: Dp = 1.dp,
    val focusRingWidth: Dp = 2.dp,
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 20.dp,
    val iconLarge: Dp = 24.dp,
    val buttonHeight: Dp = 52.dp,
    val buttonProgress: Dp = 20.dp,
    val textFieldHeight: Dp = 56.dp,
    val keypadKeyHeight: Dp = 64.dp,
    val mpinDot: Dp = 12.dp,
    val avatarSmall: Dp = 32.dp,
    val iconCircle: Dp = 40.dp,
    val actionPillHeight: Dp = 56.dp,
    val actionPillOverlap: Dp = 28.dp,
    val unreadDot: Dp = 8.dp,
    val skeletonLine: Dp = 16.dp
)

internal val DefaultAbcDimens = AbcDimens()

/** Welcome screen card stack (DESIGN_SYSTEM.md section 10). */
object WelcomeCardStack {
    const val CARD_WIDTH_FRACTION = 0.78f
    const val FRONT_ROTATION_DEGREES = -8f
    const val BACK_ROTATION_DEGREES = -4f
}

/** Bank card face proportions (ISO/IEC 7810 ID-1, DESIGN_SYSTEM.md section 06). */
const val BANK_CARD_ASPECT_RATIO = 1.586f
