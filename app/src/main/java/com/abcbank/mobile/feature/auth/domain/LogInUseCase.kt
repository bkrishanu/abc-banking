package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
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

    /** The MPIN was right but the persona's data could not be loaded. Entering it again retries. */
    data class SetupFailed(val error: DataError) : LogInResult
}

/**
 * Customer ID + MPIN login (MobileBuild.md 1.1). After a correct MPIN the persona's data is made
 * ready, and only then is the customer ID remembered, so the lock screen never greets a customer
 * whose data is not there.
 */
class LogInUseCase @Inject constructor(
    private val checkCustomerId: CheckCustomerIdUseCase,
    private val mpinVerifier: MpinVerifier,
    private val rememberedCustomerStore: RememberedCustomerStore,
    private val prepareDemoData: PrepareDemoDataUseCase
) {
    /**
     * Does not keep [mpin]; the caller clears it. [onMpinAccepted] runs once the MPIN is verified,
     * before the data is prepared, so the caller can show progress and clear the digits early.
     */
    suspend operator fun invoke(
        customerIdInput: String,
        mpin: CharArray,
        onMpinAccepted: () -> Unit = {}
    ): LogInResult {
        val check = checkCustomerId(customerIdInput) as? CustomerIdCheck.Recognised
            ?: return LogInResult.InvalidCustomerId
        return when (val result = mpinVerifier.verify(mpin)) {
            MpinCheck.Correct -> {
                onMpinAccepted()
                when (val prepared = prepareDemoData(check.personaId)) {
                    is DataResult.Success -> {
                        rememberedCustomerStore.rememberCustomer(check.customerId)
                        LogInResult.LoggedIn(check.personaId)
                    }
                    is DataResult.Failure -> LogInResult.SetupFailed(prepared.error)
                }
            }
            is MpinCheck.Wrong -> LogInResult.WrongMpin(result.attemptsLeft)
            is MpinCheck.LockedOut -> LogInResult.LockedOut(result.until)
        }
    }
}
