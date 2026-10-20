package com.abcbank.mobile.feature.products

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Rate
import com.abcbank.mobile.feature.products.data.catalogProducts
import com.abcbank.mobile.feature.products.domain.Application
import com.abcbank.mobile.feature.products.domain.ApplicationAnswers
import com.abcbank.mobile.feature.products.domain.ApplicationPlan
import com.abcbank.mobile.feature.products.domain.ApplicationPlanner
import com.abcbank.mobile.feature.products.domain.ApplicationResult
import com.abcbank.mobile.feature.products.domain.Decision
import com.abcbank.mobile.feature.products.domain.EmploymentStatus
import com.abcbank.mobile.feature.products.domain.NewCardAccount
import com.abcbank.mobile.feature.products.domain.NewLoanAccount
import com.abcbank.mobile.feature.products.domain.NewSavingsAccount
import com.abcbank.mobile.feature.products.domain.ObserveProductsUseCase
import com.abcbank.mobile.feature.products.domain.Product
import com.abcbank.mobile.feature.products.domain.ProductCatalog
import com.abcbank.mobile.feature.products.domain.ProductRules
import com.abcbank.mobile.feature.products.domain.ProductsData
import com.abcbank.mobile.feature.products.domain.ProductsRepository
import com.abcbank.mobile.feature.products.domain.SubmitApplicationUseCase
import com.abcbank.mobile.feature.products.ui.CheckState
import com.abcbank.mobile.feature.products.ui.ProductsPreviewData
import com.abcbank.mobile.feature.products.ui.ProductsUiState
import com.abcbank.mobile.feature.products.ui.ProductsViewModel
import com.abcbank.mobile.testing.MainDispatcherRule
import com.abcbank.mobile.testing.ManualClock
import java.io.File
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val NOW: Instant = Instant.parse("2026-10-20T15:00:00Z")
private val HYSA = ProductsPreviewData.hysa
private val CARD = ProductsPreviewData.card
private val LOAN = ProductsPreviewData.loan
private val SAMANTA = ProductsPreviewData.profile
private val MARCUS = SAMANTA.copy(
    name = "Marcus Lee",
    creditScore = 688,
    annualIncome = Money.ofDollars(68_000),
    employment = "Employed, Software Engineer I at Brightpath Software"
)
private val LOW_SCORE = SAMANTA.copy(creditScore = 640)

class ProductRulesTest {
    @Test
    fun `the bundled catalog maps to the three documented products`() {
        val catalog = Json.parseToJsonElement(
            File("src/main/assets/seed/catalog.json").readText()
        ).jsonObject
        assertEquals(listOf(HYSA, CARD, LOAN), catalogProducts(catalog))
    }

    @Test
    fun `credit tiers follow the documented decision table`() {
        val top = ProductRules.offer(792, LOAN)
        assertEquals(Decision.APPROVED, top.decision)
        assertEquals(Rate(799), top.loanApr)
        assertEquals(Money.ofDollars(50_000), top.maxLoan)
        val middle = ProductRules.offer(688, CARD)
        assertEquals(Money.ofDollars(3_000), middle.cardLimit)
        assertEquals(Rate(2_824), middle.cardApr)
        assertEquals(Rate(2_124), ProductRules.offer(740, CARD).cardApr)
        assertEquals(Decision.REFERRED, ProductRules.offer(659, LOAN).decision)
        // Opening a savings account needs no credit check.
        assertEquals(Decision.APPROVED, ProductRules.offer(500, HYSA).decision)
        assertEquals(Money.ofDollars(15_000), ProductRules.maxLoan(LOAN, middle))
    }

    @Test
    fun `the installment is amortised and rounded half up to the cent`() {
        assertEquals(
            Money(31_332),
            ProductRules.monthlyPayment(Money.ofDollars(10_000), Rate(799), 36)
        )
        assertEquals(
            Money(33_739),
            ProductRules.monthlyPayment(Money.ofDollars(15_000), Rate(1_249), 60)
        )
        assertEquals(
            Money(18_335),
            ProductRules.monthlyPayment(Money.ofDollars(2_000), Rate(1_799), 12)
        )
        assertEquals(Money(8_333), ProductRules.monthlyPayment(Money.ofDollars(1_000), Rate(0), 12))
        val quote = ProductRules.quote(Money.ofDollars(10_000), 36, Rate(799))
        assertEquals(Money(31_332 * 36L), quote.totalRepayment)
        assertEquals(quote.totalRepayment - Money.ofDollars(10_000), quote.totalInterest)
    }

