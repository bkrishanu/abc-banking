package com.abcbank.mobile.feature.payments.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.AbcDatePickerDialog
import com.abcbank.mobile.core.designsystem.component.AbcSwitchRow
import com.abcbank.mobile.core.designsystem.component.AbcTextField
import com.abcbank.mobile.core.designsystem.component.AdaptiveRow
import com.abcbank.mobile.core.designsystem.component.BadgeTone
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.FlowScaffold
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.SectionHeader
import com.abcbank.mobile.core.designsystem.component.StatusBadge
import com.abcbank.mobile.core.designsystem.component.StepUpSheet
import com.abcbank.mobile.core.designsystem.component.SummaryRow
import com.abcbank.mobile.core.designsystem.component.SummaryRows
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.TABULAR_FIGURES
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.feature.payments.domain.BillListing
import com.abcbank.mobile.feature.payments.domain.BillStatus
import com.abcbank.mobile.feature.payments.domain.Biller
import java.time.LocalDate

/** Every bill pay event, so each screen takes one parameter. */
data class BillPayEvents(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenBill: (String) -> Unit = {},
    val onAddBiller: () -> Unit = {},
    val onSearch: (String) -> Unit = {},
    val onChooseBiller: (Biller?) -> Unit = {},
    val onAccountNumberChange: (String) -> Unit = {},
    val onFetchBill: () -> Unit = {},
    val onSaveBiller: () -> Unit = {},
    val onSelectSource: (String) -> Unit = {},
    val onPay: () -> Unit = {},
    val onSchedule: (LocalDate?) -> Unit = {},
    val canScheduleFor: (LocalDate) -> Boolean = { false },
    val onAutoPayChange: (Boolean) -> Unit = {},
    val onViewTransaction: (String) -> Unit = {},
    val onStepUpDigit: (Int) -> Unit = {},
    val onStepUpDelete: () -> Unit = {},
    val onStepUpCancel: () -> Unit = {}
)

/**
 * Pay Bills (MobileBuild.md 3.3): what is due in total, the bills to pay as cards with their due
 * badges, then Recently Paid, and Add a Biller.
 */
@Composable
fun BillsScreen(state: BillPayUiState, events: BillPayEvents, modifier: Modifier = Modifier) {
    val spacing = AbcTheme.spacing
    FlowScaffold(
        step = null,
        totalSteps = null,
        title = stringResource(R.string.bills_title),
        onBack = events.onBack,
        modifier = modifier,
        bottom = {
            if (state is BillPayUiState.Content) {
                AbcButton(
                    text = stringResource(R.string.action_add_biller),
                    onClick = events.onAddBiller,
                    variant = AbcButtonVariant.Secondary
                )
            }
        }
    ) {
        val content = state as? BillPayUiState.Content
        if (content == null) {
            BillStatePlaceholder(state, events.onRetry)
            return@FlowScaffold
        }
        if (content.isOffline) {
            OfflineBanner()
            Spacer(Modifier.height(spacing.space4))
        }
        if (content.bills.isEmpty()) {
            SectionHeader(title = stringResource(R.string.bills_yours))
            Text(
                stringResource(R.string.bills_empty),
                style = AbcTheme.typography.body,
                color = AbcTheme.colors.textMuted
            )
            return@FlowScaffold
        }
        val (paid, open) = content.bills.partition { it.bill.status == BillStatus.PAID }
        content.earliestDue?.let { earliest ->
            TotalDueCard(content.totalDue, earliest, content.balancesHidden)
            Spacer(Modifier.height(spacing.space6))
        }
        if (open.isNotEmpty()) {
            SectionHeader(title = stringResource(R.string.bills_yours))
            BillCards(open, content.balancesHidden, events.onOpenBill)
        }
        if (paid.isNotEmpty()) {
            Spacer(Modifier.height(spacing.space6))
            SectionHeader(title = stringResource(R.string.bills_recently_paid))
            BillCards(paid, content.balancesHidden, events.onOpenBill)
        }
    }
}

/** Total Due across unpaid bills, and the earliest due day (Stitch Pay Bills summary). */
@Composable
private fun TotalDueCard(total: Money, earliest: LocalDate, hidden: Boolean) {
    val colors = AbcTheme.colors
    ContentCard {
        AdaptiveRow(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            start = {
                Column {
                    Text(
                        stringResource(R.string.bills_total_due),
                        style = AbcTheme.typography.small,
                        color = colors.textMuted
                    )
                    Text(
                        text = if (hidden) AMOUNT_MASK else MoneyFormatter.format(total),
                        style = AbcTheme.typography.h2.copy(fontFeatureSettings = TABULAR_FIGURES),
                        color = colors.text
                    )
                }
            }
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.bills_earliest_due),
                    style = AbcTheme.typography.small,
                    color = colors.textMuted
                )
                Text(
                    DateFormatter.shortDate(earliest),
                    style = AbcTheme.typography.body,
                    color = colors.text
                )
            }
        }
    }
}

