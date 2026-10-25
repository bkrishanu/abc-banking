package com.abcbank.mobile.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.TABULAR_FIGURES
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TransactionCategory

/**
 * `TransactionRow` from DESIGN_SYSTEM.md section 06. [amount] is signed: incoming shows
 * "+$38.60" in positiveText, outgoing "-$125.00" in text colour. When [hidden], the amount is
 * dots on screen and left out of what TalkBack reads.
 */
@Composable
fun TransactionRow(
    title: String,
    subtitle: String,
    amount: Money,
    hidden: Boolean,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AbcTheme.colors
    val spacing = AbcTheme.spacing
    val amountText = MoneyFormatter.formatSigned(amount)
    val direction = stringResource(
        if (amount.isNegative) R.string.transaction_debit else R.string.transaction_credit
    )
    val description = if (hidden) {
        stringResource(R.string.cd_transaction_hidden, title, subtitle)
    } else {
        stringResource(R.string.cd_transaction, title, subtitle, amountText, direction)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.space3)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(spacing.space3))
        AdaptiveRow(
            modifier = Modifier.weight(1f),
            start = {
                Column {
                    Text(text = title, style = AbcTheme.typography.body, color = colors.text)
                    Text(
                        text = subtitle,
                        style = AbcTheme.typography.small,
                        color = colors.textMuted
                    )
                }
            }
        ) {
            Text(
                text = if (hidden) AMOUNT_MASK else amountText,
                style = AbcTheme.typography.body.copy(fontFeatureSettings = TABULAR_FIGURES),
                color = if (amount.isPositive && !hidden) colors.positiveText else colors.text,
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * The leading circle for a category, tonal as in Stitch: accentDeep on accentTint for money out,
 * positiveText on positiveTint for money in.
 */
@Composable
fun CategoryIcon(category: TransactionCategory, incoming: Boolean, modifier: Modifier = Modifier) {
    val colors = AbcTheme.colors
    Box(
        modifier = modifier
            .size(AbcTheme.dimens.iconCircle)
            .clip(AbcTheme.shapes.full)
            .background(if (incoming) colors.positiveTint else colors.accentTint),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(category.iconRes()),
            contentDescription = null,
            tint = if (incoming) colors.positiveText else colors.accentDeep,
            modifier = Modifier.size(AbcTheme.dimens.iconMedium)
        )
    }
}

/** People (transfers in and out) get initials; businesses get their category icon. */
@Composable
fun TransactionLeadingIcon(
    category: TransactionCategory,
    merchant: String,
    incoming: Boolean,
    modifier: Modifier = Modifier
) {
    if (category == TransactionCategory.TRANSFER_IN ||
        category == TransactionCategory.TRANSFER_OUT
    ) {
        InitialsAvatar(merchant, modifier)
    } else {
        CategoryIcon(category, incoming, modifier)
    }
}

/** The category's display name, a label in Title Case. */
@Composable
fun categoryLabel(category: TransactionCategory): String = stringResource(category.labelRes())

/** The category name's string resource, for places outside Compose such as PDF receipts. */
@StringRes
fun TransactionCategory.labelRes(): Int = when (this) {
    TransactionCategory.SHOPPING -> R.string.category_shopping
    TransactionCategory.DINING -> R.string.category_dining
    TransactionCategory.GROCERIES -> R.string.category_groceries
    TransactionCategory.ENTERTAINMENT -> R.string.category_entertainment
    TransactionCategory.TRAVEL -> R.string.category_travel
    TransactionCategory.TRANSPORT -> R.string.category_transport
    TransactionCategory.HEALTH -> R.string.category_health
    TransactionCategory.UTILITIES -> R.string.category_utilities
    TransactionCategory.HOUSING -> R.string.category_housing
    TransactionCategory.INCOME -> R.string.category_income
    TransactionCategory.INTEREST -> R.string.category_interest
    TransactionCategory.TRANSFER -> R.string.category_transfer
    TransactionCategory.TRANSFER_IN -> R.string.category_transfer_in
    TransactionCategory.TRANSFER_OUT -> R.string.category_transfer_out
    TransactionCategory.DEPOSIT -> R.string.category_deposit
}

@DrawableRes
fun TransactionCategory.iconRes(): Int = when (this) {
    TransactionCategory.SHOPPING -> R.drawable.ic_shopping_bag
    TransactionCategory.DINING -> R.drawable.ic_utensils
    TransactionCategory.GROCERIES -> R.drawable.ic_shopping_cart
    TransactionCategory.ENTERTAINMENT -> R.drawable.ic_ticket
    TransactionCategory.TRAVEL -> R.drawable.ic_plane
    TransactionCategory.TRANSPORT -> R.drawable.ic_car
    TransactionCategory.HEALTH -> R.drawable.ic_heart_pulse
    TransactionCategory.UTILITIES -> R.drawable.ic_zap
    TransactionCategory.HOUSING -> R.drawable.ic_house
    TransactionCategory.INCOME -> R.drawable.ic_banknote
    TransactionCategory.INTEREST -> R.drawable.ic_percent
    TransactionCategory.TRANSFER,
    TransactionCategory.TRANSFER_IN,
    TransactionCategory.TRANSFER_OUT -> R.drawable.ic_arrow_left_right
    TransactionCategory.DEPOSIT -> R.drawable.ic_landmark
}

private const val AMOUNT_MASK = "••••"

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun TransactionRowPreview() {
    AbcTheme {
        Column(Modifier.padding(AbcTheme.spacing.space4)) {
            TransactionRow(
                title = "Aster & Vine Boutique",
                subtitle = "Today, 3:14 pm",
                amount = Money(-12_500),
                hidden = false,
                leading = { CategoryIcon(TransactionCategory.SHOPPING, incoming = false) }
            )
            TransactionRow(
                title = "Helen Torres",
                subtitle = "Today, 8:09 am",
                amount = Money(3_860),
                hidden = false,
                leading = { InitialsAvatar("Helen Torres") }
            )
            TransactionRow(
                title = "Lumen Studio Payroll",
                subtitle = "Oct 16, 7:00 am",
                amount = Money(711_538),
                hidden = true,
                leading = { CategoryIcon(TransactionCategory.INCOME, incoming = true) }
            )
        }
    }
}
