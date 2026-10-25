package com.abcbank.mobile.feature.payments.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AbcBottomSheet
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.AdaptiveRow
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.FlowScaffold
import com.abcbank.mobile.core.designsystem.component.InitialsAvatar
import com.abcbank.mobile.core.designsystem.component.StatusBadge
import com.abcbank.mobile.core.designsystem.component.StepUpSheet
import com.abcbank.mobile.core.designsystem.component.SummaryRow
import com.abcbank.mobile.core.designsystem.component.SummaryRows
import com.abcbank.mobile.core.designsystem.component.categoryLabel
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.TABULAR_FIGURES
import com.abcbank.mobile.feature.payments.domain.TransferRules
import com.abcbank.mobile.feature.payments.domain.TransferSpeed

/**
 * Review (MobileBuild.md 3.2, step 4): to, from, amount, fee, total and estimated delivery.
 * P2P opens the disclaimer sheet first; Agree and Send then asks for the MPIN over $1,000.
 */
@Composable
fun TransferReviewScreen(
    state: TransferUiState,
    events: TransferEvents,
    modifier: Modifier = Modifier
) {
    val content = state as? TransferUiState.Content
    var disclaimer by rememberSaveable { mutableStateOf(false) }
    val mode = content?.mode ?: TransferMode.SEND
    FlowScaffold(
        step = mode.totalSteps() - 1,
        totalSteps = mode.totalSteps(),
        title = stringResource(R.string.transfer_review_title),
        onBack = events.onBack,
        modifier = modifier,
        bottom = {
            if (content?.failed == true) {
                Text(
                    text = stringResource(R.string.transfer_failed),
                    style = AbcTheme.typography.small,
                    color = AbcTheme.colors.danger
                )
            }
            content?.amountError?.let {
                Text(
                    text = amountErrorText(it),
                    style = AbcTheme.typography.small,
                    color = AbcTheme.colors.danger
                )
            }
            AbcButton(
                text = stringResource(R.string.action_confirm_transfer),
                onClick = {
                    if (mode == TransferMode.SEND) disclaimer = true else events.onConfirm()
                },
                enabled = content?.canContinueFromAmount == true,
                loading = content?.sending == true
            )
        }
    ) {
        if (content == null) {
            TransferStatePlaceholder(state, events.onRetry)
            return@FlowScaffold
        }
        ReviewSummary(content)
        // Under the summary, not pinned: Confirm stays the one main action at the bottom.
        if (!content.sending) {
            Spacer(Modifier.height(AbcTheme.spacing.space2))
            AbcButton(
                text = stringResource(R.string.transfer_edit),
                onClick = events.onEdit,
                variant = AbcButtonVariant.Ghost
            )
            AbcButton(
                text = stringResource(R.string.transfer_cancel),
                onClick = events.onCancel,
                variant = AbcButtonVariant.Ghost
            )
        }
        StepUpSheet(
            state = content.stepUp,
            onDigit = events.onStepUpDigit,
            onDelete = events.onStepUpDelete,
            onCancel = events.onStepUpCancel
        )
    }
    if (disclaimer) {
        DisclaimerSheet(
            onAgree = {
                disclaimer = false
                events.onConfirm()
            },
            onDismiss = { disclaimer = false }
        )
    }
}

@Composable
private fun ReviewSummary(content: TransferUiState.Content) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    ContentCard(padding = spacing.space5) {
        RecipientHeader(content)
        Spacer(Modifier.height(spacing.space4))
        // The amount on its own tile, with the speed under it (Stitch Review).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AbcTheme.shapes.lg)
                .background(colors.background)
                .padding(spacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            Text(
                stringResource(R.string.transfer_amount),
                style = AbcTheme.typography.small,
                color = colors.textMuted
            )
            Text(
                text = MoneyFormatter.format(content.amount),
                style = AbcTheme.typography.balance.copy(fontFeatureSettings = TABULAR_FIGURES),
                color = colors.text,
                textAlign = TextAlign.Center
            )
            if (content.mode == TransferMode.SEND) StatusBadge(speedLabel(content.speed))
        }
        Spacer(Modifier.height(spacing.space2))
        val rows = buildList {
            content.source?.let {
                add(
                    stringResource(R.string.transfer_from) to
                        stringResource(R.string.account_name_last4, it.nickname, it.last4)
                )
            }
            if (content.mode == TransferMode.SEND) {
                add(
                    stringResource(R.string.transfer_fee) to
                        if (content.fee.isPositive) {
                            MoneyFormatter.format(content.fee)
                        } else {
                            stringResource(R.string.transfer_fee_free)
                        }
                )
            }
            add(
                stringResource(R.string.transfer_delivery) to
                    deliveryText(content.mode, content.speed)
            )
            content.memo.trim().takeIf { it.isNotEmpty() }?.let {
                add(stringResource(R.string.transfer_memo) to it)
            }
            content.category?.let {
                add(stringResource(R.string.transfer_category) to categoryLabel(it))
            }
        }
        SummaryRows(rows)
        Spacer(Modifier.height(spacing.space2))
        TotalTile(MoneyFormatter.format(content.total))
    }
}