@Composable
private fun BillCards(bills: List<BillListing>, hidden: Boolean, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2)) {
        bills.forEach { listing -> BillRow(listing, hidden) { onOpen(listing.bill.id) } }
    }
}

@Composable
private fun BillStatePlaceholder(state: BillPayUiState, onRetry: () -> Unit) {
    // The transfer flow's placeholder already draws loading and error the same way.
    when (state) {
        BillPayUiState.Loading -> TransferStatePlaceholder(TransferUiState.Loading, onRetry)
        is BillPayUiState.Error -> TransferStatePlaceholder(
            TransferUiState.Error(state.error),
            onRetry
        )
        is BillPayUiState.Content -> Unit
    }
}

/** One bill card: receipt icon, biller, when, its badges, and the amount. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BillRow(listing: BillListing, hidden: Boolean, onClick: () -> Unit) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AbcTheme.dimens.touchTarget)
            .clip(AbcTheme.shapes.xl)
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(spacing.space3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(R.drawable.ic_receipt)
        Spacer(Modifier.width(spacing.space3))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
            Text(listing.biller.name, style = AbcTheme.typography.body, color = colors.text)
            Text(billWhen(listing), style = AbcTheme.typography.small, color = colors.textMuted)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space1)
            ) {
                StatusBadge(
                    billBadge(listing),
                    tone = if (listing.dueSoon) BadgeTone.Warning else BadgeTone.Neutral
                )
                if (listing.bill.autoPay && listing.bill.status != BillStatus.PAID) {
                    StatusBadge(stringResource(R.string.bills_auto_pay))
                }
            }
        }
        Spacer(Modifier.width(spacing.space2))
        Text(
            text = if (hidden) AMOUNT_MASK else MoneyFormatter.format(listing.bill.amountDue),
            style = AbcTheme.typography.body.copy(fontFeatureSettings = TABULAR_FIGURES),
            color = colors.text
        )
    }
}

/** Paid, Scheduled, Overdue, Due Today or "Due in 4 Days"; warning only while due soon. */
@Composable
private fun billBadge(listing: BillListing): String = when {
    listing.bill.status == BillStatus.PAID -> stringResource(R.string.bills_paid)
    listing.bill.status == BillStatus.SCHEDULED -> stringResource(R.string.bills_scheduled)
    listing.daysUntilDue < 0 -> stringResource(R.string.bills_overdue)
    listing.daysUntilDue == 0L -> stringResource(R.string.account_due_today)
    else -> pluralStringResource(
        R.plurals.account_due_in,
        listing.daysUntilDue.toInt(),
        listing.daysUntilDue.toInt()
    )
}

/** "Due Oct 24", "Scheduled for Oct 23" or "Paid Oct 20". */
@Composable
private fun billWhen(listing: BillListing): String = when {
    listing.paidDay != null && listing.bill.status == BillStatus.PAID ->
        stringResource(R.string.bills_paid_on, DateFormatter.shortDate(listing.paidDay))
    listing.scheduledDay != null -> stringResource(
        R.string.bills_scheduled_for,
        DateFormatter.shortDate(listing.scheduledDay)
    )
    else -> stringResource(R.string.bills_due_on, DateFormatter.shortDate(listing.dueDay))
}

/**
 * Add a Biller (MobileBuild.md 3.3): search the directory, enter the account number and fetch
 * the bill; saving it asks for the MPIN, as adding a payee does.
 */
