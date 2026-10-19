package com.abcbank.mobile.feature.home.domain

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TransactionStatus
import com.abcbank.mobile.feature.home.FakeHomeRepository
import com.abcbank.mobile.feature.home.HOME_NOW
import com.abcbank.mobile.feature.home.deposit
import com.abcbank.mobile.feature.home.homeData
import com.abcbank.mobile.feature.home.transaction
import com.abcbank.mobile.testing.ManualClock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveHomeUseCaseTest {
    private val repository = FakeHomeRepository()
    private val observe = ObserveHomeUseCase(repository, ManualClock(HOME_NOW)) { ZoneOffset.UTC }

    private suspend fun summary(data: HomeData): HomeSummary {
        repository.result.value = DataResult.Success(data)
        return (observe().first() as DataResult.Success).value
    }

    private fun daysAgo(days: Long): Instant = HOME_NOW.minusSeconds(days * 86_400)

    @Test
    fun `asks for today and the 29 days before it`() = runTest {
        summary(homeData())

        assertEquals(Instant.parse("2026-09-20T00:00:00Z"), repository.requestedSince)
    }

    @Test
    fun `the wallet is the Main card's account`() = runTest {
        assertEquals(Money(15_583_200), summary(homeData()).walletBalance)
    }

    @Test
    fun `without cards the wallet falls back to checking`() = runTest {
        assertEquals(Money(15_583_200), summary(homeData(cards = emptyList())).walletBalance)
    }

    @Test
    fun `money in and out count the wallet account only, pending included`() = runTest {
        val result = summary(
            homeData(
                transactions = listOf(
                    transaction("pay", "checking", 711_538, daysAgo(3)),
                    transaction("shop", "checking", -12_500, daysAgo(0)),
                    transaction(
                        "sofa",
                        "checking",
                        -37_500,
                        daysAgo(0),
                        status = TransactionStatus.PENDING
                    ),
                    transaction("interest", "savings", 3_066, daysAgo(12))
                )
            )
        )

        assertEquals(Money(711_538), result.moneyIn)
        assertEquals(Money(50_000), result.moneyOut)
    }

    @Test
    fun `accounts are ordered by type and flagged when low`() = runTest {
        val result = summary(
            homeData(
                accounts = listOf(
                    deposit("savings", AccountType.SAVINGS, 125_000),
                    deposit("checking", AccountType.CHECKING, 41_217)
                )
            )
        )

        assertEquals(listOf("checking", "savings"), result.accounts.map { it.account.id })
        assertEquals(listOf(true, false), result.accounts.map { it.isLowBalance })
        assertFalse(result.hasInvestments)
    }

    @Test
    fun `latest shows the five newest across accounts`() = runTest {
        val transactions = (1..7).map {
            transaction("t$it", "checking", -100L * it, daysAgo(it.toLong()))
        }

        val result = summary(homeData(transactions = transactions))

        assertEquals(listOf("t1", "t2", "t3", "t4", "t5"), result.latestTransactions.map { it.id })
        assertTrue(result.hasInvestments)
    }

    @Test
    fun `offline cache and failures pass through`() = runTest {
        assertTrue(summary(homeData(fromCache = true)).isOffline)

        repository.result.value = DataResult.Failure(DataError.OFFLINE)
        assertEquals(DataResult.Failure(DataError.OFFLINE), observe().first())
    }
}
