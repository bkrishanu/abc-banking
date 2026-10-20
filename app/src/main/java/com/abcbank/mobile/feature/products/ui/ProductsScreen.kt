package com.abcbank.mobile.feature.products.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.AbcSliderRow
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.FlowScaffold
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.SkeletonBlock
import com.abcbank.mobile.core.designsystem.component.StatusBadge
import com.abcbank.mobile.core.designsystem.component.SummaryRows
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.TABULAR_FIGURES
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.feature.products.domain.CardProduct
import com.abcbank.mobile.feature.products.domain.Decision
import com.abcbank.mobile.feature.products.domain.EmploymentStatus
import com.abcbank.mobile.feature.products.domain.LoanProduct
import com.abcbank.mobile.feature.products.domain.ProductListing
import com.abcbank.mobile.feature.products.domain.ProductRules
import com.abcbank.mobile.feature.products.domain.SavingsProduct

/** Every products event, so each screen takes one parameter. */
data class ProductsEvents(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onApply: (String) -> Unit = {},
    val onViewAccount: (String) -> Unit = {},
    val onAmountChange: (Money) -> Unit = {},
    val onTermChange: (Int) -> Unit = {},
    val onContinue: () -> Unit = {},
    val onEmploymentSelect: (EmploymentStatus) -> Unit = {},
    val onIncomeChange: (String) -> Unit = {},
    val onAgreeChange: (Boolean) -> Unit = {},
    val onStartCheck: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onDone: () -> Unit = {}
)

/**
 * Explore Products (MobileBuild.md 5.1): High-Yield Savings, the Signature Rewards card and the
 * personal loan, with what this customer is offered and anything they already opened.
 */
@Composable
fun ProductsScreen(state: ProductsUiState, events: ProductsEvents, modifier: Modifier = Modifier) {
    val spacing = AbcTheme.spacing
    FlowScaffold(
        step = null,
        totalSteps = null,
        title = stringResource(R.string.products_title),
        onBack = events.onBack,
        modifier = modifier
    ) {
        when (state) {
            ProductsUiState.Loading -> Column(
                verticalArrangement = Arrangement.spacedBy(spacing.space4)
            ) {
                repeat(PLACEHOLDER_CARDS) {
                    SkeletonBlock(
                        Modifier
                            .fillMaxWidth()
                            .height(AbcTheme.dimens.avatarLarge * 2),
                        shape = AbcTheme.shapes.xl
                    )
                }
            }
            ProductsUiState.Empty -> ContentCard {
                Text(
                    stringResource(R.string.products_empty),
                    style = AbcTheme.typography.body,
                    color = AbcTheme.colors.text
                )
            }
            is ProductsUiState.Error -> ErrorState(
                message = stringResource(
                    if (state.error ==
                        DataError.OFFLINE
                    ) {
                        R.string.state_offline_no_data
                    } else {
                        R.string.products_error
                    }
                ),
                onRetry = events.onRetry
            )
            is ProductsUiState.Content -> {
                if (state.isOffline) {
                    OfflineBanner()
                    Spacer(Modifier.height(spacing.space4))
                }
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                    state.listings.forEach { ProductCard(it, events) }
                }
            }
        }
    }
}

private const val PLACEHOLDER_CARDS = 3

