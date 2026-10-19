package com.abcbank.mobile.feature.home.ui

import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.InvestmentAccount
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.model.Rate
import com.abcbank.mobile.core.model.TermDepositAccount
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Persona-shaped Home states for previews and screenshot tests, with values from DEMO_DATA.md
 * sections 03 and 04. Fixed dates so screenshots never change.
 */
internal object HomePreviewData {
    private val TODAY: LocalDate = LocalDate.of(2026, 10, 19)
    private val EPOCH: Instant = Instant.parse("2026-10-19T12:00:00Z")

    fun samanta(hidden: Boolean = false, offline: Boolean = false) = HomeUiState.Content(
        personaId = PersonaId.SAMANTA,
        firstName = "Samanta",
        unreadNotifications = 4,
        walletBalance = Money(15_583_200),
        moneyIn = Money(1_430_936),
        moneyOut = Money(1_036_443),
        balancesHidden = hidden,
        accounts = listOf(
            HomeAccountItem(
                DepositAccount(
                    "checking",
                    AccountType.CHECKING,
                    "Everyday Checking",
                    "3144",
                    available = Money(15_583_200),
                    ledger = Money(15_620_700),
                    apy = null
                ),
                isLowBalance = false
            ),
            HomeAccountItem(
                DepositAccount(
                    "savings",
                    AccountType.SAVINGS,
                    "Travel Savings",
                    "7720",
                    available = Money(1_203_200),
                    ledger = Money(1_203_200),
                    apy = Rate(310)
                ),
                isLowBalance = false
            ),
            HomeAccountItem(
                CreditCardAccount(
                    "credit", "Signature Credit Card", "5821",
                    creditLimit = Money(5_000_000), currentBalance = Money(1_176_500),
                    availableCredit = Money(3_823_500), statementBalance = Money(1_176_500),
                    minimumDue = Money(28_500), dueDate = EPOCH, autoPayFull = false
                ),
                isLowBalance = false,
                dueDate = TODAY.plusDays(18)
            ),
            HomeAccountItem(
                LoanAccount(
                    "line-of-credit", "Personal Line of Credit", "6608",
                    creditLimit = Money(1_500_000), owed = Money(250_000),
                    availableToDraw = Money(
                        1_250_000
                    ),
                    apr = Rate(925), principal = null, openedAt = null
                ),
                isLowBalance = false
            ),
            HomeAccountItem(
                TermDepositAccount(
                    "term-deposit",
                    "12-Month Term Deposit",
                    "9034",
                    principal = Money(2_500_000),
                    apy = Rate(435),
                    openedAt = null,
                    maturesAt = EPOCH
                ),
                isLowBalance = false,
                maturesOn = TODAY.plusDays(120)
            ),
            HomeAccountItem(
                InvestmentAccount("investment", "Investment Portfolio", "4410", Money(8_461_240)),
                isLowBalance = false
            )
        ),
        showNoInvestments = false,
        latestTransactions = listOf(
            item(
                "tx-001",
                "Aster & Vine Boutique",
                TransactionCategory.SHOPPING,
                -12_500,
                TransactionDay.TODAY,
                15,
                14
            ),
            item(
                "tx-002",
                "Northwind Furniture",
                TransactionCategory.SHOPPING,
                -37_500,
                TransactionDay.TODAY,
                11,
                40,
                pending = true
            ),
            item(
                "tx-003",
                "Helen Torres",
                TransactionCategory.TRANSFER_IN,
                3_860,
                TransactionDay.TODAY,
                8,
                9
            ),
            item(
                "tx-004",
                "Olive & Ember",
                TransactionCategory.DINING,
                -7_200,
                TransactionDay.YESTERDAY,
                21,
                21
            ),
            item(
                "tx-007",
                "Lumen Studio Payroll",
                TransactionCategory.INCOME,
                711_538,
                TransactionDay.EARLIER,
                7,
                0
            )
        ),
        isOffline = offline
    )

    fun marcus() = HomeUiState.Content(
        personaId = PersonaId.MARCUS,
        firstName = "Marcus",
        unreadNotifications = 4,
        walletBalance = Money(41_217),
        moneyIn = Money(433_692),
        moneyOut = Money(509_501),
        balancesHidden = false,
        accounts = listOf(
            HomeAccountItem(
                DepositAccount(
                    "checking",
                    AccountType.CHECKING,
                    "Everyday Checking",
                    "2087",
                    available = Money(41_217),
                    ledger = Money(41_217),
                    apy = null
                ),
                isLowBalance = true
            ),
            HomeAccountItem(
                DepositAccount(
                    "savings",
                    AccountType.SAVINGS,
                    "Starter Savings",
                    "6120",
                    available = Money(125_000),
                    ledger = Money(125_000),
                    apy = Rate(310)
                ),
                isLowBalance = false
            ),
            HomeAccountItem(
                CreditCardAccount(
                    "credit", "Cash Back Card", "6634",
                    creditLimit = Money(300_000), currentBalance = Money(194_055),
                    availableCredit = Money(105_945), statementBalance = Money(288_056),
                    minimumDue = Money(4_000), dueDate = EPOCH, autoPayFull = false
                ),
                isLowBalance = false,
                dueDate = TODAY.plusDays(3)
            )
        ),
        showNoInvestments = true,
        latestTransactions = listOf(
            item(
                "tx-001",
                "Campus Deli",
                TransactionCategory.DINING,
                -1_480,
                TransactionDay.TODAY,
                12,
                31
            ),
            item(
                "tx-002",
                "QuickRide",
                TransactionCategory.TRANSPORT,
                -1_825,
                TransactionDay.YESTERDAY,
                19,
                44
            ),
            item(
                "tx-003",
                "Oakwood Properties",
                TransactionCategory.HOUSING,
                -145_000,
                TransactionDay.EARLIER,
                8,
                0
            )
        ),
        isOffline = false
    )

    private fun item(
        id: String,
        merchant: String,
        category: TransactionCategory,
        signedCents: Long,
        day: TransactionDay,
        hour: Int,
        minute: Int,
        pending: Boolean = false
    ): HomeTransactionItem {
        val date = when (day) {
            TransactionDay.TODAY -> TODAY
            TransactionDay.YESTERDAY -> TODAY.minusDays(1)
            TransactionDay.EARLIER -> TODAY.minusDays(3)
        }
        val transaction = Transaction(
            id = id,
            accountId = "checking",
            direction = if (signedCents <
                0
            ) {
                TransactionDirection.DEBIT
            } else {
                TransactionDirection.CREDIT
            },
            amount = Money(kotlin.math.abs(signedCents)),
            merchant = merchant,
            category = category,
            reference = id,
            runningBalance = Money.ZERO,
            status = if (pending) TransactionStatus.PENDING else TransactionStatus.POSTED,
            disputed = false,
            createdAt = EPOCH,
            clearsAt = null
        )
        return HomeTransactionItem(transaction, day, date, LocalTime.of(hour, minute))
    }
}
