package com.abcbank.mobile.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.security.LogOutUseCase
import com.abcbank.mobile.feature.profile.domain.ObserveProfileUseCase
import com.abcbank.mobile.feature.profile.domain.ResetDemoDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeProfile: ObserveProfileUseCase,
    private val logOut: LogOutUseCase,
    private val resetDemoData: ResetDemoDataUseCase
) : ViewModel() {

    private val attempt = MutableStateFlow(0)
    private val loggedOut = MutableStateFlow(false)
    private val reset = MutableStateFlow(ResetStatus.IDLE)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProfileUiState> = combine(
        attempt.flatMapLatest { observeProfile() },
        loggedOut,
        reset
    ) { result, isLoggedOut, resetStatus ->
        when {
            isLoggedOut -> ProfileUiState.LoggedOut
            result is DataResult.Failure -> ProfileUiState.Error(result.error)
            result is DataResult.Success -> result.value?.let {
                ProfileUiState.Content(
                    personaId = it.profile.personaId,
                    name = it.profile.name,
                    customerId = it.profile.customerId.value,
                    isOffline = it.isFromCache,
                    reset = resetStatus
                )
            } ?: ProfileUiState.Empty
            else -> ProfileUiState.Loading
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        ProfileUiState.Loading
    )

    fun onLogOut() {
        viewModelScope.launch {
            logOut()
            loggedOut.value = true
        }
    }

    /** After the confirmation dialog. One at a time, so a second tap does nothing. */
    fun onResetDemoData() {
        if (reset.value == ResetStatus.RESETTING) return
        reset.value = ResetStatus.RESETTING
        viewModelScope.launch {
            reset.value =
                if (resetDemoData() is DataResult.Success) ResetStatus.DONE else ResetStatus.FAILED
        }
    }

    fun onRetry() {
        attempt.value++
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
