package com.abcbank.mobile.core.data.firebase

import com.google.firebase.Timestamp

/**
 * An `applications/{applicationId}` document (ARCHITECTURE.md section 04). [answers] holds what
 * the customer confirmed: employment, annualIncome, and for loans amount and termMonths.
 * Mapped to the products model in `feature/products/data`.
 */
data class ApplicationDto(
    val productId: String? = null,
    val step: String? = null,
    val answers: Map<String, Any>? = null,
    val decision: String? = null,
    val accountId: String? = null,
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = firestoreFields(
        PRODUCT_ID to productId,
        STEP to step,
        ANSWERS to answers,
        DECISION to decision,
        ACCOUNT_ID to accountId,
        CREATED_AT to createdAt
    )

    companion object {
        const val PRODUCT_ID = "productId"
        const val STEP = "step"
        const val ANSWERS = "answers"
        const val DECISION = "decision"
        const val ACCOUNT_ID = "accountId"
        const val CREATED_AT = "createdAt"

        const val ANSWER_EMPLOYMENT = "employment"
        const val ANSWER_ANNUAL_INCOME = "annualIncome"
        const val ANSWER_AMOUNT = "amount"
        const val ANSWER_TERM_MONTHS = "termMonths"

        /** Every saved application has finished all four steps. */
        const val STEP_COMPLETE = "COMPLETE"

        fun fromMap(map: Map<String, Any?>): ApplicationDto = ApplicationDto(
            productId = map.stringField(PRODUCT_ID),
            step = map.stringField(STEP),
            decision = map.stringField(DECISION),
            accountId = map.stringField(ACCOUNT_ID),
            createdAt = map.timestampField(CREATED_AT)
        )
    }
}
