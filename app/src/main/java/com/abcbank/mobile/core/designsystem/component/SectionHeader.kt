package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * `SectionHeader` from DESIGN_SYSTEM.md section 06: H3 title, and an optional underlined Small
 * muted action ("View all") with a full touch target.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AbcTheme.dimens.touchTarget),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = AbcTheme.typography.h3,
            color = AbcTheme.colors.text,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (actionLabel != null) {
            Box(
                modifier = Modifier
                    .heightIn(min = AbcTheme.dimens.touchTarget)
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(start = AbcTheme.spacing.space3),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = actionLabel,
                    style = AbcTheme.typography.small.copy(
                        textDecoration = TextDecoration.Underline
                    ),
                    color = AbcTheme.colors.textMuted
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun SectionHeaderPreview() {
    AbcTheme {
        SectionHeader(
            title = "Latest transactions",
            actionLabel = "View all",
            modifier = Modifier.padding(AbcTheme.spacing.space4)
        )
    }
}
