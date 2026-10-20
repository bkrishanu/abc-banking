package com.abcbank.mobile.feature.payments.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AbcBottomSheet
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.StepUpSheet
import com.abcbank.mobile.core.designsystem.component.categoryLabel
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.TABULAR_FIGURES
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
    TransferStepScaffold(
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
                    if (mode ==
                        TransferMode.SEND
                    ) {
                        disclaimer = true
                    } else {
                        events.onConfirm()
                    }
                },
                enabled = content?.canContinueFromAmount == true
            )
        }
    ) {
        if (content == null) {
            TransferStatePlaceholder(state, events.onRetry)
            return@TransferStepScaffold
        }
        ReviewSummary(content)
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space1)
        ) {
            Text(
                stringResource(R.string.transfer_total),
                style = AbcTheme.typography.small,
                color = colors.textMuted
            )
            Text(
                text = MoneyFormatter.format(content.total),
                style = AbcTheme.typography.balance.copy(fontFeatureSettings = TABULAR_FIGURES),
                color = colors.text,
                textAlign = TextAlign.Center
            )
        }
    }
    Spacer(Modifier.height(spacing.space4))
    ContentCard {
        val recipient = content.beneficiary?.let {
            stringResource(R.string.transfer_payee_line, it.name, it.bankName, it.accountLast4)
        }
            ?: content.destination?.let {
                stringResource(R.string.account_name_last4, it.nickname, it.last4)
            }
        val rows = buildList {
            add(stringResource(R.string.transfer_to) to recipient.orEmpty())
            content.source?.let {
                add(
                    stringResource(R.string.transfer_from) to
                        stringResource(R.string.account_name_last4, it.nickname, it.last4)
                )
            }
            add(stringResource(R.string.transfer_amount) to MoneyFormatter.format(content.amount))
            if (content.mode == TransferMode.SEND) {
                add(stringResource(R.string.transfer_speed) to speedLabel(content.speed))
                add(
                    stringResource(R.string.transfer_fee) to
                        if (content.fee.isPositive) {
                            MoneyFormatter.format(
                                content.fee
                            )
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
                add(
                    stringResource(R.string.transfer_memo) to it
                )
            }
            content.category?.let {
                add(
                    stringResource(R.string.transfer_category) to categoryLabel(it)
                )
            }
        }
        SummaryRows(rows)
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
    TransferStepScaffold(
        step = mode.totalSteps(),
        totalSteps = mode.totalSteps(),
        title = stringResource(
            if (mode ==
                TransferMode.MOVE
            ) {
                R.string.transfer_receipt_moved
            } else {
                R.string.transfer_receipt_sent
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
        if (receipt == null) return@TransferStepScaffold
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
                    background = AbcTheme.colors.positive
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
            HorizontalDivider(color = AbcTheme.colors.border)
            SummaryRow(
                stringResource(R.string.transfer_reference),
                receipt.reference,
                AbcTheme.typography.mono
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
