package com.abcbank.mobile.feature.cards.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.designsystem.component.AbcBottomSheet
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcDateRangePicker
import com.abcbank.mobile.core.designsystem.component.AbcTextField
import com.abcbank.mobile.core.designsystem.component.AdaptiveRow
import com.abcbank.mobile.core.designsystem.component.StatusBadge
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.cards.domain.CardRules
import com.abcbank.mobile.feature.cards.domain.TravelNoticeError
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Add a travel notice (MobileBuild.md 4.2): destination, then the trip's days on a range calendar
 * with a day count (Stitch). A single tapped day is a one-day trip.
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
    var end by rememberSaveable {
        mutableStateOf<LocalDate?>(LocalDate.now().plusDays(DEFAULT_TRIP_DAYS))
    }
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
            AdaptiveRow(
                start = {
                    Text(
                        text = stringResource(R.string.cards_travel_dates_title),
                        style = AbcTheme.typography.h3,
                        color = AbcTheme.colors.text
                    )
                }
            ) {
                val days = ChronoUnit.DAYS.between(start, end ?: start) + 1
                StatusBadge(
                    pluralStringResource(
                        R.plurals.cards_travel_day_count,
                        days.toInt(),
                        days.toInt()
                    )
                )
            }
            Text(
                text = stringResource(
                    R.string.cards_travel_dates,
                    DateFormatter.shortDate(start),
                    DateFormatter.fullDate(end ?: start)
                ),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted
            )
            Spacer(Modifier.height(AbcTheme.spacing.space2))
            // Travel notices start today or later (CardRules checks the rest on save).
            val today = remember { LocalDate.now() }
            AbcDateRangePicker(
                start = start,
                end = end,
                isSelectable = { !it.isBefore(today) },
                onRangeChange = { from, to ->
                    start = from
                    end = to
                }
            )
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
                onSave(destination, start, end ?: start)
            })
        }
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
