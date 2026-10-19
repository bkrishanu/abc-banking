package com.abcbank.mobile.feature.home.ui

import app.cash.turbine.test
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.feature.home.FakeHomeRepository
import com.abcbank.mobile.feature.home.HOME_NOW
import com.abcbank.mobile.feature.home.deposit
import com.abcbank.mobile.feature.home.domain.ObserveHomeUseCase
import com.abcbank.mobile.feature.home.homeData
import com.abcbank.mobile.feature.home.transaction
import com.abcbank.mobile.testing.MainDispatcherRule
import com.abcbank.mobile.testing.ManualClock
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeHomeRepository()
    private val session = SessionManager()
    private val clock = ManualClock(HOME_NOW)

    private fun createViewModel() = HomeViewModel(
        observeHome = ObserveHomeUseCase(repository, clock) { ZoneOffset.UTC },
        sessionManager = session,
        clock = clock,
        zone = { ZoneOffset.UTC }
    )

    @Test
    fun `loading, then content with the persona's greeting and wallet`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            repository.result.value = DataResult.Success(homeData(unread = 3))
            val content = awaitItem() as HomeUiState.Content

            assertEquals("Samanta", content.firstName)
            assertEquals(PersonaId.SAMANTA, content.personaId)
            assertEquals(3, content.unreadNotifications)
            assertEquals(
                listOf("checking", "savings", "investment"),
                content.accounts.map {
                    it.account.id
                }
            )
            assertFalse(content.showNoInvestments)
        }
    }

    @Test
    fun `no investment account shows the empty investments card`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            repository.result.value = DataResult.Success(
                homeData(accounts = listOf(deposit("checking", AccountType.CHECKING, 41_217)))
            )
            val content = awaitItem() as HomeUiState.Content
            assertTrue(content.showNoInvestments)
            assertTrue(content.accounts.single().isLowBalance)
        }
    }

    @Test
    fun `transactions are labelled today, yesterday or earlier in local time`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            repository.result.value = DataResult.Success(
                homeData(
                    transactions = listOf(
                        transaction("today", "checking", -100, HOME_NOW.minusSeconds(3_600)),
                        transaction("yesterday", "checking", -100, HOME_NOW.minusSeconds(86_400)),
                        transaction("earlier", "checking", -100, HOME_NOW.minusSeconds(3 * 86_400))
                    )
                )
            )
            val days = (awaitItem() as HomeUiState.Content).latestTransactions.map { it.day }
            assertEquals(
                listOf(TransactionDay.TODAY, TransactionDay.YESTERDAY, TransactionDay.EARLIER),
                days
            )
        }
    }

    @Test
    fun `the eye toggle hides balances for the whole session`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            repository.result.value = DataResult.Success(homeData())
            assertFalse((awaitItem() as HomeUiState.Content).balancesHidden)

            viewModel.onToggleBalances()

            assertTrue((awaitItem() as HomeUiState.Content).balancesHidden)
            assertTrue(session.balancesHidden.value)
        }
        // A new Home (after navigating away and back) keeps the session's choice.
        createViewModel().uiState.test {
            skipItems(1)
            assertTrue((awaitItem() as HomeUiState.Content).balancesHidden)
        }
    }

    @Test
    fun `no accounts is the empty state`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            repository.result.value = DataResult.Success(homeData(accounts = emptyList()))
            assertEquals(HomeUiState.Empty(PersonaId.SAMANTA, "Samanta"), awaitItem())
        }
    }

    @Test
    fun `an error shows, and Retry subscribes again`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            repository.result.value = DataResult.Failure(DataError.OFFLINE)
            assertEquals(HomeUiState.Error(DataError.OFFLINE), awaitItem())

            repository.result.value = DataResult.Success(homeData())
            viewModel.onRetry()

            assertTrue(awaitItem() is HomeUiState.Content)
            assertEquals(2, repository.subscriptions)
        }
    }
}
