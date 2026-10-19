package com.abcbank.mobile.core.data.firebase

import com.abcbank.mobile.core.common.DataError
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseErrorsTest {
    @Test
    fun `network failures read as offline`() {
        assertEquals(DataError.OFFLINE, FirebaseNetworkException("offline").toDataError())
        assertEquals(
            DataError.OFFLINE,
            firestoreError(FirebaseFirestoreException.Code.UNAVAILABLE).toDataError()
        )
    }

    @Test
    fun `rule rejections read as permission denied`() {
        assertEquals(
            DataError.PERMISSION_DENIED,
            firestoreError(FirebaseFirestoreException.Code.PERMISSION_DENIED).toDataError()
        )
        assertEquals(
            DataError.PERMISSION_DENIED,
            firestoreError(FirebaseFirestoreException.Code.UNAUTHENTICATED).toDataError()
        )
    }

    @Test
    fun `anything else is unknown`() {
        assertEquals(
            DataError.UNKNOWN,
            firestoreError(FirebaseFirestoreException.Code.INTERNAL).toDataError()
        )
        assertEquals(DataError.UNKNOWN, IllegalStateException().toDataError())
    }

    private fun firestoreError(code: FirebaseFirestoreException.Code) =
        FirebaseFirestoreException("failed", code)
}
