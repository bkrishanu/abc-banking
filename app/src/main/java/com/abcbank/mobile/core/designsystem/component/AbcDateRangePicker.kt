package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * `AbcDateRangePicker` from DESIGN_SYSTEM.md section 06: the Material 3 range calendar inline (in a
 * sheet), for a start and end day such as a travel notice. Months scroll inside its fixed height.
 * Only days [isSelectable] allows can be picked; the rule belongs to the caller's domain.
 * [onRangeChange] reports the start and the end, which is null until a second day is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbcDateRangePicker(
    start: LocalDate,
    end: LocalDate?,
    isSelectable: (LocalDate) -> Boolean,
    onRangeChange: (LocalDate, LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    // The Material picker speaks UTC milliseconds for calendar days.
    fun LocalDate.millis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    fun Long.date(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = start.millis(),
        initialSelectedEndDateMillis = end?.millis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                isSelectable(utcTimeMillis.date())
        }
    )
    LaunchedEffect(state.selectedStartDateMillis, state.selectedEndDateMillis) {
        val picked = state.selectedStartDateMillis ?: return@LaunchedEffect
        onRangeChange(picked.date(), state.selectedEndDateMillis?.date())
    }
    val colors = AbcTheme.colors
    DateRangePicker(
        state = state,
        title = null,
        headline = null,
        showModeToggle = false,
        colors = DatePickerDefaults.colors(
            containerColor = colors.surface,
            selectedDayContainerColor = colors.primary,
            selectedDayContentColor = colors.onPrimary,
            dayInSelectionRangeContainerColor = colors.positiveTint,
            dayInSelectionRangeContentColor = colors.text,
            todayDateBorderColor = colors.primary,
            todayContentColor = colors.text,
            dayContentColor = colors.text,
            weekdayContentColor = colors.textMuted,
            subheadContentColor = colors.text
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(AbcTheme.dimens.rangePickerHeight)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AbcDateRangePickerPreview() {
    AbcTheme {
        AbcDateRangePicker(
            start = LocalDate.of(2026, 12, 12),
            end = LocalDate.of(2026, 12, 24),
            isSelectable = { true },
            onRangeChange = { _, _ -> },
            modifier = Modifier.padding(AbcTheme.spacing.space4)
        )
    }
}
