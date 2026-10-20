package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.common.ApplicationScope
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.security.MpinCheck
import com.abcbank.mobile.core.security.MpinVerifier
import com.abcbank.mobile.core.security.RememberedCustomerStore
import com.abcbank.mobile.core.security.SessionLock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed interface LogInResult {
    data class LoggedIn(val personaId: PersonaId) : LogInResult

    data class WrongMpin(val attemptsLeft: Int) : LogInResult

    data class LockedOut(val until: Instant) : LogInResult

    data object InvalidCustomerId : LogInResult
}

/**
 * Customer ID + MPIN login (MobileBuild.md 1.1). Entirely local: the MPIN check and remembering
 * the customer never touch the network, so login cannot fail or stall on Firebase. Preparing the
 * persona's data starts in the background and the screens show it as soon as it is in the cache.
 */
class LogInUseCase @Inject constructor(
    private val checkCustomerId: CheckCustomerIdUseCase,
    private val mpinVerifier: MpinVerifier,
    private val rememberedCustomerStore: RememberedCustomerStore,
    private val prepareDemoData: PrepareDemoDataUseCase,
    private val demoData: DemoDataRepository,
    private val sessionLock: SessionLock,
    @param:ApplicationScope private val applicationScope: CoroutineScope
) {
    /** Does not keep [mpin]; the caller clears it. */
    suspend operator fun invoke(customerIdInput: String, mpin: CharArray): LogInResult {
        val check = checkCustomerId(customerIdInput) as? CustomerIdCheck.Recognised
            ?: return LogInResult.InvalidCustomerId
        return when (val result = mpinVerifier.verify(mpin)) {
            MpinCheck.Correct -> {
                rememberedCustomerStore.rememberCustomer(check.customerId)
                sessionLock.onLoggedIn()
                // Not awaited: Home shows the data once it is in the cache.
                applicationScope.launch {
                    demoData.recordLogin(check.personaId, check.customerId)
                    prepareDemoData(check.personaId)
                }
                LogInResult.LoggedIn(check.personaId)
            }
            is MpinCheck.Wrong -> LogInResult.WrongMpin(result.attemptsLeft)
            is MpinCheck.LockedOut -> LogInResult.LockedOut(result.until)
        }
    }
}
