package com.abcbank.mobile.core.model

/**
 * A saved payee (MobileBuild.md 3.2). Avatars are initials, so there is no photo field.
 * [recentOrder] places the recent ones in the Quick transfer row; null when not recent.
 */
data class Beneficiary(
    val id: String,
    val name: String,
    val shortName: String,
    val bankName: String,
    val routingNumber: String,
    val accountLast4: String,
    val recentOrder: Int?
) {
    val isRecent: Boolean get() = recentOrder != null
}
