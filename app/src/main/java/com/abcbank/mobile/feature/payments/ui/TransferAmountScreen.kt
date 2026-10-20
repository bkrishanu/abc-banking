package com.abcbank.mobile.feature.payments.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcTextField
import com.abcbank.mobile.core.designsystem.component.AmountKeypad
import com.abcbank.mobile.core.designsystem.component.ChoiceChip
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.FlowScaffold
import com.abcbank.mobile.core.designsystem.component.InitialsAvatar
import com.abcbank.mobile.core.designsystem.component.categoryLabel
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.TABULAR_FIGURES
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.feature.payments.domain.TransferRules
import com.abcbank.mobile.feature.payments.domain.TransferSpeed

/** The categories a P2P transfer can be tagged with (MobileBuild.md 3.2, step 2). */
private val TAG_CATEGORIES = listOf(
    TransactionCategory.HOUSING,
    TransactionCategory.UTILITIES,
    TransactionCategory.DINING,
    TransactionCategory.GROCERIES,
    TransactionCategory.SHOPPING,
    TransactionCategory.TRAVEL,
    TransactionCategory.ENTERTAINMENT,
    TransactionCategory.HEALTH
)

private enum class PickerTarget { SOURCE, DESTINATION }

/**
 * Step 2: From and To, the amount typed on the keypad with inline validation against the
 * available balance (MobileBuild.md 3.1), and for P2P an optional memo and category tag.
 */
@Composable
fun TransferAmountScreen(
    state: TransferUiState,
    events: TransferEvents,
    modifier: Modifier = Modifier
) {
    val content = state as? TransferUiState.Content
    var picker by rememberSaveable { mutableStateOf<PickerTarget?>(null) }
    FlowScaffold(
        step = 2,
        totalSteps = content?.mode?.totalSteps() ?: TransferMode.SEND.totalSteps(),
        title = stringResource(
            if (content?.mode ==
                TransferMode.MOVE
            ) {
                R.string.transfer_move_title
            } else {
                R.string.transfer_amount_title
            }
        ),
        onBack = events.onBack,
        modifier = modifier,
        bottom = {
            AbcButton(
                text = stringResource(R.string.action_continue),
                onClick = events.onContinue,
                enabled = content?.canContinueFromAmount == true
            )
        }
    ) {
        if (content == null) {
            TransferStatePlaceholder(state, events.onRetry)
            return@FlowScaffold
        }
        AmountForm(content, events, onPick = { picker = it })
    }
    if (content != null) {
        when (picker) {
            PickerTarget.SOURCE -> AccountPickerSheet(
                title = stringResource(R.string.transfer_from),
                accounts = content.sources,
                selectedId = content.source?.id,
                balancesHidden = content.balancesHidden,
                onSelect = events.onSelectSource,
                onDismiss = { picker = null }
            )
            PickerTarget.DESTINATION -> AccountPickerSheet(
                title = stringResource(R.string.transfer_to),
                accounts = content.destinations,
                selectedId = content.destination?.id,
                balancesHidden = content.balancesHidden,
                onSelect = events.onSelectDestination,
                onDismiss = { picker = null }
            )
            null -> Unit
        }
    }
}

