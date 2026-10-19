package com.abcbank.mobile.feature.home

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.Card
import com.abcbank.mobile.core.model.CardArt
import com.abcbank.mobile.core.model.CardChannels
import com.abcbank.mobile.core.model.CardLimits
import com.abcbank.mobile.core.model.CardNetwork
import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.InvestmentAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import com.abcbank.mobile.core.model.UserProfile
import com.abcbank.mobile.feature.home.domain.HomeData
import com.abcbank.mobile.feature.home.domain.HomeRepository
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

/** 2026-10-19 13:00 UTC; tests run in UTC. */
val HOME_NOW: Instant = Instant.parse("2026-10-19T13:00:00Z")

class FakeHomeRepository : HomeRepository {
    val result = MutableStateFlow<DataResult<HomeData>?>(null)
    var requestedSince: Instant? = null
    var subscriptions = 0

    override fun observeHome(transactionsSince: Instant): Flow<DataResult<HomeData>> {
        requestedSince = transactionsSince
        subscriptions++
        return result.filterNotNull()
    }
}

fun profile(personaId: PersonaId = PersonaId.SAMANTA, name: String = "Samanta Johns") = UserProfile(
    personaId = personaId,
    customerId = requireNotNull(CustomerId.parse("SAMJOH01")),
    seedVersion = 3,
    name = name,
    holderName = name.uppercase(),
    email = "samanta.johns@example.com",
    phone = "+1 (555) 010-2231",
    employment = "Employed",
    annualIncome = Money(18_500_000),
    monthlyBudget = Money(1_890_000),
    creditScore = 792,
    checkDepositLimit = Money(1_000_000),
    createdAt = HOME_NOW
)

fun deposit(id: String, type: AccountType, available: Long, ledger: Long = available) =
    DepositAccount(id, type, id, "3144", Money(available), Money(ledger), apy = null)

fun card(id: String, accountId: String, stackOrder: Int) = Card(
    id = id,
    accountId = accountId,
    tag = null,
    network = CardNetwork.MASTERCARD,
    art = CardArt.PEACH,
    holderName = "SAMANTA JOHNS",
    last4 = "3144",
    expiry = YearMonth.of(2029, 8),
    frozen = false,
    limits = CardLimits(Money(100_000), Money(250_000), Money(500_000), Money(1_000_000)),
    channels = CardChannels(international = true, online = true, contactless = true),
    stackOrder = stackOrder
)

fun transaction(
    id: String,
    accountId: String,
    signedCents: Long,
    at: Instant,
    category: TransactionCategory = TransactionCategory.SHOPPING,
    status: TransactionStatus = TransactionStatus.POSTED
) = Transaction(
    id = id,
    accountId = accountId,
    direction = if (signedCents < 0) TransactionDirection.DEBIT else TransactionDirection.CREDIT,
    amount = Money(kotlin.math.abs(signedCents)),
    merchant = id,
    category = category,
    reference = id,
    runningBalance = Money.ZERO,
    status = status,
    disputed = false,
    createdAt = at,
    clearsAt = null
)

fun homeData(
    accounts: List<Account> = listOf(
        deposit("savings", AccountType.SAVINGS, 1_203_200),
        InvestmentAccount("investment", "Investment Portfolio", "4410", Money(8_461_240)),
        deposit("checking", AccountType.CHECKING, 15_583_200, 15_620_700)
    ),
    cards: List<Card> = listOf(card("travel", "savings", 2), card("main", "checking", 0)),
    transactions: List<Transaction> = emptyList(),
    unread: Int = 0,
    fromCache: Boolean = false,
    personaId: PersonaId = PersonaId.SAMANTA
) = HomeData(profile(personaId), accounts, cards, transactions, unread, fromCache)
