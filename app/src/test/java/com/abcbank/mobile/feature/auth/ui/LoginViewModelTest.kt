package com.abcbank.mobile.feature.auth.ui

import androidx.lifecycle.SavedStateHandle
import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.security.MpinAttempts
import com.abcbank.mobile.core.security.MpinVerifier
import com.abcbank.mobile.feature.auth.data.CatalogPersonaDirectory
import com.abcbank.mobile.feature.auth.domain.CheckCustomerIdUseCase
import com.abcbank.mobile.feature.auth.domain.GetStoredDemoDataUseCase
import com.abcbank.mobile.feature.auth.domain.LogInUseCase
import com.abcbank.mobile.feature.auth.domain.PrepareDemoDataUseCase
import com.abcbank.mobile.feature.auth.domain.StoredDemoData
import com.abcbank.mobile.testing.FakeDemoDataRepository
import com.abcbank.mobile.testing.FakeMpinAttemptStore
import com.abcbank.mobile.testing.FakeRememberedCustomerStore
import com.abcbank.mobile.testing.MainDispatcherRule
import com.abcbank.mobile.testing.SchedulerClock
import com.abcbank.mobile.testing.TEST_START
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val attemptStore = FakeMpinAttemptStore()
    private val rememberedStore = FakeRememberedCustomerStore()
    private val demoData = FakeDemoDataRepository()

    private fun TestScope.createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ): LoginViewModel {
        val clock: Clock = SchedulerClock(testScheduler)
        val check = CheckCustomerIdUseCase(CatalogPersonaDirectory())
        val verifier = MpinVerifier(attemptStore, clock)
        return LoginViewModel(
            savedStateHandle = savedStateHandle,
            checkCustomerId = check,
            logIn = LogInUseCase(
                check,
                verifier,
                rememberedStore,
                PrepareDemoDataUseCase(demoData)
            ),
            getStoredDemoData = GetStoredDemoDataUseCase(demoData),
            mpinVerifier = verifier,
            clock = clock
        )
    }

    private val LoginViewModel.content get() = uiState.value as LoginUiState.Content

    private fun LoginViewModel.enterMpin(digits: String) = digits.forEach {
        onMpinDigit(it.digitToInt())
    }

    @Test
    fun `starts loading, then shows an empty form with the pad disabled`() = runTest {
        val viewModel = createViewModel()
        assertEquals(LoginUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals("", viewModel.content.customerId)
        assertFalse(viewModel.content.isMpinEnabled)
        assertEquals(6, viewModel.content.mpinLength)
    }

    @Test
    fun `a recognised ID is upper-cased and enables the pad`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onCustomerIdChange("samjoh01")

        assertEquals("SAMJOH01", viewModel.content.customerId)
        assertNull(viewModel.content.customerIdError)
        assertTrue(viewModel.content.isMpinEnabled)
    }

    @Test
    fun `input is capped at 8 characters`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onCustomerIdChange("SAMJOH01X")

        assertEquals("SAMJOH01", viewModel.content.customerId)
    }

    @Test
    fun `a symbol shows the format error and keeps the pad disabled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onCustomerIdChange("SAM.JOH")

        assertEquals(CustomerIdError.WrongFormat, viewModel.content.customerIdError)
        assertFalse(viewModel.content.isMpinEnabled)
    }

    @Test
    fun `an unknown ID shows the unknown error`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onCustomerIdChange("ABCDEF12")

        assertEquals(CustomerIdError.Unknown, viewModel.content.customerIdError)
        assertFalse(viewModel.content.isMpinEnabled)
    }

    @Test
    fun `pressing Done on an unfinished ID shows the format error`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCustomerIdChange("SAMJ")
        assertNull(viewModel.content.customerIdError)

        viewModel.onCustomerIdDone()

        assertEquals(CustomerIdError.WrongFormat, viewModel.content.customerIdError)
    }

    @Test
    fun `digits are ignored until the ID is recognised`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMpinDigit(1)

        assertEquals(0, viewModel.content.mpinEntered)
    }

    @Test
    fun `delete removes the last digit`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCustomerIdChange("MARLEE01")
        viewModel.enterMpin("123")

        viewModel.onMpinDelete()

        assertEquals(2, viewModel.content.mpinEntered)
    }

    @Test
    fun `the correct MPIN logs in and remembers the customer`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCustomerIdChange("DANOKA01")

        viewModel.enterMpin("123456")
        advanceUntilIdle()

        assertTrue(viewModel.content.isLoggedIn)
        assertEquals("DANOKA01", rememberedStore.remembered?.value)
    }

    @Test
    fun `a wrong MPIN clears the dots and shows the attempts left`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCustomerIdChange("SAMJOH01")

        viewModel.enterMpin("000000")
        advanceUntilIdle()

        assertEquals(0, viewModel.content.mpinEntered)
        assertEquals(MpinMessage.WrongMpin(attemptsLeft = 4), viewModel.content.mpinMessage)
        assertTrue(viewModel.content.isMpinEnabled)
        assertFalse(viewModel.content.isLoggedIn)
    }

    @Test
    fun `five wrong MPINs lock the pad with a countdown that ends after 30 seconds`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCustomerIdChange("SAMJOH01")
        repeat(4) {
            viewModel.enterMpin("000000")
            advanceUntilIdle()
        }

        viewModel.enterMpin("000000")
        runCurrent()

        assertEquals(MpinMessage.LockedOut(secondsLeft = 30), viewModel.content.mpinMessage)
        assertFalse(viewModel.content.isMpinEnabled)

        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(MpinMessage.LockedOut(secondsLeft = 24), viewModel.content.mpinMessage)

        advanceTimeBy(24_000)
        runCurrent()
        assertNull(viewModel.content.mpinMessage)
        assertTrue(viewModel.content.isMpinEnabled)
    }

    @Test
    fun `a saved lockout is restored when the screen opens`() = runTest {
        attemptStore.attempts = MpinAttempts(lockedUntil = TEST_START.plusSeconds(10))
        val viewModel = createViewModel(SavedStateHandle(mapOf("customerId" to "GRAWHI01")))

        runCurrent()

        assertEquals("GRAWHI01", viewModel.content.customerId)
        assertEquals(MpinMessage.LockedOut(secondsLeft = 10), viewModel.content.mpinMessage)
        assertFalse(viewModel.content.isMpinEnabled)
    }

    @Test
    fun `the pad waits until the stored data has been checked`() = runTest {
        demoData.storedGate = CompletableDeferred()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCustomerIdChange("SAMJOH01")
        assertFalse(viewModel.content.isMpinEnabled)

        demoData.storedGate?.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.content.isMpinEnabled)
    }

    @Test
    fun `another persona's data shows the replace warning`() = runTest {
        demoData.stored = StoredDemoData(PersonaId.SAMANTA, "Samanta Johns", seedVersion = 2L)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onCustomerIdChange("MARLEE01")
        assertEquals("Samanta Johns", viewModel.content.replacesDataOf)

        viewModel.onCustomerIdChange("SAMJOH01")
        assertNull(viewModel.content.replacesDataOf)

        viewModel.onCustomerIdChange("SAMJ")
        assertNull(viewModel.content.replacesDataOf)
    }

    @Test
    fun `no warning when the stored data cannot be read`() = runTest {
        demoData.stored = StoredDemoData(PersonaId.SAMANTA, "Samanta Johns", seedVersion = 2L)
        demoData.failWith = DataError.OFFLINE
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onCustomerIdChange("MARLEE01")

        assertNull(viewModel.content.replacesDataOf)
        assertTrue(viewModel.content.isMpinEnabled)
    }

    @Test
    fun `the correct MPIN shows setting up, then seeds and logs in`() = runTest {
        demoData.seedGate = CompletableDeferred()
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCustomerIdChange("GRAWHI01")

        viewModel.enterMpin("123456")
        advanceUntilIdle()

        assertEquals(MpinMessage.SettingUp, viewModel.content.mpinMessage)
        assertEquals(0, viewModel.content.mpinEntered)
        assertFalse(viewModel.content.isMpinEnabled)
        assertNull(rememberedStore.remembered)

        demoData.seedGate?.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.content.isLoggedIn)
        assertEquals(listOf(PersonaId.GRACE), demoData.seeded)
        assertEquals("GRAWHI01", rememberedStore.remembered?.value)
    }

    @Test
    fun `a failed setup asks for the MPIN again and does not log in`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCustomerIdChange("SAMJOH01")
        demoData.failWith = DataError.OFFLINE

        viewModel.enterMpin("123456")
        advanceUntilIdle()

        assertEquals(MpinMessage.SetupFailed, viewModel.content.mpinMessage)
        assertTrue(viewModel.content.isMpinEnabled)
        assertFalse(viewModel.content.isLoggedIn)
        assertNull(rememberedStore.remembered)

        demoData.failWith = null
        viewModel.enterMpin("1")
        assertNull(viewModel.content.mpinMessage)
        viewModel.enterMpin("23456")
        advanceUntilIdle()

        assertTrue(viewModel.content.isLoggedIn)
    }
}
