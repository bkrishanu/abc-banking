package com.abcbank.mobile.feature.documents.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.FlowScaffold
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.SectionHeader
import com.abcbank.mobile.core.designsystem.component.SkeletonBlock
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * Statements and documents (MobileBuild.md 6): monthly statements for the last 3 months, newest
 * first, and the 1099-INT for the previous tax year. Each opens in the in-app viewer.
 */
@Composable
fun DocumentsScreen(
    state: DocumentsUiState,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = AbcTheme.spacing
    FlowScaffold(
        step = null,
        totalSteps = null,
        title = stringResource(R.string.documents_title),
        onBack = onBack,
        modifier = modifier
    ) {
        when (state) {
            DocumentsUiState.Loading -> Column(
                verticalArrangement = Arrangement.spacedBy(spacing.space3)
            ) {
                repeat(PLACEHOLDER_ROWS) {
                    SkeletonBlock(
                        Modifier
                            .fillMaxWidth()
                            .height(AbcTheme.dimens.textFieldHeight),
                        shape = AbcTheme.shapes.lg
                    )
                }
            }
            DocumentsUiState.Empty -> Text(
                stringResource(R.string.documents_empty),
                style = AbcTheme.typography.body,
                color = AbcTheme.colors.textMuted
            )
            is DocumentsUiState.Error -> ErrorState(
                message = stringResource(
                    if (state.error ==
                        DataError.OFFLINE
                    ) {
                        R.string.state_offline_no_data
                    } else {
                        R.string.documents_error
                    }
                ),
                onRetry = onRetry
            )
            is DocumentsUiState.Content -> DocumentsContent(state, onOpen)
        }
    }
}

private const val PLACEHOLDER_ROWS = 4

@Composable
private fun DocumentsContent(state: DocumentsUiState.Content, onOpen: (String) -> Unit) {
    val spacing = AbcTheme.spacing
    if (state.isOffline) {
        OfflineBanner()
        Spacer(Modifier.height(spacing.space4))
    }
    state.statementsByMonth.forEach { (month, statements) ->
        SectionHeader(title = DateFormatter.monthYear(month))
        ContentCard(padding = spacing.space2) {
            statements.forEachIndexed { index, statement ->
                if (index > 0) HorizontalDivider(color = AbcTheme.colors.border)
                DocumentRow(
                    title = stringResource(
                        R.string.account_name_last4,
                        statement.account.nickname,
                        statement.account.last4
                    ),
                    detail = stringResource(
                        closingLabel(statement.owedStyle),
                        MoneyFormatter.format(statement.closing)
                    ),
                    onClick = { onOpen(statement.id) }
                )
            }
        }
        Spacer(Modifier.height(spacing.space4))
    }
    SectionHeader(title = stringResource(R.string.documents_tax))
    val form = state.overview.taxForm
    if (form == null) {
        Text(
            stringResource(R.string.documents_no_1099, state.overview.taxYear),
            style = AbcTheme.typography.body,
            color = AbcTheme.colors.textMuted
        )
    } else {
        ContentCard(padding = spacing.space2) {
            DocumentRow(
                title = stringResource(R.string.documents_1099, form.year),
                detail = stringResource(
                    R.string.documents_interest,
                    MoneyFormatter.format(form.interest)
                ),
                onClick = { onOpen(form.id) }
            )
        }
    }
    Spacer(Modifier.height(spacing.space3))
    Text(
        stringResource(R.string.documents_note),
        style = AbcTheme.typography.small,
        color = AbcTheme.colors.textMuted
    )
}

private fun closingLabel(owed: Boolean): Int =
    if (owed) R.string.documents_closing_owed else R.string.documents_closing

@Composable
private fun DocumentRow(title: String, detail: String, onClick: () -> Unit) {
    val spacing = AbcTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AbcTheme.dimens.touchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(spacing.space2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AbcTheme.dimens.iconCircle)
                .clip(AbcTheme.shapes.full)
                .background(AbcTheme.colors.surfaceMuted)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_file_text),
                contentDescription = null,
                tint = AbcTheme.colors.text,
                modifier = Modifier.size(AbcTheme.dimens.iconMedium)
            )
        }
        Spacer(Modifier.width(spacing.space3))
        Column(Modifier.weight(1f)) {
            Text(title, style = AbcTheme.typography.body, color = AbcTheme.colors.text)
            Text(detail, style = AbcTheme.typography.small, color = AbcTheme.colors.textMuted)
        }
    }
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun DocumentsPreview() {
    AbcTheme {
        DocumentsScreen(DocumentsPreviewData.samanta(), onBack = {}, onOpen = {}, onRetry = {})
    }
}
