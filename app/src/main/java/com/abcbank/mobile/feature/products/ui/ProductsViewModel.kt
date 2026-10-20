package com.abcbank.mobile.feature.products.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.UserProfile
import com.abcbank.mobile.feature.products.domain.ApplicationAnswers
import com.abcbank.mobile.feature.products.domain.ApplicationPlan
import com.abcbank.mobile.feature.products.domain.ApplicationResult
import com.abcbank.mobile.feature.products.domain.EmploymentStatus
import com.abcbank.mobile.feature.products.domain.LoanProduct
import com.abcbank.mobile.feature.products.domain.LoanQuote
import com.abcbank.mobile.feature.products.domain.ObserveProductsUseCase
import com.abcbank.mobile.feature.products.domain.ProductListing
import com.abcbank.mobile.feature.products.domain.ProductRules
import com.abcbank.mobile.feature.products.domain.ProductsOverview
import com.abcbank.mobile.feature.products.domain.SubmitApplicationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where step 3 is: checking for a moment, then the decision shown, then submitted. */
enum class CheckState { NOT_STARTED, CHECKING, DECIDED }

/** The loan calculator: the quote, and the slider ranges for this customer. */
data class LoanCalculator(
    val quote: LoanQuote,
    val amountMin: Money,
    val amountMax: Money,
    val termMin: Int,
    val termMax: Int
)

sealed interface ProductsUiState {
    data object Loading : ProductsUiState

    /** The catalog has no products. */
    data object Empty : ProductsUiState

    data class Error(val error: DataError) : ProductsUiState

    data class Content(
        val profile: UserProfile,
        val listings: List<ProductListing>,
        val isOffline: Boolean,
        /** The product being applied for, or null on the catalog. */
        val selected: ProductListing?,
        val calculator: LoanCalculator?,
        val employment: EmploymentStatus,
        val employmentDetail: String?,
        /** Whole dollars, as typed. */
        val incomeText: String,
        val incomeError: Boolean,
        val agreed: Boolean,
        val check: CheckState,
        /** What the application wrote, once submitted. */
        val submitted: ApplicationPlan?,
        val submitting: Boolean,
        val failed: Boolean
    ) : ProductsUiState {
        val canContinueIdentity: Boolean get() = !incomeError && incomeText.isNotEmpty()
    }
}

/** The draft, kept in SavedStateHandle so process death does not lose the application. */
private data class Draft(
    val productId: String? = null,
    val amount: Money? = null,
    val termMonths: Int? = null,
    val employment: EmploymentStatus? = null,
    val incomeText: String? = null,
    val agreed: Boolean = false
)

private data class FlowState(
    val check: CheckState = CheckState.NOT_STARTED,
    val submitted: ApplicationPlan? = null,
    val submitting: Boolean = false,
    val failed: Boolean = false
)

