package com.abcbank.mobile.core.data.firebase

import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Holding
import com.abcbank.mobile.core.model.InvestmentAccount
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.TermDepositAccount
import com.google.firebase.Timestamp

/**
 * A `users/{uid}/accounts/{accountId}` document. One shape for every type; which fields are set
 * depends on [type] (ARCHITECTURE.md section 04). Money is cents, [rate] is basis points.
 * For CREDIT and LOAN, [ledgerBalance] is the amount owed and [availableBalance] what can still be
 * spent or drawn.
 */
data class AccountDto(
    val type: String? = null,
    val nickname: String? = null,
    val last4: String? = null,
    val availableBalance: Long? = null,
    val ledgerBalance: Long? = null,
    val rate: Long? = null,
    val creditLimit: Long? = null,
    val statementBalance: Long? = null,
    val minimumDue: Long? = null,
    val dueDate: Timestamp? = null,
    val autoPayFull: Boolean? = null,
    val principal: Long? = null,
    val openedAt: Timestamp? = null,
    val maturesAt: Timestamp? = null,
    val portfolioValue: Long? = null
) {
    fun toMap(): Map<String, Any> = firestoreFields(
        TYPE to type,
        NICKNAME to nickname,
        LAST4 to last4,
        AVAILABLE_BALANCE to availableBalance,
        LEDGER_BALANCE to ledgerBalance,
        RATE to rate,
        CREDIT_LIMIT to creditLimit,
        STATEMENT_BALANCE to statementBalance,
        MINIMUM_DUE to minimumDue,
        DUE_DATE to dueDate,
        AUTO_PAY_FULL to autoPayFull,
        PRINCIPAL to principal,
        OPENED_AT to openedAt,
        MATURES_AT to maturesAt,
        PORTFOLIO_VALUE to portfolioValue
    )

    /** Null when a field this type needs is missing or invalid. */
    fun toAccount(id: String): Account? {
        val accountType = enumOrNull<AccountType>(type) ?: return null
        val nickname = nickname?.takeIf(String::isNotBlank) ?: return null
        val last4 = last4?.takeIf(String::isLast4) ?: return null
        return when (accountType) {
            AccountType.CHECKING, AccountType.SAVINGS -> DepositAccount(
                id = id,
                type = accountType,
                nickname = nickname,
                last4 = last4,
                available = money(availableBalance) ?: return null,
                ledger = money(ledgerBalance) ?: return null,
                apy = rate?.let { rateOf(it) ?: return null }
            )
            AccountType.CREDIT -> CreditCardAccount(
                id = id,
                nickname = nickname,
                last4 = last4,
                creditLimit = money(creditLimit) ?: return null,
                currentBalance = money(ledgerBalance) ?: return null,
                availableCredit = money(availableBalance) ?: return null,
                statementBalance = money(statementBalance) ?: return null,
                minimumDue = minimumDue?.let { money(it) ?: return null },
                dueDate = dueDate?.toInstantUtc() ?: return null,
                autoPayFull = autoPayFull ?: false
            )
            AccountType.LOAN -> LoanAccount(
                id = id,
                nickname = nickname,
                last4 = last4,
                creditLimit = creditLimit?.let { money(it) ?: return null },
                owed = money(ledgerBalance) ?: return null,
                availableToDraw = money(availableBalance ?: 0L) ?: return null,
                apr = rateOf(rate) ?: return null,
                principal = principal?.let { money(it) ?: return null },
                openedAt = openedAt?.toInstantUtc()
            )
            AccountType.TERM_DEPOSIT -> TermDepositAccount(
                id = id,
                nickname = nickname,
                last4 = last4,
                principal = money(principal) ?: return null,
                apy = rateOf(rate) ?: return null,
                openedAt = openedAt?.toInstantUtc(),
                maturesAt = maturesAt?.toInstantUtc() ?: return null
            )
            AccountType.INVESTMENT -> InvestmentAccount(
                id = id,
                nickname = nickname,
                last4 = last4,
                portfolioValue = money(portfolioValue) ?: return null
            )
        }
    }

    companion object {
        const val TYPE = "type"
        const val NICKNAME = "nickname"
        const val LAST4 = "last4"
        const val AVAILABLE_BALANCE = "availableBalance"
        const val LEDGER_BALANCE = "ledgerBalance"
        const val RATE = "rate"
        const val CREDIT_LIMIT = "creditLimit"
        const val STATEMENT_BALANCE = "statementBalance"
        const val MINIMUM_DUE = "minimumDue"
        const val DUE_DATE = "dueDate"
        const val AUTO_PAY_FULL = "autoPayFull"
        const val PRINCIPAL = "principal"
        const val OPENED_AT = "openedAt"
        const val MATURES_AT = "maturesAt"
        const val PORTFOLIO_VALUE = "portfolioValue"

        fun fromMap(map: Map<String, Any?>): AccountDto = AccountDto(
            type = map.stringField(TYPE),
            nickname = map.stringField(NICKNAME),
            last4 = map.stringField(LAST4),
            availableBalance = map.longField(AVAILABLE_BALANCE),
            ledgerBalance = map.longField(LEDGER_BALANCE),
            rate = map.longField(RATE),
            creditLimit = map.longField(CREDIT_LIMIT),
            statementBalance = map.longField(STATEMENT_BALANCE),
            minimumDue = map.longField(MINIMUM_DUE),
            dueDate = map.timestampField(DUE_DATE),
            autoPayFull = map.booleanField(AUTO_PAY_FULL),
            principal = map.longField(PRINCIPAL),
            openedAt = map.timestampField(OPENED_AT),
            maturesAt = map.timestampField(MATURES_AT),
            portfolioValue = map.longField(PORTFOLIO_VALUE)
        )
    }
}

/** A `users/{uid}/accounts/{accountId}/holdings/{holdingId}` document. */
data class HoldingDto(val name: String? = null, val value: Long? = null) {
    fun toMap(): Map<String, Any> = firestoreFields(NAME to name, VALUE to value)

    fun toHolding(id: String): Holding? = Holding(
        id = id,
        name = name?.takeIf(String::isNotBlank) ?: return null,
        value = money(value) ?: return null
    )

    companion object {
        const val NAME = "name"
        const val VALUE = "value"

        fun fromMap(map: Map<String, Any?>): HoldingDto =
            HoldingDto(name = map.stringField(NAME), value = map.longField(VALUE))
    }
}
