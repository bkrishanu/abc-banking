package com.abcbank.mobile.feature.insights.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AdaptiveRow
import com.abcbank.mobile.core.designsystem.component.ChartLabel
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.DonutChart
import com.abcbank.mobile.core.designsystem.component.DonutSegment
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.FilterChipRow
import com.abcbank.mobile.core.designsystem.component.LegendChip
import com.abcbank.mobile.core.designsystem.component.LineChart
import com.abcbank.mobile.core.designsystem.component.LocalBottomBarClearance
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.SkeletonBlock
import com.abcbank.mobile.core.designsystem.component.categoryLabel
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.TABULAR_FIGURES
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.feature.insights.domain.GoalProgress
import com.abcbank.mobile.feature.insights.domain.InsightsRules
import com.abcbank.mobile.feature.insights.domain.SavingsSummary
import com.abcbank.mobile.feature.insights.domain.SpendingPeriod
import com.abcbank.mobile.feature.insights.domain.SpendingSummary
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsRoute(viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreen(
        state = state,
        onPeriodSelect = viewModel::onPeriodSelect,
        onPointSelect = viewModel::onPointSelect,
        onRetry = viewModel::onRetry
    )
}

/**
 * The Insights tab (PRD feature 11, Sample 1 right and Sample 2 right top): spending by category
 * in a donut against the monthly budget, and the savings goal over time. All computed from the
 * customer's own data.
 */
@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onPeriodSelect: (SpendingPeriod) -> Unit,
    onPointSelect: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = AbcTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AbcTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.space4)
    ) {
        Spacer(Modifier.height(spacing.space4))
        Text(
            text = stringResource(R.string.insights_title),
            style = AbcTheme.typography.h1,
            color = AbcTheme.colors.text,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(spacing.space6))
        when (state) {
            InsightsUiState.Loading -> Column(
                verticalArrangement = Arrangement.spacedBy(spacing.space4)
            ) {
                repeat(2) {
                    SkeletonBlock(
                        Modifier
                            .fillMaxWidth()
                            .height(AbcTheme.dimens.avatarLarge * 4),
                        shape = AbcTheme.shapes.xl
                    )
                }
            }
            InsightsUiState.Empty -> ContentCard {
                Text(
                    stringResource(R.string.insights_empty),
                    style = AbcTheme.typography.body,
                    color = AbcTheme.colors.text
                )
            }
            is InsightsUiState.Error -> ErrorState(
                message = stringResource(
                    if (state.error ==
                        DataError.OFFLINE
                    ) {
                        R.string.state_offline_no_data
                    } else {
                        R.string.insights_error
                    }
                ),
                onRetry = onRetry
            )
            is InsightsUiState.Content -> {
                if (state.isOffline) {
                    OfflineBanner()
                    Spacer(Modifier.height(spacing.space4))
                }
                SpendingCard(state.spending, state.balancesHidden, onPeriodSelect)
                Spacer(Modifier.height(spacing.space4))
                state.savings?.let {
                    SavingsCard(it, state.selectedPoint, state.balancesHidden, onPointSelect)
                } ?: ContentCard {
                    Text(
                        stringResource(R.string.insights_no_goal),
                        style = AbcTheme.typography.body,
                        color = AbcTheme.colors.text
                    )
                }
            }
        }
        Spacer(Modifier.height(spacing.space6 + LocalBottomBarClearance.current))
    }
}

private val PERIODS =
    listOf(SpendingPeriod.LAST_30_DAYS, SpendingPeriod.THIS_MONTH, SpendingPeriod.LAST_MONTH)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpendingCard(
    summary: SpendingSummary,
    hidden: Boolean,
    onPeriodSelect: (SpendingPeriod) -> Unit
) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    val sliceColors = listOf(colors.accent, colors.accentDeep, colors.positive)
    ContentCard {
        // At large font sizes the dates move under the title instead of breaking it.
        AdaptiveRow(
            start = {
                Text(
                    text = stringResource(R.string.insights_spending),
                    style = AbcTheme.typography.h3,
                    color = colors.text,
                    modifier = Modifier.semantics { heading() }
                )
            }
        ) {
            Text(
                text = stringResource(
                    R.string.insights_date_range,
                    DateFormatter.shortDate(summary.start),
                    DateFormatter.shortDate(summary.end)
                ),
                style = AbcTheme.typography.small,
                color = colors.textMuted
            )
        }
        Spacer(Modifier.height(spacing.space3))
        FilterChipRow(
            options = PERIODS.map { periodLabel(it) },
            selectedIndex = PERIODS.indexOf(summary.period),
            onSelect = { onPeriodSelect(PERIODS[it]) }
        )
        Spacer(Modifier.height(spacing.space4))
        if (!summary.total.isPositive) {
            Text(
                text = stringResource(R.string.insights_no_spending),
                style = AbcTheme.typography.body,
                color = colors.textMuted
            )
            return@ContentCard
        }
        val labels = summary.slices.map { slice ->
            slice.category?.let { categoryLabel(it) }
                ?: stringResource(R.string.insights_other)
        }
        // The ring is the budget; spending past it fills the ring.
        val ring = maxOf(summary.budget.cents, summary.total.cents).toFloat()
        val total = money(summary.total, hidden)
        val budget = money(summary.budget, hidden)
        val description = stringResource(
            R.string.cd_spending_donut,
            total,
            budget,
            summary.percentOfBudget,
            summary.slices.zip(labels).joinToString { (slice, label) ->
                "$label ${slice.percentOfSpending}%"
            }
        )
        DonutChart(
            segments = summary.slices.mapIndexed { index, slice ->
                DonutSegment(
                    fraction = slice.amount.cents / ring,
                    color = sliceColors[index % sliceColors.size],
                    label = stringResource(R.string.insights_percent, slice.percentOfSpending)
                )
            },
            description = description,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total,
                    style = AbcTheme.typography.h2.copy(fontFeatureSettings = TABULAR_FIGURES),
                    color = colors.text,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.insights_of_budget, budget),
                    style = AbcTheme.typography.small,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(spacing.space2))
                Text(
                    text = stringResource(R.string.insights_percent, summary.percentOfBudget),
                    style = AbcTheme.typography.label,
                    color = colors.text,
                    modifier = Modifier
                        .clip(AbcTheme.shapes.full)
                        .background(colors.surfaceMuted)
                        .padding(horizontal = spacing.space3, vertical = spacing.space1)
                )
            }
        }
        Spacer(Modifier.height(spacing.space4))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            labels.forEachIndexed { index, label ->
                LegendChip(
                    label,
                    sliceColors[index % sliceColors.size],
                    background = colors.background
                )
            }
        }
    }
}