@Composable
private fun ProductCard(listing: ProductListing, events: ProductsEvents) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    val product = listing.product
    ContentCard(padding = spacing.space5) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AbcTheme.dimens.iconCircle)
                    .clip(AbcTheme.shapes.full)
                    .background(colors.surfaceMuted)
            ) {
                Icon(
                    painter = painterResource(productIcon(listing)),
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(AbcTheme.dimens.iconMedium)
                )
            }
            Spacer(Modifier.width(spacing.space3))
            Text(
                text = product.name,
                style = AbcTheme.typography.h3,
                color = colors.text,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
        }
        Spacer(Modifier.height(spacing.space3))
        productTerms(listing).forEach {
            Text(text = it, style = AbcTheme.typography.body, color = colors.text)
        }
        offerLine(listing)?.let {
            Spacer(Modifier.height(spacing.space2))
            Text(text = it, style = AbcTheme.typography.small, color = colors.positiveText)
        }
        Spacer(Modifier.height(spacing.space4))
        val opened = listing.openedAccountId
        when {
            opened != null -> {
                StatusBadge(stringResource(R.string.products_opened))
                Spacer(Modifier.height(spacing.space3))
                AbcButton(
                    text = stringResource(R.string.action_view_account),
                    onClick = { events.onViewAccount(opened) },
                    variant = AbcButtonVariant.Secondary
                )
            }
            listing.underReview -> StatusBadge(stringResource(R.string.products_under_review))
            else -> AbcButton(
                text = stringResource(
                    if (product is LoanProduct) {
                        R.string.action_check_your_rate
                    } else {
                        R.string.action_apply_now
                    }
                ),
                onClick = { events.onApply(product.id) },
                variant = AbcButtonVariant.Secondary
            )
        }
    }
}

private fun productIcon(listing: ProductListing): Int = when (listing.product) {
    is SavingsProduct -> R.drawable.ic_landmark
    is CardProduct -> R.drawable.ic_credit_card
    is LoanProduct -> R.drawable.ic_banknote
}

/** The key terms from DEMO_DATA.md, built from the catalog's numbers. */
@Composable
private fun productTerms(listing: ProductListing): List<String> =
    when (val product = listing.product) {
        is SavingsProduct -> listOf(
            stringResource(R.string.products_apy, MoneyFormatter.formatRate(product.apy)),
            stringResource(R.string.products_no_minimum_no_fee)
        )
        is CardProduct -> listOf(
            stringResource(
                if (product.annualFeeWaivedFirstYear) {
                    R.string.products_annual_fee_waived
                } else {
                    R.string.products_annual_fee
                },
                MoneyFormatter.formatWhole(product.annualFee)
            ),
            stringResource(R.string.products_card_points),
            stringResource(
                R.string.products_apr_range,
                MoneyFormatter.formatRate(product.aprMin),
                MoneyFormatter.formatRate(product.aprMax)
            )
        )
        is LoanProduct -> listOf(
            pluralStringResource(
                R.plurals.products_loan_amounts,
                product.termMonthsMax,
                MoneyFormatter.formatWhole(product.amountMin),
                MoneyFormatter.formatWhole(product.amountMax),
                product.termMonthsMin,
                product.termMonthsMax
            ),
            stringResource(
                R.string.products_apr_range,
                MoneyFormatter.formatRate(product.aprMin),
                MoneyFormatter.formatRate(product.aprMax)
            ),
            stringResource(R.string.products_no_origination_fee)
        )
    }

/** What the simulated check would offer this customer, shown before they apply. */
@Composable
private fun offerLine(listing: ProductListing): String? {
    val offer = listing.offer
    if (offer.decision != Decision.APPROVED || listing.openedAccountId != null) return null
    return when (val product = listing.product) {
        is SavingsProduct -> null
        is CardProduct -> stringResource(
            R.string.products_card_offer,
            MoneyFormatter.formatWhole(offer.cardLimit ?: return null),
            MoneyFormatter.formatRate(offer.cardApr ?: return null)
        )
        is LoanProduct -> stringResource(
            R.string.products_loan_offer,
            MoneyFormatter.formatRate(offer.loanApr ?: return null),
            MoneyFormatter.formatWhole(ProductRules.maxLoan(product, offer))
        )
    }
}

/**
 * The loan calculator (MobileBuild.md 5.1): amount and term sliders, the monthly installment,
 * APR and totals, updating as the sliders move.
 */
