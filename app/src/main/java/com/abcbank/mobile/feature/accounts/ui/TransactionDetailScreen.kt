package com.abcbank.mobile.feature.accounts.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.AbcConfirmDialog
import com.abcbank.mobile.core.designsystem.component.AdaptiveRow
import com.abcbank.mobile.core.designsystem.component.BadgeTone
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.SkeletonBlock
import com.abcbank.mobile.core.designsystem.component.StatusBadge
import com.abcbank.mobile.core.designsystem.component.TransactionLeadingIcon
import com.abcbank.mobile.core.designsystem.component.categoryLabel
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.TABULAR_FIGURES
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import com.abcbank.mobile.feature.accounts.domain.DisputeMessages
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TransactionDetailRoute(
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.receipt_share_title)
    val receipt = (state as? TransactionDetailUiState.Content)?.receipt
    val ready = (receipt as? ReceiptStatus.Ready)?.file
    // Handing a finished file to the share sheet is UI work; building it happened in data.
    LaunchedEffect(ready) {
        if (ready != null) {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = PDF_MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, ready.contentUri.toUri())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, shareTitle))
            viewModel.onReceiptShared()
        }
    }
    val disputeMessages = disputeMessages(state)
    TransactionDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        onDownloadReceipt = viewModel::onDownloadReceipt,
        onConfirmDispute = { disputeMessages?.let(viewModel::onDispute) }
    )
}

/** The new case's texts, from string resources like all other copy. */
@Composable
private fun disputeMessages(state: TransactionDetailUiState): DisputeMessages? {
    val content = state as? TransactionDetailUiState.Content ?: return null
    return DisputeMessages(
        topic = stringResource(R.string.dispute_topic),
        customerMessage = stringResource(
            R.string.dispute_customer_message,
            content.transaction.merchant
        ),
        bankReply = stringResource(R.string.dispute_bank_reply)
    )
}

/**
 * Transaction drill-down (MobileBuild.md 2.2): merchant, amount, time, account, category, status,
 * running balance and reference. Download Receipt and Dispute come with part 2.
 */
@Composable
fun TransactionDetailScreen(
    state: TransactionDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onDownloadReceipt: () -> Unit = {},
    onConfirmDispute: () -> Unit = {}
) {
    val spacing = AbcTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AbcTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(AbcTheme.dimens.touchTarget)) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.cd_back),
                tint = AbcTheme.colors.text,
                modifier = Modifier.size(AbcTheme.dimens.iconLarge)
            )
        }
        Column(Modifier.padding(horizontal = spacing.space4)) {
            Text(
                text = stringResource(R.string.transaction_detail_title),
                style = AbcTheme.typography.h1,
                color = AbcTheme.colors.text,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(spacing.space6))
            when (state) {
                TransactionDetailUiState.Loading -> SkeletonBlock(
                    Modifier
                        .fillMaxWidth()
                        .height(AbcTheme.dimens.avatarLarge * 3),
                    shape = AbcTheme.shapes.xl
                )
                is TransactionDetailUiState.Error -> ErrorState(
                    message = stringResource(
                        if (state.error ==
                            DataError.OFFLINE
                        ) {
                            R.string.state_offline_no_data
                        } else {
                            R.string.transactions_error
                        }
                    ),
                    onRetry = onRetry
                )
                TransactionDetailUiState.Empty -> Text(
                    text = stringResource(R.string.transaction_not_found),
                    style = AbcTheme.typography.body,
                    color = AbcTheme.colors.text
                )
                is TransactionDetailUiState.Content -> {
                    DetailContent(state)
                    DetailActions(state, onDownloadReceipt, onConfirmDispute)
                }
            }
            Spacer(Modifier.height(spacing.space6))
        }
    }
}

