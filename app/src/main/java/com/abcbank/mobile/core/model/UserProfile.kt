package com.abcbank.mobile.core.model

import java.time.Instant

/** The seeded persona's profile, the `users/{uid}` document (DEMO_DATA.md sections 03 to 06). */
data class UserProfile(
    val personaId: PersonaId,
    val customerId: CustomerId,
    val seedVersion: Int,
    val name: String,
    val holderName: String,
    val email: String,
    val phone: String,
    val employment: String,
    val annualIncome: Money,
    val monthlyBudget: Money,
    val creditScore: Int,
    val checkDepositLimit: Money,
    val createdAt: Instant
) {
    /** "Samanta" for the "Hello, Samanta" greeting. */
    val firstName: String get() = name.substringBefore(' ')
}
