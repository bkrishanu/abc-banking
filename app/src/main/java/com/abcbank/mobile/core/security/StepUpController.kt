package com.abcbank.mobile.core.security

import com.abcbank.mobile.core.common.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface StepUpMessage {
    data class WrongMpin(val attemptsLeft: Int) : StepUpMessage

    data class LockedOut(val secondsLeft: Long) : StepUpMessage
}

/** What the step-up sheet shows. [action] is null while the sheet is closed. */
data class StepUpState(
    val action: StepUpAction? = null,
    val entered: Int = 0,
    val length: Int = MpinVerifier.LENGTH,
    val message: StepUpMessage? = null,
    val verifying: Boolean = false
) {
    val isVisible: Boolean get() = action != null
    val isPadEnabled: Boolean get() = isVisible && !verifying && message !is StepUpMessage.LockedOut
}

/**
 * The MPIN entry behind the step-up sheet (DESIGN_SYSTEM.md section 06, StepUpSheet), shared by
 * every screen that needs step-up. A ViewModel creates one with its own scope. Wrong entries count
 * toward the same 5-attempt lockout as login, through MpinVerifier. The digits live only here and
 * are wiped after every attempt and on cancel.
 */
class StepUpController(
    verifier: MpinVerifier,
    private val manager: StepUpManager,
    clock: Clock,
    scope: CoroutineScope
) {
    private var action: StepUpAction? = null
    private val _state = MutableStateFlow(StepUpState())
    val state: StateFlow<StepUpState> = _state.asStateFlow()
    private val entry = MpinEntry(verifier, clock, scope) { pad ->
        val current = action
        _state.value = if (current == null || !pad.active) {
            StepUpState()
        } else {
            StepUpState(current, pad.entered, pad.length, pad.message, pad.verifying)
        }
    }

    /** Opens the sheet for [action], or grants at once when the action does not need step-up. */
    fun request(action: StepUpAction, onGranted: (StepUpGrant) -> Unit) {
        if (!manager.isRequired(action)) {
            onGranted(manager.issue(action))
            return
        }
        this.action = action
        entry.start {
            this.action = null
            onGranted(manager.issue(action))
        }
    }

    fun onDigit(digit: Int) = entry.onDigit(digit)

    fun onDelete() = entry.onDelete()

    fun cancel() {
        action = null
        entry.cancel()
    }
}
