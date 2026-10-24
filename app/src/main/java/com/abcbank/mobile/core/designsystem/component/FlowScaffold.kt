package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * The frame for flows that leave the tabs (DESIGN_SYSTEM.md section 06): back arrow, "Step 2 of 5",
 * H1 title, scrolling content, and the step's buttons pinned at the bottom. A null [onBack] hides
 * the arrow (receipts and confirmations); null steps hide the indicator (a flow's first screen).
 */
@Composable
fun FlowScaffold(
    step: Int?,
    totalSteps: Int?,
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    bottom: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = AbcTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AbcTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AbcTheme.dimens.touchTarget),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(AbcTheme.dimens.touchTarget)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_left),
                        contentDescription = stringResource(R.string.cd_back),
                        tint = AbcTheme.colors.text,
                        modifier = Modifier.size(AbcTheme.dimens.iconLarge)
                    )
                }
            } else {
                Spacer(Modifier.width(spacing.space4))
            }
            Spacer(Modifier.weight(1f))
            if (step != null && totalSteps != null) {
                Text(
                    text = stringResource(R.string.flow_step, step, totalSteps),
                    style = AbcTheme.typography.small,
                    color = AbcTheme.colors.textMuted,
                    modifier = Modifier.padding(end = spacing.space4)
                )
            }
        }
        if (step != null && totalSteps != null) {
            StepProgress(
                step = step,
                totalSteps = totalSteps,
                modifier = Modifier.padding(
                    start = spacing.space4,
                    end = spacing.space4,
                    bottom = spacing.space4
                )
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.space4)
        ) {
            Text(
                text = title,
                style = AbcTheme.typography.h1,
                color = AbcTheme.colors.text,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(spacing.space6))
            content()
            Spacer(Modifier.height(spacing.space6))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space4, vertical = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
            content = bottom
        )
    }
}

/**
 * One 4dp segment per step, space-1 apart: finished and current steps in primary, the rest in
 * surfaceMuted. Decorative; TalkBack reads the "Step 2 of 5" text instead.
 */
@Composable
private fun StepProgress(step: Int, totalSteps: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space1)
    ) {
        repeat(totalSteps) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(AbcTheme.dimens.stepProgressHeight)
                    .clip(AbcTheme.shapes.full)
                    .background(
                        if (index < step) AbcTheme.colors.primary else AbcTheme.colors.surfaceMuted
                    )
            )
        }
    }
}

/** A label and value row inside a content card, wrapping the value below at large font sizes. */
@Composable
fun SummaryRow(label: String, value: String, valueStyle: TextStyle = AbcTheme.typography.body) {
    AdaptiveRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AbcTheme.spacing.space3)
            .semantics(mergeDescendants = true) {},
        start = {
            Text(text = label, style = AbcTheme.typography.small, color = AbcTheme.colors.textMuted)
        }
    ) {
        Text(
            text = value,
            style = valueStyle,
            color = AbcTheme.colors.text,
            textAlign = TextAlign.End
        )
    }
}

/** Summary rows with dividers between them. */
@Composable
fun SummaryRows(rows: List<Pair<String, String>>) {
    rows.forEachIndexed { index, (label, value) ->
        if (index > 0) HorizontalDivider(color = AbcTheme.colors.border)
        SummaryRow(label, value)
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun FlowScaffoldPreview() {
    AbcTheme {
        FlowScaffold(
            step = 2,
            totalSteps = 4,
            title = "Review",
            onBack = {},
            bottom = { AbcButton(text = "Continue", onClick = {}) }
        ) {
            ContentCard { SummaryRows(listOf("Amount" to "$250.00", "Fee" to "Free")) }
        }
    }
}
