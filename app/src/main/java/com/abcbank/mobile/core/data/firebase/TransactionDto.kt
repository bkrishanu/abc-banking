package com.abcbank.mobile.core.data.firebase

import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import com.google.firebase.Timestamp

/**
 * A `users/{uid}/transactions/{txId}` document. [amount] is positive cents; [runningBalance] may be
 * negative. [logoUrl] is kept for the documented layout but unused: logos are category icons.
 */
data class TransactionDto(
    val accountId: String? = null,
    val direction: String? = null,
    val amount: Long? = null,
    val merchant: String? = null,
    val logoUrl: String? = null,
    val category: String? = null,
    val reference: String? = null,
    val runningBalance: Long? = null,
    val status: String? = null,
    val disputed: Boolean? = null,
    val createdAt: Timestamp? = null,
    val clearsAt: Timestamp? = null,
    val memo: String? = null
) {
    fun toMap(): Map<String, Any> = firestoreFields(
        ACCOUNT_ID to accountId,
        DIRECTION to direction,
        AMOUNT to amount,
        MERCHANT to merchant,
        LOGO_URL to logoUrl,
        CATEGORY to category,
        REFERENCE to reference,
        RUNNING_BALANCE to runningBalance,
        STATUS to status,
        DISPUTED to disputed,
        CREATED_AT to createdAt,
        CLEARS_AT to clearsAt,
        MEMO to memo
    )

    /** Null when a field is missing or invalid. */
    fun toTransaction(id: String): Transaction? = Transaction(
        id = id,
        accountId = accountId?.takeIf(String::isNotBlank) ?: return null,
        direction = enumOrNull<TransactionDirection>(direction) ?: return null,
        amount = amount?.takeIf { it > 0 }?.let(::Money) ?: return null,
        merchant = merchant?.takeIf(String::isNotBlank) ?: return null,
        category = enumOrNull<TransactionCategory>(category) ?: return null,
        reference = reference?.takeIf(String::isNotBlank) ?: return null,
        runningBalance = runningBalance?.let(::Money) ?: return null,
        status = enumOrNull<TransactionStatus>(status) ?: return null,
        disputed = disputed ?: false,
        createdAt = createdAt?.toInstantUtc() ?: return null,
        clearsAt = clearsAt?.toInstantUtc(),
        memo = memo?.takeIf(String::isNotBlank)
    )

    companion object {
        const val ACCOUNT_ID = "accountId"
        const val DIRECTION = "direction"
        const val AMOUNT = "amount"
        const val MERCHANT = "merchant"
        const val LOGO_URL = "logoUrl"
        const val CATEGORY = "category"
        const val REFERENCE = "reference"
        const val RUNNING_BALANCE = "runningBalance"
        const val STATUS = "status"
        const val DISPUTED = "disputed"
        const val CREATED_AT = "createdAt"
        const val CLEARS_AT = "clearsAt"
        const val MEMO = "memo"

        fun fromMap(map: Map<String, Any?>): TransactionDto = TransactionDto(
            accountId = map.stringField(ACCOUNT_ID),
            direction = map.stringField(DIRECTION),
            amount = map.longField(AMOUNT),
            merchant = map.stringField(MERCHANT),
            logoUrl = map.stringField(LOGO_URL),
            category = map.stringField(CATEGORY),
            reference = map.stringField(REFERENCE),
            runningBalance = map.longField(RUNNING_BALANCE),
            status = map.stringField(STATUS),
            disputed = map.booleanField(DISPUTED),
            createdAt = map.timestampField(CREATED_AT),
            clearsAt = map.timestampField(CLEARS_AT),
            memo = map.stringField(MEMO)
        )
    }
}
