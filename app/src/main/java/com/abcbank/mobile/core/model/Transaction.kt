package com.abcbank.mobile.core.model

import java.time.Instant

enum class TransactionDirection {
    DEBIT,
    CREDIT
}

enum class TransactionStatus {
    POSTED,
    PENDING
}

/** Categories used by the seed data (DEMO_DATA.md sections 03 to 07). */
enum class TransactionCategory {
    SHOPPING,
    DINING,
    GROCERIES,
    ENTERTAINMENT,
    TRAVEL,
    TRANSPORT,
    HEALTH,
    UTILITIES,
    HOUSING,
    INCOME,
    INTEREST,
    TRANSFER,
    TRANSFER_IN,
    TRANSFER_OUT,
    DEPOSIT
}

/**
 * One ledger entry (MobileBuild.md 2.2). [amount] is always positive; [direction] gives the sign.
 * [clearsAt] is set only on pending check deposits.
 */
data class Transaction(
    val id: String,
    val accountId: String,
    val direction: TransactionDirection,
    val amount: Money,
    val merchant: String,
    val category: TransactionCategory,
    val reference: String,
    val runningBalance: Money,
    val status: TransactionStatus,
    val disputed: Boolean,
    val createdAt: Instant,
    val clearsAt: Instant?,
    /** The customer's note on a transfer they sent; null for everything else. */
    val memo: String? = null
) {
    init {
        require(amount.isPositive) { "A transaction amount is positive; direction gives the sign" }
    }

    val signedAmount: Money get() = if (direction == TransactionDirection.DEBIT) -amount else amount
}
