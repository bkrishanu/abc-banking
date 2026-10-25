package com.abcbank.mobile.feature.accounts.domain

import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionDirection
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Ledger groups (PRD feature 6): Today, Yesterday, Last 30 Days, then Earlier for longer ranges. */
enum class LedgerGroup { TODAY, YESTERDAY, LAST_30_DAYS, EARLIER }

data class LedgerSection(val group: LedgerGroup, val transactions: List<Transaction>)

/** Pure ledger rules: what a filter keeps, and how the kept transactions are grouped. */
object LedgerRules {
    /** The first instant [range] covers: today and the days before it, in [zone]. Null for all. */
    fun since(range: DateRange, today: LocalDate, zone: ZoneId): Instant? =
        range.days?.let { today.minusDays(it - 1).atStartOfDay(zone).toInstant() }

    /** Applies every part of [filter] except the date range, which the query already did. */
    fun apply(transactions: List<Transaction>, filter: TransactionFilter): List<Transaction> {
        val query = filter.query.trim().lowercase(Locale.ROOT)
        return transactions.filter { transaction ->
            (filter.accountId == null || transaction.accountId == filter.accountId) &&
                when (filter.type) {
                    TypeFilter.ALL -> true
                    TypeFilter.SPENDINGS -> transaction.direction == TransactionDirection.DEBIT
                    TypeFilter.EARNINGS -> transaction.direction == TransactionDirection.CREDIT
                } &&
                (filter.categories.isEmpty() || transaction.category in filter.categories) &&
                (query.isEmpty() || query in transaction.merchant.lowercase(Locale.ROOT))
        }
    }

    /**
     * How many transactions each type tab would show under the rest of [filter] (search,
     * categories, account), for "All 9", "Spendings 7", "Earnings 2".
     */
    fun typeCounts(
        transactions: List<Transaction>,
        filter: TransactionFilter
    ): Map<TypeFilter, Int> =
        TypeFilter.entries.associateWith { apply(transactions, filter.copy(type = it)).size }

    /** Newest first, in sections; empty sections are left out. */
    fun group(
        transactions: List<Transaction>,
        today: LocalDate,
        zone: ZoneId
    ): List<LedgerSection> = transactions
        .sortedWith(
            compareByDescending<Transaction> {
                it.createdAt
            }.thenByDescending { it.id }
        )
        .groupBy { groupOf(it.createdAt.atZone(zone).toLocalDate(), today) }
        .toSortedMap()
        .map { (group, items) -> LedgerSection(group, items) }

    private fun groupOf(date: LocalDate, today: LocalDate): LedgerGroup = when {
        date >= today -> LedgerGroup.TODAY
        date == today.minusDays(1) -> LedgerGroup.YESTERDAY
        date >= today.minusDays(LAST_30_DAYS - 1) -> LedgerGroup.LAST_30_DAYS
        else -> LedgerGroup.EARLIER
    }

    private const val LAST_30_DAYS = 30L
}
