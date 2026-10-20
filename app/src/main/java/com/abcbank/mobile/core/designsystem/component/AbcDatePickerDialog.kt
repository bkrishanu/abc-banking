package com.abcbank.mobile.core.designsystem.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The Material date picker in a dialog (DESIGN_SYSTEM.md section 06, sheets and dialogs), for
 * calendar days: travel notices and scheduled bill payments. Only days [isSelectable] allows can
 * be picked; the rule itself belongs to the caller's domain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbcDatePickerDialog(
    initial: LocalDate,
    isSelectable: (LocalDate) -> Boolean,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    // The Material picker speaks UTC milliseconds for calendar days.
    fun LocalDate.millis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    fun Long.date(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.millis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                isSelectable(utcTimeMillis.date())
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPicked(it.date()) }
                    ?: onDismiss()
            }) {
                Text(stringResource(R.string.action_ok), color = AbcTheme.colors.text)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = AbcTheme.colors.text)
            }
        }
    ) {
        DatePicker(state = state)
    }
}
