package com.abcbank.mobile.core.data.firebase

import com.google.firebase.firestore.FieldValue

/**
 * The `users/{uid}` document: a small summary of who last logged in on this install, so the
 * Firebase console shows each install at a glance. The app never reads it; screens find the
 * active persona locally. Written at login without waiting, merged so other fields stay.
 */
object UserSummaryDto {
    const val ACTIVE_PERSONA_ID = "activePersonaId"
    const val CUSTOMER_ID = "customerId"
    const val LAST_LOGIN_AT = "lastLoginAt"

    fun forLogin(personaId: String, customerId: String): Map<String, Any> = mapOf(
        ACTIVE_PERSONA_ID to personaId,
        CUSTOMER_ID to customerId,
        LAST_LOGIN_AT to FieldValue.serverTimestamp()
    )
}