    @Test
    fun `sliders snap to $500 and 6-month steps inside the range`() {
        val min = Money.ofDollars(2_000)
        val max = Money.ofDollars(15_000)
        assertEquals(
            Money.ofDollars(10_500),
            ProductRules.snapAmount(Money.ofDollars(10_260), min, max)
        )
        assertEquals(
            Money.ofDollars(10_000),
            ProductRules.snapAmount(Money.ofDollars(10_240), min, max)
        )
        assertEquals(max, ProductRules.snapAmount(Money.ofDollars(40_000), min, max))
        assertEquals(min, ProductRules.snapAmount(Money.ofDollars(100), min, max))
        assertEquals(36, ProductRules.snapTerm(35, 12, 60))
        assertEquals(12, ProductRules.snapTerm(2, 12, 60))
        assertEquals(60, ProductRules.snapTerm(70, 12, 60))
    }

    @Test
    fun `employment is read from the profile text`() {
        assertEquals(EmploymentStatus.EMPLOYED, ProductRules.employmentStatus(SAMANTA.employment))
        assertEquals(
            EmploymentStatus.SELF_EMPLOYED,
            ProductRules.employmentStatus(
                "Self-employed, Founder and CEO of Okafor Freight Partners"
            )
        )
        assertEquals(EmploymentStatus.RETIRED, ProductRules.employmentStatus("Retired"))
        assertNull(ProductRules.employmentDetail("Retired"))
        assertEquals(
            "Product Design Lead at Lumen Studio",
            ProductRules.employmentDetail(SAMANTA.employment)
        )
        assertFalse(ProductRules.isValidIncome(Money.ZERO))
        assertTrue(ProductRules.isValidIncome(Money.ofDollars(68_000)))
    }
}

class ApplicationPlannerTest {
    private val answers =
        ApplicationAnswers(
            EmploymentStatus.EMPLOYED,
            Money.ofDollars(185_000),
            Money.ofDollars(10_000),
            36
        )

    @Test
    fun `each product opens its own kind of account`() {
        val savings = ApplicationPlanner.plan(
            HYSA,
            ProductRules.offer(792, HYSA),
            answers,
            "3f6c2a9e-81d4",
            NOW
        )
        val account = savings.account as NewSavingsAccount
        assertEquals("hysa-3f6c2a9e", account.id)
        assertEquals("High-Yield Savings", account.nickname)
        assertEquals(Rate(450), account.apy)
        assertEquals(account.id, savings.application.accountId)

        val card = ApplicationPlanner.plan(
            CARD,
            ProductRules.offer(688, CARD),
            answers,
            "a1",
            NOW
        ).account as NewCardAccount
        assertEquals("Signature Rewards Card", card.nickname)
        assertEquals(Money.ofDollars(3_000), card.creditLimit)
        assertEquals(NOW.plus(Duration.ofDays(25)), card.firstDueDate)

        val loan = ApplicationPlanner.plan(
            LOAN,
            ProductRules.offer(792, LOAN),
            answers,
            "b2",
            NOW
        ).account as NewLoanAccount
        assertEquals(Money.ofDollars(10_000), loan.principal)
        assertEquals(Money(31_332), loan.monthlyPayment)
    }

    @Test
    fun `a referred application opens nothing, and endings are stable four digits`() {
        val plan = ApplicationPlanner.plan(LOAN, ProductRules.offer(640, LOAN), answers, "c3", NOW)
        assertNull(plan.account)
        assertNull(plan.application.accountId)
        assertEquals(Decision.REFERRED, plan.application.decision)
        val last4 = ApplicationPlanner.last4("3f6c2a9e-81d4-4b7a-9c35-0e2d7f1b6a48")
        assertEquals(4, last4.length)
        assertTrue(last4.all { it.isDigit() })
        assertEquals(last4, ApplicationPlanner.last4("3f6c2a9e-81d4-4b7a-9c35-0e2d7f1b6a48"))
    }
}

private class FakeProductsRepository : ProductsRepository {
    val data = MutableStateFlow<DataResult<ProductsData>?>(null)
    val plans = mutableListOf<ApplicationPlan>()

    override fun observeProductsData(): Flow<DataResult<ProductsData>> = data.filterNotNull()

    override suspend fun submit(plan: ApplicationPlan): DataResult<Unit> {
        plans += plan
        return DataResult.Success(Unit)
    }
}

private val CATALOG = object : ProductCatalog {
    override suspend fun products(): List<Product> = listOf(HYSA, CARD, LOAN)
}

class ProductUseCasesTest {
    private val repository = FakeProductsRepository()
    private val submit = SubmitApplicationUseCase(repository, ManualClock(NOW))
    private val answers =
        ApplicationAnswers(
            EmploymentStatus.EMPLOYED,
            Money.ofDollars(68_000),
            Money.ofDollars(10_000),
            36
        )

    @Test
    fun `loan amounts above the tier's maximum are refused before anything is written`() = runTest {
        assertEquals(
            ApplicationResult.InvalidAnswers,
            submit(LOAN, MARCUS, answers.copy(loanAmount = Money.ofDollars(20_000)))
        )
        assertEquals(
            ApplicationResult.InvalidAnswers,
            submit(LOAN, MARCUS, answers.copy(annualIncome = Money.ZERO))
        )
        assertTrue(repository.plans.isEmpty())
        val result = submit(LOAN, MARCUS, answers) as ApplicationResult.Submitted
        assertEquals(Rate(1_249), (result.plan.account as NewLoanAccount).apr)
    }

