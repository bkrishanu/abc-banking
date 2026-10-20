package com.abcbank.mobile.feature.payments.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.AbcTextField
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.FlowScaffold
import com.abcbank.mobile.core.designsystem.component.InitialsAvatar
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.SectionHeader
import com.abcbank.mobile.core.designsystem.component.StepUpSheet
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.Beneficiary
import com.abcbank.mobile.feature.payments.domain.PayeeError

/**
 * Step 1 (MobileBuild.md 3.1 and 3.2): Between My Accounts, then saved payees with recent ones
 * first, and Add New Payee.
 */
@Composable
fun TransferRecipientScreen(
    state: TransferUiState,
    events: TransferEvents,
    modifier: Modifier = Modifier
) {
    val spacing = AbcTheme.spacing
    FlowScaffold(
        step = 1,
        totalSteps = (state as? TransferUiState.Content)?.mode?.totalSteps()
            ?: TransferMode.SEND.totalSteps(),
        title = stringResource(R.string.transfer_recipient_title),
        onBack = events.onBack,
        modifier = modifier,
        bottom = {
            if (state is TransferUiState.Content) {
                AbcButton(
                    text = stringResource(R.string.action_add_new_payee),
                    onClick = events.onAddPayee,
                    variant = AbcButtonVariant.Secondary
                )
            }
        }
    ) {
        if (state !is TransferUiState.Content) {
            TransferStatePlaceholder(state, events.onRetry)
            return@FlowScaffold
        }
        if (state.isOffline) {
            OfflineBanner()
            Spacer(Modifier.height(spacing.space4))
        }
        ContentCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AbcTheme.dimens.touchTarget)
                    .clickable(role = Role.Button, onClick = events.onMoveBetweenAccounts),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconCircle(R.drawable.ic_arrow_left_right)
                Spacer(Modifier.width(spacing.space3))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.transfer_between_accounts),
                        style = AbcTheme.typography.body,
                        color = AbcTheme.colors.text
                    )
                    Text(
                        text = stringResource(R.string.transfer_between_accounts_hint),
                        style = AbcTheme.typography.small,
                        color = AbcTheme.colors.textMuted
                    )
                }
            }
        }
        Spacer(Modifier.height(spacing.space4))
        SectionHeader(title = stringResource(R.string.transfer_saved_payees))
        if (state.beneficiaries.isEmpty()) {
            Text(
                text = stringResource(R.string.transfer_no_payees),
                style = AbcTheme.typography.body,
                color = AbcTheme.colors.textMuted
            )
        } else {
            ContentCard(padding = spacing.space2) {
                state.beneficiaries.forEachIndexed { index, payee ->
                    if (index > 0) HorizontalDivider(color = AbcTheme.colors.border)
                    PayeeRow(payee) { events.onSendTo(payee.id) }
                }
            }
        }
    }
}

@Composable
private fun PayeeRow(payee: Beneficiary, onClick: () -> Unit) {
    val spacing = AbcTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AbcTheme.dimens.touchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(spacing.space2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InitialsAvatar(payee.name)
        Spacer(Modifier.width(spacing.space3))
        Column(Modifier.weight(1f)) {
            Text(text = payee.name, style = AbcTheme.typography.body, color = AbcTheme.colors.text)
            Text(
                text = stringResource(
                    R.string.account_name_last4,
                    payee.bankName,
                    payee.accountLast4
                ),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted
            )
        }
        if (payee.isRecent) {
            Text(
                text = stringResource(R.string.transfer_recent),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted
            )
        }
    }
}

/**
 * Add Beneficiary (MobileBuild.md 3.2): name, routing number with the bank looked up as it is
 * typed, and account number. Checked before step-up, so a typo never costs an MPIN entry.
 */
@Composable
fun AddPayeeScreen(state: TransferUiState, events: TransferEvents, modifier: Modifier = Modifier) {
    val spacing = AbcTheme.spacing
    val content = state as? TransferUiState.Content
    val form = content?.payee ?: PayeeForm()
    FlowScaffold(
        step = 1,
        totalSteps = TransferMode.SEND.totalSteps(),
        title = stringResource(R.string.payee_title),
        onBack = events.onBack,
        modifier = modifier,
        bottom = {
            if (content?.failed == true) {
                Text(
                    text = stringResource(R.string.payee_failed),
                    style = AbcTheme.typography.small,
                    color = AbcTheme.colors.danger
                )
            }
            AbcButton(
                text = stringResource(R.string.action_save_payee),
                onClick = events.onSavePayee,
                enabled = content != null
            )
        }
    ) {
        if (content == null) {
            TransferStatePlaceholder(state, events.onRetry)
            return@FlowScaffold
        }
        val error = form.error
        val nameError = error?.takeIf { it.isNameError() }
        val routingError = error?.takeIf { it.isRoutingError() }
        val accountError = error?.takeIf { it.isAccountError() }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            AbcTextField(
                value = form.name,
                onValueChange = events.onPayeeNameChange,
                label = stringResource(R.string.payee_name),
                placeholder = stringResource(R.string.payee_name_placeholder),
                errorText = nameError?.let { payeeErrorText(it) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                AbcTextField(
                    value = form.routing,
                    onValueChange = events.onPayeeRoutingChange,
                    label = stringResource(R.string.payee_routing),
                    placeholder = stringResource(R.string.payee_routing_placeholder),
                    errorText = routingError?.let { payeeErrorText(it) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Next
                    )
                )
                form.bankName?.takeIf { routingError == null }?.let {
                    Text(
                        text = it,
                        style = AbcTheme.typography.small,
                        color = AbcTheme.colors.positiveText
                    )
                }
            }
            AbcTextField(
                value = form.accountNumber,
                onValueChange = events.onPayeeAccountChange,
                label = stringResource(R.string.payee_account),
                placeholder = stringResource(R.string.payee_account_placeholder),
                errorText = accountError?.let { payeeErrorText(it) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                )
            )
            Text(
                text = stringResource(R.string.payee_step_up_note),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted
            )
        }
        StepUpSheet(
            state = content.stepUp,
            onDigit = events.onStepUpDigit,
            onDelete = events.onStepUpDelete,
            onCancel = events.onStepUpCancel
        )
    }
}

private fun PayeeError.isNameError() = this == PayeeError.NAME_MISSING ||
    this == PayeeError.NAME_TOO_LONG

private fun PayeeError.isRoutingError() = this == PayeeError.ROUTING_FORMAT ||
    this == PayeeError.ROUTING_CHECKSUM ||
    this == PayeeError.BANK_NOT_FOUND

private fun PayeeError.isAccountError() = !isNameError() && !isRoutingError()

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun TransferRecipientPreview() {
    AbcTheme { TransferRecipientScreen(TransferPreviewData.content(), TransferEvents()) }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun AddPayeePreview() {
    AbcTheme {
        AddPayeeScreen(
            TransferPreviewData.content().copy(
                payee = PayeeForm(
                    name = "Jordan Ellis",
                    routing = "500000021",
                    accountNumber = "40021187",
                    bankName = "Northfield Bank"
                )
            ),
            TransferEvents()
        )
    }
}
