package com.abcbank.mobile.feature.products.ui

import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.model.Rate
import com.abcbank.mobile.core.model.UserProfile
import com.abcbank.mobile.feature.products.domain.ApplicationAnswers
import com.abcbank.mobile.feature.products.domain.ApplicationPlanner
import com.abcbank.mobile.feature.products.domain.CardProduct
import com.abcbank.mobile.feature.products.domain.EmploymentStatus
import com.abcbank.mobile.feature.products.domain.LoanProduct
import com.abcbank.mobile.feature.products.domain.ProductListing
import com.abcbank.mobile.feature.products.domain.ProductRules
import com.abcbank.mobile.feature.products.domain.SavingsProduct
import java.time.Instant

/** Samanta (credit score 792) and the catalog from DEMO_DATA.md, for previews and screenshots. */
internal object ProductsPreviewData {
    private val NOW: Instant = Instant.parse("2026-10-20T15:00:00Z")

    val profile = UserProfile(
        personaId = PersonaId.SAMANTA,
        // Always valid: eight letters and digits.
        customerId = requireNotNull(CustomerId.parse("SAMJOH01")),
        seedVersion = 3,
        name = "Samanta Johns",
        holderName = "SAMANTA JOHNS",
        email = "samanta.johns@example.com",
        phone = "+1 (555) 010-2231",
        employment = "Employed, Product Design Lead at Lumen Studio",
        annualIncome = Money.ofDollars(185_000),
        monthlyBudget = Money.ofDollars(18_900),
        creditScore = 792,
        checkDepositLimit = Money.ofDollars(10_000),
        createdAt = NOW
    )

    val hysa = SavingsProduct("hysa", "ABC High-Yield Savings", Rate(450), Money.ZERO, Money.ZERO)
    val card =
        CardProduct(
            "signature-rewards",
            "ABC Signature Rewards Card",
            Money(9_500),
            true,
            Rate(2_124),
            Rate(2_824)
        )
    val loan = LoanProduct(
        id = "personal-loan",
        name = "ABC Personal Loan",
        amountMin = Money.ofDollars(2_000),
        amountMax = Money.ofDollars(50_000),
        termMonthsMin = 12,
        termMonthsMax = 60,
        aprMin = Rate(799),
        aprMax = Rate(1_799),
        defaultAmount = Money.ofDollars(10_000),
        defaultTermMonths = 36
    )

    fun listings(score: Int = profile.creditScore, openedCard: Boolean = false) =
        listOf(hysa, card, loan).map {
            ProductListing(
                product = it,
                offer = ProductRules.offer(score, it),
                openedAccountId = if (openedCard &&
                    it == card
                ) {
                    "signature-rewards-1a2b3c4d"
                } else {
                    null
                },
                underReview = false
            )
        }

    fun content(selected: String? = null, openedCard: Boolean = false): ProductsUiState.Content {
        val listings = listings(openedCard = openedCard)
        val listing = listings.firstOrNull { it.product.id == selected }
        val calculator = (listing?.product as? LoanProduct)?.let { loan ->
            LoanCalculator(
                quote = ProductRules.quote(loan.defaultAmount, loan.defaultTermMonths, Rate(799)),
                amountMin = loan.amountMin,
                amountMax = ProductRules.maxLoan(loan, listing.offer),
                termMin = loan.termMonthsMin,
                termMax = loan.termMonthsMax
            )
        }
        return ProductsUiState.Content(
            profile = profile,
            listings = listings,
            isOffline = false,
            selected = listing,
            calculator = calculator,
            employment = EmploymentStatus.EMPLOYED,
            employmentDetail = "Product Design Lead at Lumen Studio",
            incomeText = "185000",
            incomeError = false,
            agreed = false,
            check = CheckState.NOT_STARTED,
            submitted = null,
            submitting = false,
            failed = false
        )
    }

    fun completedLoan(): ProductsUiState.Content {
        val base = content(selected = "personal-loan")
        val plan = ApplicationPlanner.plan(
            product = loan,
            offer = ProductRules.offer(profile.creditScore, loan),
            answers = ApplicationAnswers(
                EmploymentStatus.EMPLOYED,
                Money.ofDollars(185_000),
                Money.ofDollars(10_000),
                36
            ),
            applicationId = "3f6c2a9e-81d4-4b7a-9c35-0e2d7f1b6a48",
            now = NOW,
            holderName = profile.holderName
        )
        return base.copy(check = CheckState.DECIDED, submitted = plan)
    }
}
