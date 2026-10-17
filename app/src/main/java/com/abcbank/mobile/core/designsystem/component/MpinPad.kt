package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.DISABLED_ALPHA

/**
 * MPIN dots and a 3 x 4 keypad (DESIGN_SYSTEM.md section 06). Stateless: the caller holds the digits
 * and only passes how many are entered, so the digits never reach the UI or accessibility tree.
 * [supportingText] shows wrong-MPIN and lockout messages below the dots.
 */
@Composable
fun MpinPad(
    enteredCount: Int,
    length: Int,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    isError: Boolean = false
) {
    val spacing = AbcTheme.spacing
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MpinDots(enteredCount = enteredCount, length = length)
        Spacer(Modifier.height(spacing.space3))
        Text(
            text = supportingText.orEmpty(),
            style = AbcTheme.typography.small,
            color = if (isError) AbcTheme.colors.danger else AbcTheme.colors.textMuted,
            textAlign = TextAlign.Center,
            minLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                // Announce a wrong MPIN, but not every tick of the lockout countdown.
                .semantics { if (isError) liveRegion = LiveRegionMode.Polite }
        )
        Spacer(Modifier.height(spacing.space4))
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    row.forEach { digit -> DigitKey(digit, enabled, onDigit) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                Spacer(Modifier.weight(1f))
                DigitKey(0, enabled, onDigit)
                DeleteKey(enabled = enabled && enteredCount > 0, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun MpinDots(enteredCount: Int, length: Int) {
    val progress = stringResource(R.string.mpin_progress, enteredCount, length)
    Row(
        horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space4),
        modifier = Modifier.clearAndSetSemantics { contentDescription = progress }
    ) {
        repeat(length) { index ->
            val dot = Modifier
                .size(AbcTheme.dimens.mpinDot)
                .clip(CircleShape)
            val filled = index < enteredCount
            Box(
                modifier = if (filled) {
                    dot.background(AbcTheme.colors.text)
                } else {
                    // Outline, not a muted fill: a fill close to the background disappears.
                    dot.border(AbcTheme.dimens.borderWidth, AbcTheme.colors.textMuted, CircleShape)
                }
            )
        }
    }
}

@Composable
private fun RowScope.DigitKey(digit: Int, enabled: Boolean, onDigit: (Int) -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .height(AbcTheme.dimens.keypadKeyHeight)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(AbcTheme.shapes.md)
            .background(AbcTheme.colors.surface)
            .clickable(enabled = enabled, role = Role.Button) { onDigit(digit) }
    ) {
        Text(text = digit.toString(), style = AbcTheme.typography.h1, color = AbcTheme.colors.text)
    }
}

@Composable
private fun RowScope.DeleteKey(enabled: Boolean, onDelete: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .height(AbcTheme.dimens.keypadKeyHeight)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(AbcTheme.shapes.md)
            .background(Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onDelete)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = stringResource(R.string.cd_delete),
            tint = AbcTheme.colors.text,
            modifier = Modifier.size(AbcTheme.dimens.iconLarge)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun MpinPadPreview() {
    AbcTheme {
        MpinPad(
            enteredCount = 3,
            length = 6,
            onDigit = {},
            onDelete = {},
            supportingText = "Wrong MPIN. 4 attempts left.",
            isError = true,
            modifier = Modifier.padding(AbcTheme.spacing.space4)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun MpinPadLockedPreview() {
    AbcTheme {
        MpinPad(
            enteredCount = 0,
            length = 6,
            onDigit = {},
            onDelete = {},
            enabled = false,
            supportingText = "Try again in 24 s",
            modifier = Modifier.padding(AbcTheme.spacing.space4)
        )
    }
}
