package com.abcbank.mobile.feature.payments.data

import com.abcbank.mobile.core.data.seed.SeedAssetReader
import com.abcbank.mobile.feature.payments.domain.Biller
import com.abcbank.mobile.feature.payments.domain.BillerDirectory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The billers from the bundled catalog (the source of catalog/billers), so they work offline. */
@Singleton
class AssetBillerDirectory @Inject constructor(private val assets: SeedAssetReader) :
    BillerDirectory {
    private val mutex = Mutex()
    private var billers: List<Biller>? = null

    override suspend fun billers(): List<Biller> = mutex.withLock {
        billers ?: catalogBillers(assets.catalog()).also { billers = it }
    }
}

/** The catalog's billers; one that does not map is left out. */
internal fun catalogBillers(catalog: JsonObject): List<Biller> {
    val list = catalog[BILLERS] as? JsonArray ?: return emptyList()
    return list.mapNotNull { element ->
        val biller = element as? JsonObject ?: return@mapNotNull null
        fun field(key: String) = (biller[key] as? JsonPrimitive)?.takeIf {
            it.isString
        }?.content?.takeIf(String::isNotBlank)
        Biller(
            id = field(ID) ?: return@mapNotNull null,
            name = field(NAME) ?: return@mapNotNull null,
            category = field(CATEGORY) ?: return@mapNotNull null
        )
    }
}

private const val BILLERS = "billers"
private const val ID = "id"
private const val NAME = "name"
private const val CATEGORY = "category"
