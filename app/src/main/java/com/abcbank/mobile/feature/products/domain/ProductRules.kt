package com.abcbank.mobile.feature.products.domain

import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Rate
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** What the simulated credit check offers (DEMO_DATA.md, simulated credit decision). */
data class CreditOffer(
    val decision: Decision,
    val loanApr: Rate?,
    val maxLoan: Money?,
    val cardLimit: Money?,
    val cardApr: Rate?
)

/** The loan calculator's result (MobileBuild.md 5.1). */
data class LoanQuote(
    val amount: Money,
    val termMonths: Int,
    val apr: Rate,
    val monthlyPayment: Money,
    val totalInterest: Money,
    val totalRepayment: Money
)

/** Product origination rules (MobileBuild.md 5.1 and 5.2). Pure, so each one is unit-tested. */
object ProductRules {
    private const val TOP_TIER = 740
    private const val MIDDLE_TIER = 660
    private const val MONTHS_PER_YEAR = 12
    private val PERCENT_SCALE = BigDecimal(10_000)
    private val CENTS = Money.CENTS_PER_DOLLAR
    val AMOUNT_STEP: Money = Money.ofDollars(500)
    const val TERM_STEP_MONTHS = 6

    /**
     * 740 and above: 7.99% loans up to $50,000 and a $15,000 card; 660 to 739: 12.49% up to
     * $15,000 and a $3,000 card; below 660: referred. Card APRs are the catalog's ends of the
     * range, 21.24% for the top tier and 28.24% for the middle one. Savings never need a check.
     */
    fun offer(creditScore: Int, product: Product): CreditOffer {
        if (product is SavingsProduct) return CreditOffer(Decision.APPROVED, null, null, null, null)
        val cardProduct = product as? CardProduct
        return when {
            creditScore >= TOP_TIER -> CreditOffer(
                Decision.APPROVED,
                Rate(799),
                Money.ofDollars(50_000),
                Money.ofDollars(15_000),
                cardProduct?.aprMin
            )
            creditScore >= MIDDLE_TIER -> CreditOffer(
                Decision.APPROVED,
                Rate(1_249),
                Money.ofDollars(15_000),
                Money.ofDollars(3_000),
                cardProduct?.aprMax
            )
            else -> CreditOffer(Decision.REFERRED, null, null, null, null)
        }
    }

    /** The most this customer can borrow: the catalog's maximum, capped by their tier. */
    fun maxLoan(product: LoanProduct, offer: CreditOffer): Money =
        offer.maxLoan?.let { minOf(it, product.amountMax) } ?: product.amountMax

    /**
     * Monthly installment, amortised: P x r x (1 + r)^n / ((1 + r)^n - 1) with r = APR / 12,
     * rounded half up to the cent. A 0% APR divides evenly.
     */
    fun monthlyPayment(amount: Money, apr: Rate, months: Int): Money {
        require(months > 0) { "A loan needs at least one month" }
        val principal = BigDecimal(amount.cents)
        if (apr.basisPoints == 0) {
            return Money(principal.divide(BigDecimal(months), 0, RoundingMode.HALF_UP).toLong())
        }
        val monthly = BigDecimal(
            apr.basisPoints
        ).divide(PERCENT_SCALE * BigDecimal(MONTHS_PER_YEAR), MathContext.DECIMAL128)
        val growth = (BigDecimal.ONE + monthly).pow(months, MathContext.DECIMAL128)
        val payment = principal * monthly * growth / (growth - BigDecimal.ONE)
        return Money(payment.setScale(0, RoundingMode.HALF_UP).toLong())
    }

    fun quote(amount: Money, months: Int, apr: Rate): LoanQuote {
        val payment = monthlyPayment(amount, apr, months)
        val total = Money(payment.cents * months)
        return LoanQuote(amount, months, apr, payment, total - amount, total)
    }

    /** Slider values snap to $500 and 6-month steps inside the allowed range. */
    fun snapAmount(amount: Money, min: Money, max: Money): Money {
        val step = AMOUNT_STEP.cents
        val snapped = (amount.cents + step / 2) / step * step
        return Money(snapped.coerceIn(min.cents, max.cents))
    }

    fun snapTerm(months: Int, min: Int, max: Int): Int =
        ((months + TERM_STEP_MONTHS / 2) / TERM_STEP_MONTHS * TERM_STEP_MONTHS).coerceIn(min, max)

    /** "Employed, Product Design Lead at Lumen Studio" is Employed; unknown text is Employed too. */
    fun employmentStatus(employment: String): EmploymentStatus {
        val lead = employment.substringBefore(',').trim().lowercase()
        return when (lead) {
            "self-employed" -> EmploymentStatus.SELF_EMPLOYED
            "retired" -> EmploymentStatus.RETIRED
            "student" -> EmploymentStatus.STUDENT
            "not employed", "unemployed" -> EmploymentStatus.NOT_EMPLOYED
            else -> EmploymentStatus.EMPLOYED
        }
    }

    /** The role and employer after the status, or null ("Retired" has none). */
    fun employmentDetail(employment: String): String? =
        employment.substringAfter(',', "").trim().takeIf { it.isNotEmpty() }

    /** Income must be more than zero and at most $99,999,999 (whole dollars on the form). */
    fun isValidIncome(income: Money): Boolean =
        income.isPositive && income.cents <= MAX_INCOME_DOLLARS * CENTS

    private const val MAX_INCOME_DOLLARS = 99_999_999L
}
