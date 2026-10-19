package com.abcbank.mobile.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * `ActionPill` from DESIGN_SYSTEM.md section 06: a white floating pill with a black icon circle
 * and a Body label (Transfer and Pay Bills on Home).
 */
@Composable
fun ActionPill(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AbcTheme.colors
    val dimens = AbcTheme.dimens
    val spacing = AbcTheme.spacing
    Row(
        modifier = modifier
            .floatingPill()
            .heightIn(min = dimens.actionPillHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = spacing.space1, end = spacing.space4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimens.iconCircle)
                .clip(AbcTheme.shapes.full)
                .background(colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(dimens.iconMedium)
            )
        }
        Spacer(Modifier.width(spacing.space2))
        Text(text = label, style = AbcTheme.typography.body, color = colors.text)
    }
}

/** `GridPill` from DESIGN_SYSTEM.md section 06: a white circle with the grid icon for More actions. */
@Composable
fun GridPill(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = AbcTheme.dimens
    Box(
        modifier = modifier
            .size(dimens.actionPillHeight)
            .floatingPill()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_layout_grid),
            contentDescription = null,
            tint = AbcTheme.colors.text,
            modifier = Modifier.size(dimens.iconLarge)
        )
    }
}

@Composable
private fun Modifier.floatingPill(): Modifier {
    val shape = AbcTheme.shapes.full
    val colors = AbcTheme.colors
    return shadow(
        elevation = AbcTheme.elevation.floating,
        shape = shape,
        ambientColor = colors.shadow,
        spotColor = colors.shadow
    )
        .clip(shape)
        .background(colors.surface)
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun ActionPillPreview() {
    AbcTheme {
        Row(
            modifier = Modifier.padding(AbcTheme.spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2)
        ) {
            ActionPill(label = "Transfer", icon = R.drawable.ic_arrow_up_right, onClick = {})
            ActionPill(label = "Pay Bills", icon = R.drawable.ic_receipt, onClick = {})
            GridPill(contentDescription = "More actions", onClick = {})
        }
    }
}
