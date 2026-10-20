package com.abcbank.mobile.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.security.LogOutUseCase
import com.abcbank.mobile.core.security.MpinEntry
import com.abcbank.mobile.core.security.MpinEntryState
import com.abcbank.mobile.core.security.MpinVerifier
import com.abcbank.mobile.core.security.SessionLock
import com.abcbank.mobile.core.security.SessionState
import com.abcbank.mobile.feature.auth.domain.GetLockIdentityUseCase
import com.abcbank.mobile.feature.auth.domain.LockIdentity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The lock screen: who is locked out, and the MPIN pad. A form screen, so Loading and Content. */
sealed interface LockUiState {
    data object Loading : LockUiState

    data class Content(val identity: LockIdentity?, val pad: MpinEntryState) : LockUiState
}

/**
 * The session lock screen (MobileBuild.md 1.1, PRD feature 2): MPIN only for the remembered
 * customer, sharing the 5-attempt lockout with login and step-up; "Not you?" logs out so another
 * customer ID can log in.
 */
@HiltViewModel
class LockViewModel @Inject constructor(
    private val getLockIdentity: GetLockIdentityUseCase,
    private val sessionLock: SessionLock,
    private val logOut: LogOutUseCase,
    verifier: MpinVerifier,
    clock: Clock
) : ViewModel() {
    private val entry = MpinEntry(verifier, clock, viewModelScope)

    /** Whether the app is locked; the navigation layer shows the lock screen over everything. */
    val sessionState: StateFlow<SessionState> = sessionLock.state
    private val identity = MutableStateFlow<LockIdentity?>(null)
    private val loaded = MutableStateFlow(false)

    val uiState: StateFlow<LockUiState> = combine(identity, loaded, entry.state) {
            who,
            ready,
            pad
        ->
        if (ready) LockUiState.Content(who, pad) else LockUiState.Loading
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LockUiState.Loading)

    /** Called each time the lock screen shows, so every lock starts with an empty pad. */
    fun onShown() {
        viewModelScope.launch {
            identity.value = getLockIdentity()
            loaded.value = true
        }
        entry.start { sessionLock.onUnlocked() }
    }

    fun onDigit(digit: Int) = entry.onDigit(digit)

    fun onDelete() = entry.onDelete()

    fun onNotYou(onLoggedOut: () -> Unit) {
        entry.cancel()
        viewModelScope.launch {
            logOut()
            onLoggedOut()
        }
    }

    override fun onCleared() = entry.cancel()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