/** Catalog, loan calculator and the four application steps (MobileBuild.md 5.1 and 5.2). */
@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeProducts: ObserveProductsUseCase,
    private val submitApplication: SubmitApplicationUseCase
) : ViewModel() {
    private val draft = MutableStateFlow(restoreDraft())
    private val flowState = MutableStateFlow(FlowState())
    private val attempt = MutableStateFlow(0)
    private var latest: ProductsUiState.Content? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProductsUiState> = combine(
        attempt.flatMapLatest { observeProducts() },
        draft,
        flowState
    ) { result, currentDraft, currentFlow ->
        when (result) {
            is DataResult.Failure -> ProductsUiState.Error(result.error)
            is DataResult.Success -> if (result.value.listings.isEmpty()) {
                ProductsUiState.Empty
            } else {
                toContent(result.value, currentDraft, currentFlow)
            }
        }.also { latest = it as? ProductsUiState.Content }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        ProductsUiState.Loading
    )

    /** Starts an application (or the calculator for a loan) afresh. */
    fun onSelectProduct(productId: String) {
        editDraft { Draft(productId = productId) }
        flowState.value = FlowState()
    }

    fun onAmountChange(amount: Money) = editDraft { it.copy(amount = amount) }

    fun onTermChange(months: Int) = editDraft { it.copy(termMonths = months) }

    fun onEmploymentSelect(status: EmploymentStatus) = editDraft { it.copy(employment = status) }

    fun onIncomeChange(text: String) = editDraft {
        it.copy(
            incomeText = text.filter { c ->
                c in '0'..'9'
            }.trimStart('0').take(MAX_INCOME_DIGITS)
        )
    }

    fun onAgreeChange(agreed: Boolean) = editDraft { it.copy(agreed = agreed) }

    /** Step 3 opens with a short check, as a real decision would take a moment. */
    fun onStartCheck() {
        if (flowState.value.check != CheckState.NOT_STARTED) return
        flowState.update { it.copy(check = CheckState.CHECKING) }
        viewModelScope.launch {
            delay(CHECK_MS)
            flowState.update { it.copy(check = CheckState.DECIDED) }
        }
    }

    /** Accept the offer (or, when referred, record the application) and write it. */
    fun onSubmit() {
        val content = latest ?: return
        val listing = content.selected ?: return
        // The live flow state, not the last emitted UI state, so a quick double tap sends once.
        val current = flowState.value
        if (current.submitted != null || current.submitting) return
        flowState.update { it.copy(submitting = true, failed = false) }
        viewModelScope.launch {
            val answers = ApplicationAnswers(
                employment = content.employment,
                annualIncome = Money.ofDollars(content.incomeText.toLongOrNull() ?: 0),
                loanAmount = content.calculator?.quote?.amount,
                termMonths = content.calculator?.quote?.termMonths
            )
            when (val result = submitApplication(listing.product, content.profile, answers)) {
                is ApplicationResult.Submitted -> flowState.update {
                    it.copy(submitted = result.plan, submitting = false)
                }
                else -> flowState.update { it.copy(submitting = false, failed = true) }
            }
        }
    }

    fun onRetry() {
        attempt.value++
    }

    private fun editDraft(change: (Draft) -> Draft) {
        draft.update(change)
        saveDraft(draft.value)
    }

    private fun toContent(
        overview: ProductsOverview,
        d: Draft,
        f: FlowState
    ): ProductsUiState.Content {
        val profile = overview.profile
        val selected = overview.listings.firstOrNull { it.product.id == d.productId }
        val calculator = (selected?.product as? LoanProduct)?.let { loan ->
            val max = ProductRules.maxLoan(loan, selected.offer)
            val amount = ProductRules.snapAmount(
                d.amount ?: loan.defaultAmount,
                loan.amountMin,
                max
            )
            val term = ProductRules.snapTerm(
                d.termMonths ?: loan.defaultTermMonths,
                loan.termMonthsMin,
                loan.termMonthsMax
            )
            // Referred customers still see the calculator at the catalog's highest rate.
            val apr = selected.offer.loanApr ?: loan.aprMax
            LoanCalculator(
                ProductRules.quote(amount, term, apr),
                loan.amountMin,
                max,
                loan.termMonthsMin,
                loan.termMonthsMax
            )
        }
        val incomeText =
            d.incomeText ?: (profile.annualIncome.cents / Money.CENTS_PER_DOLLAR).toString()
        val income = incomeText.toLongOrNull()?.let(Money::ofDollars) ?: Money.ZERO
        return ProductsUiState.Content(
            profile = profile,
            listings = overview.listings,
            isOffline = overview.isFromCache,
            selected = selected,
            calculator = calculator,
            employment = d.employment ?: ProductRules.employmentStatus(profile.employment),
            employmentDetail = ProductRules.employmentDetail(profile.employment),
            incomeText = incomeText,
            incomeError = !ProductRules.isValidIncome(income),
            agreed = d.agreed,
            check = f.check,
            submitted = f.submitted,
            submitting = f.submitting,
            failed = f.failed
        )
    }

    private fun restoreDraft(): Draft = Draft(
        productId = savedStateHandle.get<String>(KEY_PRODUCT)?.takeIf { it.isNotBlank() },
        amount = savedStateHandle.get<Long>(KEY_AMOUNT)?.takeIf { it > 0 }?.let(::Money),
        termMonths = savedStateHandle.get<Int>(KEY_TERM)?.takeIf { it > 0 },
        employment = savedStateHandle.get<String>(KEY_EMPLOYMENT)?.let { value ->
            EmploymentStatus.entries.firstOrNull { it.name == value }
        },
        incomeText = savedStateHandle.get<String>(KEY_INCOME)?.filter {
            it in '0'..'9'
        }?.take(MAX_INCOME_DIGITS),
        agreed = savedStateHandle.get<Boolean>(KEY_AGREED) ?: false
    )

    private fun saveDraft(d: Draft) {
        savedStateHandle[KEY_PRODUCT] = d.productId
        savedStateHandle[KEY_AMOUNT] = d.amount?.cents
        savedStateHandle[KEY_TERM] = d.termMonths
        savedStateHandle[KEY_EMPLOYMENT] = d.employment?.name
        savedStateHandle[KEY_INCOME] = d.incomeText
        savedStateHandle[KEY_AGREED] = d.agreed
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val CHECK_MS = 1_500L
        const val MAX_INCOME_DIGITS = 8
        const val KEY_PRODUCT = "applicationProduct"
        const val KEY_AMOUNT = "applicationAmount"
        const val KEY_TERM = "applicationTerm"
        const val KEY_EMPLOYMENT = "applicationEmployment"
        const val KEY_INCOME = "applicationIncome"
        const val KEY_AGREED = "applicationAgreed"
    }
}
