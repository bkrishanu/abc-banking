package com.abcbank.mobile.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** One donut segment: its share of the whole ring (0 to 1), colour, and the label on it. */
data class DonutSegment(val fraction: Float, val color: Color, val label: String)

private val DONUT_STROKE = 24.dp
private val DONUT_GAP = 4.dp
private val DONUT_LABEL = 36.dp
private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f
private const val DONUT_MS = 600
private const val LINE_MS = 600

/**
 * Whether charts play their entrance (DESIGN_SYSTEM.md section 09). Screenshot tests turn it off,
 * since they capture the first frame; previews skip it through inspection mode.
 */
val LocalChartEntrance = staticCompositionLocalOf { true }

/** Plays [durationMs] once per composition, or starts finished when entrances are off. */
@Composable
private fun rememberEntrance(durationMs: Int): Float {
    val still = !LocalChartEntrance.current || LocalInspectionMode.current
    val progress = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!still) progress.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing))
    }
    return progress.value
}

/**
 * `DonutChart` from DESIGN_SYSTEM.md section 06: 24dp ring on a surfaceMuted track, rounded caps,
 * 4dp gaps, segments clockwise from the top with their labels in white circles, and [center]
 * inside. The segments sweep in over 600ms. TalkBack reads [description] instead of the drawing.
 */
@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    description: String,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit = {}
) {
    val track = AbcTheme.colors.surfaceMuted
    val progress = rememberEntrance(DONUT_MS)
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val labelSize = DONUT_LABEL
        // Labels sit on the ring's centre line, so the ring is inset by half a label.
        val ringInset = labelSize / 2
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(ringInset)
        ) {
            val stroke = DONUT_STROKE.toPx()
            val radius = (size.minDimension - stroke) / 2
            val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)
            val arcSize = Size(radius * 2, radius * 2)
            drawArc(track, 0f, FULL_CIRCLE, false, topLeft, arcSize, style = Stroke(stroke))
            // A round cap adds half the stroke to each end; the gap is measured between caps.
            val capDegrees = degrees(stroke / 2, radius)
            val gapDegrees = degrees(DONUT_GAP.toPx(), radius)
            var start = START_ANGLE
            segments.forEach { segment ->
                val sweep = segment.fraction.coerceIn(0f, 1f) * FULL_CIRCLE * progress
                val drawn = sweep - gapDegrees - capDegrees * 2
                if (drawn > 0f) {
                    drawArc(
                        segment.color,
                        start + gapDegrees / 2 + capDegrees,
                        drawn,
                        false,
                        topLeft,
                        arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                start += sweep
            }
        }
        Box(Modifier.padding(ringInset + DONUT_STROKE), contentAlignment = Alignment.Center) {
            center()
        }
        if (progress >= 1f) {
            val ring =
                with(density) {
                    (minOf(maxWidth, maxHeight) - ringInset * 2 - DONUT_STROKE).toPx() /
                        2
                }
            var start = START_ANGLE
            segments.forEach { segment ->
                val sweep = segment.fraction.coerceIn(0f, 1f) * FULL_CIRCLE
                val middle = (start + sweep / 2) * PI.toFloat() / (FULL_CIRCLE / 2)
                start += sweep
                if (sweep <= 0f) return@forEach
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (cos(middle) * ring).roundToInt(),
                                (sin(middle) * ring).roundToInt()
                            )
                        }
                        .defaultMinSize(labelSize, labelSize)
                        .clip(AbcTheme.shapes.full)
                        .background(AbcTheme.colors.surface)
                        .padding(AbcTheme.spacing.space1)
                        .clearAndSetSemantics {}
                ) {
                    Text(
                        segment.label,
                        style = AbcTheme.typography.small,
                        color = AbcTheme.colors.text
                    )
                }
            }
        }
    }
}

private fun degrees(length: Float, radius: Float): Float =
    length / radius * (FULL_CIRCLE / 2) / PI.toFloat()

/** A label under the line chart at point [index]. */
data class ChartLabel(val index: Int, val text: String)

private val LINE_STROKE = 2.dp
private val LINE_DOT = 10.dp
private val LINE_HEIGHT = 180.dp
private val TOOLTIP_SPACE = 40.dp
private const val FILL_TOP_ALPHA = 0.3f
private const val AVERAGE_ALPHA = 0.4f
private const val DASH = 8f
private const val VERTICAL_PADDING = 0.12f

/**
 * `LineChart` from DESIGN_SYSTEM.md section 06: 2dp positive line over a positive gradient fill,
 * the dashed [average] line, and the selected point with a vertical marker and a black tooltip.
 * Tapping picks the nearest point. Drawn left to right over 600ms.
 */
