package com.abcbank.mobile.feature.products.domain

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.UserProfile
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A catalog entry with what this customer has done with it. */
data class ProductListing(
    val product: Product,
    val offer: CreditOffer,
    /** The account an earlier application opened, when there is one. */
    val openedAccountId: String?,
    /** An earlier application was referred and is still under review. */
    val underReview: Boolean
)

data class ProductsOverview(
    val profile: UserProfile,
    val listings: List<ProductListing>,
    val isFromCache: Boolean
)

class ObserveProductsUseCase @Inject constructor(
    private val repository: ProductsRepository,
    private val catalog: ProductCatalog
) {
    operator fun invoke(): Flow<DataResult<ProductsOverview>> =
        repository.observeProductsData().map { result ->
            when (result) {
                is DataResult.Failure -> result
                is DataResult.Success -> {
                    val data = result.value
                    val products = runCatching { catalog.products() }.getOrElse {
                        return@map DataResult.Failure(DataError.MALFORMED_DATA)
                    }
                    val accountIds = data.accounts.map { it.id }.toSet()
                    DataResult.Success(
                        ProductsOverview(
                            profile = data.profile,
                            listings = products.map { product ->
                                val mine = data.applications.filter { it.productId == product.id }
                                ProductListing(
                                    product = product,
                                    offer = ProductRules.offer(data.profile.creditScore, product),
                                    openedAccountId = mine.mapNotNull { it.accountId }.firstOrNull {
                                        it in
                                            accountIds
                                    },
                                    underReview = mine.any { it.decision == Decision.REFERRED }
                                )
                            },
                            isFromCache = data.isFromCache
                        )
                    )
                }
            }
        }
}

sealed interface ApplicationResult {
    data class Submitted(val plan: ApplicationPlan) : ApplicationResult

    data object InvalidAnswers : ApplicationResult

    data class Failed(val error: DataError) : ApplicationResult
}

/**
 * Steps 3 and 4 of the application (MobileBuild.md 5.2): the simulated decision from the
 * credit score, then the application and, when approved, the new account in one write.
 */
class SubmitApplicationUseCase @Inject constructor(
    private val repository: ProductsRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(
        product: Product,
        profile: UserProfile,
        answers: ApplicationAnswers
    ): ApplicationResult {
        if (!ProductRules.isValidIncome(
                answers.annualIncome
            )
        ) {
            return ApplicationResult.InvalidAnswers
        }
        val offer = ProductRules.offer(profile.creditScore, product)
        if (product is LoanProduct && offer.decision == Decision.APPROVED) {
            val amount = answers.loanAmount ?: return ApplicationResult.InvalidAnswers
            val months = answers.termMonths ?: return ApplicationResult.InvalidAnswers
            val inRange =
                amount >= product.amountMin &&
                    amount <= ProductRules.maxLoan(product, offer) &&
                    months in product.termMonthsMin..product.termMonthsMax
            if (!inRange) return ApplicationResult.InvalidAnswers
        }
        val plan = ApplicationPlanner.plan(
            product,
            offer,
            answers,
            UUID.randomUUID().toString(),
            clock.now()
        )
        return when (val result = repository.submit(plan)) {
            is DataResult.Success -> ApplicationResult.Submitted(plan)
            is DataResult.Failure -> ApplicationResult.Failed(result.error)
        }
    }
}
