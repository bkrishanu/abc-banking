package com.abcbank.mobile.core.security

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide session state (ARCHITECTURE.md section 04). For now only balance masking: the eye
 * toggle hides every amount and stays hidden for the session, in memory only, so a new process or
 * a new login starts with balances shown. The session lock is added here with the lock screen.
 */
@Singleton
class SessionManager @Inject constructor() {
    private val _balancesHidden = MutableStateFlow(false)
    val balancesHidden: StateFlow<Boolean> = _balancesHidden.asStateFlow()

    fun toggleBalancesHidden() {
        _balancesHidden.value = !_balancesHidden.value
    }

    /** Log Out: the next person to log in starts a fresh session. */
    fun endSession() {
        _balancesHidden.value = false
    }
}