@Composable
private fun DetailContent(state: TransactionDetailUiState.Content) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    val transaction = state.transaction
    val incoming = transaction.direction == TransactionDirection.CREDIT
    val hiddenDescription = stringResource(R.string.cd_balance_hidden)
    if (state.isOffline) {
        OfflineBanner()
        Spacer(Modifier.height(spacing.space4))
    }
    ContentCard(padding = spacing.space5) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            TransactionLeadingIcon(transaction.category, transaction.merchant, incoming)
            Text(
                text = transaction.merchant,
                style = AbcTheme.typography.h2,
                color = colors.text,
                textAlign = TextAlign.Center
            )
            val amount = MoneyFormatter.formatSigned(transaction.signedAmount)
            Text(
                text = if (state.balancesHidden) AMOUNT_MASK else amount,
                style = AbcTheme.typography.h1.copy(fontFeatureSettings = TABULAR_FIGURES),
                color = if (incoming && !state.balancesHidden) colors.positiveText else colors.text,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = if (state.balancesHidden) hiddenDescription else amount
                }
            )
            val pending = transaction.status == TransactionStatus.PENDING
            if (pending || transaction.disputed) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    if (pending) StatusBadge(stringResource(R.string.transaction_status_pending))
                    if (transaction.disputed) {
                        StatusBadge(
                            stringResource(R.string.transaction_disputed),
                            tone = BadgeTone.Warning
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(spacing.space4))
    ContentCard {
        val rows = buildList {
            add(
                stringResource(R.string.transaction_detail_date) to
                    DateFormatter.fullDate(state.date)
            )
            add(stringResource(R.string.transaction_detail_time) to DateFormatter.time(state.time))
            state.account?.let {
                add(
                    stringResource(R.string.transaction_detail_account) to
                        stringResource(R.string.account_name_last4, it.nickname, it.last4)
                )
            }
            add(
                stringResource(R.string.transaction_detail_category) to
                    categoryLabel(transaction.category)
            )
            add(
                stringResource(R.string.transaction_detail_status) to stringResource(
                    if (transaction.status == TransactionStatus.PENDING) {
                        R.string.transaction_status_pending
                    } else {
                        R.string.transaction_status_posted
                    }
                )
            )
            add(
                stringResource(R.string.transaction_detail_running_balance) to
                    if (state.balancesHidden) {
                        AMOUNT_MASK
                    } else {
                        MoneyFormatter.format(
                            transaction.runningBalance
                        )
                    }
            )
        }
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) HorizontalDivider(color = colors.border)
            DetailRow(label, value, AbcTheme.typography.body)
        }
        HorizontalDivider(color = colors.border)
        // The reference is long; AdaptiveRow moves it under its label when it does not fit.
        DetailRow(
            stringResource(R.string.transaction_detail_reference),
            transaction.reference,
            AbcTheme.typography.mono
        )
    }
}

/** Download Receipt for every transaction; Dispute only for charges DisputeRules allows. */
@Composable
private fun DetailActions(
    state: TransactionDetailUiState.Content,
    onDownloadReceipt: () -> Unit,
    onConfirmDispute: () -> Unit
) {
    var confirmDispute by rememberSaveable { mutableStateOf(false) }
    val spacing = AbcTheme.spacing
    Spacer(Modifier.height(spacing.space6))
    AbcButton(
        text = stringResource(R.string.action_download_receipt),
        onClick = onDownloadReceipt,
        variant = AbcButtonVariant.Secondary,
        loading = state.receipt == ReceiptStatus.Creating
    )
    if (state.receipt == ReceiptStatus.Failed) {
        ActionMessage(stringResource(R.string.receipt_failed), AbcTheme.colors.danger)
    }
    when {
        state.transaction.disputed ->
            ActionMessage(stringResource(R.string.dispute_under_review), AbcTheme.colors.textMuted)
        state.canDispute -> {
            Spacer(Modifier.height(spacing.space3))
            AbcButton(
                text = stringResource(R.string.action_dispute_transaction),
                onClick = { confirmDispute = true },
                variant = AbcButtonVariant.Danger
            )
        }
    }
    if (state.disputeFailed) {
        ActionMessage(
            stringResource(R.string.dispute_failed),
            AbcTheme.colors.danger
        )
    }
    if (confirmDispute) {
        AbcConfirmDialog(
            title = stringResource(R.string.dispute_dialog_title),
            text = stringResource(R.string.dispute_dialog_text),
            confirmLabel = stringResource(R.string.action_dispute),
            cancelLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                confirmDispute = false
                onConfirmDispute()
            },
            onDismiss = { confirmDispute = false }
        )
    }
}

@Composable
private fun ActionMessage(text: String, color: Color) {
    Text(
        text = text,
        style = AbcTheme.typography.small,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AbcTheme.spacing.space2)
    )
}

@Composable
private fun DetailRow(label: String, value: String, valueStyle: TextStyle) {
    AdaptiveRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AbcTheme.spacing.space3)
            .semantics(mergeDescendants = true) {},
        start = {
            Text(text = label, style = AbcTheme.typography.small, color = AbcTheme.colors.textMuted)
        }
    ) {
        Text(
            text = value,
            style = valueStyle,
            color = AbcTheme.colors.text,
            textAlign = TextAlign.End
        )
    }
}

private const val AMOUNT_MASK = "••••"
private const val PDF_MIME_TYPE = "application/pdf"

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun TransactionDetailPreview() {
    AbcTheme {
        TransactionDetailScreen(
            state = TransactionDetailUiState.Content(
                transaction = TransactionsPreviewData.transaction(
                    "tx-016",
                    "Melody Live Tickets",
                    TransactionCategory.ENTERTAINMENT,
                    -42_000,
                    disputed = true
                ),
                account = TransactionsPreviewData.checking,
                date = LocalDate.of(2026, 10, 8),
                time = LocalTime.of(22, 10),
                balancesHidden = false,
                isOffline = false,
                canDispute = false
            ),
            onBack = {},
            onRetry = {}
        )
    }
}