@Composable
private fun SavingsCard(
    summary: SavingsSummary,
    selected: Int,
    hidden: Boolean,
    onPointSelect: (Int) -> Unit
) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    val goal = summary.goal
    ContentCard {
        Text(
            text = money(goal.saved, hidden),
            style = AbcTheme.typography.h1.copy(fontFeatureSettings = TABULAR_FIGURES),
            color = colors.text
        )
        Text(
            text = stringResource(R.string.insights_saved_so_far),
            style = AbcTheme.typography.small,
            color = colors.textMuted
        )
        Spacer(Modifier.height(spacing.space2))
        val point = summary.points.getOrNull(selected)
        val labels = monthLabels(summary)
        LineChart(
            values = summary.points.map { it.saved.cents },
            average = summary.average.cents,
            selectedIndex = selected,
            tooltip = point?.let { money(it.saved, hidden) }.orEmpty(),
            labels = labels,
            description = stringResource(
                R.string.cd_savings_chart,
                money(summary.points.firstOrNull()?.saved ?: Money.ZERO, hidden),
                money(goal.saved, hidden)
            ),
            onSelect = onPointSelect
        )
        Spacer(Modifier.height(spacing.space4))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(AbcTheme.dimens.iconCircle)
                    .clip(AbcTheme.shapes.full)
                    .background(colors.surfaceMuted)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_smile),
                    contentDescription = null,
                    tint = colors.positiveText,
                    modifier = Modifier.size(AbcTheme.dimens.iconMedium)
                )
            }
            Spacer(Modifier.width(spacing.space3))
            Column(Modifier.weight(1f)) {
                Text(goal.name, style = AbcTheme.typography.body, color = colors.text)
                Text(
                    text = stringResource(
                        R.string.insights_goal_line,
                        money(goal.saved, hidden),
                        money(goal.target, hidden),
                        progressText(summary.progress)
                    ),
                    style = AbcTheme.typography.small,
                    color = colors.textMuted
                )
            }
            Spacer(Modifier.width(spacing.space2))
            Text(
                text = stringResource(
                    R.string.insights_percent,
                    InsightsRules.percent(goal.saved, goal.target)
                ),
                style = AbcTheme.typography.label,
                color = colors.text
            )
        }
    }
}

/**
 * One label at the first point of each month, as in the sample's July / August / September. A
 * partial first month whose label would touch the next is left out.
 */
private fun monthLabels(summary: SavingsSummary): List<ChartLabel> {
    var lastMonth = -1
    val labels = summary.points.mapIndexedNotNull { index, point ->
        val month = point.date.monthValue
        if (month == lastMonth) return@mapIndexedNotNull null
        lastMonth = month
        ChartLabel(index, point.date.month.getDisplayName(TextStyle.SHORT, Locale.US))
    }
    val minimumGap = summary.points.size / LABEL_SPACING
    val crowded = labels.size > 1 && labels[1].index - labels[0].index < minimumGap
    return if (crowded) labels.drop(1) else labels
}

private const val LABEL_SPACING = 4

private fun money(amount: Money, hidden: Boolean): String =
    if (hidden) AMOUNT_MASK else MoneyFormatter.formatWhole(amount)

@Composable
private fun periodLabel(period: SpendingPeriod): String = stringResource(
    when (period) {
        SpendingPeriod.LAST_30_DAYS -> R.string.insights_last_30_days
        SpendingPeriod.THIS_MONTH -> R.string.insights_this_month
        SpendingPeriod.LAST_MONTH -> R.string.insights_last_month
    }
)

@Composable
private fun progressText(progress: GoalProgress): String = stringResource(
    when (progress) {
        GoalProgress.REACHED -> R.string.insights_goal_reached
        GoalProgress.ALMOST -> R.string.insights_goal_almost
        GoalProgress.HALFWAY -> R.string.insights_goal_halfway
        GoalProgress.STARTED -> R.string.insights_goal_started
    }
)

private const val AMOUNT_MASK = "••••"

@Preview(widthDp = 360, heightDp = 1400)
@Composable
private fun InsightsPreview() {
    AbcTheme {
        InsightsScreen(
            state = InsightsPreviewData.samanta(),
            onPeriodSelect = {},
            onPointSelect = {},
            onRetry = {}
        )
    }
}