/** Who receives the money: the payee with initials, or the own account it moves to. */
@Composable
private fun RecipientHeader(content: TransferUiState.Content) {
    val colors = AbcTheme.colors
    val payee = content.beneficiary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {}
    ) {
        if (payee != null) {
            InitialsAvatar(payee.name)
        } else {
            IconCircle(R.drawable.ic_arrow_left_right)
        }
        Spacer(Modifier.width(AbcTheme.spacing.space3))
        Column {
            Text(
                text = payee?.name ?: content.destination?.nickname.orEmpty(),
                style = AbcTheme.typography.h3,
                color = colors.text
            )
            Text(
                text = payee?.let {
                    stringResource(R.string.account_name_last4, it.bankName, it.accountLast4)
                } ?: content.destination?.let {
                    stringResource(R.string.account_last4, it.last4)
                }.orEmpty(),
                style = AbcTheme.typography.small,
                color = colors.textMuted
            )
        }
    }
}

/** "Total Deducted" and the amount plus fee, on a background tile. */
@Composable
internal fun TotalTile(total: String) {
    val colors = AbcTheme.colors
    AdaptiveRow(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AbcTheme.shapes.lg)
            .background(colors.background)
            .padding(horizontal = AbcTheme.spacing.space4, vertical = AbcTheme.spacing.space3)
            .semantics(mergeDescendants = true) {},
        start = {
            Text(
                stringResource(R.string.transfer_total_deducted),
                style = AbcTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.text
            )
        }
    ) {
        Text(
            total,
            style = AbcTheme.typography.h2.copy(fontFeatureSettings = TABULAR_FIGURES),
            color = colors.text
        )
    }
}

/** The disclaimer modal from MobileBuild.md 3.2, step 4. */
@Composable
private fun DisclaimerSheet(onAgree: () -> Unit, onDismiss: () -> Unit) {
    val spacing = AbcTheme.spacing
    AbcBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.space4)
                .padding(bottom = spacing.space6),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            Text(
                text = stringResource(R.string.transfer_disclaimer_title),
                style = AbcTheme.typography.h2,
                color = AbcTheme.colors.text,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.transfer_disclaimer_text),
                style = AbcTheme.typography.body,
                color = AbcTheme.colors.text
            )
            Spacer(Modifier.height(spacing.space2))
            AbcButton(text = stringResource(R.string.action_agree_and_send), onClick = onAgree)
            AbcButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                variant = AbcButtonVariant.Ghost
            )
        }
    }
}

/**
 * Receipt (MobileBuild.md 3.2, step 5): what was sent, the reference UUID, View Transaction
 * (which offers Download Receipt) and Done. Back also finishes, so the flow cannot resend.
 */
