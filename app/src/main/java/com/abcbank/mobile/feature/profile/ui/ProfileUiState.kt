package com.abcbank.mobile.feature.profile.ui

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.model.PersonaId

/** Log Out is offered in every state, even while loading, so a broken or unwritten profile never traps the user. */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    /** Nobody is logged in. */
    data object Empty : ProfileUiState

    data class Error(val error: DataError) : ProfileUiState

    data class Content(
        val personaId: PersonaId,
        val name: String,
        val customerId: String,
        val isOffline: Boolean,
        val reset: ResetStatus = ResetStatus.IDLE
    ) : ProfileUiState

    /** Logged out; the screen hands over to the login screen. */
    data object LoggedOut : ProfileUiState
}

/** Reset Demo Data: not asked, rewriting, done, or failed. */
enum class ResetStatus { IDLE, RESETTING, DONE, FAILED }
