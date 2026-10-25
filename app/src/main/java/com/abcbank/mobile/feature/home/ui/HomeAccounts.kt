package com.abcbank.mobile.feature.home.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.common.LowBalanceRule
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AdaptiveRow
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.RateChip
import com.abcbank.mobile.core.designsystem.component.RateTone
import com.abcbank.mobile.core.designsystem.component.StatusBadge
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.TABULAR_FIGURES
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.InvestmentAccount
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TermDepositAccount

/**
 * Home's accounts, one card each as in Stitch: a tonal type icon, the name and last 4 with its
 * rate chip, and the amount with what MobileBuild.md 2.1 lists for the type.
 */
@Composable
internal fun AccountCards(
    accounts: List<HomeAccountItem>,
    hidden: Boolean,
    onAccount: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space3)) {
        accounts.forEach { item ->
            ContentCard(
                padding = AbcTheme.spacing.space4,
                modifier = Modifier
                    .clip(AbcTheme.shapes.xl)
                    .clickable(role = Role.Button, onClick = { onAccount(item.account.id) })
                    .semantics(mergeDescendants = true) {}
            ) {
                AccountRow(item, hidden)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountRow(item: HomeAccountItem, hidden: Boolean) {
    val colors = AbcTheme.colors
    val typography = AbcTheme.typography
    val spacing = AbcTheme.spacing
    val account = item.account
    val details = accountDetails(item, hidden)
    val chips = accountChips(item)
    Row(verticalAlignment = Alignment.Top) {
        AccountIcon(account)
        Spacer(Modifier.width(spacing.space3))
        Column(Modifier.weight(1f)) {
            AdaptiveRow(
                start = {
                    Column {
                        Text(text = account.nickname, style = typography.body, color = colors.text)
                        Text(
                            text = stringResource(R.string.account_last4, account.last4),
                            style = typography.small,
                            color = colors.textMuted
                        )
                    }
                }
            ) {
                AccountAmounts(item, details, hidden)
            }
            // Under the whole row, so a chip never wraps inside the narrow name column.
            if (chips.isNotEmpty()) {
                Spacer(Modifier.height(spacing.space2))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    verticalArrangement = Arrangement.spacedBy(spacing.space1)
                ) { chips.forEach { it() } }
            }
        }
    }
}

@Composable
private fun AccountAmounts(item: HomeAccountItem, details: AccountDetails, hidden: Boolean) {
    val colors = AbcTheme.colors
    val typography = AbcTheme.typography
    Column(horizontalAlignment = Alignment.End) {
        MaskedAmount(details.amount, hidden)
        if (item.isLowBalance) {
            Text(
                text = stringResource(
                    R.string.account_low_balance,
                    MoneyFormatter.format(LowBalanceRule.THRESHOLD)
                ),
                style = typography.small,
                color = colors.warning,
                textAlign = TextAlign.End
            )
        }
        details.lines.forEach { line ->
            Text(
                text = line,
                style = typography.small,
                color = colors.textMuted,
                textAlign = TextAlign.End
            )
        }
    }
}

/** A 40dp tonal circle with the account type's icon: green for savings, terracotta for debt. */
@Composable
private fun AccountIcon(account: Account) {
    val colors = AbcTheme.colors
    val (fill: Color, ink: Color) = when (account.type) {
        AccountType.CHECKING, AccountType.SAVINGS, AccountType.TERM_DEPOSIT ->
            colors.positiveTint to colors.positiveText
        AccountType.CREDIT, AccountType.LOAN -> colors.accentTint to colors.accentDeep
        AccountType.INVESTMENT -> colors.surfaceMuted to colors.text
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(AbcTheme.dimens.iconCircle)
            .clip(AbcTheme.shapes.full)
            .background(fill)
    ) {
        Icon(
            painter = painterResource(account.type.iconRes()),
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(AbcTheme.dimens.iconMedium)
        )
    }
}

@DrawableRes
private fun AccountType.iconRes(): Int = when (this) {
    AccountType.CHECKING -> R.drawable.ic_wallet
    AccountType.SAVINGS -> R.drawable.ic_piggy_bank
    AccountType.CREDIT -> R.drawable.ic_credit_card
    AccountType.LOAN -> R.drawable.ic_landmark
    AccountType.TERM_DEPOSIT -> R.drawable.ic_lock
    AccountType.INVESTMENT -> R.drawable.ic_trending_up
}

/** The rate chip (APY or APR) and, for a card with a coming due date, "Due in 3 Days". */
@Composable
private fun accountChips(item: HomeAccountItem): List<@Composable () -> Unit> {
    val chips = mutableListOf<@Composable () -> Unit>()
    when (val account = item.account) {
        is DepositAccount -> account.apy?.let { apy ->
            val text = stringResource(R.string.account_apy, MoneyFormatter.formatRate(apy))
            chips += { RateChip(text, RateTone.Earn) }
        }
        is TermDepositAccount -> {
            val text = stringResource(R.string.account_apy, MoneyFormatter.formatRate(account.apy))
            chips += { RateChip(text, RateTone.Earn) }
        }
        is LoanAccount -> {
            val text = stringResource(R.string.account_apr, MoneyFormatter.formatRate(account.apr))
            chips += { RateChip(text, RateTone.Cost) }
        }
        is CreditCardAccount, is InvestmentAccount -> Unit
    }
    item.dueInDays?.let { days ->
        val text = if (days == 0L) {
            stringResource(R.string.account_due_today)
        } else {
            pluralStringResource(R.plurals.account_due_in, days.toInt(), days.toInt())
        }
        chips += { StatusBadge(text) }
    }
    return chips
}

/** An amount in Body with tabular figures; dots and "Balance hidden" when masked. */
@Composable
internal fun MaskedAmount(money: Money, hidden: Boolean, modifier: Modifier = Modifier) {
    val hiddenDescription = stringResource(R.string.cd_balance_hidden)
    Text(
        text = if (hidden) AMOUNT_MASK else MoneyFormatter.format(money),
        style = AbcTheme.typography.body.copy(fontFeatureSettings = TABULAR_FIGURES),
        color = AbcTheme.colors.text,
        modifier = if (hidden) {
            modifier.semantics { contentDescription = hiddenDescription }
        } else {
            modifier
        }
    )
}

private class AccountDetails(val amount: Money, val lines: List<String>)

/** The amount and its detail lines; rates are chips, so they are not repeated here. */
@Composable
private fun accountDetails(item: HomeAccountItem, hidden: Boolean): AccountDetails {
    fun money(value: Money) = if (hidden) AMOUNT_MASK else MoneyFormatter.format(value)
    return when (val account = item.account) {
        is DepositAccount -> AccountDetails(
            amount = account.available,
            lines = listOfNotNull(
                if (account.ledger != account.available) {
                    stringResource(R.string.account_ledger, money(account.ledger))
                } else if (account.type == AccountType.CHECKING) {
                    stringResource(R.string.account_available_label)
                } else {
                    null
                }
            )
        )
        is CreditCardAccount -> AccountDetails(
            amount = account.currentBalance,
            lines = listOfNotNull(
                stringResource(R.string.account_available, money(account.availableCredit)),
                stringResource(R.string.account_statement, money(account.statementBalance)),
                item.dueDate?.let {
                    stringResource(R.string.account_due, DateFormatter.shortDate(it))
                }
            )
        )
        is LoanAccount -> AccountDetails(
            amount = account.owed,
            lines = listOfNotNull(
                account.creditLimit?.let {
                    stringResource(R.string.account_available, money(account.availableToDraw))
                }
            )
        )
        is TermDepositAccount -> AccountDetails(
            amount = account.principal,
            lines = listOfNotNull(
                item.maturesOn?.let {
                    stringResource(R.string.account_matures, DateFormatter.fullDate(it))
                }
            )
        )
        is InvestmentAccount -> AccountDetails(
            amount = account.portfolioValue,
            lines = listOf(stringResource(R.string.account_portfolio))
        )
    }
}

internal const val AMOUNT_MASK = "••••"
