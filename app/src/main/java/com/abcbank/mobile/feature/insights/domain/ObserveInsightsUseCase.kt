package com.abcbank.mobile.feature.insights.domain

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataResult
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Everything the Insights tab shows, worked out for every period at once. */
data class InsightsOverview(
    val spending: Map<SpendingPeriod, SpendingSummary>,
    /** The goal started first, or null when the customer has none. */
    val savings: SavingsSummary?,
    val isFromCache: Boolean
)

class ObserveInsightsUseCase @Inject constructor(
    private val repository: InsightsRepository,
    private val clock: Clock,
    private val zone: Provider<ZoneId>
) {
    operator fun invoke(): Flow<DataResult<InsightsOverview>> {
        val zoneId = zone.get()
        val today = clock.now().atZone(zoneId).toLocalDate()
        val since = InsightsRules.earliestDay(today).atStartOfDay(zoneId).toInstant()
        return repository.observeInsights(since).map { result ->
            when (result) {
                is DataResult.Failure -> result
                is DataResult.Success -> {
                    val data = result.value
                    DataResult.Success(
                        InsightsOverview(
                            spending = SpendingPeriod.entries.associateWith {
                                InsightsRules.spending(
                                    data.transactions,
                                    data.monthlyBudget,
                                    it,
                                    today,
                                    zoneId
                                )
                            },
                            savings = data.goals.minByOrNull {
                                it.startedAt
                            }?.let { InsightsRules.savings(it, zoneId) },
                            isFromCache = data.isFromCache
                        )
                    )
                }
            }
        }
    }
}
