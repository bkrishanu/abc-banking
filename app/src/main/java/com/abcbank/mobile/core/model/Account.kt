package com.abcbank.mobile.core.model

import java.time.Instant

enum class AccountType {
    CHECKING,
    SAVINGS,
    CREDIT,
    TERM_DEPOSIT,
    INVESTMENT,
    LOAN
}

/** One of the customer's accounts (MobileBuild.md 2.1). Each type carries only its own fields. */
sealed interface Account {
    val id: String
    val type: AccountType
    val nickname: String
    val last4: String
}

/** Checking or savings. [ledger] includes pending items; [available] is what can be spent. */
data class DepositAccount(
    override val id: String,
    override val type: AccountType,
    override val nickname: String,
    override val last4: String,
    val available: Money,
    val ledger: Money,
    val apy: Rate?
) : Account {
    init {
        require(type == AccountType.CHECKING || type == AccountType.SAVINGS) {
            "A deposit account is checking or savings"
        }
    }
}

/** [minimumDue] is null when the full balance is paid automatically. */
data class CreditCardAccount(
    override val id: String,
    override val nickname: String,
    override val last4: String,
    val creditLimit: Money,
    val currentBalance: Money,
    val availableCredit: Money,
    val statementBalance: Money,
    val minimumDue: Money?,
    val dueDate: Instant,
    val autoPayFull: Boolean
) : Account {
    override val type: AccountType get() = AccountType.CREDIT
}

/**
 * A line of credit or a personal loan. [creditLimit] is null for a loan paid out in one go, and
 * then [availableToDraw] is zero.
 */
data class LoanAccount(
    override val id: String,
    override val nickname: String,
    override val last4: String,
    val creditLimit: Money?,
    val owed: Money,
    val availableToDraw: Money,
    val apr: Rate,
    val principal: Money?,
    val openedAt: Instant?
) : Account {
    override val type: AccountType get() = AccountType.LOAN
}

data class TermDepositAccount(
    override val id: String,
    override val nickname: String,
    override val last4: String,
    val principal: Money,
    val apy: Rate,
    val openedAt: Instant?,
    val maturesAt: Instant
) : Account {
    override val type: AccountType get() = AccountType.TERM_DEPOSIT
}

/** Holdings are a subcollection, loaded separately when the portfolio is opened. */
data class InvestmentAccount(
    override val id: String,
    override val nickname: String,
    override val last4: String,
    val portfolioValue: Money
) : Account {
    override val type: AccountType get() = AccountType.INVESTMENT
}

data class Holding(val id: String, val name: String, val value: Money)
