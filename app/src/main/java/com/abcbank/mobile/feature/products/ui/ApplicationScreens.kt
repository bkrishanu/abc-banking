package com.abcbank.mobile.feature.products.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.AbcTextField
import com.abcbank.mobile.core.designsystem.component.ChoiceChip
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.FlowScaffold
import com.abcbank.mobile.core.designsystem.component.SummaryRow
import com.abcbank.mobile.core.designsystem.component.SummaryRows
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.products.domain.CardProduct
import com.abcbank.mobile.feature.products.domain.Decision
import com.abcbank.mobile.feature.products.domain.EmploymentStatus
import com.abcbank.mobile.feature.products.domain.LoanProduct
import com.abcbank.mobile.feature.products.domain.NewCardAccount
import com.abcbank.mobile.feature.products.domain.NewLoanAccount
import com.abcbank.mobile.feature.products.domain.NewSavingsAccount
import com.abcbank.mobile.feature.products.domain.SavingsProduct

private const val STEPS = 4

/** Step 1 (MobileBuild.md 5.2): the profile's details, with employment and income to confirm. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApplicationIdentityScreen(
    state: ProductsUiState,
    events: ProductsEvents,
    modifier: Modifier = Modifier
) {
    val content = state as? ProductsUiState.Content
    val spacing = AbcTheme.spacing
    FlowScaffold(
        step = 1,
        totalSteps = STEPS,
        title = stringResource(R.string.application_identity_title),
        onBack = events.onBack,
        modifier = modifier,
        bottom = {
            AbcButton(
                text = stringResource(R.string.action_continue),
                onClick = events.onContinue,
                enabled = content?.canContinueIdentity == true
            )
        }
    ) {
        if (content == null) return@FlowScaffold
        val profile = content.profile
        Text(
            text = stringResource(
                R.string.application_identity_intro,
                content.selected?.product?.name.orEmpty()
            ),
            style = AbcTheme.typography.body,
            color = AbcTheme.colors.textMuted
        )
        Spacer(Modifier.height(spacing.space4))
        ContentCard {
            SummaryRows(
                listOf(
                    stringResource(R.string.application_name) to profile.name,
                    stringResource(R.string.application_email) to profile.email,
                    stringResource(R.string.application_phone) to profile.phone,
                    stringResource(R.string.application_customer_id) to profile.customerId.value
                )
            )
        }
        Spacer(Modifier.height(spacing.space4))
        Text(
            stringResource(R.string.application_employment),
            style = AbcTheme.typography.small,
            color = AbcTheme.colors.textMuted
        )
        Spacer(Modifier.height(spacing.space2))
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            EmploymentStatus.entries.forEach { status ->
                ChoiceChip(
                    label = employmentLabel(status),
                    selected = content.employment == status,
                    onClick = { events.onEmploymentSelect(status) }
                )
            }
        }
        content.employmentDetail?.let {
            Spacer(Modifier.height(spacing.space2))
            Text(it, style = AbcTheme.typography.small, color = AbcTheme.colors.textMuted)
        }
        Spacer(Modifier.height(spacing.space4))
        AbcTextField(
            value = content.incomeText,
            onValueChange = events.onIncomeChange,
            label = stringResource(R.string.application_income),
            placeholder = stringResource(R.string.application_income_placeholder),
            errorText = if (content.incomeError) {
                stringResource(
                    R.string.application_income_error
                )
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )
    }
}

/** Step 2: the product's terms and disclosures, accepted with one checkbox. */
@Composable
fun ApplicationTermsScreen(
    state: ProductsUiState,
    events: ProductsEvents,
    modifier: Modifier = Modifier
) {
    val content = state as? ProductsUiState.Content
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    FlowScaffold(
        step = 2,
        totalSteps = STEPS,
        title = stringResource(R.string.application_terms_title),
        onBack = events.onBack,
        modifier = modifier,
        bottom = {
            AbcButton(
                text = stringResource(R.string.action_agree_and_continue),
                onClick = events.onContinue,
                enabled = content?.agreed == true
            )
        }
    ) {
        if (content == null) return@FlowScaffold
        ContentCard {
            disclosures(content).forEachIndexed { index, text ->
                if (index > 0) HorizontalDivider(color = colors.border)
                Text(
                    text = text,
                    style = AbcTheme.typography.body,
                    color = colors.text,
                    modifier = Modifier.padding(vertical = spacing.space3)
                )
            }
        }
        Spacer(Modifier.height(spacing.space4))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AbcTheme.dimens.touchTarget)
                .toggleable(
                    value = content.agreed,
                    role = Role.Checkbox,
                    onValueChange = events.onAgreeChange
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = content.agreed,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = colors.primary)
            )
            Spacer(Modifier.width(spacing.space2))
            Text(
                stringResource(R.string.application_agree),
                style = AbcTheme.typography.body,
                color = colors.text
            )
        }
    }
}

