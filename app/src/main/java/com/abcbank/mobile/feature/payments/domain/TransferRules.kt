package com.abcbank.mobile.feature.payments.domain

import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.Money

enum class TransferSpeed { STANDARD, INSTANT }

enum class AmountError { ZERO, OVER_AVAILABLE, OVER_OWED, SAME_ACCOUNT }

enum class PayeeError {
    NAME_MISSING,
    NAME_TOO_LONG,
    ROUTING_FORMAT,
    ROUTING_CHECKSUM,
    BANK_NOT_FOUND,
    ACCOUNT_FORMAT,
    ACCOUNT_NOT_VERIFIED
}

/** Money movement rules (MobileBuild.md 3.1 and 3.2, PRD section 08, DEMO_DATA.md section 07). */
object TransferRules {
    private val FEE_MINIMUM = Money(25)
    private val FEE_MAXIMUM = Money.ofDollars(15)
    private const val FEE_BASIS_POINTS = 150L
    private const val BASIS_POINTS = 10_000L
    const val MAX_NAME_LENGTH = 40
    private val ACCOUNT_DIGITS = 6..17
    private const val ROUTING_DIGITS = 9
    private val ABA_WEIGHTS = listOf(3, 7, 1, 3, 7, 1, 3, 7, 1)

    /** Standard ACH is free; Instant is 1.5%, at least $0.25 and at most $15.00, rounded half up. */
    fun fee(amount: Money, speed: TransferSpeed): Money {
        if (speed == TransferSpeed.STANDARD || amount.cents <= 0) return Money.ZERO
        val raw = (amount.cents * FEE_BASIS_POINTS + BASIS_POINTS / 2) / BASIS_POINTS
        return Money(raw).coerceIn(FEE_MINIMUM, FEE_MAXIMUM)
    }

    /** What an account can send: a deposit account's available balance, a line of credit's undrawn credit. */
    fun spendable(account: Account): Money? = when (account) {
        is DepositAccount -> account.available
        is LoanAccount -> account.creditLimit?.let { account.availableToDraw }
        else -> null
    }

    /** Own accounts money can move between (MobileBuild.md 3.1): checking, savings, line of credit. */
    fun canMoveBetween(account: Account): Boolean = when (account) {
        is DepositAccount -> true
        is LoanAccount -> account.creditLimit != null
        else -> false
    }

    /** P2P transfers leave from checking or savings. */
    fun canSendFrom(account: Account): Boolean =
        account.type == AccountType.CHECKING || account.type == AccountType.SAVINGS

    /** The amount plus any fee must fit what the source can send. */
    fun validateSend(amount: Money, fee: Money, source: Account): AmountError? {
        if (!amount.isPositive) return AmountError.ZERO
        val spendable = spendable(source) ?: return AmountError.OVER_AVAILABLE
        return if (amount + fee > spendable) AmountError.OVER_AVAILABLE else null
    }

    fun validateMove(amount: Money, source: Account, destination: Account): AmountError? {
        if (source.id == destination.id) return AmountError.SAME_ACCOUNT
        validateSend(amount, Money.ZERO, source)?.let { return it }
        // Paying a line of credit cannot go below nothing owed.
        if (destination is LoanAccount && amount > destination.owed) return AmountError.OVER_OWED
        return null
    }

    fun passesAbaChecksum(routing: String): Boolean {
        if (routing.length != ROUTING_DIGITS || !routing.all { it in '0'..'9' }) return false
        return routing.map {
            it - '0'
        }.zip(ABA_WEIGHTS).sumOf { (digit, weight) -> digit * weight } %
            10 ==
            0
    }

    /**
     * Payee checks (MobileBuild.md 3.2): name, a routing number that passes the ABA checksum and
     * belongs to a known bank, and an account number of 6 to 17 digits. Numbers ending in 0000
     * fail verification on purpose, so the error state can be shown (DEMO_DATA.md section 07).
     */
    fun validatePayee(
        name: String,
        routing: String,
        accountNumber: String,
        bankName: String?
    ): PayeeError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> PayeeError.NAME_MISSING
            trimmed.length > MAX_NAME_LENGTH -> PayeeError.NAME_TOO_LONG
            routing.length != ROUTING_DIGITS ||
                !routing.all {
                    it in '0'..'9'
                } -> PayeeError.ROUTING_FORMAT
            !passesAbaChecksum(routing) -> PayeeError.ROUTING_CHECKSUM
            bankName == null -> PayeeError.BANK_NOT_FOUND
            accountNumber.length !in ACCOUNT_DIGITS ||
                !accountNumber.all {
                    it in '0'..'9'
                } -> PayeeError.ACCOUNT_FORMAT
            accountNumber.endsWith(FAILING_ACCOUNT_SUFFIX) -> PayeeError.ACCOUNT_NOT_VERIFIED
            else -> null
        }
    }

    /** "Mark Robinson" becomes "Mark R." for the Quick transfer row, as in the seed data. */
    fun shortName(name: String): String {
        val words = name.trim().split(' ').filter { it.isNotBlank() }
        return if (words.size <
            2
        ) {
            name.trim()
        } else {
            "${words.first()} ${words.last().first().uppercaseChar()}."
        }
    }

    private const val FAILING_ACCOUNT_SUFFIX = "0000"
}
