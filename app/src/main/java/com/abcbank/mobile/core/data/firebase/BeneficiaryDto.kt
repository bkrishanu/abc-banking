package com.abcbank.mobile.core.data.firebase

import com.abcbank.mobile.core.model.Beneficiary

/**
 * A `users/{uid}/beneficiaries/{beneficiaryId}` document. [avatarUrl] is kept for the documented
 * layout but unused: beneficiaries show initials (DEMO_DATA.md section 01).
 */
data class BeneficiaryDto(
    val name: String? = null,
    val shortName: String? = null,
    val avatarUrl: String? = null,
    val bankName: String? = null,
    val routingNumber: String? = null,
    val accountLast4: String? = null,
    val isRecent: Boolean? = null,
    val recentOrder: Long? = null
) {
    fun toMap(): Map<String, Any> = firestoreFields(
        NAME to name,
        SHORT_NAME to shortName,
        AVATAR_URL to avatarUrl,
        BANK_NAME to bankName,
        ROUTING_NUMBER to routingNumber,
        ACCOUNT_LAST4 to accountLast4,
        IS_RECENT to isRecent,
        RECENT_ORDER to recentOrder
    )

    /** Null when a field is missing or invalid, or a recent payee has no order. */
    fun toBeneficiary(id: String): Beneficiary? {
        val recentOrder = if (isRecent == true) {
            recentOrder?.toIntOrNull() ?: return null
        } else {
            null
        }
        val name = name?.takeIf(String::isNotBlank) ?: return null
        return Beneficiary(
            id = id,
            name = name,
            shortName = shortName?.takeIf(String::isNotBlank) ?: name,
            bankName = bankName?.takeIf(String::isNotBlank) ?: return null,
            routingNumber = routingNumber?.takeIf(::isRoutingNumberShape) ?: return null,
            accountLast4 = accountLast4?.takeIf(String::isLast4) ?: return null,
            recentOrder = recentOrder
        )
    }

    companion object {
        const val NAME = "name"
        const val SHORT_NAME = "shortName"
        const val AVATAR_URL = "avatarUrl"
        const val BANK_NAME = "bankName"
        const val ROUTING_NUMBER = "routingNumber"
        const val ACCOUNT_LAST4 = "accountLast4"
        const val IS_RECENT = "isRecent"
        const val RECENT_ORDER = "recentOrder"

        private const val ROUTING_NUMBER_LENGTH = 9

        fun fromMap(map: Map<String, Any?>): BeneficiaryDto = BeneficiaryDto(
            name = map.stringField(NAME),
            shortName = map.stringField(SHORT_NAME),
            avatarUrl = map.stringField(AVATAR_URL),
            bankName = map.stringField(BANK_NAME),
            routingNumber = map.stringField(ROUTING_NUMBER),
            accountLast4 = map.stringField(ACCOUNT_LAST4),
            isRecent = map.booleanField(IS_RECENT),
            recentOrder = map.longField(RECENT_ORDER)
        )

        // Shape only. The ABA checksum and bank lookup belong to the Add beneficiary rules.
        private fun isRoutingNumberShape(value: String): Boolean =
            value.length == ROUTING_NUMBER_LENGTH && value.all { it in '0'..'9' }
    }
}
