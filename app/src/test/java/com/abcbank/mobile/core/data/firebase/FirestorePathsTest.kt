package com.abcbank.mobile.core.data.firebase

import org.junit.Assert.assertEquals
import org.junit.Test

class FirestorePathsTest {
    @Test
    fun `user paths sit under the uid`() {
        assertEquals("users/u1", FirestorePaths.user("u1"))
        assertEquals(
            "users/u1/accounts",
            FirestorePaths.userCollection("u1", FirestorePaths.ACCOUNTS)
        )
        assertEquals("users/u1/accounts/a1/holdings", FirestorePaths.holdings("u1", "a1"))
        assertEquals("users/u1/cards/c1/travelNotices", FirestorePaths.travelNotices("u1", "c1"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an id with a slash is rejected`() {
        FirestorePaths.user("u1/accounts")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a blank id is rejected`() {
        FirestorePaths.holdings("u1", " ")
    }
}
