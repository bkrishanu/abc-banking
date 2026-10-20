package com.abcbank.mobile.feature.payments.data

import com.abcbank.mobile.core.data.seed.SeedAssetReader
import com.abcbank.mobile.feature.payments.domain.BankDirectory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The routing number lookup for Add Beneficiary, read from the bundled catalog (the same
 * catalog.json the console's catalog/banks was created from), so it works offline and never
 * depends on how the console copy was laid out.
 */
@Singleton
class AssetBankDirectory @Inject constructor(private val assets: SeedAssetReader) : BankDirectory {
    private val mutex = Mutex()
    private var banks: Map<String, String>? = null

    override suspend fun bankName(routingNumber: String): String? = mutex.withLock {
        val loaded = banks ?: load().also { banks = it }
        loaded[routingNumber]
    }

    private suspend fun load(): Map<String, String> {
        val list = assets.catalog()[BANKS] as? JsonArray ?: return emptyMap()
        return list.mapNotNull { element ->
            val bank = element as? JsonObject ?: return@mapNotNull null
            val routing =
                (bank[ROUTING_NUMBER] as? JsonPrimitive)?.content ?: return@mapNotNull null
            val name = (bank[NAME] as? JsonPrimitive)?.content ?: return@mapNotNull null
            routing to name
        }.toMap()
    }

    private companion object {
        const val BANKS = "banks"
        const val ROUTING_NUMBER = "routingNumber"
        const val NAME = "name"
    }
}