@Composable
private fun disclosures(content: ProductsUiState.Content): List<String> {
    val common =
        listOf(
            stringResource(R.string.disclosure_demo),
            stringResource(R.string.disclosure_electronic)
        )
    val specific = when (val product = content.selected?.product) {
        is SavingsProduct -> listOf(
            stringResource(
                R.string.disclosure_savings_rate,
                MoneyFormatter.formatRate(product.apy)
            ),
            stringResource(R.string.disclosure_savings_variable)
        )
        is CardProduct -> listOf(
            stringResource(
                R.string.disclosure_card_apr,
                MoneyFormatter.formatRate(product.aprMin),
                MoneyFormatter.formatRate(product.aprMax)
            ),
            stringResource(
                R.string.disclosure_card_fee,
                MoneyFormatter.formatWhole(product.annualFee)
            )
        )
        is LoanProduct -> listOf(
            stringResource(R.string.disclosure_loan_rate),
            stringResource(R.string.disclosure_loan_payments)
        )
        null -> emptyList()
    }
    return specific + stringResource(R.string.disclosure_credit_check) + common
}

/**
 * Step 3: a moment of checking, then the simulated decision from the credit score. Approved
 * shows the offer and opens the account; referred records the application for review.
 */
@Composable
fun ApplicationDecisionScreen(
    state: ProductsUiState,
    events: ProductsEvents,
    modifier: Modifier = Modifier
) {
    val content = state as? ProductsUiState.Content
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    LaunchedEffect(content != null) { if (content != null) events.onStartCheck() }
    val listing = content?.selected
    val decided = content?.check == CheckState.DECIDED
    val approved = listing?.offer?.decision == Decision.APPROVED
    FlowScaffold(
        step = 3,
        totalSteps = STEPS,
        title = stringResource(
            when {
                !decided -> R.string.application_checking_title
                approved -> R.string.application_approved_title
                else -> R.string.application_referred_title
            }
        ),
        onBack = if (decided) events.onBack else null,
        modifier = modifier,
        bottom = {
            if (content?.failed == true) {
                Text(
                    stringResource(R.string.application_failed),
                    style = AbcTheme.typography.small,
                    color = colors.danger
                )
            }
            if (decided) {
                AbcButton(
                    text = stringResource(
                        if (approved) {
                            R.string.action_accept_and_open
                        } else {
                            R.string.action_submit_for_review
                        }
                    ),
                    onClick = events.onSubmit,
                    loading = content?.submitting == true
                )
            }
        }
    ) {
        if (listing == null) return@FlowScaffold
        if (!decided) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.space8)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space4)
            ) {
                CircularProgressIndicator(
                    color = colors.primary,
                    modifier = Modifier.size(AbcTheme.dimens.iconCircle)
                )
                Text(
                    text = stringResource(R.string.application_checking_text),
                    style = AbcTheme.typography.body,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )
            }
            return@FlowScaffold
        }
        Text(
            text = stringResource(
                if (approved) {
                    R.string.application_approved_text
                } else {
                    R.string.application_referred_text
                },
                listing.product.name
            ),
            style = AbcTheme.typography.body,
            color = colors.text,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        if (approved) {
            Spacer(Modifier.height(spacing.space4))
            ContentCard { SummaryRows(offerRows(content)) }
        }
    }
}

@Composable
private fun offerRows(content: ProductsUiState.Content): List<Pair<String, String>> {
    val listing = content.selected ?: return emptyList()
    val offer = listing.offer
    return when (val product = listing.product) {
        is SavingsProduct -> listOf(
            stringResource(R.string.application_row_apy) to MoneyFormatter.formatRate(product.apy),
            stringResource(R.string.application_row_monthly_fee) to
                MoneyFormatter.format(product.monthlyFee)
        )
        is CardProduct -> listOfNotNull(
            offer.cardLimit?.let {
                stringResource(R.string.application_row_limit) to
                    MoneyFormatter.formatWhole(it)
            },
            offer.cardApr?.let {
                stringResource(R.string.application_row_apr) to
                    MoneyFormatter.formatRate(it)
            },
            stringResource(R.string.application_row_annual_fee) to
                if (product.annualFeeWaivedFirstYear) {
                    stringResource(
                        R.string.application_fee_waived,
                        MoneyFormatter.formatWhole(product.annualFee)
                    )
                } else {
                    MoneyFormatter.formatWhole(product.annualFee)
                }
        )
        is LoanProduct -> content.calculator?.quote?.let { quote ->
            listOf(
                stringResource(
                    R.string.products_loan_amount
                ) to MoneyFormatter.format(quote.amount),
                stringResource(R.string.products_loan_term) to
                    pluralStringResource(
                        R.plurals.products_months,
                        quote.termMonths,
                        quote.termMonths
                    ),
                stringResource(
                    R.string.application_row_apr
                ) to MoneyFormatter.formatRate(quote.apr),
                stringResource(R.string.products_monthly_payment) to
                    MoneyFormatter.format(quote.monthlyPayment)
            )
        }.orEmpty()
    }
}

