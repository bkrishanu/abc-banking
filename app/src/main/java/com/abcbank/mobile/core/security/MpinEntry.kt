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

/** What an MPIN pad shows outside login: digits entered, the message, and whether it is busy. */
data class MpinEntryState(
    val active: Boolean = false,
    val entered: Int = 0,
    val length: Int = MpinVerifier.LENGTH,
    val message: StepUpMessage? = null,
    val verifying: Boolean = false
) {
    val isPadEnabled: Boolean get() = active && !verifying && message !is StepUpMessage.LockedOut
}

/**
 * One MPIN entry, shared by the step-up sheet and the lock screen: six digits checked by
 * MpinVerifier, so wrong entries count toward the same 5-attempt lockout as login, with the
 * lockout countdown. The digits live only here and are wiped after every attempt and on cancel.
 */
class MpinEntry(
    private val verifier: MpinVerifier,
    private val clock: Clock,
    private val scope: CoroutineScope,
    /** Told of every change at once, for owners that derive their own state from it. */
    private val onChange: (MpinEntryState) -> Unit = {}
) {
    private val _state = MutableStateFlow(MpinEntryState())
    val state: StateFlow<MpinEntryState> = _state.asStateFlow()

    private var current: MpinEntryState
        get() = _state.value
        set(value) {
            _state.value = value
            onChange(value)
        }

    private val digits = CharArray(MpinVerifier.LENGTH)
    private var count = 0
    private var onCorrect: (() -> Unit)? = null
    private var countdown: Job? = null

    /** Opens the pad; [onCorrect] runs once after the right MPIN. */
    fun start(onCorrect: () -> Unit) {
        clear()
        this.onCorrect = onCorrect
        current = MpinEntryState(active = true)
        scope.launch { verifier.lockedUntil()?.let(::startCountdown) }
    }

    fun onDigit(digit: Int) {
        if (!current.isPadEnabled || count >= digits.size || digit !in 0..9) return
        digits[count++] = '0' + digit
        val message = current.message.takeUnless { it is StepUpMessage.WrongMpin }
        current = current.copy(entered = count, message = message)
        if (count == digits.size) submit()
    }

    fun onDelete() {
        if (!current.isPadEnabled || count == 0) return
        digits[--count] = Char.MIN_VALUE
        current = current.copy(entered = count)
    }

    fun cancel() {
        clear()
        countdown?.cancel()
        onCorrect = null
        current = MpinEntryState()
    }

    private fun submit() {
        current = current.copy(verifying = true)
        scope.launch {
            val result = try {
                verifier.verify(digits)
            } finally {
                clear()
            }
            when (result) {
                MpinCheck.Correct -> {
                    val callback = onCorrect
                    onCorrect = null
                    current = MpinEntryState()
                    callback?.invoke()
                }
                is MpinCheck.Wrong -> current = current.copy(
                    entered = 0,
                    verifying = false,
                    message = StepUpMessage.WrongMpin(result.attemptsLeft)
                )
                is MpinCheck.LockedOut -> {
                    current = current.copy(entered = 0, verifying = false)
                    startCountdown(result.until)
                }
            }
        }
    }

    private fun startCountdown(until: Instant) {
        countdown?.cancel()
        countdown = scope.launch {
            while (current.active) {
                val millis = Duration.between(clock.now(), until).toMillis()
                if (millis <= 0) {
                    current = current.copy(message = null)
                    break
                }
                current =
                    current.copy(
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
