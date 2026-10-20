package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * The confirmation dialog for destructive actions (DESIGN_SYSTEM.md section 06, "Dialogs only for
 * destructive confirmation. Always a cancel option."): H2 title, Body text, a Danger confirm and a
 * Ghost cancel. Back and tapping outside cancel too.
 */
@Composable
fun AbcConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        ContentCard(padding = AbcTheme.spacing.space5) {
            Column(verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space3)) {
                Text(
                    text = title,
                    style = AbcTheme.typography.h2,
                    color = AbcTheme.colors.text,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = text,
                    style = AbcTheme.typography.body,
                    color = AbcTheme.colors.textMuted
                )
                Column(
                    modifier = Modifier.padding(top = AbcTheme.spacing.space2),
                    verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2)
                ) {
                    AbcButton(
                        text = confirmLabel,
                        onClick = onConfirm,
                        variant = AbcButtonVariant.Danger
                    )
                    AbcButton(
                        text = cancelLabel,
                        onClick = onDismiss,
                        variant = AbcButtonVariant.Ghost
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun AbcConfirmDialogPreview() {
    AbcTheme {
        AbcConfirmDialog(
            title = "Dispute This Transaction?",
            text = "We'll review the charge and update you in Support.",
            confirmLabel = "Dispute",
            cancelLabel = "Cancel",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
