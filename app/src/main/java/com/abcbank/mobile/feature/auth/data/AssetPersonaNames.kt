package com.abcbank.mobile.feature.auth.data

import com.abcbank.mobile.core.data.seed.MalformedSeedException
import com.abcbank.mobile.core.data.seed.SeedAssetReader
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.feature.auth.domain.PersonaNames
import java.io.IOException
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Reads `profile.name` from the bundled persona file and keeps the first word. */
class AssetPersonaNames @Inject constructor(private val assets: SeedAssetReader) : PersonaNames {
    override suspend fun firstName(personaId: PersonaId): String? {
        // An unreadable file greets without a name rather than failing the lock screen.
        val persona = try {
            assets.persona(personaId)
        } catch (error: IOException) {
            return null
        } catch (error: MalformedSeedException) {
            return null
        } catch (error: SerializationException) {
            return null
        }
        val profile = persona[PROFILE] as? JsonObject ?: return null
        val name = (profile[NAME] as? JsonPrimitive)?.content ?: return null
        return name.trim().substringBefore(' ').takeIf(String::isNotEmpty)
    }

    private companion object {
        const val PROFILE = "profile"
        const val NAME = "name"
    }
}
