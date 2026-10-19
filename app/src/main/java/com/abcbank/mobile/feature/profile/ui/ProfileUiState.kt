package com.abcbank.mobile.feature.profile.ui

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.model.PersonaId

/** Log Out is offered in every state except Loading, so a broken profile never traps the user. */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    /** Nothing seeded on this install. */
    data object Empty : ProfileUiState

    data class Error(val error: DataError) : ProfileUiState

    data class Content(
        val personaId: PersonaId,
        val name: String,
        val customerId: String,
        val isOffline: Boolean
    ) : ProfileUiState

    /** Logged out; the screen hands over to the login screen. */
    data object LoggedOut : ProfileUiState
}
