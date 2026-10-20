package com.abcbank.mobile.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * `AbcSwitch` from DESIGN_SYSTEM.md section 06, as a full row: optional icon, title and Small muted
 * subtitle, Material 3 switch in primary. The whole row is the touch target.
 */
@Composable
fun AbcSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null
) {
    val colors = AbcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AbcTheme.dimens.touchTarget)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = AbcTheme.spacing.space2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(AbcTheme.dimens.iconMedium)
            )
            Spacer(Modifier.width(AbcTheme.spacing.space3))
        }
        Column(Modifier.weight(1f)) {
            Text(text = title, style = AbcTheme.typography.body, color = colors.text)
            if (subtitle !=
                null
            ) {
                Text(
                    text = subtitle,
                    style = AbcTheme.typography.small,
                    color = colors.textMuted
                )
            }
        }
        Spacer(Modifier.width(AbcTheme.spacing.space3))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.primary,
                checkedThumbColor = colors.onPrimary,
                uncheckedTrackColor = colors.surfaceMuted,
                uncheckedBorderColor = colors.border,
                uncheckedThumbColor = colors.textMuted
            )
        )
    }
}

/**
 * `AbcSlider` from DESIGN_SYSTEM.md section 06: label and the current value as text above a
 * Material 3 slider with a primary active track and accent thumb. [steps] is the number of stops
 * between the ends, as Material counts them.
 */
@Composable
fun AbcSliderRow(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier
) {
    val colors = AbcTheme.colors
    Column(modifier.fillMaxWidth()) {
        // Side by side when both fit; at large font scales the value moves under the label.
        AdaptiveRow(start = {
            Text(text = label, style = AbcTheme.typography.body, color = colors.text)
        }) {
            Text(text = valueText, style = AbcTheme.typography.body, color = colors.text)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.surfaceMuted,
                activeTickColor = colors.primary,
                inactiveTickColor = colors.surfaceMuted
            ),
            // TalkBack reads the money value, not a percentage.
            modifier = Modifier.semantics { stateDescription = valueText }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AbcControlsPreview() {
    AbcTheme {
        Column(Modifier.padding(AbcTheme.spacing.space4)) {
            AbcSwitchRow(
                title = "Freeze Card",
                subtitle = "Blocks card, ATM and online payments.",
                checked = true,
                onCheckedChange = {}
            )
            AbcSliderRow(
                label = "ATM Withdrawals",
                valueText = "$1,000.00",
                value = 1_000f,
                onValueChange = {},
                valueRange = 100f..2_500f,
                steps = 23
            )
        }
    }
}
