package com.abcbank.mobile.core.data.seed

import android.content.Context
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.model.PersonaId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Reads the bundled seed files in `assets/seed/` (DEMO_DATA.md section 08). */
class SeedAssetReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider
) {
    suspend fun persona(personaId: PersonaId): JsonObject =
        read("seed/personas/${personaId.key}.json")

    suspend fun catalog(): JsonObject = read("seed/catalog.json")

    private suspend fun read(path: String): JsonObject = withContext(dispatchers.io) {
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        Json.parseToJsonElement(text) as? JsonObject
            ?: throw MalformedSeedException("$path is not a JSON object")
    }
}
