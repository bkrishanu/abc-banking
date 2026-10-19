package com.abcbank.mobile.core.data.firebase

import com.abcbank.mobile.core.common.DataError
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException

/** Maps a Firebase failure to what the UI can say about it. The exception itself goes no further. */
internal fun Throwable.toDataError(): DataError = when (this) {
    is FirebaseNetworkException -> DataError.OFFLINE
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE -> DataError.OFFLINE
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> DataError.PERMISSION_DENIED
        else -> DataError.UNKNOWN
    }
    else -> DataError.UNKNOWN
}
