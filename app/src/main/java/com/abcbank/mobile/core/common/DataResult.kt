package com.abcbank.mobile.core.common

/** What a repository returns, so callers handle failure without catching vendor exceptions. */
sealed interface DataResult<out T> {
    data class Success<T>(val value: T) : DataResult<T>

    data class Failure(val error: DataError) : DataResult<Nothing>
}

/**
 * Failures the UI can explain in plain words (ARCHITECTURE.md section 04, Errors). Firebase error
 * codes and messages never travel past the data layer.
 */
enum class DataError {
    OFFLINE,
    PERMISSION_DENIED,

    /** A Firestore document is missing a field or has a value of the wrong shape. */
    MALFORMED_DATA,
    UNKNOWN
}
