package com.abcbank.mobile.feature.insights.ui

import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.feature.insights.domain.InsightsRules
import com.abcbank.mobile.feature.insights.domain.SavingsGoal
import com.abcbank.mobile.feature.insights.domain.SpendingPeriod
import com.abcbank.mobile.feature.insights.domain.SpendingSlice
import com.abcbank.mobile.feature.insights.domain.SpendingSummary
import com.abcbank.mobile.feature.insights.domain.TopCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Samanta's documented Insights (DEMO_DATA.md): $12,480.00 of $18,900.00, Summer in Lisbon. */
internal object InsightsPreviewData {
    private val TODAY = LocalDate.of(2026, 10, 20)

    val spending = SpendingSummary(
        period = SpendingPeriod.LAST_30_DAYS,
        start = TODAY.minusDays(29),
        end = TODAY,
        total = Money(1_248_000),
        budget = Money(1_890_000),
        percentOfBudget = 66,
        slices = listOf(
            SpendingSlice(TransactionCategory.SHOPPING, Money(462_000), 37),
            SpendingSlice(TransactionCategory.ENTERTAINMENT, Money(262_000), 21),
            SpendingSlice(null, Money(524_000), 42)
        ),
        // The four biggest of DEMO_DATA.md's last-30-day targets, with seeded merchants.
        topCategories = listOf(
            TopCategory(
                TransactionCategory.SHOPPING,
                Money(462_000),
                37,
                listOf("Northwind Furniture", "Aster & Vine Boutique")
            ),
            TopCategory(
                TransactionCategory.ENTERTAINMENT,
                Money(262_000),
                21,
                listOf("Melody Live Tickets", "Cineplex Grand")
            ),
            TopCategory(
                TransactionCategory.DINING,
                Money(185_931),
                15,
                listOf("Olive & Ember", "Bluebird Coffee")
            ),
            TopCategory(TransactionCategory.TRAVEL, Money(166_000), 13, listOf("SkyWays Airlines"))
        )
    )

    val goal = SavingsGoal(
        id = "summer-in-lisbon",
        name = "Summer in Lisbon",
        target = Money(400_000),
        saved = Money(365_000),
        accountId = "savings",
        startedAt = Instant.parse("2026-07-22T13:00:00Z"),
        weeklyHistory = listOf(
            120_000L, 135_000, 190_000, 150_000, 178_000, 165_000, 162_000,
            170_000, 248_000, 220_000, 240_000, 356_000, 365_000
        ).map(::Money)
    )

    fun samanta(hidden: Boolean = false, offline: Boolean = false, selectedPoint: Int = 11) =
        InsightsUiState.Content(
            period = SpendingPeriod.LAST_30_DAYS,
            spending = spending,
            savings = InsightsRules.savings(goal, ZoneOffset.UTC),
            selectedPoint = selectedPoint,
            balancesHidden = hidden,
            isOffline = offline
        )
}
