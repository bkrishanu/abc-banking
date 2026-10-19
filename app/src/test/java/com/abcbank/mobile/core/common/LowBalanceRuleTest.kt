package com.abcbank.mobile.core.common

import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.InvestmentAccount
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Rate
import com.abcbank.mobile.core.model.TermDepositAccount
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LowBalanceRuleTest {
    private fun checking(available: Long) = DepositAccount(
        "checking",
        AccountType.CHECKING,
        "Everyday Checking",
        "2087",
        available = Money(available),
        ledger = Money(available),
        apy = null
    )

    private fun card(available: Long) = CreditCardAccount(
        "credit", "Cash Back Card", "6634",
        creditLimit = Money(300_000), currentBalance = Money(300_000 - available),
        availableCredit = Money(available), statementBalance = Money.ZERO,
        minimumDue = null, dueDate = Instant.EPOCH, autoPayFull = false
    )

    private fun loan(limit: Long?, available: Long) = LoanAccount(
        "loan", "Line of Credit", "6608",
        creditLimit = limit?.let(::Money), owed = Money.ZERO,
        availableToDraw = Money(available), apr = Rate(925), principal = null, openedAt = null
    )

    @Test
    fun `checking below $500 is low, $500 exactly is not`() {
        assertTrue(LowBalanceRule.isLow(checking(41_217)))
        assertTrue(LowBalanceRule.isLow(checking(49_999)))
        assertFalse(LowBalanceRule.isLow(checking(50_000)))
    }

    @Test
    fun `cards and lines of credit use available credit`() {
        assertTrue(LowBalanceRule.isLow(card(available = 45_000)))
        assertFalse(LowBalanceRule.isLow(card(available = 105_945)))
        assertTrue(LowBalanceRule.isLow(loan(limit = 1_500_000, available = 10_000)))
    }

    @Test
    fun `a loan paid out in one go, term deposits and investments never alert`() {
        assertFalse(LowBalanceRule.isLow(loan(limit = null, available = 0)))
        assertFalse(
            LowBalanceRule.isLow(
                TermDepositAccount(
                    "td",
                    "Term Deposit",
                    "9034",
                    Money(1),
                    Rate(435),
                    null,
                    Instant.EPOCH
                )
            )
        )
        assertFalse(LowBalanceRule.isLow(InvestmentAccount("inv", "Portfolio", "4410", Money(1))))
    }
}
