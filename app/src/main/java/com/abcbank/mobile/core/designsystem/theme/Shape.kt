package com.abcbank.mobile.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radii from DESIGN_SYSTEM.md section 05. */
@Immutable
data class AbcShapes(
    val sm: Shape = RoundedCornerShape(8.dp),
    val md: Shape = RoundedCornerShape(12.dp),
    val lg: Shape = RoundedCornerShape(20.dp),
    val xl: Shape = RoundedCornerShape(28.dp),
    val sheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    // The first and last row of a long list drawn as one content card, row by row.
    val xlTop: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    val xlBottom: Shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
    val full: Shape = CircleShape
)

/** The single elevation level for floating elements (DESIGN_SYSTEM.md section 05). Content cards stay flat. */
@Immutable
data class AbcElevation(val floating: Dp = 8.dp)
