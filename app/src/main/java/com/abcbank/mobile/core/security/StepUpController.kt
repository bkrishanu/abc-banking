package com.abcbank.mobile.core.security

import com.abcbank.mobile.core.common.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    private val verifier: MpinVerifier,
    private val manager: StepUpManager,
    private val clock: Clock,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(StepUpState())
    val state: StateFlow<StepUpState> = _state.asStateFlow()

    private val digits = CharArray(MpinVerifier.LENGTH)
    private var count = 0
    private var onGranted: ((StepUpGrant) -> Unit)? = null
    private var countdown: Job? = null

    /** Opens the sheet for [action], or grants at once when the action does not need step-up. */
    fun request(action: StepUpAction, onGranted: (StepUpGrant) -> Unit) {
        if (!manager.isRequired(action)) {
            onGranted(manager.issue(action))
            return
        }
        clear()
        this.onGranted = onGranted
        _state.value = StepUpState(action = action)
        scope.launch { verifier.lockedUntil()?.let(::startCountdown) }
    }

    fun onDigit(digit: Int) {
        if (!_state.value.isPadEnabled || count >= digits.size || digit !in 0..9) return
        digits[count++] = '0' + digit
        val message = _state.value.message.takeUnless { it is StepUpMessage.WrongMpin }
        _state.value = _state.value.copy(entered = count, message = message)
        if (count == digits.size) submit()
    }

    fun onDelete() {
        if (!_state.value.isPadEnabled || count == 0) return
        digits[--count] = Char.MIN_VALUE
        _state.value = _state.value.copy(entered = count)
    }

    fun cancel() {
        clear()
        countdown?.cancel()
        onGranted = null
        _state.value = StepUpState()
    }

    private fun submit() {
        val action = _state.value.action ?: return
        _state.value = _state.value.copy(verifying = true)
        scope.launch {
            val result = try {
                verifier.verify(digits)
            } finally {
                clear()
            }
            when (result) {
                MpinCheck.Correct -> {
                    val callback = onGranted
                    onGranted = null
                    _state.value = StepUpState()
                    callback?.invoke(manager.issue(action))
                }
                is MpinCheck.Wrong -> _state.value = _state.value.copy(
                    entered = 0,
                    verifying = false,
                    message = StepUpMessage.WrongMpin(result.attemptsLeft)
                )
                is MpinCheck.LockedOut -> {
                    _state.value = _state.value.copy(entered = 0, verifying = false)
                    startCountdown(result.until)
                }
            }
        }
    }

    private fun startCountdown(until: Instant) {
        countdown?.cancel()
        countdown = scope.launch {
            while (_state.value.isVisible) {
                val millis = Duration.between(clock.now(), until).toMillis()
                if (millis <= 0) {
                    _state.value = _state.value.copy(message = null)
                    break
                }
                _state.value = _state.value.copy(
                    message = StepUpMessage.LockedOut((millis + TICK_MS - 1) / TICK_MS)
                )
                delay(TICK_MS)
            }
        }
    }

    private fun clear() {
        digits.fill(Char.MIN_VALUE)
        count = 0
    }

    private companion object {
        const val TICK_MS = 1_000L
    }
}
