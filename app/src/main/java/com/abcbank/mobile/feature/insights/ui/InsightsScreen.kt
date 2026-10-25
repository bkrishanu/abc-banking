package com.abcbank.mobile.feature.insights.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import com.abcbank.mobile.core.designsystem.component.BadgeTone
import com.abcbank.mobile.core.designsystem.component.CategoryIcon
import com.abcbank.mobile.core.designsystem.component.ChartLabel
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.DonutChart
import com.abcbank.mobile.core.designsystem.component.DonutSegment
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.FilterChipRow
import com.abcbank.mobile.core.designsystem.component.LineChart
import com.abcbank.mobile.core.designsystem.component.LocalBottomBarClearance
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.RateChip
import com.abcbank.mobile.core.designsystem.component.RateTone
import com.abcbank.mobile.core.designsystem.component.SectionHeader
import com.abcbank.mobile.core.designsystem.component.SkeletonBlock
import com.abcbank.mobile.core.designsystem.component.StatusBadge
import com.abcbank.mobile.core.designsystem.component.TitleTopBar
import com.abcbank.mobile.core.designsystem.component.categoryLabel
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.TABULAR_FIGURES
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.feature.insights.domain.GoalProgress
import com.abcbank.mobile.feature.insights.domain.InsightsRules
import com.abcbank.mobile.feature.insights.domain.SavingsSummary
import com.abcbank.mobile.feature.insights.domain.SpendingPeriod
import com.abcbank.mobile.feature.insights.domain.SpendingSummary
import com.abcbank.mobile.feature.insights.domain.TopCategory
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsRoute(
    onViewAllTransactions: () -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreen(
        state = state,
        onPeriodSelect = viewModel::onPeriodSelect,
        onPointSelect = viewModel::onPointSelect,
        onRetry = viewModel::onRetry,
        onViewAllTransactions = onViewAllTransactions
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
    modifier: Modifier = Modifier,
    onViewAllTransactions: () -> Unit = {}
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
        TitleTopBar(title = stringResource(R.string.insights_title))
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
                // The period drives Spending and Top Categories; the savings goal has its own timeline.
                FilterChipRow(
                    options = PERIODS.map { periodLabel(it) },
                    selectedIndex = PERIODS.indexOf(state.period),
                    onSelect = { onPeriodSelect(PERIODS[it]) }
                )
                Spacer(Modifier.height(spacing.space4))
                SpendingCard(state.spending, state.balancesHidden)
                if (state.spending.topCategories.isNotEmpty()) {
                    Spacer(Modifier.height(spacing.space6))
                    TopCategories(state.spending, state.balancesHidden, onViewAllTransactions)
                }
                Spacer(Modifier.height(spacing.space6))
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

@Composable
private fun SpendingCard(summary: SpendingSummary, hidden: Boolean) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    val sliceColors = listOf(colors.accent, colors.accentDeep, colors.positive)
    ContentCard {
        // At large font sizes the chip moves under the title instead of breaking it.
        AdaptiveRow(
            start = {
                Column {
                    Text(
                        text = stringResource(R.string.insights_spending),
                        style = AbcTheme.typography.h3,
                        color = colors.text,
                        modifier = Modifier.semantics { heading() }
                    )
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
            }
        ) {
            BudgetChip(summary, hidden)
        }
        Spacer(Modifier.height(spacing.space4))
        if (!summary.total.isPositive) {
            Text(
                text = stringResource(R.string.insights_no_spending),
                style = AbcTheme.typography.body,
                color = colors.textMuted
            )
            return@ContentCard
        }
        val total = money(summary.total, hidden)
        val budget = money(summary.budget, hidden)
        Text(
            text = total,
            style = AbcTheme.typography.h1.copy(fontFeatureSettings = TABULAR_FIGURES),
            color = colors.text
        )
        Text(
            text = stringResource(R.string.insights_of_budget, budget),
            style = AbcTheme.typography.small,
            color = colors.textMuted
        )
        Spacer(Modifier.height(spacing.space4))
        val labels = summary.slices.map { slice ->
            slice.category?.let { categoryLabel(it) }
                ?: stringResource(R.string.insights_other)
        }
        // The ring is the budget; spending past it fills the ring.
        val ring = maxOf(summary.budget.cents, summary.total.cents).toFloat()
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
                    text = stringResource(R.string.insights_percent, summary.percentOfBudget),
                    style = AbcTheme.typography.h2.copy(fontFeatureSettings = TABULAR_FIGURES),
                    color = colors.text,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.insights_budget_used),
                    style = AbcTheme.typography.small,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(spacing.space4))
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            summary.slices.forEachIndexed { index, slice ->
                SliceRow(
                    label = labels[index],
                    color = sliceColors[index % sliceColors.size],
                    percent = slice.percentOfSpending,
                    amount = money(slice.amount, hidden, cents = true)
                )
            }
        }
    }
}

/** "$6,420 Left" while under budget, "Over by $X" in warning past it. */
@Composable
private fun BudgetChip(summary: SpendingSummary, hidden: Boolean) {
    val over = summary.remaining.isNegative
    val amount = money(summary.remaining.absoluteValue(), hidden)
    StatusBadge(
        text = stringResource(
            if (over) R.string.insights_over_budget else R.string.insights_left,
            amount
        ),
        tone = if (over) BadgeTone.Warning else BadgeTone.Neutral
    )
}

