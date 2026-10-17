package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.model.PersonaId
import javax.inject.Inject

sealed interface CustomerIdCheck {
    /** Still typing: fewer than 8 valid characters. No error yet. */
    data object Incomplete : CustomerIdCheck

    /** Contains something other than letters and digits, or is too long. */
    data object WrongFormat : CustomerIdCheck

    /** Valid format but not one of the demo customers. */
    data object Unknown : CustomerIdCheck

    data class Recognised(val customerId: CustomerId, val personaId: PersonaId) : CustomerIdCheck
}

/** Classifies login input against the customer ID rules in DEMO_DATA.md section 02. */
class CheckCustomerIdUseCase @Inject constructor(private val directory: PersonaDirectory) {
    operator fun invoke(input: String): CustomerIdCheck {
        val trimmed = input.trim()
        if (!trimmed.all(CustomerId::isAsciiLetterOrDigit)) return CustomerIdCheck.WrongFormat
        if (trimmed.length < CustomerId.LENGTH) return CustomerIdCheck.Incomplete
        val customerId = CustomerId.parse(trimmed) ?: return CustomerIdCheck.WrongFormat
        val personaId = directory.personaFor(customerId) ?: return CustomerIdCheck.Unknown
        return CustomerIdCheck.Recognised(customerId, personaId)
    }
}
