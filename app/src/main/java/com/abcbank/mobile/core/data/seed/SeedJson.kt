package com.abcbank.mobile.core.data.seed

import java.time.LocalTime
import java.time.format.DateTimeParseException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

// Typed reads from the bundled seed JSON (DEMO_DATA.md section 08). The files are checked by
// SeedFilesTest at build time, so a missing or wrongly typed field here is a packaging bug and
// fails the seed with MalformedSeedException instead of writing half a persona.

/** The bundled seed files do not match the format in DEMO_DATA.md section 08. */
class MalformedSeedException(message: String) : Exception(message)

internal fun JsonObject.seedString(name: String): String =
    (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: malformed(name)

internal fun JsonObject.seedLong(name: String): Long =
    (this[name] as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull ?: malformed(name)

internal fun JsonObject.seedObject(name: String): JsonObject =
    this[name] as? JsonObject ?: malformed(name)

internal fun JsonObject.seedArray(name: String): List<JsonObject> {
    val array = this[name] as? JsonArray ?: malformed(name)
    return array.map { it as? JsonObject ?: malformed(name) }
}

internal fun JsonObject.seedStrings(name: String): List<String> {
    val array = this[name] as? JsonArray ?: malformed(name)
    return array.map {
        (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content ?: malformed(name)
    }
}

/** A `{ "dayOffset": n, "time": "HH:mm" }` value; [time] is null for date-only fields. */
internal data class SeedDate(val dayOffset: Int, val time: LocalTime?)

internal fun JsonObject.seedDate(name: String): SeedDate =
    seedObject(name).toSeedDate() ?: malformed(name)

internal fun JsonObject.isSeedDate(): Boolean = DAY_OFFSET in this

internal fun JsonObject.toSeedDate(): SeedDate? {
    val dayOffset = (this[DAY_OFFSET] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
        ?: return null
    val time = when (val raw = this[TIME]) {
        null -> null
        is JsonPrimitive -> if (raw.isString) parseTime(raw.content) ?: return null else return null
        else -> return null
    }
    return SeedDate(dayOffset, time)
}

/**
 * Converts a seed value to a Firestore field value: whole numbers become Long, dates become
 * whatever [resolveDate] returns, nested objects and arrays are converted the same way, and nulls
 * are dropped (Firestore documents leave unset fields out rather than storing null).
 */
internal fun JsonElement.toFieldValue(resolveDate: (SeedDate) -> Any): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> booleanOrNull
        else -> longOrNull ?: throw MalformedSeedException("Seed numbers are whole cents")
    }
    is JsonObject -> if (isSeedDate()) {
        resolveDate(toSeedDate() ?: throw MalformedSeedException("Bad date"))
    } else {
        toFields(resolveDate)
    }
    is JsonArray -> map { it.toFieldValue(resolveDate) }
}

internal fun JsonObject.toFields(
    resolveDate: (SeedDate) -> Any,
    except: Set<String> = emptySet()
): Map<String, Any> = buildMap {
    for ((name, value) in this@toFields) {
        if (name in except) continue
        value.toFieldValue(resolveDate)?.let { put(name, it) }
    }
}

private const val DAY_OFFSET = "dayOffset"
private const val TIME = "time"

private fun parseTime(value: String): LocalTime? = try {
    LocalTime.parse(value)
} catch (_: DateTimeParseException) {
    null
}

private fun malformed(field: String): Nothing =
    throw MalformedSeedException("Missing or invalid seed field: $field")
