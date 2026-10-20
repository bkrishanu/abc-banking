package com.abcbank.mobile.feature.cards.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.designsystem.component.AbcBottomSheet
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.AbcTextField
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.cards.domain.CardRules
import com.abcbank.mobile.feature.cards.domain.TravelNoticeError
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Add a travel notice (MobileBuild.md 4.2): destination and start and end dates from date pickers.
 * The rules live in CardRules; this sheet shows their errors and closes once the notice is added.
 */
@Composable
internal fun TravelNoticeSheet(
    error: TravelNoticeError?,
    noticesAdded: Int,
    onSave: (String, LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var destination by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var end by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(DEFAULT_TRIP_DAYS)) }
    var picking by rememberSaveable { mutableStateOf<DateField?>(null) }
    // Close once the ViewModel reports a new notice, not when Save is merely tapped.
    val addedWhenOpened = remember { noticesAdded }
    LaunchedEffect(noticesAdded) { if (noticesAdded > addedWhenOpened) onDismiss() }

    AbcBottomSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AbcTheme.spacing.space4)
                .padding(bottom = AbcTheme.spacing.space6)
        ) {
            Text(
                text = stringResource(R.string.cards_add_travel_notice),
                style = AbcTheme.typography.h2,
                color = AbcTheme.colors.text,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(AbcTheme.spacing.space4))
            AbcTextField(
                value = destination,
                onValueChange = { destination = it.take(CardRules.MAX_DESTINATION_LENGTH + 1) },
                label = stringResource(R.string.cards_travel_destination),
                placeholder = stringResource(R.string.cards_travel_destination_placeholder),
                errorText = destinationError(error),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(AbcTheme.spacing.space4))
            DateButton(stringResource(R.string.cards_travel_start), start) {
                picking =
                    DateField.START
            }
            Spacer(Modifier.height(AbcTheme.spacing.space2))
            DateButton(stringResource(R.string.cards_travel_end), end) { picking = DateField.END }
            dateError(error)?.let {
                Text(
                    text = it,
                    style = AbcTheme.typography.small,
                    color = AbcTheme.colors.danger,
                    modifier = Modifier.padding(top = AbcTheme.spacing.space2)
                )
            }
            Spacer(Modifier.height(AbcTheme.spacing.space4))
            AbcButton(text = stringResource(R.string.cards_travel_save), onClick = {
                onSave(destination, start, end)
            })
        }
    }
    picking?.let { field ->
        DatePickerFor(
            initial = if (field == DateField.START) start else end,
            onPicked = { picked ->
                if (field == DateField.START) {
                    start = picked
                    if (end < picked) end = picked
                } else {
                    end = picked
                }
                picking = null
            },
            onDismiss = { picking = null }
        )
    }
}

private enum class DateField { START, END }

@Composable
private fun DateButton(label: String, date: LocalDate, onClick: () -> Unit) {
    AbcButton(
        text = "$label · ${DateFormatter.fullDate(date)}",
        onClick = onClick,
        variant = AbcButtonVariant.Secondary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerFor(
    initial: LocalDate,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    // The Material picker speaks UTC milliseconds for calendar days.
    val today = LocalDate.now()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = !Instant.ofEpochMilli(
                utcTimeMillis
            ).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text(stringResource(R.string.action_ok), color = AbcTheme.colors.text) }
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

@Composable
private fun destinationError(error: TravelNoticeError?): String? = when (error) {
    TravelNoticeError.DESTINATION_MISSING -> stringResource(R.string.cards_travel_error_destination)
    TravelNoticeError.DESTINATION_TOO_LONG -> stringResource(
        R.string.cards_travel_error_destination_long
    )
    else -> null
}

@Composable
private fun dateError(error: TravelNoticeError?): String? = when (error) {
    TravelNoticeError.STARTS_IN_PAST -> stringResource(R.string.cards_travel_error_past)
    TravelNoticeError.ENDS_BEFORE_START -> stringResource(R.string.cards_travel_error_order)
    TravelNoticeError.TOO_LONG -> stringResource(R.string.cards_travel_error_long)
    else -> null
}

private const val DEFAULT_TRIP_DAYS = 7L
