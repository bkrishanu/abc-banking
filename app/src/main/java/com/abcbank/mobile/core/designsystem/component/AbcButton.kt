package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.DISABLED_ALPHA

enum class AbcButtonVariant { Primary, Secondary, Ghost, Danger }

/**
 * Full-width pill button from DESIGN_SYSTEM.md section 06. One Primary per screen.
 * While [loading], a progress indicator replaces the label, the width stays fixed and taps are ignored.
 */
@Composable
fun AbcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AbcButtonVariant = AbcButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val colors = AbcTheme.colors
    val dimens = AbcTheme.dimens
    val shape = AbcTheme.shapes.full
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val clickable = enabled && !loading

    val container = when (variant) {
        AbcButtonVariant.Primary -> if (pressed) colors.primaryPressed else colors.primary
        AbcButtonVariant.Secondary -> colors.accentTint
        AbcButtonVariant.Ghost -> Color.Transparent
        AbcButtonVariant.Danger -> colors.danger
    }
    val content = when (variant) {
        AbcButtonVariant.Primary, AbcButtonVariant.Danger -> colors.onPrimary
        AbcButtonVariant.Secondary -> colors.accentDeep
        AbcButtonVariant.Ghost -> colors.text
    }
    // The ring is drawn inside the pill, so it must reach 3:1 against the button's own fill.
    val focusRing = when (variant) {
        AbcButtonVariant.Primary -> colors.accent
        AbcButtonVariant.Secondary, AbcButtonVariant.Ghost -> colors.primary
        AbcButtonVariant.Danger -> colors.onPrimary
    }
    val loadingLabel = stringResource(R.string.button_loading)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimens.buttonHeight)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(shape)
            .background(container)
            .then(
                if (focused) {
                    Modifier.border(dimens.focusRingWidth, focusRing, shape)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = clickable,
                onClick = onClick
            )
            .clearAndSetSemantics {
                contentDescription = text
                role = Role.Button
                if (loading) stateDescription = loadingLabel
                if (clickable) {
                    onClick {
                        onClick()
                        true
                    }
                } else {
                    disabled()
                }
            }
            .padding(horizontal = AbcTheme.spacing.space4),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimens.buttonProgress),
                color = content,
                strokeWidth = dimens.focusRingWidth
            )
        } else {
            Text(
                text = text,
                style = AbcTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = content
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun AbcButtonPreview() {
    AbcTheme {
        Column(
            modifier = Modifier.padding(AbcTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space3)
        ) {
            AbcButton(text = "Get Started", onClick = {})
            AbcButton(text = "Download Receipt", onClick = {}, variant = AbcButtonVariant.Secondary)
            AbcButton(text = "View All", onClick = {}, variant = AbcButtonVariant.Ghost)
            AbcButton(text = "Freeze Card", onClick = {}, variant = AbcButtonVariant.Danger)
            AbcButton(text = "Confirm Transfer", onClick = {}, loading = true)
            AbcButton(text = "Confirm Transfer", onClick = {}, enabled = false)
        }
    }
}