@Composable
fun TransferReceiptScreen(
    state: TransferUiState,
    events: TransferEvents,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = events.onDone)
    val receipt = (state as? TransferUiState.Content)?.receipt
    val mode = receipt?.mode ?: TransferMode.SEND
    val spacing = AbcTheme.spacing
    FlowScaffold(
        step = mode.totalSteps(),
        totalSteps = mode.totalSteps(),
        title = stringResource(
            if (mode ==
                TransferMode.MOVE
            ) {
                R.string.transfer_money_moved
            } else {
                R.string.transfer_money_sent
            }
        ),
        onBack = null,
        modifier = modifier,
        bottom = {
            if (receipt != null) {
                AbcButton(
                    text = stringResource(R.string.action_view_transaction),
                    onClick = { events.onViewTransaction(receipt.transactionId) },
                    variant = AbcButtonVariant.Secondary
                )
            }
            AbcButton(text = stringResource(R.string.action_done), onClick = events.onDone)
        }
    ) {
        val content = state as? TransferUiState.Content
        if (receipt == null || content == null) return@FlowScaffold
        ContentCard(padding = spacing.space5) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                IconCircle(
                    icon = R.drawable.ic_check,
                    size = AbcTheme.dimens.avatarLarge,
                    iconSize = AbcTheme.dimens.iconLarge,
                    emphasized = true
                )
                Text(
                    text = MoneyFormatter.format(receipt.amount),
                    style = AbcTheme.typography.balance.copy(fontFeatureSettings = TABULAR_FIGURES),
                    color = AbcTheme.colors.text
                )
                Text(
                    text = stringResource(
                        if (mode ==
                            TransferMode.MOVE
                        ) {
                            R.string.transfer_receipt_to_account
                        } else {
                            R.string.transfer_receipt_to
                        },
                        receipt.recipient
                    ),
                    style = AbcTheme.typography.body,
                    color = AbcTheme.colors.text,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(spacing.space4))
        ContentCard {
            val rows = buildList {
                add(stringResource(R.string.transfer_from) to receipt.source)
                if (mode == TransferMode.SEND) {
                    add(stringResource(R.string.transfer_speed) to speedLabel(receipt.speed))
                    add(
                        stringResource(R.string.transfer_fee) to
                            if (receipt.fee.isPositive) {
                                MoneyFormatter.format(
                                    receipt.fee
                                )
                            } else {
                                stringResource(R.string.transfer_fee_free)
                            }
                    )
                }
                add(stringResource(R.string.transfer_delivery) to deliveryText(mode, receipt.speed))
            }
            SummaryRows(rows)
            Spacer(Modifier.height(spacing.space2))
            TotalTile(MoneyFormatter.format(receipt.amount + receipt.fee))
            Spacer(Modifier.height(spacing.space2))
            ReferenceRow(receipt.reference)
            // The From account's balance from live data: writes apply to the cache at once.
            val newBalance = content.sources.firstOrNull { it.id == receipt.sourceAccountId }
                ?.let(TransferRules::spendable)
            if (newBalance != null) {
                HorizontalDivider(color = AbcTheme.colors.border)
                SummaryRow(
                    stringResource(R.string.transfer_new_balance),
                    if (content.balancesHidden) AMOUNT_MASK else MoneyFormatter.format(newBalance)
                )
            }
        }
    }
}

/** The reference in Mono with Copy; a short line confirms the copy. */
@Composable
private fun ReferenceRow(reference: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Column(Modifier.padding(vertical = AbcTheme.spacing.space2)) {
        AdaptiveRow(
            start = {
                Column {
                    Text(
                        stringResource(R.string.transfer_reference),
                        style = AbcTheme.typography.small,
                        color = AbcTheme.colors.textMuted
                    )
                    Text(reference, style = AbcTheme.typography.mono, color = AbcTheme.colors.text)
                }
            }
        ) {
            AbcButton(
                text = stringResource(R.string.action_copy),
                onClick = {
                    clipboard.setText(AnnotatedString(reference))
                    copied = true
                },
                variant = AbcButtonVariant.Secondary,
                modifier = Modifier.width(IntrinsicSize.Max)
            )
        }
        if (copied) {
            Text(
                stringResource(R.string.transfer_reference_copied),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.positiveText,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun TransferReviewPreview() {
    AbcTheme {
        TransferReviewScreen(
            TransferPreviewData.content(
                amountText = "1250",
                speed = TransferSpeed.INSTANT,
                memo = "Rent share"
            ),
            TransferEvents()
        )
    }
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun TransferReceiptPreview() {
    AbcTheme {
        TransferReceiptScreen(
            TransferPreviewData.content(
                amountText = "250"
            ).copy(receipt = TransferPreviewData.receipt),
            TransferEvents()
        )
    }
}
