package com.abcbank.mobile.core.data.firebase

import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.model.UserProfile
import com.google.firebase.Timestamp

/** The `users/{uid}` document. Money fields are cents. */
data class UserProfileDto(
    val personaId: String? = null,
    val customerId: String? = null,
    val seedVersion: Long? = null,
    val name: String? = null,
    val holderName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val avatarKey: String? = null,
    val employment: String? = null,
    val annualIncome: Long? = null,
    val monthlyBudget: Long? = null,
    val creditScore: Long? = null,
    val checkDepositLimit: Long? = null,
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = firestoreFields(
        PERSONA_ID to personaId,
        CUSTOMER_ID to customerId,
        SEED_VERSION to seedVersion,
        NAME to name,
        HOLDER_NAME to holderName,
        EMAIL to email,
        PHONE to phone,
        AVATAR_KEY to avatarKey,
        EMPLOYMENT to employment,
        ANNUAL_INCOME to annualIncome,
        MONTHLY_BUDGET to monthlyBudget,
        CREDIT_SCORE to creditScore,
        CHECK_DEPOSIT_LIMIT to checkDepositLimit,
        CREATED_AT to createdAt
    )

    /** Null when a field is missing or invalid. */
    fun toUserProfile(): UserProfile? {
        val persona = PersonaId.entries.firstOrNull { it.key == personaId } ?: return null
        val checkDepositLimit = checkDepositLimit?.takeIf { it > 0 } ?: return null
        return UserProfile(
            personaId = persona,
            customerId = customerId?.let(CustomerId::parse) ?: return null,
            seedVersion = seedVersion?.takeIf { it in 1..Int.MAX_VALUE }?.toInt() ?: return null,
            name = name?.takeIf(String::isNotBlank) ?: return null,
            holderName = holderName?.takeIf(String::isNotBlank) ?: return null,
            email = email ?: return null,
            phone = phone ?: return null,
            employment = employment ?: return null,
            annualIncome = Money(annualIncome?.takeIf { it >= 0 } ?: return null),
            monthlyBudget = Money(monthlyBudget?.takeIf { it >= 0 } ?: return null),
            creditScore = creditScore?.takeIf { it in CREDIT_SCORE_RANGE }?.toInt() ?: return null,
            checkDepositLimit = Money(checkDepositLimit),
            createdAt = createdAt?.toInstantUtc() ?: return null
        )
    }

    companion object {
        const val PERSONA_ID = "personaId"
        const val CUSTOMER_ID = "customerId"
        const val SEED_VERSION = "seedVersion"
        const val NAME = "name"
        const val HOLDER_NAME = "holderName"
        const val EMAIL = "email"
        const val PHONE = "phone"
        const val AVATAR_KEY = "avatarKey"
        const val EMPLOYMENT = "employment"
        const val ANNUAL_INCOME = "annualIncome"
        const val MONTHLY_BUDGET = "monthlyBudget"
        const val CREDIT_SCORE = "creditScore"
        const val CHECK_DEPOSIT_LIMIT = "checkDepositLimit"
        const val CREATED_AT = "createdAt"

        private val CREDIT_SCORE_RANGE = 300L..850L

        fun fromMap(map: Map<String, Any?>): UserProfileDto = UserProfileDto(
            personaId = map.stringField(PERSONA_ID),
            customerId = map.stringField(CUSTOMER_ID),
            seedVersion = map.longField(SEED_VERSION),
            name = map.stringField(NAME),
            holderName = map.stringField(HOLDER_NAME),
            email = map.stringField(EMAIL),
            phone = map.stringField(PHONE),
            avatarKey = map.stringField(AVATAR_KEY),
            employment = map.stringField(EMPLOYMENT),
            annualIncome = map.longField(ANNUAL_INCOME),
            monthlyBudget = map.longField(MONTHLY_BUDGET),
            creditScore = map.longField(CREDIT_SCORE),
            checkDepositLimit = map.longField(CHECK_DEPOSIT_LIMIT),
            createdAt = map.timestampField(CREATED_AT)
        )
    }
}
