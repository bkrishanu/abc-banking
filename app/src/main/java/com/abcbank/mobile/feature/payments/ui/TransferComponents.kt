package com.abcbank.mobile.feature.payments.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AbcBottomSheet
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.SkeletonBlock
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.feature.payments.domain.AmountError
import com.abcbank.mobile.feature.payments.domain.PayeeError
import com.abcbank.mobile.feature.payments.domain.TransferRules
import com.abcbank.mobile.feature.payments.domain.TransferSpeed

/** Every transfer event, so each step screen takes one parameter instead of a dozen lambdas. */
data class TransferEvents(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onSendTo: (String) -> Unit = {},
    val onMoveBetweenAccounts: () -> Unit = {},
    val onAddPayee: () -> Unit = {},
    val onPayeeNameChange: (String) -> Unit = {},
    val onPayeeRoutingChange: (String) -> Unit = {},
    val onPayeeAccountChange: (String) -> Unit = {},
    val onSavePayee: () -> Unit = {},
    val onSelectSource: (String) -> Unit = {},
    val onSelectDestination: (String) -> Unit = {},
    val onKey: (Char) -> Unit = {},
    val onDeleteKey: () -> Unit = {},
    val onMemoChange: (String) -> Unit = {},
    val onCategorySelect: (TransactionCategory?) -> Unit = {},
    val onSpeedSelect: (TransferSpeed) -> Unit = {},
    val onContinue: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onViewTransaction: (String) -> Unit = {},
    val onDone: () -> Unit = {},
    val onStepUpDigit: (Int) -> Unit = {},
    val onStepUpDelete: () -> Unit = {},
    val onStepUpCancel: () -> Unit = {}
)

/** Total steps: Recipient, Amount, Speed, Review, Receipt for P2P; no Speed between own accounts. */
fun TransferMode.totalSteps(): Int = if (this == TransferMode.SEND) SEND_STEPS else MOVE_STEPS

private const val SEND_STEPS = 5
private const val MOVE_STEPS = 4

/** Loading and error for the steps that need accounts or payees. */
@Composable
internal fun TransferStatePlaceholder(state: TransferUiState, onRetry: () -> Unit) {
    when (state) {
        TransferUiState.Loading -> Column(
            verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space3)
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
        is TransferUiState.Error -> ErrorState(
            message = stringResource(
                if (state.error ==
                    DataError.OFFLINE
                ) {
                    R.string.state_offline_no_data
                } else {
                    R.string.transfer_error
                }
            ),
            onRetry = onRetry
        )
        is TransferUiState.Content -> Unit
    }
}

private const val PLACEHOLDER_ROWS = 3

/**
 * A tappable field showing the chosen account (From / To on the amount step). Opens the
 * account sheet when there is more than one choice.
 */
@Composable
internal fun AccountField(
    label: String,
    account: Account?,
    balancesHidden: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = AbcTheme.colors
    val spacing = AbcTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
        Text(text = label, style = AbcTheme.typography.small, color = colors.textMuted)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AbcTheme.dimens.textFieldHeight)
                .clip(AbcTheme.shapes.sm)
                .background(colors.surface)
                .border(AbcTheme.dimens.borderWidth, colors.border, AbcTheme.shapes.sm)
                .then(
                    if (onClick !=
                        null
                    ) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = spacing.space4, vertical = spacing.space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = account?.let {
                        stringResource(R.string.account_name_last4, it.nickname, it.last4)
                    }.orEmpty(),
                    style = AbcTheme.typography.body,
                    color = colors.text
                )
                account?.let { spendableText(it, balancesHidden) }?.let {
                    Text(text = it, style = AbcTheme.typography.small, color = colors.textMuted)
                }
            }
            if (onClick != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(AbcTheme.dimens.iconMedium)
                )
            }
        }
    }
}

/** "Available $4,250.00", or "Available to Draw" for a line of credit; masked when hidden. */
@Composable
internal fun spendableText(account: Account, balancesHidden: Boolean): String? {
    val spendable = TransferRules.spendable(account) ?: return null
    val amount = if (balancesHidden) AMOUNT_MASK else MoneyFormatter.format(spendable)
    return stringResource(
        if (account is LoanAccount) {
            R.string.transfer_available_to_draw
        } else {
            R.string.transfer_available
        },
        amount
    )
}

/** Radio rows of accounts in a sheet. */
@Composable
internal fun AccountPickerSheet(
    title: String,
    accounts: List<Account>,
    selectedId: String?,
    balancesHidden: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    AbcBottomSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.space4)
                .padding(bottom = spacing.space6)
        ) {
            Text(
                text = title,
                style = AbcTheme.typography.h2,
                color = colors.text,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(spacing.space4))
            accounts.forEach { account ->
                val selected = account.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AbcTheme.dimens.touchTarget)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = {
                            onSelect(account.id)
                            onDismiss()
                        })
                        .padding(vertical = spacing.space1),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                    )
                    Spacer(Modifier.width(spacing.space2))
                    Column {
                        Text(
                            text = stringResource(
                                R.string.account_name_last4,
                                account.nickname,
                                account.last4
                            ),
                            style = AbcTheme.typography.body,
                            color = colors.text
                        )
                        spendableText(account, balancesHidden)?.let {
                            Text(
                                text = it,
                                style = AbcTheme.typography.small,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A circle with an icon: Between My Accounts, and the receipt's check on the positive fill. */
@Composable
internal fun IconCircle(
    icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = AbcTheme.dimens.iconCircle,
    iconSize: Dp = AbcTheme.dimens.iconMedium,
    background: Color = AbcTheme.colors.surfaceMuted
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(AbcTheme.shapes.full)
            .background(background)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = AbcTheme.colors.text,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun amountErrorText(error: AmountError): String = stringResource(
    when (error) {
        AmountError.ZERO -> R.string.transfer_error_zero
        AmountError.OVER_AVAILABLE -> R.string.transfer_error_over_available
        AmountError.OVER_OWED -> R.string.transfer_error_over_owed
        AmountError.SAME_ACCOUNT -> R.string.transfer_error_same_account
    }
)

@Composable
internal fun payeeErrorText(error: PayeeError): String = stringResource(
    when (error) {
        PayeeError.NAME_MISSING -> R.string.payee_error_name_missing
        PayeeError.NAME_TOO_LONG -> R.string.payee_error_name_too_long
        PayeeError.ROUTING_FORMAT -> R.string.payee_error_routing_format
        PayeeError.ROUTING_CHECKSUM -> R.string.payee_error_routing_checksum
        PayeeError.BANK_NOT_FOUND -> R.string.payee_error_bank_not_found
        PayeeError.ACCOUNT_FORMAT -> R.string.payee_error_account_format
        PayeeError.ACCOUNT_NOT_VERIFIED -> R.string.payee_error_account_not_verified
    }
)

@Composable
internal fun speedLabel(speed: TransferSpeed): String = stringResource(
    if (speed ==
        TransferSpeed.INSTANT
    ) {
        R.string.transfer_speed_instant
    } else {
        R.string.transfer_speed_standard
    }
)

@Composable
internal fun deliveryText(mode: TransferMode, speed: TransferSpeed): String = stringResource(
    when {
        mode == TransferMode.MOVE -> R.string.transfer_delivery_now
        speed == TransferSpeed.INSTANT -> R.string.transfer_delivery_instant
        else -> R.string.transfer_delivery_standard
    }
)

internal const val AMOUNT_MASK = "••••"
