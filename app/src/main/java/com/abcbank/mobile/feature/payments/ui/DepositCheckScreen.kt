package com.abcbank.mobile.feature.payments.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.AmountKeypad
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.FlowScaffold
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.SummaryRow
import com.abcbank.mobile.core.designsystem.component.SummaryRows
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.TABULAR_FIGURES
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.feature.payments.domain.DepositError

@Composable
fun DepositCheckRoute(
    onBack: () -> Unit,
    onViewTransactions: (String) -> Unit,
    viewModel: DepositCheckViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DepositCheckScreen(
        state = state,
        events = DepositCheckEvents(
            onBack = onBack,
            onRetry = viewModel::onRetry,
            onSelectAccount = viewModel::onSelectAccount,
            onKey = viewModel::onKey,
            onDeleteKey = viewModel::onDeleteKey,
            onToggleFront = viewModel::onToggleFront,
            onToggleBack = viewModel::onToggleBack,
            onConfirm = viewModel::onConfirm,
            onViewTransactions = onViewTransactions
        )
    )
}

data class DepositCheckEvents(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onSelectAccount: (String) -> Unit = {},
    val onKey: (Char) -> Unit = {},
    val onDeleteKey: () -> Unit = {},
    val onToggleFront: () -> Unit = {},
    val onToggleBack: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onViewTransactions: (String) -> Unit = {}
)

/**
 * Deposit check (MobileBuild.md 2.3): the amount on the keypad, checking or savings, optional
 * placeholder cheque images, then a pending credit that clears in about 30 seconds.
 */
