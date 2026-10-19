package com.abcbank.mobile.core.data.firebase

/**
 * Every Firestore path the app uses (ARCHITECTURE.md section 04). Each user path sits under
 * `users/{uid}`, which is what the security rules check against `request.auth.uid`.
 */
object FirestorePaths {
    const val USERS = "users"
    const val ACCOUNTS = "accounts"
    const val HOLDINGS = "holdings"
    const val TRANSACTIONS = "transactions"
    const val BENEFICIARIES = "beneficiaries"
    const val CARDS = "cards"
    const val TRAVEL_NOTICES = "travelNotices"
    const val BILLS = "bills"
    const val SAVINGS_GOALS = "savingsGoals"
    const val APPLICATIONS = "applications"
    const val NOTIFICATIONS = "notifications"
    const val SUPPORT_CASES = "supportCases"
    const val DEVICES = "devices"

    const val CATALOG_BANKS = "catalog/banks"
    const val CATALOG_BILLERS = "catalog/billers"
    const val CATALOG_PRODUCTS = "catalog/products"
    const val CATALOG_FAQS = "catalog/faqs"

    /** Collections directly under `users/{uid}`, which a reseed deletes. */
    val USER_SUBCOLLECTIONS = listOf(
        ACCOUNTS,
        TRANSACTIONS,
        BENEFICIARIES,
        CARDS,
        BILLS,
        SAVINGS_GOALS,
        APPLICATIONS,
        NOTIFICATIONS,
        SUPPORT_CASES,
        DEVICES
    )

    fun user(uid: String): String = "$USERS/${segment(uid)}"

    fun userCollection(uid: String, collection: String): String = "${user(uid)}/$collection"

    fun holdings(uid: String, accountId: String): String =
        "${userCollection(uid, ACCOUNTS)}/${segment(accountId)}/$HOLDINGS"

    fun travelNotices(uid: String, cardId: String): String =
        "${userCollection(uid, CARDS)}/${segment(cardId)}/$TRAVEL_NOTICES"

    // An id containing "/" would silently point at a different document.
    private fun segment(id: String): String {
        require(id.isNotBlank() && '/' !in id) { "Not a valid Firestore id" }
        return id
    }
}
