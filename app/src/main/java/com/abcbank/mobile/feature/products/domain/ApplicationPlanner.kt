package com.abcbank.mobile.feature.products.domain

import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Rate
import java.time.Duration
import java.time.Instant
import kotlin.math.absoluteValue

/** What the customer confirmed on step 1, and for a loan the amount and term chosen. */
data class ApplicationAnswers(
    val employment: EmploymentStatus,
    val annualIncome: Money,
    val loanAmount: Money? = null,
    val termMonths: Int? = null
)

/** The account an approved application opens (DEMO_DATA.md section 09, new accounts). */
sealed interface NewAccount {
    val id: String
    val nickname: String
    val last4: String
    val openedAt: Instant
}

data class NewSavingsAccount(
    override val id: String,
    override val nickname: String,
    override val last4: String,
    override val openedAt: Instant,
    val apy: Rate
) : NewAccount

data class NewCardAccount(
    override val id: String,
    override val nickname: String,
    override val last4: String,
    override val openedAt: Instant,
    val creditLimit: Money,
    val apr: Rate,
    val firstDueDate: Instant
) : NewAccount

data class NewLoanAccount(
    override val id: String,
    override val nickname: String,
    override val last4: String,
    override val openedAt: Instant,
    val principal: Money,
    val apr: Rate,
    val termMonths: Int,
    val monthlyPayment: Money
) : NewAccount

data class SubmittedApplication(
    val id: String,
    val productId: String,
    val productName: String,
    val decision: Decision,
    val answers: ApplicationAnswers,
    val accountId: String?,
    val createdAt: Instant
)

/** Everything one application writes, as one batch, plus the alert. */
data class ApplicationPlan(
    val application: SubmittedApplication,
    val account: NewAccount?,
    /** The card an approved card application issues, for the Cards tab. */
    val card: NewCard? = null
)

/** Works out what an application writes. Pure, so every rule is unit-tested. */
object ApplicationPlanner {
    private val FIRST_STATEMENT = Duration.ofDays(25)
    private const val LAST4_RANGE = 10_000
    private const val ID_PREFIX_LENGTH = 8

    fun plan(
        product: Product,
        offer: CreditOffer,
        answers: ApplicationAnswers,
        applicationId: String,
        now: Instant,
        holderName: String
    ): ApplicationPlan {
        val approved = offer.decision == Decision.APPROVED
        val account = if (approved) account(product, offer, answers, applicationId, now) else null
        val card = (account as? NewCardAccount)?.let {
            CardIssuer.issue(it, it.nickname, holderName, applicationId)
        }
        return ApplicationPlan(
            application = SubmittedApplication(
                id = applicationId,
                productId = product.id,
                productName = product.name,
                decision = offer.decision,
                answers = answers,
                accountId = account?.id,
                createdAt = now
            ),
            account = account,
            card = card
        )
    }

    private fun account(
        product: Product,
        offer: CreditOffer,
        answers: ApplicationAnswers,
        applicationId: String,
        now: Instant
    ): NewAccount {
        val id = "${product.id}-${applicationId.filter {
            it.isLetterOrDigit()
        }.take(ID_PREFIX_LENGTH)}"
        val last4 = last4(applicationId)
        return when (product) {
            is SavingsProduct -> NewSavingsAccount(
                id,
                nickname(product.name),
                last4,
                now,
                product.apy
            )
            is CardProduct -> NewCardAccount(
                id = id,
                nickname = nickname(product.name),
                last4 = last4,
                openedAt = now,
                creditLimit = requireNotNull(offer.cardLimit) { "An approved card has a limit" },
                apr = requireNotNull(offer.cardApr) { "An approved card has an APR" },
                firstDueDate = now.plus(FIRST_STATEMENT)
            )
            is LoanProduct -> {
                val amount =
                    requireNotNull(answers.loanAmount) { "A loan application has an amount" }
                val months = requireNotNull(answers.termMonths) { "A loan application has a term" }
                val apr = requireNotNull(offer.loanApr) { "An approved loan has an APR" }
                NewLoanAccount(
                    id = id,
                    nickname = nickname(product.name),
                    last4 = last4,
                    openedAt = now,
                    principal = amount,
                    apr = apr,
                    termMonths = months,
                    monthlyPayment = ProductRules.monthlyPayment(amount, apr, months)
                )
            }
        }
    }

    /** "ABC Personal Loan" opens as "Personal Loan", like the seeded account nicknames. */
    fun nickname(productName: String): String = productName.removePrefix(BANK_PREFIX).trim()

    /** Four digits from the application id, so each new account has its own stable ending. */
    fun last4(applicationId: String): String =
        (applicationId.hashCode().toLong().absoluteValue % LAST4_RANGE).toString().padStart(4, '0')

    private const val BANK_PREFIX = "ABC "
}