    @Test
    fun `a referred application is recorded without an account`() = runTest {
        val result = submit(CARD, LOW_SCORE, answers) as ApplicationResult.Submitted
        assertNull(result.plan.account)
        assertEquals(1, repository.plans.size)
    }

    @Test
    fun `listings show what was opened and what is under review`() = runTest {
        val observe = ObserveProductsUseCase(repository, CATALOG)
        repository.data.value = DataResult.Success(
            ProductsData(
                profile = SAMANTA,
                accounts = emptyList(),
                applications = listOf(
                    Application("a", "personal-loan", Decision.REFERRED, null, NOW),
                    // Its account has not arrived yet, so it does not count as opened.
                    Application("b", "hysa", Decision.APPROVED, "hysa-1", NOW)
                ),
                isFromCache = false
            )
        )
        observe().test {
            val overview = (awaitItem() as DataResult.Success).value
            val (hysa, card, loan) = overview.listings
            assertNull(hysa.openedAccountId)
            assertFalse(card.underReview)
            assertTrue(loan.underReview)
            assertEquals(Decision.APPROVED, card.offer.decision)
        }
    }
}

class ProductsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProductsRepository()
    private val savedState = SavedStateHandle()

    private fun viewModel() = ProductsViewModel(
        savedStateHandle = savedState,
        observeProducts = ObserveProductsUseCase(repository, CATALOG),
        submitApplication = SubmitApplicationUseCase(repository, ManualClock(NOW))
    )

    private fun load() {
        repository.data.value =
            DataResult.Success(ProductsData(MARCUS, emptyList(), emptyList(), isFromCache = false))
    }

    @Test
    fun `the calculator starts at the defaults, capped by the tier, and updates live`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(ProductsUiState.Loading, awaitItem())
            load()
            awaitItem()
            viewModel.onSelectProduct("personal-loan")
            testScheduler.runCurrent()
            val calculator = (expectMostRecentItem() as ProductsUiState.Content).calculator
            assertNotNull(calculator)
            assertEquals(Money.ofDollars(10_000), calculator?.quote?.amount)
            assertEquals(Money.ofDollars(15_000), calculator?.amountMax)
            assertEquals(Rate(1_249), calculator?.quote?.apr)

            viewModel.onAmountChange(Money.ofDollars(14_000))
            viewModel.onTermChange(24)
            testScheduler.runCurrent()
            val quote = (expectMostRecentItem() as ProductsUiState.Content).calculator?.quote
            assertEquals(
                ProductRules.monthlyPayment(Money.ofDollars(14_000), Rate(1_249), 24),
                quote?.monthlyPayment
            )
        }
    }

    @Test
    fun `identity is prefilled, the check takes a moment, then the account opens once`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            load()
            awaitItem()
            viewModel.onSelectProduct("signature-rewards")
            testScheduler.runCurrent()
            val start = expectMostRecentItem() as ProductsUiState.Content
            assertEquals(EmploymentStatus.EMPLOYED, start.employment)
            assertEquals("68000", start.incomeText)
            assertTrue(start.canContinueIdentity)

            viewModel.onIncomeChange("")
            testScheduler.runCurrent()
            assertFalse((expectMostRecentItem() as ProductsUiState.Content).canContinueIdentity)
            viewModel.onIncomeChange("072,500")
            viewModel.onAgreeChange(true)
            viewModel.onStartCheck()
            testScheduler.runCurrent()
            assertEquals(
                CheckState.CHECKING,
                (expectMostRecentItem() as ProductsUiState.Content).check
            )
            testScheduler.advanceTimeBy(1_501)
            testScheduler.runCurrent()
            assertEquals(
                CheckState.DECIDED,
                (expectMostRecentItem() as ProductsUiState.Content).check
            )

            viewModel.onSubmit()
            viewModel.onSubmit()
            testScheduler.runCurrent()
            val done = expectMostRecentItem() as ProductsUiState.Content
            val card = done.submitted?.account as NewCardAccount
            assertEquals(Money.ofDollars(3_000), card.creditLimit)
            assertEquals(
                Money.ofDollars(72_500),
                done.submitted?.application?.answers?.annualIncome
            )
            assertEquals(1, repository.plans.size)
        }
    }

    @Test
    fun `the draft survives process death`() = runTest {
        viewModel().apply {
            onSelectProduct("personal-loan")
            onAmountChange(Money.ofDollars(12_000))
            onTermChange(48)
            onAgreeChange(true)
        }
        viewModel().uiState.test {
            awaitItem()
            load()
            val content = awaitItem() as ProductsUiState.Content
            assertEquals("personal-loan", content.selected?.product?.id)
            assertEquals(Money.ofDollars(12_000), content.calculator?.quote?.amount)
            assertEquals(48, content.calculator?.quote?.termMonths)
            assertTrue(content.agreed)
        }
    }
}
