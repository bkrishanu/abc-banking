package com.abcbank.mobile.feature.products.domain

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Rate
import com.abcbank.mobile.core.model.UserProfile
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** A catalog product (DEMO_DATA.md, Products). */
sealed interface Product {
    val id: String
    val name: String
}

data class SavingsProduct(
    override val id: String,
    override val name: String,
    val apy: Rate,
    val minimumBalance: Money,
    val monthlyFee: Money
) : Product

data class CardProduct(
    override val id: String,
    override val name: String,
    val annualFee: Money,
    val annualFeeWaivedFirstYear: Boolean,
    val aprMin: Rate,
    val aprMax: Rate
) : Product

data class LoanProduct(
    override val id: String,
    override val name: String,
    val amountMin: Money,
    val amountMax: Money,
    val termMonthsMin: Int,
    val termMonthsMax: Int,
    val aprMin: Rate,
    val aprMax: Rate,
    val defaultAmount: Money,
    val defaultTermMonths: Int
) : Product

/** The bundled catalog, read on the device so the catalog works offline. */
interface ProductCatalog {
    suspend fun products(): List<Product>
}

enum class EmploymentStatus { EMPLOYED, SELF_EMPLOYED, RETIRED, STUDENT, NOT_EMPLOYED }

enum class Decision { APPROVED, REFERRED }

/** A submitted application: `applications/{applicationId}` (ARCHITECTURE.md section 04). */
data class Application(
    val id: String,
    val productId: String,
    val decision: Decision,
    /** The account it opened; null when referred. */
    val accountId: String?,
    val createdAt: Instant
)

data class ProductsData(
    val profile: UserProfile,
    val accounts: List<Account>,
    val applications: List<Application>,
    val isFromCache: Boolean
)

interface ProductsRepository {
    fun observeProductsData(): Flow<DataResult<ProductsData>>

    /** Writes the application, and the new account when approved, as one batch. */
    suspend fun submit(plan: ApplicationPlan): DataResult<Unit>
}
