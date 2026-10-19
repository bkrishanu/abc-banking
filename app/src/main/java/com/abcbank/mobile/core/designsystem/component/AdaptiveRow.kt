package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * A label on the start side and an amount on the end side, as in account and transaction rows.
 * They sit side by side when the amount fits unwrapped and no word of the label has to break;
 * otherwise, as at 200% font scale, the amount moves below the label, still end-aligned, so money
 * is never split across lines (DESIGN_SYSTEM.md section 08).
 */
@Composable
fun AdaptiveRow(
    start: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    gap: Dp = AbcTheme.spacing.space3,
    end: @Composable () -> Unit
) {
    Layout(
        content = {
            Box { start() }
            Box { end() }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val (startMeasurable, endMeasurable) = measurables
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val endWidth = endMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        val startMinWidth = startMeasurable.minIntrinsicWidth(Constraints.Infinity)
        val loose = constraints.copy(minWidth = 0, minHeight = 0)

        if (endWidth + gapPx + startMinWidth <= width) {
            val endPlaceable = endMeasurable.measure(loose.copy(maxWidth = endWidth))
            val startPlaceable = startMeasurable.measure(
                loose.copy(maxWidth = width - endPlaceable.width - gapPx)
            )
            layout(width, maxOf(startPlaceable.height, endPlaceable.height)) {
                startPlaceable.place(0, 0)
                endPlaceable.place(width - endPlaceable.width, 0)
            }
        } else {
            val startPlaceable = startMeasurable.measure(loose)
            val endPlaceable = endMeasurable.measure(loose)
            layout(width, startPlaceable.height + endPlaceable.height) {
                startPlaceable.place(0, 0)
                endPlaceable.place(width - endPlaceable.width, startPlaceable.height)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AdaptiveRowPreview() {
    AbcTheme {
        Column(Modifier.padding(AbcTheme.spacing.space4)) {
            AdaptiveRow(
                start = { Text("Everyday Checking", style = AbcTheme.typography.body) },
                end = { Text("$155,832.00", style = AbcTheme.typography.body) }
            )
            // Too narrow for both: the amount moves below.
            AdaptiveRow(
                start = { Text("Signature Credit Card", style = AbcTheme.typography.body) },
                end = { Text("$11,765.00", style = AbcTheme.typography.body) },
                modifier = Modifier.width(PREVIEW_NARROW_WIDTH)
            )
        }
    }
}

private val PREVIEW_NARROW_WIDTH = 160.dp
