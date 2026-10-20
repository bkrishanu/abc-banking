package com.abcbank.mobile.feature.documents.ui

import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.feature.documents.domain.DocumentsOverview
import com.abcbank.mobile.feature.documents.domain.Statement
import com.abcbank.mobile.feature.documents.domain.TaxForm
import java.time.Instant
import java.time.YearMonth

/** Samanta's documents (DEMO_DATA.md section 03): checking and card statements, 1099-INT $412.86. */
internal object DocumentsPreviewData {
    private val checking =
        DepositAccount(
            "checking",
            AccountType.CHECKING,
            "Everyday Checking",
            "3144",
            Money(15_583_200),
            Money(15_620_700),
            null
        )
    private val credit = CreditCardAccount(
        id = "credit",
        nickname = "Signature Credit Card",
        last4 = "5821",
        creditLimit = Money.ofDollars(50_000),
        currentBalance = Money(1_176_500),
        availableCredit = Money(3_823_500),
        statementBalance = Money(1_176_500),
        minimumDue = Money(28_500),
        dueDate = Instant.parse("2026-11-07T00:00:00Z"),
        autoPayFull = false
    )

    private fun statement(account: Account, month: YearMonth, closing: Long, owed: Boolean) =
        Statement(
            account = account,
            month = month,
            customerName = "Samanta Johns",
            customerId = "SAMJOH01",
            owedStyle = owed,
            opening = Money(closing - 120_000),
            closing = Money(closing),
            moneyIn = Money(711_538),
            moneyOut = Money(591_538),
            lines = emptyList()
        )

    fun overview(withTaxForm: Boolean = true): DocumentsOverview {
        val months = listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 8), YearMonth.of(2026, 7))
        return DocumentsOverview(
            statements = months.flatMap {
                listOf(
                    statement(checking, it, 15_101_900, false),
                    statement(credit, it, 1_042_300, true)
                )
            },
            taxForm = if (withTaxForm) {
                TaxForm(
                    2025,
                    Money(41_286),
                    "Samanta Johns",
                    "SAMJOH01"
                )
            } else {
                null
            },
            taxYear = 2025,
            isFromCache = false
        )
    }

    fun samanta(withTaxForm: Boolean = true): DocumentsUiState.Content {
        val overview = overview(withTaxForm)
        return DocumentsUiState.Content(
            statementsByMonth = overview.statements.groupBy { it.month }.toList(),
            overview = overview,
            viewer = ViewerState.Closed,
            isOffline = false
        )
    }
}