@Composable
private fun AmountForm(
    content: TransferUiState.Content,
    events: TransferEvents,
    onPick: (PickerTarget) -> Unit
) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
        AccountField(
            label = stringResource(R.string.transfer_from),
            account = content.source,
            balancesHidden = content.balancesHidden,
            onClick = if (content.sources.size > 1) ({ onPick(PickerTarget.SOURCE) }) else null
        )
        if (content.mode == TransferMode.MOVE) {
            AccountField(
                label = stringResource(R.string.transfer_to),
                account = content.destination,
                balancesHidden = content.balancesHidden,
                onClick = if (content.destinations.size >
                    1
                ) {
                    ({ onPick(PickerTarget.DESTINATION) })
                } else {
                    null
                }
            )
        } else {
            content.beneficiary?.let { payee ->
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                    Text(
                        stringResource(R.string.transfer_to),
                        style = AbcTheme.typography.small,
                        color = colors.textMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(payee.name)
                        Spacer(Modifier.width(spacing.space3))
                        Column {
                            Text(payee.name, style = AbcTheme.typography.body, color = colors.text)
                            Text(
                                stringResource(
                                    R.string.account_name_last4,
                                    payee.bankName,
                                    payee.accountLast4
                                ),
                                style = AbcTheme.typography.small,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
        AmountDisplay(content)
        AmountKeypad(
            onKey = events.onKey,
            onDelete = events.onDeleteKey,
            modifier = Modifier.fillMaxWidth()
        )
        if (content.mode == TransferMode.SEND) {
            AbcTextField(
                value = content.memo,
                onValueChange = events.onMemoChange,
                label = stringResource(R.string.transfer_memo),
                placeholder = stringResource(R.string.transfer_memo_placeholder),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                )
            )
            CategoryTags(content.category, events.onCategorySelect)
        }
    }
}

/** The typed amount in the balance style, and the inline error or what is available. */
@Composable
private fun AmountDisplay(content: TransferUiState.Content) {
    val colors = AbcTheme.colors
    val shown = if (content.amountText.isEmpty()) {
        MoneyFormatter.format(content.amount)
    } else {
        MoneyFormatter.formatTyped(content.amountText)
    }
    val spoken = MoneyFormatter.format(content.amount)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space1)
    ) {
        Text(
            text = shown,
            style = AbcTheme.typography.balance.copy(fontFeatureSettings = TABULAR_FIGURES),
            color = if (content.amount.isPositive) colors.text else colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }
        )
        val error = content.amountError
        val message = when {
            error != null -> amountErrorText(error)
            content.mode == TransferMode.SEND &&
                content.speed == TransferSpeed.INSTANT &&
                content.fee.isPositive ->
                stringResource(R.string.transfer_fee_note, MoneyFormatter.format(content.fee))
            else -> content.source?.let { spendableText(it, content.balancesHidden) }
        }
        message?.let {
            Text(
                text = it,
                style = AbcTheme.typography.small,
                color = if (error != null) colors.danger else colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

/** Optional single-choice category tag; tapping the selected tag clears it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryTags(selected: TransactionCategory?, onSelect: (TransactionCategory?) -> Unit) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            stringResource(R.string.transfer_category),
            style = AbcTheme.typography.small,
            color = colors.textMuted
        )
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            TAG_CATEGORIES.forEach { category ->
                val isSelected = category == selected
                ChoiceChip(
                    label = categoryLabel(category),
                    selected = isSelected,
                    onClick = { onSelect(if (isSelected) null else category) }
                )
            }
        }
    }
}

/** Step 3, P2P only: Standard ACH (free, 1 to 2 business days) or Instant with its fee. */
@Composable
fun TransferSpeedScreen(
    state: TransferUiState,
    events: TransferEvents,
    modifier: Modifier = Modifier
) {
    val content = state as? TransferUiState.Content
    FlowScaffold(
        step = 3,
        totalSteps = TransferMode.SEND.totalSteps(),
        title = stringResource(R.string.transfer_speed_title),
        onBack = events.onBack,
        modifier = modifier,
        bottom = {
            content?.amountError?.let {
                Text(
                    text = amountErrorText(it),
                    style = AbcTheme.typography.small,
                    color = AbcTheme.colors.danger
                )
            }
            AbcButton(
                text = stringResource(R.string.action_continue),
                onClick = events.onContinue,
                enabled = content?.canContinueFromAmount == true
            )
        }
    ) {
        if (content == null) {
            TransferStatePlaceholder(state, events.onRetry)
            return@FlowScaffold
        }
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space3)
        ) {
            TransferSpeed.entries.forEach { speed ->
                val fee = TransferRules.fee(content.amount, speed)
                SpeedOption(
                    title = speedLabel(speed),
                    detail = deliveryText(TransferMode.SEND, speed),
                    price = if (fee.isPositive) {
                        stringResource(R.string.transfer_fee_amount, MoneyFormatter.format(fee))
                    } else {
                        stringResource(R.string.transfer_fee_free)
                    },
                    selected = content.speed == speed,
                    onClick = { events.onSpeedSelect(speed) }
                )
            }
            Text(
                text = stringResource(R.string.transfer_fee_rule),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted
            )
        }
    }
}

@Composable
private fun SpeedOption(
    title: String,
    detail: String,
    price: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AbcTheme.colors
    ContentCard(
        modifier = Modifier
            .border(
                AbcTheme.dimens.focusRingWidth,
                if (selected) colors.primary else colors.surface,
                AbcTheme.shapes.xl
            )
            .clip(AbcTheme.shapes.xl)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
            )
            Spacer(Modifier.width(AbcTheme.spacing.space3))
            Column(Modifier.weight(1f)) {
                Text(title, style = AbcTheme.typography.h3, color = colors.text)
                Text(detail, style = AbcTheme.typography.small, color = colors.textMuted)
            }
            Spacer(Modifier.width(AbcTheme.spacing.space2))
            Text(price, style = AbcTheme.typography.label, color = colors.text)
        }
    }
}

@Preview(widthDp = 360, heightDp = 1100)
@Composable
private fun TransferAmountPreview() {
    AbcTheme {
        TransferAmountScreen(TransferPreviewData.content(amountText = "250"), TransferEvents())
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun TransferSpeedPreview() {
    AbcTheme {
        TransferSpeedScreen(
            TransferPreviewData.content(amountText = "250", speed = TransferSpeed.INSTANT),
            TransferEvents()
        )
    }
}
