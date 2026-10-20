package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * `AbcBottomSheet` from DESIGN_SYSTEM.md section 06: surface, top radius-xl, drag handle and scrim.
 * Used for filters now, and for step-up, More actions and disclaimers later.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbcBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = AbcTheme.shapes.sheet,
        containerColor = AbcTheme.colors.surface,
        contentColor = AbcTheme.colors.text,
        scrimColor = AbcTheme.colors.scrim,
        modifier = modifier,
        content = content
    )
}
