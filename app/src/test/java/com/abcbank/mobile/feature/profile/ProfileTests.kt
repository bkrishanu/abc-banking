package com.abcbank.mobile.feature.profile

import app.cash.turbine.test
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.security.LogOutUseCase
import com.abcbank.mobile.core.security.SessionLock
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.core.security.SessionState
import com.abcbank.mobile.feature.home.profile
import com.abcbank.mobile.feature.profile.domain.DemoDataResetter
import com.abcbank.mobile.feature.profile.domain.ObserveProfileUseCase
import com.abcbank.mobile.feature.profile.domain.ProfileData
import com.abcbank.mobile.feature.profile.domain.ProfileRepository
import com.abcbank.mobile.feature.profile.domain.ResetDemoDataUseCase
import com.abcbank.mobile.feature.profile.ui.ProfileUiState
import com.abcbank.mobile.feature.profile.ui.ProfileViewModel
import com.abcbank.mobile.feature.profile.ui.ResetStatus
import com.abcbank.mobile.testing.FakeRememberedCustomerStore
import com.abcbank.mobile.testing.MainDispatcherRule
import com.abcbank.mobile.testing.ManualClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeProfileRepository : ProfileRepository {
    val result = MutableStateFlow<DataResult<ProfileData?>?>(null)

    override fun observeProfile(): Flow<DataResult<ProfileData?>> = result.filterNotNull()
}

class LogOutUseCaseTest {
    @Test
    fun `log out forgets the customer, resets balance masking and ends the session lock`() =
        runTest {
            val store = FakeRememberedCustomerStore().apply {
                remembered = CustomerId.parse("SAMJOH01")
            }
            val session = SessionManager().apply { toggleBalancesHidden() }
            val lock = SessionLock(ManualClock()).apply { onLoggedIn() }

            LogOutUseCase(store, session, lock)()

            assertNull(store.remembered)
            assertFalse(session.balancesHidden.value)
            assertEquals(SessionState.LOGGED_OUT, lock.state.value)
        }
}

class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProfileRepository()
    private val store = FakeRememberedCustomerStore().apply {
        remembered =
            CustomerId.parse("SAMJOH01")
    }
    private val resetter = object : DemoDataResetter {
        var calls = 0

        override suspend fun resetActivePersona(): DataResult<Unit> {
            calls++
            return DataResult.Success(Unit)
        }
    }
    private val viewModel = ProfileViewModel(
        ObserveProfileUseCase(repository),
        LogOutUseCase(store, SessionManager(), SessionLock(ManualClock())),
        ResetDemoDataUseCase(resetter)
    )

    @Test
    fun `Reset Demo Data runs once per tap and reports when done`() = runTest {
        // Built inside the test, so its scope runs on this test's scheduler.
        val viewModel = ProfileViewModel(
            ObserveProfileUseCase(repository),
            LogOutUseCase(store, SessionManager(), SessionLock(ManualClock())),
            ResetDemoDataUseCase(resetter)
        )
        viewModel.uiState.test {
            skipItems(1)
            repository.result.value =
                DataResult.Success(ProfileData(profile(), isFromCache = false))
            awaitItem()
            viewModel.onResetDemoData()
            viewModel.onResetDemoData()
            testScheduler.advanceUntilIdle()
            assertEquals(ResetStatus.DONE, (expectMostRecentItem() as ProfileUiState.Content).reset)
            assertEquals(1, resetter.calls)
        }
    }

    @Test
    fun `shows the persona, then logs out`() = runTest {
        viewModel.uiState.test {
            assertEquals(ProfileUiState.Loading, awaitItem())

            repository.result.value =
                DataResult.Success(ProfileData(profile(), isFromCache = false))
            assertEquals(
                ProfileUiState.Content(
                    PersonaId.SAMANTA,
                    "Samanta Johns",
                    "SAMJOH01",
                    isOffline = false
                ),
                awaitItem()
            )

            viewModel.onLogOut()

            assertEquals(ProfileUiState.LoggedOut, awaitItem())
            assertNull(store.remembered)
        }
    }

    @Test
    fun `an error or an unseeded install still allows logging out`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            repository.result.value = DataResult.Failure(DataError.OFFLINE)
            assertEquals(ProfileUiState.Error(DataError.OFFLINE), awaitItem())

            repository.result.value = DataResult.Success(null)
            assertEquals(ProfileUiState.Empty, awaitItem())

            viewModel.onLogOut()
            assertTrue(awaitItem() == ProfileUiState.LoggedOut)
        }
    }
}
