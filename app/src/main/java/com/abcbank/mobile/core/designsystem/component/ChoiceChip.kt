package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * `ChoiceChip` from DESIGN_SYSTEM.md section 06: one choice in a wrapping group (transfer
 * category, employment status). Small label, radius-full, 40dp; surface with a border, or primary
 * fill with onPrimary text when selected. Put the group in a `selectableGroup()`.
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AbcTheme.colors
    val shape = AbcTheme.shapes.full
    Text(
        text = label,
        style = AbcTheme.typography.small,
        color = if (selected) colors.onPrimary else colors.text,
        modifier = modifier
            .heightIn(min = AbcTheme.dimens.chipHeight)
            .clip(shape)
            .background(if (selected) colors.primary else colors.surface)
            .border(
                AbcTheme.dimens.borderWidth,
                if (selected) colors.primary else colors.border,
                shape
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = AbcTheme.spacing.space4, vertical = AbcTheme.spacing.space3)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun ChoiceChipPreview() {
    AbcTheme {
        Row(
            modifier = Modifier.padding(AbcTheme.spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2)
        ) {
            ChoiceChip("Employed", selected = true, onClick = {})
            ChoiceChip("Retired", selected = false, onClick = {})
        }
    }
}
