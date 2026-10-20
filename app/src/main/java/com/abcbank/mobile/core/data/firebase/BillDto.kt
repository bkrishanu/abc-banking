package com.abcbank.mobile.core.data.firebase

import com.google.firebase.Timestamp

/**
 * A `bills/{billId}` document (ARCHITECTURE.md section 04). [paidAt] and [accountLast4] are
 * written by bill pay only; seeded bills have neither.
 */
data class BillDto(
    val billerId: String? = null,
    val amountDue: Long? = null,
    val dueDate: Timestamp? = null,
    val autoPay: Boolean? = null,
    val scheduledFor: Timestamp? = null,
    val status: String? = null,
    val paidAt: Timestamp? = null,
    val accountLast4: String? = null
) {
    fun toMap(): Map<String, Any> = firestoreFields(
        BILLER_ID to billerId,
        AMOUNT_DUE to amountDue,
        DUE_DATE to dueDate,
        AUTO_PAY to autoPay,
        SCHEDULED_FOR to scheduledFor,
        STATUS to status,
        PAID_AT to paidAt,
        ACCOUNT_LAST4 to accountLast4
    )

    companion object {
        const val BILLER_ID = "billerId"
        const val AMOUNT_DUE = "amountDue"
        const val DUE_DATE = "dueDate"
        const val AUTO_PAY = "autoPay"
        const val SCHEDULED_FOR = "scheduledFor"
        const val STATUS = "status"
        const val PAID_AT = "paidAt"
        const val ACCOUNT_LAST4 = "accountLast4"

        fun fromMap(map: Map<String, Any?>): BillDto = BillDto(
            billerId = map.stringField(BILLER_ID),
            amountDue = map.longField(AMOUNT_DUE),
            dueDate = map.timestampField(DUE_DATE),
            autoPay = map.booleanField(AUTO_PAY),
            scheduledFor = map.timestampField(SCHEDULED_FOR),
            status = map.stringField(STATUS),
            paidAt = map.timestampField(PAID_AT),
            accountLast4 = map.stringField(ACCOUNT_LAST4)
        )
    }
}
