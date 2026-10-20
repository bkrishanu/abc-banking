package com.abcbank.mobile.core.data.firebase

/**
 * Every Firestore path the app uses (ARCHITECTURE.md section 04). Each user path sits under
 * `users/{uid}`, which is what the security rules check against `request.auth.uid`.
 */
object FirestorePaths {
    const val USERS = "users"
    const val PERSONAS = "personas"
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

    fun user(uid: String): String = "$USERS/${segment(uid)}"

    /** Collections that belong to the install, not a persona (devices). */
    fun userCollection(uid: String, collection: String): String = "${user(uid)}/$collection"

    /**
     * One persona's data tree: the document holds the profile, and the persona's accounts, cards,
     * transactions and so on sit below it. Each persona keeps its own tree, so switching persona
     * never deletes anything.
     */
    fun persona(uid: String, personaId: String): String =
        "${user(uid)}/$PERSONAS/${segment(personaId)}"

    fun personaCollection(uid: String, personaId: String, collection: String): String =
        "${persona(uid, personaId)}/$collection"

    fun holdings(uid: String, personaId: String, accountId: String): String =
        "${personaCollection(uid, personaId, ACCOUNTS)}/${segment(accountId)}/$HOLDINGS"

    fun travelNotices(uid: String, personaId: String, cardId: String): String =
        "${personaCollection(uid, personaId, CARDS)}/${segment(cardId)}/$TRAVEL_NOTICES"

    // An id containing "/" would silently point at a different document.
    private fun segment(id: String): String {
        require(id.isNotBlank() && '/' !in id) { "Not a valid Firestore id" }
        return id
    }
}
