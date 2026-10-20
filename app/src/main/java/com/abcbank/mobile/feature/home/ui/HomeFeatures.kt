package com.abcbank.mobile.feature.home.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.component.FeatureTile
import com.abcbank.mobile.core.designsystem.component.SectionHeader
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * Home's Features carousel (Sample 2): shortcuts to what the demo shows best. Multi-currency in
 * the sample is out of scope (PRD section 07), so its place goes to Deposit a Check. View All
 * opens the More Actions sheet.
 */
@Composable
internal fun HomeFeatures(actions: HomeActions) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    SectionHeader(
        title = stringResource(R.string.home_features),
        actionLabel = stringResource(R.string.action_view_all),
        onAction = actions.onMoreActions
    )
    Row(
        modifier = Modifier
            .bleed(spacing.space4)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3)
    ) {
        FeatureTile(
            stringResource(R.string.home_feature_transfers),
            R.drawable.ic_arrow_up_right,
            colors.accent,
            actions.onTransfer
        )
        FeatureTile(
            stringResource(R.string.home_feature_analytics),
            R.drawable.ic_chart_column,
            colors.positive,
            actions.onInsights
        )
        FeatureTile(
            stringResource(R.string.home_feature_deposit),
            R.drawable.ic_arrow_down,
            colors.surfaceMuted,
            actions.onDepositCheck
        )
        FeatureTile(
            stringResource(R.string.home_feature_products),
            R.drawable.ic_layout_grid,
            colors.accentDeep,
            actions.onExploreProducts
        )
    }
}

/** Widens the content by [gutter] on both sides, so a carousel runs to the screen edges. */
private fun Modifier.bleed(gutter: Dp): Modifier = layout { measurable, constraints ->
    val extra = gutter.roundToPx() * 2
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}
