package com.abcbank.mobile.feature.home.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.TABULAR_FIGURES
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.InvestmentAccount
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TermDepositAccount

/** The Accounts card: one row per account, as MobileBuild.md 2.1 lists what each type shows. */
@Composable
internal fun AccountsCard(
    accounts: List<HomeAccountItem>,
    hidden: Boolean,
    onAccount: (String) -> Unit
) {
    ContentCard(padding = AbcTheme.spacing.space4) {
        accounts.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = AbcTheme.colors.border)
            AccountRow(item, hidden, onClick = { onAccount(item.account.id) })
        }
    }
}

@Composable
private fun AccountRow(item: HomeAccountItem, hidden: Boolean, onClick: () -> Unit) {
    val colors = AbcTheme.colors
    val typography = AbcTheme.typography
    val account = item.account
    val details = accountDetails(item, hidden)
    AdaptiveRow(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = AbcTheme.spacing.space3)
            .semantics(mergeDescendants = true) {},
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

@Composable
private fun accountDetails(item: HomeAccountItem, hidden: Boolean): AccountDetails {
    fun money(value: Money) = if (hidden) AMOUNT_MASK else MoneyFormatter.format(value)
    return when (val account = item.account) {
        is DepositAccount -> AccountDetails(
            amount = account.available,
            lines = listOfNotNull(
                account.apy?.let {
                    stringResource(R.string.account_apy, MoneyFormatter.formatRate(it))
                },
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
                },
                stringResource(R.string.account_apr, MoneyFormatter.formatRate(account.apr))
            )
        )
        is TermDepositAccount -> AccountDetails(
            amount = account.principal,
            lines = listOfNotNull(
                stringResource(R.string.account_apy, MoneyFormatter.formatRate(account.apy)),
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