@Composable
fun AddBillerScreen(state: BillPayUiState, events: BillPayEvents, modifier: Modifier = Modifier) {
    val content = state as? BillPayUiState.Content
    val form = content?.form
    val spacing = AbcTheme.spacing
    val chosen = form?.biller
    // With a biller chosen, Back returns to the directory before it leaves the screen.
    BackHandler(enabled = chosen != null) { events.onChooseBiller(null) }
    FlowScaffold(
        step = null,
        totalSteps = null,
        title = chosen?.name ?: stringResource(R.string.bills_add_title),
        onBack = if (chosen != null) ({ events.onChooseBiller(null) }) else events.onBack,
        modifier = modifier,
        bottom = {
            if (content?.failed == true) {
                Text(
                    stringResource(R.string.bills_failed),
                    style = AbcTheme.typography.small,
                    color = AbcTheme.colors.danger
                )
            }
            when {
                form?.fetched != null -> AbcButton(
                    text = stringResource(R.string.action_save_biller),
                    onClick = events.onSaveBiller,
                    loading = content.busy
                )
                chosen != null -> AbcButton(
                    text = stringResource(R.string.action_fetch_bill),
                    onClick = events.onFetchBill,
                    enabled = form.accountNumber.isNotEmpty()
                )
            }
        }
    ) {
        if (content == null || form == null) {
            BillStatePlaceholder(state, events.onRetry)
            return@FlowScaffold
        }
        if (chosen == null) {
            AbcTextField(
                value = form.query,
                onValueChange = events.onSearch,
                label = stringResource(R.string.bills_search),
                placeholder = stringResource(R.string.bills_search_placeholder)
            )
            Spacer(Modifier.height(spacing.space4))
            if (content.directory.isEmpty()) {
                Text(
                    stringResource(R.string.bills_no_match),
                    style = AbcTheme.typography.body,
                    color = AbcTheme.colors.textMuted
                )
                return@FlowScaffold
            }
            ContentCard(padding = spacing.space2) {
                content.directory.forEachIndexed { index, biller ->
                    if (index > 0) HorizontalDivider(color = AbcTheme.colors.border)
                    BillerRow(biller, added = biller.id in content.addedBillerIds) {
                        events.onChooseBiller(biller)
                    }
                }
            }
            return@FlowScaffold
        }
        Text(chosen.category, style = AbcTheme.typography.body, color = AbcTheme.colors.textMuted)
        Spacer(Modifier.height(spacing.space4))
        AbcTextField(
            value = form.accountNumber,
            onValueChange = events.onAccountNumberChange,
            label = stringResource(R.string.bills_account_number),
            placeholder = stringResource(R.string.bills_account_placeholder),
            errorText = form.error?.let { stringResource(R.string.bills_account_error) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            )
        )
        form.fetched?.let { fetched ->
            Spacer(Modifier.height(spacing.space4))
            ContentCard {
                SummaryRows(
                    listOf(
                        stringResource(R.string.bills_amount_due) to
                            MoneyFormatter.format(fetched.amountDue),
                        stringResource(R.string.bills_due_date) to
                            DateFormatter.fullDate(fetched.dueDay),
                        stringResource(R.string.bills_account) to
                            stringResource(R.string.bills_account_ending, fetched.accountLast4)
                    )
                )
            }
            Spacer(Modifier.height(spacing.space3))
            Text(
                stringResource(R.string.payee_step_up_note),
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

@Composable
private fun BillerRow(biller: Biller, added: Boolean, onClick: () -> Unit) {
    val spacing = AbcTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AbcTheme.dimens.touchTarget)
            .then(
                if (added) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick)
            )
            .padding(spacing.space2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(biller.name, style = AbcTheme.typography.body, color = AbcTheme.colors.text)
            Text(
                biller.category,
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted
            )
        }
        if (added) StatusBadge(stringResource(R.string.bills_added))
    }
}

/**
 * A bill (MobileBuild.md 3.3): amount and due date, Pay Now from checking or savings, a one-time
 * scheduled payment up to the due date, and the auto-pay switch.
 */
@Composable
fun BillDetailScreen(state: BillPayUiState, events: BillPayEvents, modifier: Modifier = Modifier) {
    val content = state as? BillPayUiState.Content
    val listing = content?.selected
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    var picker by rememberSaveable { mutableStateOf(false) }
    var accounts by rememberSaveable { mutableStateOf(false) }
    val paid = listing?.bill?.status == BillStatus.PAID
    FlowScaffold(
        step = null,
        totalSteps = null,
        title = listing?.biller?.name ?: stringResource(R.string.bills_title),
        onBack = events.onBack,
        modifier = modifier,
        bottom = {
            if (content == null || listing == null) return@FlowScaffold
            if (content.failed) {
                Text(
                    stringResource(R.string.bills_failed),
                    style = AbcTheme.typography.small,
                    color = colors.danger
                )
            }
            val receipt = content.paid
            if (receipt != null) {
                AbcButton(
                    text = stringResource(R.string.action_view_transaction),
                    onClick = { events.onViewTransaction(receipt.transfer.mainTransactionId) },
                    variant = AbcButtonVariant.Secondary
                )
            } else if (!paid) {
                AbcButton(
                    text = stringResource(R.string.action_pay_now),
                    onClick = events.onPay,
                    enabled = content.canPay,
                    loading = content.busy
                )
                if (listing.scheduledDay == null) {
                    AbcButton(
                        text = stringResource(R.string.action_schedule_payment),
                        onClick = { picker = true },
                        variant = AbcButtonVariant.Secondary
                    )
                } else {
                    AbcButton(
                        text = stringResource(R.string.action_cancel_scheduled),
                        onClick = { events.onSchedule(null) },
                        variant = AbcButtonVariant.Ghost
                    )
                }
            }
        }
    ) {
        if (content == null || listing == null) {
            BillStatePlaceholder(state, events.onRetry)
            return@FlowScaffold
        }
        val bill = listing.bill
        ContentCard(padding = spacing.space5) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(
                        if (paid) R.string.bills_amount_paid else R.string.bills_amount_due
                    ),
                    style = AbcTheme.typography.small,
                    color = colors.textMuted
                )
                Text(
                    text = if (content.balancesHidden) {
                        AMOUNT_MASK
                    } else {
                        MoneyFormatter.format(
                            bill.amountDue
                        )
                    },
                    style = AbcTheme.typography.balance.copy(fontFeatureSettings = TABULAR_FIGURES),
                    color = colors.text,
                    textAlign = TextAlign.Center
                )
                Text(billWhen(listing), style = AbcTheme.typography.small, color = colors.textMuted)
                if (bill.status == BillStatus.SCHEDULED) {
                    Spacer(Modifier.height(spacing.space2))
                    StatusBadge(stringResource(R.string.bills_scheduled), tone = BadgeTone.Neutral)
                }
            }
        }
        Spacer(Modifier.height(spacing.space4))
        content.paid?.let { plan ->
            ContentCard {
                Text(
                    stringResource(R.string.bills_paid_message),
                    style = AbcTheme.typography.body,
                    color = colors.text
                )
                Spacer(Modifier.height(spacing.space2))
                SummaryRow(
                    stringResource(R.string.transfer_reference),
                    plan.transfer.reference,
                    AbcTheme.typography.mono
                )
            }
            Spacer(Modifier.height(spacing.space4))
        }
        if (!paid) {
            AccountField(
                label = stringResource(R.string.bills_pay_from),
                account = content.source,
                balancesHidden = content.balancesHidden,
                onClick = if (content.sources.size > 1) ({ accounts = true }) else null
            )
            if (content.source != null && !content.canPay) {
                Spacer(Modifier.height(spacing.space1))
                Text(
                    stringResource(R.string.transfer_error_over_available),
                    style = AbcTheme.typography.small,
                    color = colors.danger
                )
            }
            Spacer(Modifier.height(spacing.space4))
        }
        ContentCard {
            val rows = buildList {
                add(stringResource(R.string.bills_category) to listing.biller.category)
                bill.accountLast4?.let {
                    add(
                        stringResource(R.string.bills_account) to
                            stringResource(R.string.bills_account_ending, it)
                    )
                }
                add(
                    stringResource(R.string.bills_due_date) to
                        DateFormatter.fullDate(listing.dueDay)
                )
            }
            SummaryRows(rows)
            if (!paid) {
                HorizontalDivider(color = colors.border)
                AbcSwitchRow(
                    title = stringResource(R.string.bills_auto_pay),
                    subtitle = stringResource(R.string.bills_auto_pay_hint),
                    checked = bill.autoPay,
                    onCheckedChange = events.onAutoPayChange
                )
            }
        }
        StepUpSheet(
            state = content.stepUp,
            onDigit = events.onStepUpDigit,
            onDelete = events.onStepUpDelete,
            onCancel = events.onStepUpCancel
        )
    }
    if (picker && listing != null) {
        AbcDatePickerDialog(
            initial = listing.dueDay.minusDays(1).coerceAtLeast(content.today.plusDays(1)),
            isSelectable = events.canScheduleFor,
            onPicked = {
                picker = false
                events.onSchedule(it)
            },
            onDismiss = { picker = false }
        )
    }
    if (accounts && content != null) {
        AccountPickerSheet(
            title = stringResource(R.string.bills_pay_from),
            accounts = content.sources,
            selectedId = content.source?.id,
            balancesHidden = content.balancesHidden,
            onSelect = events.onSelectSource,
            onDismiss = { accounts = false }
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun BillsPreview() {
    AbcTheme { BillsScreen(BillPayPreviewData.samanta(), BillPayEvents()) }
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun BillDetailPreview() {
    AbcTheme {
        BillDetailScreen(
            BillPayPreviewData.samanta(selected = "brightline-electric"),
            BillPayEvents()
        )
    }
}
