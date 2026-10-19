package com.abcbank.mobile.feature.home.domain

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.LowBalanceRule
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionDirection
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class HomeAccount(val account: Account, val isLowBalance: Boolean)

/** Home's content, with the rules in MobileBuild.md 2.1 and DEMO_DATA.md section 03 applied. */
data class HomeSummary(
    val personaId: PersonaId,
    val firstName: String,
    /** The Main card's linked account balance (DEMO_DATA.md section 03). */
    val walletBalance: Money,
    /** Credits and debits on the wallet account over the last 30 days, pending included. */
    val moneyIn: Money,
    val moneyOut: Money,
    val accounts: List<HomeAccount>,
    val hasInvestments: Boolean,
    val latestTransactions: List<Transaction>,
    val unreadNotifications: Int,
    val isOffline: Boolean
)

class ObserveHomeUseCase @Inject constructor(
    private val repository: HomeRepository,
    private val clock: Clock,
    private val zone: Provider<ZoneId>
) {
    operator fun invoke(): Flow<DataResult<HomeSummary>> {
        val zone = zone.get()
        // "Last 30 days" is today and the 29 days before it (DEMO_DATA.md section 01).
        val since = clock.now().atZone(zone).toLocalDate()
            .minusDays(LAST_30_DAYS - 1)
            .atStartOfDay(zone)
            .toInstant()
        return repository.observeHome(since).map { result ->
            when (result) {
                is DataResult.Success -> DataResult.Success(summarize(result.value, since))
                is DataResult.Failure -> result
            }
        }
    }

    private fun summarize(data: HomeData, since: Instant): HomeSummary {
        val walletAccount = walletAccount(data)
        val walletTransactions = data.recentTransactions.filter {
            it.accountId == walletAccount?.id && !it.createdAt.isBefore(since)
        }
        return HomeSummary(
            personaId = data.profile.personaId,
            firstName = data.profile.firstName,
            walletBalance = walletAccount?.let(::spendable) ?: Money.ZERO,
            moneyIn = walletTransactions.total(TransactionDirection.CREDIT),
            moneyOut = walletTransactions.total(TransactionDirection.DEBIT),
            accounts = data.accounts
                .sortedWith(
                    compareBy<Account> {
                        ACCOUNT_ORDER.indexOf(it.type)
                    }.thenBy { it.nickname }
                )
                .map { HomeAccount(it, LowBalanceRule.isLow(it)) },
            hasInvestments = data.accounts.any { it.type == AccountType.INVESTMENT },
            latestTransactions = data.recentTransactions
                .sortedWith(
                    compareByDescending<Transaction> {
                        it.createdAt
                    }.thenByDescending { it.id }
                )
                .take(LATEST_COUNT),
            unreadNotifications = data.unreadNotifications,
            isOffline = data.isFromCache
        )
    }

    /** The Main card's account, or the first checking account if there is no Main card. */
    private fun walletAccount(data: HomeData): Account? {
        val mainCardAccountId = data.cards.minByOrNull { it.stackOrder }?.accountId
        return data.accounts.firstOrNull { it.id == mainCardAccountId }
            ?: data.accounts.firstOrNull { it.type == AccountType.CHECKING }
    }

    private fun spendable(account: Account): Money? = when (account) {
        is DepositAccount -> account.available
        is CreditCardAccount -> account.availableCredit
        else -> null
    }

    private fun List<Transaction>.total(direction: TransactionDirection): Money =
        filter { it.direction == direction }.fold(Money.ZERO) { sum, it -> sum + it.amount }

    private companion object {
        const val LAST_30_DAYS = 30L
        const val LATEST_COUNT = 5
        val ACCOUNT_ORDER = listOf(
            AccountType.CHECKING,
            AccountType.SAVINGS,
            AccountType.CREDIT,
            AccountType.LOAN,
            AccountType.TERM_DEPOSIT,
            AccountType.INVESTMENT
        )
    }
}