/**
 * Step 4: the account is open (or the application is with the team). Back finishes too, so the
 * application cannot be sent twice.
 */
@Composable
fun ApplicationCompleteScreen(
    state: ProductsUiState,
    events: ProductsEvents,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = events.onDone)
    val plan = (state as? ProductsUiState.Content)?.submitted
    val account = plan?.account
    val spacing = AbcTheme.spacing
    FlowScaffold(
        step = STEPS,
        totalSteps = STEPS,
        title = stringResource(
            if (account !=
                null
            ) {
                R.string.application_complete_title
            } else {
                R.string.application_received_title
            }
        ),
        onBack = null,
        modifier = modifier,
        bottom = {
            if (account != null) {
                AbcButton(
                    text = stringResource(R.string.action_view_account),
                    onClick = { events.onViewAccount(account.id) },
                    variant = AbcButtonVariant.Secondary
                )
            }
            AbcButton(text = stringResource(R.string.action_done), onClick = events.onDone)
        }
    ) {
        if (plan == null) return@FlowScaffold
        Text(
            text = if (account != null) {
                stringResource(R.string.application_complete_text, plan.application.productName)
            } else {
                stringResource(R.string.application_received_text, plan.application.productName)
            },
            style = AbcTheme.typography.body,
            color = AbcTheme.colors.text
        )
        if (account == null) return@FlowScaffold
        Spacer(Modifier.height(spacing.space4))
        ContentCard {
            val rows = buildList {
                add(
                    stringResource(R.string.application_row_account) to
                        stringResource(R.string.account_name_last4, account.nickname, account.last4)
                )
                when (account) {
                    is NewSavingsAccount -> add(
                        stringResource(R.string.application_row_apy) to
                            MoneyFormatter.formatRate(account.apy)
                    )
                    is NewCardAccount -> {
                        add(
                            stringResource(R.string.application_row_limit) to
                                MoneyFormatter.formatWhole(account.creditLimit)
                        )
                        add(
                            stringResource(R.string.application_row_apr) to
                                MoneyFormatter.formatRate(account.apr)
                        )
                    }
                    is NewLoanAccount -> {
                        add(
                            stringResource(R.string.products_loan_amount) to
                                MoneyFormatter.format(account.principal)
                        )
                        add(
                            stringResource(R.string.products_monthly_payment) to
                                MoneyFormatter.format(account.monthlyPayment)
                        )
                    }
                }
            }
            SummaryRows(rows)
            HorizontalDivider(color = AbcTheme.colors.border)
            SummaryRow(
                stringResource(R.string.application_row_reference),
                plan.application.id,
                AbcTheme.typography.mono
            )
        }
        Spacer(Modifier.height(spacing.space3))
        Text(
            text = stringResource(
                when (account) {
                    is NewSavingsAccount -> R.string.application_next_savings
                    is NewCardAccount -> R.string.application_next_card
                    is NewLoanAccount -> R.string.application_next_loan
                }
            ),
            style = AbcTheme.typography.small,
            color = AbcTheme.colors.textMuted
        )
    }
}

@Composable
private fun employmentLabel(status: EmploymentStatus): String = stringResource(
    when (status) {
        EmploymentStatus.EMPLOYED -> R.string.employment_employed
        EmploymentStatus.SELF_EMPLOYED -> R.string.employment_self_employed
        EmploymentStatus.RETIRED -> R.string.employment_retired
        EmploymentStatus.STUDENT -> R.string.employment_student
        EmploymentStatus.NOT_EMPLOYED -> R.string.employment_not_employed
    }
)

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun ApplicationIdentityPreview() {
    AbcTheme {
        ApplicationIdentityScreen(
            ProductsPreviewData.content(selected = "personal-loan"),
            ProductsEvents()
        )
    }
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun ApplicationDecisionPreview() {
    AbcTheme {
        ApplicationDecisionScreen(
            ProductsPreviewData.content(
                selected = "personal-loan"
            ).copy(check = CheckState.DECIDED),
            ProductsEvents()
        )
    }
}

@Preview(widthDp = 360, heightDp = 900)
@Composable
private fun ApplicationCompletePreview() {
    AbcTheme { ApplicationCompleteScreen(ProductsPreviewData.completedLoan(), ProductsEvents()) }
}
