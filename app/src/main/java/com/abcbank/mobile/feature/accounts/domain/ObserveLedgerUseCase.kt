package com.abcbank.mobile.feature.accounts.domain

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

data class Ledger(
    /** The filter these sections were computed with, so state and screen never disagree. */
    val filter: TransactionFilter,
    val sections: List<LedgerSection>,
    /** The account the ledger was opened for, when it was opened from one. */
    val account: Account?,
    /** True when the period has transactions but the filters hide them all. */
    val hiddenByFilters: Boolean,
    val accounts: Map<String, Account>,
    val today: LocalDate,
    val isOffline: Boolean,
    /** What each type tab would show under the other filters. */
    val typeCounts: Map<TypeFilter, Int> = emptyMap()
)

/**
 * The transaction ledger (MobileBuild.md 2.2). Firestore is read only by date, so changing the
 * type, category or search filters never starts a new read; only a new date range does.
 */
class ObserveLedgerUseCase @Inject constructor(
    private val repository: TransactionsRepository,
    private val clock: Clock,
    private val zone: Provider<ZoneId>
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(filters: Flow<TransactionFilter>): Flow<DataResult<Ledger>> {
        val zone = zone.get()
        val today = clock.now().atZone(zone).toLocalDate()
        val data = filters.map { it.range }
            .distinctUntilChanged()
            .flatMapLatest { range ->
                repository.observeTransactions(LedgerRules.since(range, today, zone))
            }
        return combine(data, filters) { result, filter ->
            when (result) {
                is DataResult.Failure -> result
                is DataResult.Success -> {
                    val accounts = result.value.accounts.associateBy { it.id }
                    val inScope = result.value.transactions.filter {
                        filter.accountId == null || it.accountId == filter.accountId
                    }
                    val kept = LedgerRules.apply(inScope, filter)
                    DataResult.Success(
                        Ledger(
                            filter = filter,
                            sections = LedgerRules.group(kept, today, zone),
                            account = filter.accountId?.let(accounts::get),
                            hiddenByFilters = kept.isEmpty() && inScope.isNotEmpty(),
                            accounts = accounts,
                            today = today,
                            isOffline = result.value.isFromCache,
                            typeCounts = LedgerRules.typeCounts(inScope, filter)
                        )
                    )
                }
            }
        }
    }
}
