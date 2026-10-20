package com.abcbank.mobile.feature.products.data

import com.abcbank.mobile.core.data.seed.SeedAssetReader
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Rate
import com.abcbank.mobile.feature.products.domain.CardProduct
import com.abcbank.mobile.feature.products.domain.LoanProduct
import com.abcbank.mobile.feature.products.domain.Product
import com.abcbank.mobile.feature.products.domain.ProductCatalog
import com.abcbank.mobile.feature.products.domain.SavingsProduct
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

/**
 * The products from the bundled catalog (the same catalog.json the console's catalog/products
 * was created from), like AssetBankDirectory: it works offline and the console copy's layout
 * never matters. A product that does not map is left out rather than shown half-read.
 */
@Singleton
class AssetProductCatalog @Inject constructor(private val assets: SeedAssetReader) :
    ProductCatalog {
    private val mutex = Mutex()
    private var products: List<Product>? = null

    override suspend fun products(): List<Product> = mutex.withLock {
        products ?: load().also { products = it }
    }

    private suspend fun load(): List<Product> = catalogProducts(assets.catalog())
}

/** The catalog's products; one that does not map is left out rather than shown half-read. */
internal fun catalogProducts(catalog: JsonObject): List<Product> {
    val list = catalog[PRODUCTS] as? JsonArray ?: return emptyList()
    return list.mapNotNull { (it as? JsonObject)?.toProduct() }
}

private fun JsonObject.toProduct(): Product? {
    val id = string(ID) ?: return null
    val name = string(NAME) ?: return null
    return when (string(KIND)) {
        SAVINGS -> SavingsProduct(
            id = id,
            name = name,
            apy = rate(APY) ?: return null,
            minimumBalance = money(MINIMUM_BALANCE) ?: return null,
            monthlyFee = money(MONTHLY_FEE) ?: return null
        )
        CREDIT -> CardProduct(
            id = id,
            name = name,
            annualFee = money(ANNUAL_FEE) ?: return null,
            annualFeeWaivedFirstYear =
            (this[FEE_WAIVED] as? JsonPrimitive)?.booleanOrNull ?: false,
            aprMin = rate(APR_MIN) ?: return null,
            aprMax = rate(APR_MAX) ?: return null
        )
        LOAN -> {
            val defaults = this[DEFAULTS] as? JsonObject ?: return null
            LoanProduct(
                id = id,
                name = name,
                amountMin = money(AMOUNT_MIN) ?: return null,
                amountMax = money(AMOUNT_MAX) ?: return null,
                termMonthsMin = long(TERM_MIN)?.toInt() ?: return null,
                termMonthsMax = long(TERM_MAX)?.toInt() ?: return null,
                aprMin = rate(APR_MIN) ?: return null,
                aprMax = rate(APR_MAX) ?: return null,
                defaultAmount = defaults.money(AMOUNT) ?: return null,
                defaultTermMonths = defaults.long(TERM_MONTHS)?.toInt() ?: return null
            )
        }
        else -> null
    }
}

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf(String::isNotBlank)

private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull

private fun JsonObject.money(key: String): Money? = long(key)?.takeIf { it >= 0 }?.let(::Money)

private fun JsonObject.rate(key: String): Rate? =
    long(key)?.takeIf { it in 0..Int.MAX_VALUE.toLong() }?.let { Rate(it.toInt()) }

private const val PRODUCTS = "products"
private const val ID = "id"
private const val NAME = "name"
private const val KIND = "kind"
private const val SAVINGS = "SAVINGS"
private const val CREDIT = "CREDIT"
private const val LOAN = "LOAN"
private const val APY = "apy"
private const val MINIMUM_BALANCE = "minimumBalance"
private const val MONTHLY_FEE = "monthlyFee"
private const val ANNUAL_FEE = "annualFee"
private const val FEE_WAIVED = "annualFeeWaivedFirstYear"
private const val APR_MIN = "aprMin"
private const val APR_MAX = "aprMax"
private const val AMOUNT_MIN = "amountMin"
private const val AMOUNT_MAX = "amountMax"
private const val TERM_MIN = "termMonthsMin"
private const val TERM_MAX = "termMonthsMax"
private const val DEFAULTS = "calculatorDefaults"
private const val AMOUNT = "amount"
private const val TERM_MONTHS = "termMonths"
