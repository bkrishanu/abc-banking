package com.abcbank.mobile.feature.auth.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.security.MpinVerifier
import com.abcbank.mobile.feature.auth.domain.CheckCustomerIdUseCase
import com.abcbank.mobile.feature.auth.domain.CustomerIdCheck
import com.abcbank.mobile.feature.auth.domain.LogInResult
import com.abcbank.mobile.feature.auth.domain.LogInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val checkCustomerId: CheckCustomerIdUseCase,
    private val logIn: LogInUseCase,
    private val mpinVerifier: MpinVerifier,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Loading)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // The digits stay here only: never in UiState or SavedStateHandle, and wiped after every attempt.
    private val mpin = CharArray(MpinVerifier.LENGTH)
    private var mpinCount = 0

    private var ready = false
    private var customerId = ""
    private var customerIdError: CustomerIdError? = null
    private var recognised = false
    private var mpinMessage: MpinMessage? = null
    private var lockedUntil: Instant? = null
    private var verifying = false
    private var loggedIn = false
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            applyCustomerId(savedStateHandle.get<String>(KEY_CUSTOMER_ID).orEmpty())
            ready = true
            mpinVerifier.lockedUntil()?.let(::startCountdown)
            render()
        }
    }

    fun onCustomerIdChange(input: String) {
        if (!ready) return
        val value = input.trim().uppercase(Locale.ROOT).take(CustomerId.LENGTH)
        if (value == customerId) return
        clearMpin()
        if (mpinMessage is MpinMessage.WrongMpin) mpinMessage = null
        applyCustomerId(value)
        render()
    }

    /** The keyboard's Done key: an unfinished ID is now a format error, not "still typing". */
    fun onCustomerIdDone() {
        if (!ready || customerId.isEmpty()) return
        if (checkCustomerId(customerId) == CustomerIdCheck.Incomplete) {
            customerIdError = CustomerIdError.WrongFormat
            render()
        }
    }

    fun onMpinDigit(digit: Int) {
        if (!isMpinEnabled() || mpinCount >= mpin.size || digit !in 0..9) return
        mpin[mpinCount++] = '0' + digit
        if (mpinMessage is MpinMessage.WrongMpin) mpinMessage = null
        render()
        if (mpinCount == mpin.size) submit()
    }

    fun onMpinDelete() {
        if (!isMpinEnabled() || mpinCount == 0) return
        mpin[--mpinCount] = Char.MIN_VALUE
        render()
    }

    private fun applyCustomerId(value: String) {
        customerId = value
        savedStateHandle[KEY_CUSTOMER_ID] = value
        val check = checkCustomerId(value)
        recognised = check is CustomerIdCheck.Recognised
        customerIdError = when (check) {
            CustomerIdCheck.WrongFormat -> CustomerIdError.WrongFormat
            CustomerIdCheck.Unknown -> CustomerIdError.Unknown
            CustomerIdCheck.Incomplete, is CustomerIdCheck.Recognised -> null
        }
    }

    private fun submit() {
        verifying = true
        render()
        viewModelScope.launch {
            val result = try {
                logIn(customerId, mpin)
            } finally {
                clearMpin()
            }
            verifying = false
            when (result) {
                is LogInResult.LoggedIn -> loggedIn = true
                is LogInResult.WrongMpin -> mpinMessage = MpinMessage.WrongMpin(result.attemptsLeft)
                is LogInResult.LockedOut -> startCountdown(result.until)
                LogInResult.InvalidCustomerId -> {
                    recognised = false
                    customerIdError = CustomerIdError.Unknown
                }
            }
            render()
        }
    }

    private fun startCountdown(until: Instant) {
        lockedUntil = until
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val secondsLeft = secondsUntil(until)
                if (secondsLeft <= 0) {
                    lockedUntil = null
                    mpinMessage = null
                    render()
                    break
                }
                mpinMessage = MpinMessage.LockedOut(secondsLeft)
                render()
                delay(COUNTDOWN_TICK_MS)
            }
        }
    }

    private fun secondsUntil(until: Instant): Long {
        val millis = Duration.between(clock.now(), until).toMillis()
        return (millis + COUNTDOWN_TICK_MS - 1) / COUNTDOWN_TICK_MS
    }

    private fun isMpinEnabled(): Boolean =
        ready && recognised && lockedUntil == null && !verifying && !loggedIn

    private fun clearMpin() {
        mpin.fill(Char.MIN_VALUE)
        mpinCount = 0
    }

    private fun render() {
        if (!ready) return
        _uiState.value = LoginUiState.Content(
            customerId = customerId,
            customerIdError = customerIdError,
            isMpinEnabled = isMpinEnabled(),
            mpinEntered = mpinCount,
            mpinLength = mpin.size,
            mpinMessage = mpinMessage,
            isLoggedIn = loggedIn
        )
    }

    override fun onCleared() {
        clearMpin()
    }

    private companion object {
        const val KEY_CUSTOMER_ID = "customerId"
        const val COUNTDOWN_TICK_MS = 1_000L
    }
}