@Composable
fun LineChart(
    values: List<Long>,
    average: Long?,
    selectedIndex: Int,
    tooltip: String,
    labels: List<ChartLabel>,
    description: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    chartHeight: Dp = LINE_HEIGHT
) {
    val colors = AbcTheme.colors
    val progress = rememberEntrance(LINE_MS)
    val selected = selectedIndex.coerceIn(0, (values.size - 1).coerceAtLeast(0))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(chartHeight + TOOLTIP_SPACE)
        ) {
            if (values.size < 2) return@BoxWithConstraints
            val low = values.min().toFloat()
            val high = values.max().toFloat()
            val span = (high - low).takeIf { it > 0f } ?: 1f
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { chartHeight.toPx() }
            val topPx = with(density) { TOOLTIP_SPACE.toPx() }
            fun x(index: Int) = widthPx * index / (values.size - 1)
            fun y(value: Float) = topPx +
                heightPx *
                (1 - VERTICAL_PADDING - (value - low) / span * (1 - VERTICAL_PADDING * 2))

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(values.size) {
                        detectTapGestures { tap ->
                            onSelect(
                                (tap.x / size.width * (values.size - 1)).roundToInt().coerceIn(
                                    0,
                                    values.size - 1
                                )
                            )
                        }
                    }
            ) {
                val line = Path().apply {
                    values.forEachIndexed { index, value ->
                        if (index ==
                            0
                        ) {
                            moveTo(x(index), y(value.toFloat()))
                        } else {
                            lineTo(x(index), y(value.toFloat()))
                        }
                    }
                }
                val fill = Path().apply {
                    addPath(line)
                    lineTo(x(values.size - 1), topPx + heightPx)
                    lineTo(0f, topPx + heightPx)
                    close()
                }
                clipRect(right = size.width * progress) {
                    drawPath(
                        fill,
                        Brush.verticalGradient(
                            listOf(
                                colors.positive.copy(alpha = FILL_TOP_ALPHA),
                                colors.positive.copy(alpha = 0f)
                            ),
                            startY = topPx,
                            endY = topPx + heightPx
                        )
                    )
                    drawPath(
                        line,
                        colors.positive,
                        style = Stroke(LINE_STROKE.toPx(), cap = StrokeCap.Round)
                    )
                }
                average?.let {
                    val averageY = y(it.toFloat())
                    drawLine(
                        colors.positive.copy(alpha = AVERAGE_ALPHA),
                        Offset(0f, averageY),
                        Offset(size.width, averageY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH, DASH))
                    )
                }
                if (progress >= 1f) {
                    val pointX = x(selected)
                    val pointY = y(values[selected].toFloat())
                    drawLine(
                        colors.positive,
                        Offset(pointX, pointY),
                        Offset(
                            pointX,
                            topPx + heightPx
                        ),
                        LINE_STROKE.toPx()
                    )
                    drawCircle(
                        colors.surface,
                        LINE_DOT.toPx() / 2 + 2.dp.toPx(),
                        Offset(pointX, pointY)
                    )
                    drawCircle(colors.positive, LINE_DOT.toPx() / 2, Offset(pointX, pointY))
                }
            }
            if (progress >= 1f) {
                val pointX = x(selected)
                val pointY = y(values[selected].toFloat())
                Text(
                    text = tooltip,
                    style = AbcTheme.typography.small,
                    color = colors.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints.copy(minWidth = 0))
                            val room = (constraints.maxWidth - placeable.width).coerceAtLeast(0)
                            val above = pointY - placeable.height - LINE_DOT.toPx()
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                val left = (pointX - placeable.width / 2).roundToInt().coerceIn(
                                    0,
                                    room
                                )
                                placeable.place(left, above.roundToInt().coerceAtLeast(0))
                            }
                        }
                        .clip(AbcTheme.shapes.full)
                        .background(colors.primary)
                        .padding(
                            horizontal = AbcTheme.spacing.space2,
                            vertical = AbcTheme.spacing.space1
                        )
                )
            }
        }
        if (values.size >= 2 && labels.isNotEmpty()) {
            ChartLabels(labels, values.size)
        }
    }
}

/** Labels centred under their points, kept inside the chart; one that would overlap is left out. */
@Composable
private fun ChartLabels(labels: List<ChartLabel>, pointCount: Int) {
    val gap = AbcTheme.spacing.space2
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            labels.forEach {
                Text(it.text, style = AbcTheme.typography.small, color = AbcTheme.colors.textMuted)
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            var right = Int.MIN_VALUE
            placeables.forEachIndexed { index, placeable ->
                val centre = width.toFloat() * labels[index].index / (pointCount - 1)
                val left = (centre - placeable.width / 2).roundToInt()
                    .coerceIn(0, (width - placeable.width).coerceAtLeast(0))
                if (left >= right) {
                    placeable.place(left, 0)
                    right = left + placeable.width + gap.roundToPx()
                }
            }
        }
    }
}

/** `LegendChip` from DESIGN_SYSTEM.md section 06: a coloured dot and a Small label on a pill. */
@Composable
fun LegendChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    background: Color = AbcTheme.colors.surface
) {
    Row(
        modifier = modifier
            .clip(AbcTheme.shapes.full)
            .background(background)
            .padding(horizontal = AbcTheme.spacing.space3, vertical = AbcTheme.spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2)
    ) {
        Box(
            Modifier
                .size(AbcTheme.dimens.unreadDot)
                .clip(AbcTheme.shapes.full)
                .background(color)
        )
        Text(label, style = AbcTheme.typography.small, color = AbcTheme.colors.text)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ChartsPreview() {
    AbcTheme {
        val colors = AbcTheme.colors
        Column(
            Modifier.padding(AbcTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space4)
        ) {
            DonutChart(
                segments = listOf(
                    DonutSegment(0.24f, colors.accent, "37%"),
                    DonutSegment(0.14f, colors.accentDeep, "21%"),
                    DonutSegment(0.28f, colors.positive, "42%")
                ),
                description = "",
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("$12,480", style = AbcTheme.typography.h2, color = colors.text)
            }
            LineChart(
                values = listOf(
                    1_200L, 1_350, 1_900, 1_500, 1_780, 1_650, 1_620,
                    1_700, 2_480, 2_200, 2_400, 3_560, 3_650
                ),
                average = 2_000,
                selectedIndex = 11,
                tooltip = "$3,560",
                labels = listOf(ChartLabel(0, "Jul"), ChartLabel(6, "Aug"), ChartLabel(12, "Oct")),
                description = "",
                onSelect = {}
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2)) {
                LegendChip("Shopping", colors.accent, background = colors.background)
                LegendChip("Other", colors.positive, background = colors.background)
            }
        }
    }
}
