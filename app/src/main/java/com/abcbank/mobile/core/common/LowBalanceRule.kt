package com.abcbank.mobile.core.common

import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.InvestmentAccount
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TermDepositAccount

/**
 * The low balance rule (PRD section 08, DEMO_DATA.md section 01): below $500.00 of available
 * balance for checking and savings, or of available credit for cards and lines of credit. Term
 * deposits and investments never count as low.
 */
object LowBalanceRule {
    val THRESHOLD: Money = Money.ofDollars(500)

    fun isLow(account: Account): Boolean = when (account) {
        is DepositAccount -> account.available < THRESHOLD
        is CreditCardAccount -> account.availableCredit < THRESHOLD
        // A loan paid out in one go has nothing to draw, which is not a low balance.
        is LoanAccount -> account.creditLimit != null && account.availableToDraw < THRESHOLD
        is TermDepositAccount, is InvestmentAccount -> false
    }
}
