package com.abcbank.mobile.feature.auth.ui

/** A form screen: Loading while a saved lockout is restored, then Content (AGENTS.md section 04). */
sealed interface LoginUiState {
    data object Loading : LoginUiState

    data class Content(
        val customerId: String,
        val customerIdError: CustomerIdError?,
        /** The stored persona's name when logging in would replace its data (DEMO_DATA.md 01). */
        val replacesDataOf: String?,
        val isMpinEnabled: Boolean,
        val mpinEntered: Int,
        val mpinLength: Int,
        val mpinMessage: MpinMessage?,
        val isLoggedIn: Boolean
    ) : LoginUiState
}

enum class CustomerIdError { WrongFormat, Unknown }

sealed interface MpinMessage {
    data class WrongMpin(val attemptsLeft: Int) : MpinMessage

    data class LockedOut(val secondsLeft: Long) : MpinMessage

    /** The MPIN was right and the persona's data is being loaded. */
    data object SettingUp : MpinMessage

    /** The persona's data could not be loaded; entering the MPIN again retries. */
    data object SetupFailed : MpinMessage
}
