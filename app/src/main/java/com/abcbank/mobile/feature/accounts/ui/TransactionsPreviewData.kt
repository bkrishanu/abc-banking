package com.abcbank.mobile.feature.accounts.ui

import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import com.abcbank.mobile.feature.accounts.domain.LedgerGroup
import com.abcbank.mobile.feature.accounts.domain.TransactionFilter
import com.abcbank.mobile.feature.accounts.domain.TypeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Samanta-shaped ledger states (DEMO_DATA.md section 03) for previews and screenshot tests. */
internal object TransactionsPreviewData {
    private val TODAY: LocalDate = LocalDate.of(2026, 10, 19)
    private val AT: Instant = Instant.parse("2026-10-19T12:00:00Z")

    val checking = DepositAccount(
        "checking",
        AccountType.CHECKING,
        "Everyday Checking",
        "3144",
        available = Money(15_583_200),
        ledger = Money(15_620_700),
        apy = null
    )

    fun transaction(
        id: String,
        merchant: String,
        category: TransactionCategory,
        signedCents: Long,
        pending: Boolean = false,
        disputed: Boolean = false
    ) = Transaction(
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
        reference = "cdb7b320-4aa2-49ce-bb03-e3c5815c361d",
        runningBalance = Money(15_620_700),
        status = if (pending) TransactionStatus.PENDING else TransactionStatus.POSTED,
        disputed = disputed,
        createdAt = AT,
        clearsAt = null
    )

    private fun row(transaction: Transaction, daysAgo: Long, hour: Int, minute: Int) =
        LedgerRow(transaction, TODAY.minusDays(daysAgo), LocalTime.of(hour, minute))

    fun content(filter: TransactionFilter = TransactionFilter()) = TransactionsUiState.Content(
        header = LedgerHeader(
            account = null,
            filter = filter,
            isOffline = false,
            // The eight rows below: six charges and two credits.
            typeCounts = mapOf(
                TypeFilter.ALL to 8,
                TypeFilter.SPENDINGS to 6,
                TypeFilter.EARNINGS to 2
            )
        ),
        sections = listOf(
            LedgerSectionItem(
                LedgerGroup.TODAY,
                listOf(
                    row(
                        transaction(
                            "tx-001",
                            "Aster & Vine Boutique",
                            TransactionCategory.SHOPPING,
                            -12_500
                        ),
                        0,
                        15,
                        14
                    ),
                    row(
                        transaction(
                            "tx-002",
                            "Northwind Furniture",
                            TransactionCategory.SHOPPING,
                            -37_500,
                            pending = true
                        ),
                        0,
                        11,
                        40
                    ),
                    row(
                        transaction(
                            "tx-003",
                            "Helen Torres",
                            TransactionCategory.TRANSFER_IN,
                            3_860
                        ),
                        0,
                        8,
                        9
                    )
                )
            ),
            LedgerSectionItem(
                LedgerGroup.YESTERDAY,
                listOf(
                    row(
                        transaction("tx-004", "Olive & Ember", TransactionCategory.DINING, -7_200),
                        1,
                        21,
                        21
                    ),
                    row(
                        transaction("tx-005", "Bluebird Coffee", TransactionCategory.DINING, -645),
                        1,
                        9,
                        5
                    )
                )
            ),
            LedgerSectionItem(
                LedgerGroup.LAST_30_DAYS,
                listOf(
                    row(
                        transaction(
                            "tx-007",
                            "Lumen Studio Payroll",
                            TransactionCategory.INCOME,
                            711_538
                        ),
                        3,
                        7,
                        0
                    ),
                    row(
                        transaction(
                            "tx-012",
                            "Brightline Electric",
                            TransactionCategory.UTILITIES,
                            -18_640
                        ),
                        5,
                        10,
                        15
                    ),
                    row(
                        transaction(
                            "tx-016",
                            "Melody Live Tickets",
                            TransactionCategory.ENTERTAINMENT,
                            -42_000,
                            disputed = true
                        ),
                        11,
                        22,
                        10
                    )
                )
            )
        ),
        hiddenByFilters = false,
        balancesHidden = false
    )

    fun noMatch() = TransactionsUiState.Content(
        header = LedgerHeader(
            account = checking,
            filter = TransactionFilter(
                accountId = "checking",
                type = TypeFilter.EARNINGS,
                categories = setOf(TransactionCategory.TRAVEL),
                query = "sky"
            ),
            isOffline = false,
            typeCounts = TypeFilter.entries.associateWith { 0 }
        ),
        sections = emptyList(),
        hiddenByFilters = true,
        balancesHidden = false
    )
}
