package com.abcbank.mobile.core.security

import javax.inject.Inject

/**
 * Log Out, from Profile or the lock screen's "Not you?" (ARCHITECTURE.md section 04, Auth and
 * session): forgets the remembered customer ID, ends the in-memory session and the session lock. The install's anonymous Firebase user and its
 * Firestore data stay, so logging in again with the same customer ID keeps the data; a different
 * ID opens that persona's own data tree, which is written on its first login.
 */
class LogOutUseCase @Inject constructor(
    private val rememberedCustomerStore: RememberedCustomerStore,
    private val sessionManager: SessionManager,
    private val sessionLock: SessionLock
) {
    suspend operator fun invoke() {
        rememberedCustomerStore.forgetCustomer()
        sessionManager.endSession()
        sessionLock.onLoggedOut()
    }
}