@Composable
fun LoanCalculatorScreen(
    state: ProductsUiState,
    events: ProductsEvents,
    modifier: Modifier = Modifier
) {
    val content = state as? ProductsUiState.Content
    val calculator = content?.calculator
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    FlowScaffold(
        step = null,
        totalSteps = null,
        title =
        content?.selected?.product?.name ?: stringResource(R.string.products_calculator_title),
        onBack = events.onBack,
        modifier = modifier,
        bottom = {
            if (content?.selected?.underReview == false) {
                AbcButton(
                    text = stringResource(R.string.action_apply_for_loan),
                    onClick = events.onContinue,
                    enabled = calculator != null
                )
            }
        }
    ) {
        if (calculator == null) {
            if (state is ProductsUiState.Error) {
                ErrorState(
                    stringResource(R.string.products_error),
                    events.onRetry
                )
            }
            return@FlowScaffold
        }
        val quote = calculator.quote
        ContentCard(padding = spacing.space5) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.products_monthly_payment),
                    style = AbcTheme.typography.small,
                    color = colors.textMuted
                )
                Text(
                    text = MoneyFormatter.format(quote.monthlyPayment),
                    style = AbcTheme.typography.balance.copy(fontFeatureSettings = TABULAR_FIGURES),
                    color = colors.text,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(
                        R.string.products_apr_value,
                        MoneyFormatter.formatRate(quote.apr)
                    ),
                    style = AbcTheme.typography.small,
                    color = colors.textMuted
                )
            }
        }
        Spacer(Modifier.height(spacing.space4))
        ContentCard {
            AbcSliderRow(
                label = stringResource(R.string.products_loan_amount),
                valueText = MoneyFormatter.formatWhole(quote.amount),
                value = quote.amount.cents.toFloat(),
                onValueChange = { raw ->
                    events.onAmountChange(
                        ProductRules.snapAmount(
                            Money(raw.toLong()),
                            calculator.amountMin,
                            calculator.amountMax
                        )
                    )
                },
                valueRange =
                calculator.amountMin.cents.toFloat()..calculator.amountMax.cents.toFloat(),
                steps = stops(
                    calculator.amountMax.cents - calculator.amountMin.cents,
                    ProductRules.AMOUNT_STEP.cents
                )
            )
            Spacer(Modifier.height(spacing.space3))
            AbcSliderRow(
                label = stringResource(R.string.products_loan_term),
                valueText = pluralStringResource(
                    R.plurals.products_months,
                    quote.termMonths,
                    quote.termMonths
                ),
                value = quote.termMonths.toFloat(),
                onValueChange = { raw ->
                    events.onTermChange(
                        ProductRules.snapTerm(
                            Math.round(raw),
                            calculator.termMin,
                            calculator.termMax
                        )
                    )
                },
                valueRange = calculator.termMin.toFloat()..calculator.termMax.toFloat(),
                steps = stops(
                    (calculator.termMax - calculator.termMin).toLong(),
                    ProductRules.TERM_STEP_MONTHS.toLong()
                )
            )
        }
        Spacer(Modifier.height(spacing.space4))
        ContentCard {
            SummaryRows(
                listOf(
                    stringResource(R.string.products_total_interest) to
                        MoneyFormatter.format(quote.totalInterest),
                    stringResource(R.string.products_total_repayment) to
                        MoneyFormatter.format(quote.totalRepayment)
                )
            )
        }
        Spacer(Modifier.height(spacing.space3))
        Text(
            text = stringResource(
                if (content.selected?.underReview ==
                    true
                ) {
                    R.string.products_calculator_review_note
                } else {
                    R.string.products_calculator_note
                }
            ),
            style = AbcTheme.typography.small,
            color = colors.textMuted
        )
    }
}

/** Material counts the stops between the ends. */
private fun stops(span: Long, step: Long): Int = ((span / step) - 1).coerceAtLeast(0).toInt()

@Preview(widthDp = 360, heightDp = 1300)
@Composable
private fun ProductsPreview() {
    AbcTheme { ProductsScreen(ProductsPreviewData.content(), ProductsEvents()) }
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun LoanCalculatorPreview() {
    AbcTheme {
        LoanCalculatorScreen(
            ProductsPreviewData.content(selected = "personal-loan"),
            ProductsEvents()
        )
    }
}
