package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * `FilterChipRow` from DESIGN_SYSTEM.md section 06: segmented filters such as All / Spendings /
 * Earnings. The selected chip is surface on a surfaceMuted track; the others are transparent with
 * a border. TalkBack reads them as one selectable group.
 */
@Composable
fun FilterChipRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AbcTheme.colors
    val shape = AbcTheme.shapes.full
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceMuted)
            .padding(AbcTheme.spacing.space1)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space1)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = AbcTheme.dimens.chipHeight - AbcTheme.spacing.space2)
                    .clip(shape)
                    .background(if (selected) colors.surface else Color.Transparent)
                    .then(
                        if (selected) {
                            Modifier
                        } else {
                            Modifier.border(AbcTheme.dimens.borderWidth, colors.border, shape)
                        }
                    )
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = AbcTheme.spacing.space2),
                contentAlignment = Alignment.Center
            ) {
                // One line always; at large font scales the label shrinks rather than breaking a word.
                BasicText(
                    text = label,
                    style = AbcTheme.typography.small.copy(
                        color = if (selected) colors.text else colors.textMuted,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = MIN_LABEL_SIZE.sp,
                        maxFontSize = AbcTheme.typography.small.fontSize
                    )
                )
            }
        }
    }
}

private const val MIN_LABEL_SIZE = 8

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun FilterChipRowPreview() {
    AbcTheme {
        FilterChipRow(
            options = listOf("All", "Spendings", "Earnings"),
            selectedIndex = 1,
            onSelect = {},
            modifier = Modifier.padding(AbcTheme.spacing.space4)
        )
    }
}
