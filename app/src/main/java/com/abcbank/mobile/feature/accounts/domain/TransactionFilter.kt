package com.abcbank.mobile.feature.accounts.domain

import com.abcbank.mobile.core.model.TransactionCategory

/** How far back the ledger reads (approved 2026-10-20). */
enum class DateRange(val days: Long?) {
    LAST_30_DAYS(30),
    LAST_90_DAYS(90),
    ALL(null)
}

/** The Spendings / Earnings chips: debits and credits (MobileBuild.md 2.2, "Credits, Debits"). */
enum class TypeFilter { ALL, SPENDINGS, EARNINGS }

/**
 * Everything the ledger can be narrowed by (MobileBuild.md 2.2). All parts combine. [accountId]
 * is set when the ledger was opened from one account; null means every account.
 */
data class TransactionFilter(
    val accountId: String? = null,
    val range: DateRange = DateRange.LAST_30_DAYS,
    val type: TypeFilter = TypeFilter.ALL,
    val categories: Set<TransactionCategory> = emptySet(),
    val query: String = ""
) {
    /** Filters the user chose on top of the defaults (search excluded), for the "Filter (2)" count. */
    val activeCount: Int
        get() = listOf(range != DateRange.LAST_30_DAYS, type != TypeFilter.ALL).count { it } +
            categories.size

    /** Back to the defaults, keeping the account the ledger was opened for. */
    fun cleared(): TransactionFilter = TransactionFilter(accountId = accountId)
}