/** A breakdown row under the donut: the segment's colour dot, name, share and amount. */
@Composable
private fun SliceRow(label: String, color: Color, percent: Int, amount: String) {
    val colors = AbcTheme.colors
    AdaptiveRow(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AbcTheme.shapes.lg)
            .background(colors.background)
            .padding(horizontal = AbcTheme.spacing.space4, vertical = AbcTheme.spacing.space3)
            .semantics(mergeDescendants = true) {},
        start = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(AbcTheme.dimens.unreadDot)
                        .clip(AbcTheme.shapes.full)
                        .background(color)
                )
                Spacer(Modifier.width(AbcTheme.spacing.space3))
                Column {
                    Text(label, style = AbcTheme.typography.body, color = colors.text)
                    Text(
                        text = stringResource(R.string.insights_share, percent),
                        style = AbcTheme.typography.small,
                        color = colors.textMuted
                    )
                }
            }
        }
    ) {
        Text(
            text = amount,
            style = AbcTheme.typography.body.copy(fontFeatureSettings = TABULAR_FIGURES),
            color = colors.text
        )
    }
}

/** The period's biggest categories with their biggest merchants and a bar for their share. */
@Composable
private fun TopCategories(summary: SpendingSummary, hidden: Boolean, onViewAll: () -> Unit) {
    SectionHeader(
        title = stringResource(R.string.insights_top_categories),
        actionLabel = stringResource(R.string.action_view_all),
        onAction = onViewAll
    )
    ContentCard {
        Column(verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space4)) {
            summary.topCategories.forEach { TopCategoryRow(it, hidden) }
        }
    }
}

@Composable
private fun TopCategoryRow(top: TopCategory, hidden: Boolean) {
    val colors = AbcTheme.colors
    val spacing = AbcTheme.spacing
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(top.category, incoming = false)
            Spacer(Modifier.width(spacing.space3))
            AdaptiveRow(
                modifier = Modifier.weight(1f),
                start = {
                    Column {
                        Text(
                            text = categoryLabel(top.category),
                            style = AbcTheme.typography.body,
                            color = colors.text
                        )
                        if (top.merchants.isNotEmpty()) {
                            Text(
                                text = top.merchants.joinToString(", "),
                                style = AbcTheme.typography.small,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            ) {
                Text(
                    text = money(top.amount, hidden, cents = true),
                    style = AbcTheme.typography.body.copy(fontFeatureSettings = TABULAR_FIGURES),
                    color = colors.text
                )
            }
        }
        Spacer(Modifier.height(spacing.space2))
        ShareBar(top.percentOfSpending)
    }
}

/** A thin bar for a share of spending; TalkBack reads the share as text instead. */
@Composable
private fun ShareBar(percent: Int) {
    val share = stringResource(R.string.insights_share, percent)
    Box(
        Modifier
            .fillMaxWidth()
            .height(AbcTheme.dimens.progressBarHeight)
            .clip(AbcTheme.shapes.full)
            .background(AbcTheme.colors.surfaceMuted)
            .clearAndSetSemantics { contentDescription = share }
    ) {
        Box(
            Modifier
                .fillMaxWidth(percent.coerceIn(0, PERCENT_MAX) / PERCENT_MAX.toFloat())
                .height(AbcTheme.dimens.progressBarHeight)
                .clip(AbcTheme.shapes.full)
                .background(AbcTheme.colors.primary)
        )
    }
}

private const val PERCENT_MAX = 100

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
    val reached = InsightsRules.percent(goal.saved, goal.target)
    ContentCard {
        AdaptiveRow(
            start = {
                Column {
                    Text(
                        text = stringResource(R.string.insights_savings_goal),
                        style = AbcTheme.typography.label,
                        color = colors.textMuted
                    )
                    Text(
                        text = goal.name,
                        style = AbcTheme.typography.h3,
                        color = colors.text,
                        modifier = Modifier.semantics { heading() }
                    )
                }
            }
        ) {
            RateChip(stringResource(R.string.insights_reached, reached), RateTone.Earn)
        }
        Spacer(Modifier.height(spacing.space3))
        Text(
            text = money(goal.saved, hidden, cents = true),
            style = AbcTheme.typography.h1.copy(fontFeatureSettings = TABULAR_FIGURES),
            color = colors.text
        )
        Text(
            text = stringResource(
                R.string.insights_of_target,
                money(goal.target, hidden, cents = true)
            ),
            style = AbcTheme.typography.small,
            color = colors.textMuted
        )
        Spacer(Modifier.height(spacing.space3))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(AbcTheme.shapes.md)
                .background(colors.positiveTint)
                .padding(spacing.space3)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_smile),
                contentDescription = null,
                tint = colors.positiveText,
                modifier = Modifier.size(AbcTheme.dimens.iconMedium)
            )
            Spacer(Modifier.width(spacing.space3))
            Text(
                text = progressText(summary.progress),
                style = AbcTheme.typography.small,
                color = colors.text
            )
        }
        Spacer(Modifier.height(spacing.space4))
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

private fun money(amount: Money, hidden: Boolean, cents: Boolean = false): String = when {
    hidden -> AMOUNT_MASK
    cents -> MoneyFormatter.format(amount)
    else -> MoneyFormatter.formatWhole(amount)
}

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