@Composable
fun DepositCheckScreen(
    state: DepositCheckUiState,
    events: DepositCheckEvents,
    modifier: Modifier = Modifier
) {
    val content = state as? DepositCheckUiState.Content
    val submitted = content?.submitted
    var picker by rememberSaveable { mutableStateOf(false) }
    val spacing = AbcTheme.spacing
    FlowScaffold(
        step = null,
        totalSteps = null,
        title = stringResource(
            if (submitted !=
                null
            ) {
                R.string.deposit_received_title
            } else {
                R.string.deposit_title
            }
        ),
        onBack = events.onBack,
        modifier = modifier,
        bottom = {
            if (content == null) return@FlowScaffold
            if (content.failed) {
                Text(
                    stringResource(R.string.deposit_failed),
                    style = AbcTheme.typography.small,
                    color = AbcTheme.colors.danger
                )
            }
            if (submitted != null) {
                AbcButton(
                    text = stringResource(R.string.action_view_transactions),
                    onClick = { events.onViewTransactions(submitted.accountId) },
                    variant = AbcButtonVariant.Secondary
                )
                AbcButton(text = stringResource(R.string.action_done), onClick = events.onBack)
            } else {
                AbcButton(
                    text = stringResource(R.string.action_deposit_check),
                    onClick = events.onConfirm,
                    enabled = content.canConfirm,
                    loading = content.submitting
                )
            }
        }
    ) {
        if (content == null) {
            when (state) {
                DepositCheckUiState.Loading -> TransferStatePlaceholder(
                    TransferUiState.Loading,
                    events.onRetry
                )
                is DepositCheckUiState.Error -> TransferStatePlaceholder(
                    TransferUiState.Error(state.error),
                    events.onRetry
                )
                is DepositCheckUiState.Content -> Unit
            }
            return@FlowScaffold
        }
        if (content.isOffline) {
            OfflineBanner()
            Spacer(Modifier.height(spacing.space4))
        }
        if (submitted != null) {
            Text(
                text = stringResource(R.string.deposit_received_text),
                style = AbcTheme.typography.body,
                color = AbcTheme.colors.text,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            Spacer(Modifier.height(spacing.space4))
            ContentCard {
                SummaryRows(
                    listOf(
                        stringResource(R.string.transfer_amount) to
                            MoneyFormatter.format(submitted.amount),
                        stringResource(R.string.transfer_to) to content.account?.let {
                            stringResource(R.string.account_name_last4, it.nickname, it.last4)
                        }.orEmpty(),
                        stringResource(R.string.deposit_status) to
                            stringResource(R.string.transaction_status_pending)
                    )
                )
                SummaryRow(
                    stringResource(R.string.transfer_reference),
                    submitted.reference,
                    AbcTheme.typography.mono
                )
            }
            return@FlowScaffold
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            AccountField(
                label = stringResource(R.string.deposit_to),
                account = content.account,
                balancesHidden = content.balancesHidden,
                onClick = if (content.accounts.size > 1) ({ picker = true }) else null
            )
            DepositAmount(content)
            AmountKeypad(
                onKey = events.onKey,
                onDelete = events.onDeleteKey,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.deposit_images),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                ChequeTile(
                    label = stringResource(R.string.deposit_front),
                    attached = content.frontImage,
                    image = R.drawable.cheque_front,
                    onToggle = events.onToggleFront,
                    modifier = Modifier.weight(1f)
                )
                ChequeTile(
                    label = stringResource(R.string.deposit_back),
                    attached = content.backImage,
                    image = R.drawable.cheque_back,
                    onToggle = events.onToggleBack,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                stringResource(R.string.deposit_images_note),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted
            )
        }
    }
    if (picker && content != null) {
        AccountPickerSheet(
            title = stringResource(R.string.deposit_to),
            accounts = content.accounts,
            selectedId = content.account?.id,
            balancesHidden = content.balancesHidden,
            onSelect = events.onSelectAccount,
            onDismiss = { picker = false }
        )
    }
}

@Composable
private fun DepositAmount(content: DepositCheckUiState.Content) {
    val colors = AbcTheme.colors
    val spoken = MoneyFormatter.format(content.amount)
    val shown = if (content.amountText.isEmpty()) {
        spoken
    } else {
        MoneyFormatter.formatTyped(content.amountText)
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = shown,
            style = AbcTheme.typography.balance.copy(fontFeatureSettings = TABULAR_FIGURES),
            color = if (content.amount.isPositive) colors.text else colors.textMuted,
            modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }
        )
        val error = content.amountError
        Text(
            text = when (error) {
                DepositError.OVER_LIMIT -> stringResource(
                    R.string.deposit_error_limit,
                    MoneyFormatter.format(content.limit)
                )
                DepositError.BELOW_MINIMUM -> stringResource(R.string.deposit_error_minimum)
                else -> stringResource(R.string.deposit_limit, MoneyFormatter.format(content.limit))
            },
            style = AbcTheme.typography.small,
            color = if (error != null) colors.danger else colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

/** A placeholder cheque image slot: tap to attach the bundled image, tap again to remove it. */
@Composable
private fun ChequeTile(
    label: String,
    attached: Boolean,
    image: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AbcTheme.colors
    val shape = AbcTheme.shapes.md
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(
                AbcTheme.dimens.borderWidth,
                if (attached) colors.primary else colors.border,
                shape
            )
            .toggleable(value = attached, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(AbcTheme.spacing.space3),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CHEQUE_ASPECT_RATIO)
                .clip(AbcTheme.shapes.sm)
                .background(colors.surfaceMuted)
        ) {
            Icon(
                painter = painterResource(if (attached) image else R.drawable.ic_plus),
                contentDescription = null,
                tint = if (attached) colors.text else colors.textMuted,
                modifier = if (attached) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.size(
                        AbcTheme.dimens.iconLarge
                    )
                }
            )
        }
        Text(
            text = stringResource(
                if (attached) R.string.deposit_image_attached else R.string.deposit_image_add,
                label
            ),
            style = AbcTheme.typography.small,
            color = colors.text,
            textAlign = TextAlign.Center
        )
    }
}

private const val CHEQUE_ASPECT_RATIO = 2.2f

@Preview(widthDp = 360, heightDp = 1200)
@Composable
private fun DepositCheckPreview() {
    AbcTheme {
        DepositCheckScreen(
            DepositCheckUiState.Content(
                accounts = listOf(TransferPreviewData.checking, TransferPreviewData.savings),
                account = TransferPreviewData.checking,
                amountText = "1250",
                amount = Money(125_000),
                limit = Money.ofDollars(10_000),
                amountError = null,
                frontImage = true,
                backImage = false,
                submitted = null,
                submitting = false,
                failed = false,
                balancesHidden = false,
                isOffline = false
            ),
            DepositCheckEvents()
        )
    }
}
