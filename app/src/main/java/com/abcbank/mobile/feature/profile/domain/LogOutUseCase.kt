package com.abcbank.mobile.feature.profile.domain

import com.abcbank.mobile.core.security.RememberedCustomerStore
import com.abcbank.mobile.core.security.SessionManager
import javax.inject.Inject

/**
 * Profile > Log Out (ARCHITECTURE.md section 04, Auth and session): forgets the remembered
 * customer ID and ends the in-memory session. The install's anonymous Firebase user and its
 * Firestore data stay, so logging in again with the same customer ID keeps the data; a different
 * ID opens that persona's own data tree, which is written on its first login.
 */
class LogOutUseCase @Inject constructor(
    private val rememberedCustomerStore: RememberedCustomerStore,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke() {
        rememberedCustomerStore.forgetCustomer()
        sessionManager.endSession()
    }
}
