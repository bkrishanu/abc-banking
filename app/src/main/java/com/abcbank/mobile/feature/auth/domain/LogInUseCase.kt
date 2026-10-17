package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.security.MpinCheck
import com.abcbank.mobile.core.security.MpinVerifier
import com.abcbank.mobile.core.security.RememberedCustomerStore
import java.time.Instant
import javax.inject.Inject

sealed interface LogInResult {
    data class LoggedIn(val personaId: PersonaId) : LogInResult

    data class WrongMpin(val attemptsLeft: Int) : LogInResult

    data class LockedOut(val until: Instant) : LogInResult

    data object InvalidCustomerId : LogInResult
}

/**
 * Customer ID + MPIN login (MobileBuild.md 1.1). On success the customer ID is remembered so the
 * lock screen only asks for the MPIN. Loading the persona's data is a separate step.
 */
class LogInUseCase @Inject constructor(
    private val checkCustomerId: CheckCustomerIdUseCase,
    private val mpinVerifier: MpinVerifier,
    private val rememberedCustomerStore: RememberedCustomerStore
) {
    /** Does not keep [mpin]; the caller clears it. */
    suspend operator fun invoke(customerIdInput: String, mpin: CharArray): LogInResult {
        val check = checkCustomerId(customerIdInput) as? CustomerIdCheck.Recognised
            ?: return LogInResult.InvalidCustomerId
        return when (val result = mpinVerifier.verify(mpin)) {
            MpinCheck.Correct -> {
                rememberedCustomerStore.rememberCustomer(check.customerId)
                LogInResult.LoggedIn(check.personaId)
            }
            is MpinCheck.Wrong -> LogInResult.WrongMpin(result.attemptsLeft)
            is MpinCheck.LockedOut -> LogInResult.LockedOut(result.until)
        }
    }
}
