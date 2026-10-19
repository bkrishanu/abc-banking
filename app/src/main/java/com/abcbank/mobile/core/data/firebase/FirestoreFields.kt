package com.abcbank.mobile.core.data.firebase

import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Rate
import com.google.firebase.Timestamp
import java.time.Instant

// Typed reads from a Firestore document's field map. A field of the wrong type reads as null, so
// the mapper treats it like a missing field and the document becomes MALFORMED_DATA, not a crash.
// DTOs read maps by hand instead of using toObject(): no reflection, so no R8 keep rules.

internal fun Map<String, Any?>.stringField(name: String): String? = this[name] as? String

internal fun Map<String, Any?>.longField(name: String): Long? = when (val value = this[name]) {
    is Long -> value
    is Int -> value.toLong()
    else -> null
}

internal fun Map<String, Any?>.booleanField(name: String): Boolean? = this[name] as? Boolean

internal fun Map<String, Any?>.timestampField(name: String): Timestamp? = this[name] as? Timestamp

/** Builds a document for a write, leaving out null fields rather than storing nulls. */
internal fun firestoreFields(vararg fields: Pair<String, Any?>): Map<String, Any> = buildMap {
    for ((name, value) in fields) {
        if (value != null) put(name, value)
    }
}

internal fun Timestamp.toInstantUtc(): Instant =
    Instant.ofEpochSecond(seconds, nanoseconds.toLong())

internal fun Instant.toTimestamp(): Timestamp = Timestamp(epochSecond, nano)

internal inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
    name?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

internal fun String.isLast4(): Boolean = length == 4 && all { it in '0'..'9' }

/** For balances, limits and values, which are never negative in these documents. */
internal fun money(cents: Long?): Money? = cents?.takeIf { it >= 0 }?.let(::Money)

internal fun rateOf(basisPoints: Long?): Rate? =
    basisPoints?.takeIf { it in 0L..Int.MAX_VALUE.toLong() }?.let { Rate(it.toInt()) }

/** For orders and counts stored as Firestore integers. */
internal fun Long.toIntOrNull(): Int? = takeIf {
    it in
        Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
}?.toInt()
