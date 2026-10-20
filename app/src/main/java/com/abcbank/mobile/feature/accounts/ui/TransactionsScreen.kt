package com.abcbank.mobile.feature.accounts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.designsystem.component.AbcBottomSheet
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.AbcTextField
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.FilterChipRow
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.SectionHeader
import com.abcbank.mobile.core.designsystem.component.SkeletonBlock
import com.abcbank.mobile.core.designsystem.component.TransactionLeadingIcon
import com.abcbank.mobile.core.designsystem.component.TransactionRow
import com.abcbank.mobile.core.designsystem.component.categoryLabel
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import com.abcbank.mobile.feature.accounts.domain.DateRange
import com.abcbank.mobile.feature.accounts.domain.LedgerGroup
import com.abcbank.mobile.feature.accounts.domain.TypeFilter

/** Everything the ledger screen can ask its ViewModel to do. */
data class TransactionsEvents(
    val onBack: () -> Unit = {},
    val onTransaction: (String) -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onTypeSelect: (TypeFilter) -> Unit = {},
    val onRangeSelect: (DateRange) -> Unit = {},
    val onCategoryToggle: (TransactionCategory) -> Unit = {},
    val onClearFilters: () -> Unit = {},
    val onShowAllTime: () -> Unit = {},
    val onRetry: () -> Unit = {}
)

@Composable
fun TransactionsRoute(
    onBack: () -> Unit,
    onTransaction: (String) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionsScreen(
        state = state,
        events = TransactionsEvents(
            onBack = onBack,
            onTransaction = onTransaction,
            onQueryChange = viewModel::onQueryChange,
            onTypeSelect = viewModel::onTypeSelect,
            onRangeSelect = viewModel::onRangeSelect,
            onCategoryToggle = viewModel::onCategoryToggle,
            onClearFilters = viewModel::onClearFilters,
            onShowAllTime = viewModel::onShowAllTime,
            onRetry = viewModel::onRetry
        )
    )
}

/**
 * The transaction ledger (MobileBuild.md 2.2, PRD feature 6): search, Spendings / Earnings chips
 * and a filter sheet that combine, over rows grouped Today, Yesterday, Last 30 Days and Earlier.
 */
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    events: TransactionsEvents,
    modifier: Modifier = Modifier
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val spacing = AbcTheme.spacing
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AbcTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(
            start = spacing.space4,
            end = spacing.space4,
            bottom = spacing.space6
        )
    ) {
        item { TopBar(state, events.onBack) }
        when (state) {
            TransactionsUiState.Loading -> item { LedgerSkeleton() }
            is TransactionsUiState.Error -> item {
                Spacer(Modifier.height(spacing.space12))
                ErrorState(
                    message = stringResource(
                        if (state.error ==
                            DataError.OFFLINE
                        ) {
                            R.string.state_offline_no_data
                        } else {
                            R.string.transactions_error
                        }
                    ),
                    onRetry = events.onRetry
                )
            }
            is TransactionsUiState.Empty -> {
                filterControls(state.header, events) { showFilters = true }
                item {
                    MessageCard(stringResource(R.string.transactions_empty)) {
                        if (state.header.filter.range != DateRange.ALL) {
                            AbcButton(
                                text = stringResource(R.string.transactions_show_all_time),
                                onClick = events.onShowAllTime,
                                variant = AbcButtonVariant.Secondary
                            )
                        }
                    }
                }
            }
            is TransactionsUiState.Content -> {
                filterControls(state.header, events) { showFilters = true }
                if (state.hiddenByFilters) {
                    item {
                        MessageCard(stringResource(R.string.transactions_no_match)) {
                            AbcButton(
                                text = stringResource(R.string.transactions_clear_filters),
                                onClick = events.onClearFilters,
                                variant = AbcButtonVariant.Secondary
                            )
                        }
                    }
                }
                state.sections.forEach { section ->
                    ledgerSection(section, state.balancesHidden, events.onTransaction)
                }
            }
        }
    }
    val header =
        (state as? TransactionsUiState.Content)?.header
            ?: (state as? TransactionsUiState.Empty)?.header
    if (showFilters && header != null) {
        FilterSheet(header, events, onDismiss = { showFilters = false })
    }
}

@Composable
private fun TopBar(state: TransactionsUiState, onBack: () -> Unit) {
    val account = (state as? TransactionsUiState.Content)?.header?.account
        ?: (state as? TransactionsUiState.Empty)?.header?.account
    Column {
        IconButton(onClick = onBack, modifier = Modifier.size(AbcTheme.dimens.touchTarget)) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.cd_back),
                tint = AbcTheme.colors.text,
                modifier = Modifier.size(AbcTheme.dimens.iconLarge)
            )
        }
        Text(
            text = account?.nickname ?: stringResource(R.string.transactions_title),
            style = AbcTheme.typography.h1,
            color = AbcTheme.colors.text,
            modifier = Modifier.semantics { heading() }
        )
        if (account != null) {
            Text(
                text = stringResource(R.string.account_last4, account.last4),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted
            )
        }
        Spacer(Modifier.height(AbcTheme.spacing.space4))
    }
}

private fun LazyListScope.filterControls(
    header: LedgerHeader,
    events: TransactionsEvents,
    onOpenFilters: () -> Unit
) {
    item {
        val spacing = AbcTheme.spacing
        Column {
            if (header.isOffline) {
                OfflineBanner()
                Spacer(Modifier.height(spacing.space4))
            }
            AbcTextField(
                value = header.filter.query,
                onValueChange = events.onQueryChange,
                label = stringResource(R.string.transactions_search_label),
                placeholder = stringResource(R.string.transactions_search_placeholder),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(spacing.space4))
            FilterChipRow(
                options = listOf(
                    stringResource(R.string.transactions_type_all),
                    stringResource(R.string.transactions_type_spendings),
                    stringResource(R.string.transactions_type_earnings)
                ),
                selectedIndex = header.filter.type.ordinal,
                onSelect = { events.onTypeSelect(TypeFilter.entries[it]) }
            )
            val count = header.filter.activeCount
            SectionHeader(
                title = rangeLabel(header.filter.range),
                actionLabel = if (count > 0) {
                    stringResource(R.string.transactions_filter_count, count)
                } else {
                    stringResource(R.string.transactions_filter)
                },
                onAction = onOpenFilters
            )
        }
    }
}

private fun LazyListScope.ledgerSection(
    section: LedgerSectionItem,
    hidden: Boolean,
    onTransaction: (String) -> Unit
) {
    item(key = "header-${section.group}") {
        Text(
            text = groupLabel(section.group),
            style = AbcTheme.typography.small,
            color = AbcTheme.colors.textMuted,
            modifier = Modifier
                .padding(top = AbcTheme.spacing.space4, bottom = AbcTheme.spacing.space2)
                .semantics { heading() }
        )
    }
    // Rows are separate lazy items drawn as one content card, so long ledgers stay fast.
    itemsIndexed(section.rows, key = { _, row -> row.transaction.id }) { index, row ->
        val shape = rowShape(index, section.rows.size)
        Column(
            Modifier
                .background(AbcTheme.colors.surface, shape)
                .padding(horizontal = AbcTheme.spacing.space4)
        ) {
            if (index > 0) HorizontalDivider(color = AbcTheme.colors.border)
            LedgerTransaction(row, section.group, hidden, onTransaction)
        }
    }
}

@Composable
private fun rowShape(index: Int, count: Int): Shape {
    val shapes = AbcTheme.shapes
    return when {
        count == 1 -> shapes.xl
        index == 0 -> shapes.xlTop
        index == count - 1 -> shapes.xlBottom
        else -> RectangleShape
    }
}

@Composable
private fun LedgerTransaction(
    row: LedgerRow,
    group: LedgerGroup,
    hidden: Boolean,
    onTransaction: (String) -> Unit
) {
    val transaction = row.transaction
    val time = DateFormatter.time(row.time)
    // Today and Yesterday say the day in the header; older rows need the date.
    val whenText = when (group) {
        LedgerGroup.TODAY, LedgerGroup.YESTERDAY -> time
        LedgerGroup.LAST_30_DAYS, LedgerGroup.EARLIER ->
            stringResource(R.string.transaction_on_date, DateFormatter.shortDate(row.date), time)
    }
    val subtitle = if (transaction.status == TransactionStatus.PENDING) {
        stringResource(R.string.transaction_pending, whenText)
    } else {
        whenText
    }
    TransactionRow(
        title = transaction.merchant,
        subtitle = subtitle,
        amount = transaction.signedAmount,
        hidden = hidden,
        leading = {
            TransactionLeadingIcon(
                transaction.category,
                transaction.merchant,
                incoming = transaction.direction == TransactionDirection.CREDIT
            )
        },
        modifier = Modifier.clickable(onClick = { onTransaction(transaction.id) })
    )
}

@Composable
private fun MessageCard(message: String, action: @Composable () -> Unit) {
    ContentCard {
        Text(text = message, style = AbcTheme.typography.body, color = AbcTheme.colors.text)
        Spacer(Modifier.height(AbcTheme.spacing.space3))
        action()
    }
}

/** Date range (one of three) and categories (any number); changes apply as they are made. */
@Composable
private fun FilterSheet(header: LedgerHeader, events: TransactionsEvents, onDismiss: () -> Unit) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    AbcBottomSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.space4)
                .padding(bottom = spacing.space6)
        ) {
            Text(
                text = stringResource(R.string.transactions_filter_title),
                style = AbcTheme.typography.h2,
                color = colors.text,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(spacing.space4))
            Text(
                stringResource(R.string.transactions_date_range),
                style = AbcTheme.typography.h3,
                color = colors.text
            )
            DateRange.entries.forEach { range ->
                val selected = header.filter.range == range
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AbcTheme.dimens.touchTarget)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = {
                            events.onRangeSelect(range)
                        }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                    )
                    Text(rangeLabel(range), style = AbcTheme.typography.body, color = colors.text)
                }
            }
            Spacer(Modifier.height(spacing.space4))
            Text(
                stringResource(R.string.transactions_categories),
                style = AbcTheme.typography.h3,
                color = colors.text
            )
            TransactionCategory.entries.forEach { category ->
                val checked = category in header.filter.categories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AbcTheme.dimens.touchTarget)
                        .toggleable(value = checked, role = Role.Checkbox, onValueChange = {
                            events.onCategoryToggle(category)
                        }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                    )
                    Text(
                        categoryLabel(category),
                        style = AbcTheme.typography.body,
                        color = colors.text
                    )
                }
            }
            Spacer(Modifier.height(spacing.space4))
            AbcButton(text = stringResource(R.string.action_done), onClick = onDismiss)
            Spacer(Modifier.height(spacing.space2))
            AbcButton(
                text = stringResource(R.string.transactions_clear_filters),
                onClick = events.onClearFilters,
                variant = AbcButtonVariant.Ghost
            )
        }
    }
}

@Composable
private fun LedgerSkeleton() {
    Column {
        SkeletonBlock(
            Modifier
                .fillMaxWidth()
                .height(AbcTheme.dimens.textFieldHeight)
        )
        Spacer(Modifier.height(AbcTheme.spacing.space4))
        repeat(SKELETON_ROWS) {
            SkeletonBlock(
                Modifier
                    .fillMaxWidth()
                    .height(AbcTheme.dimens.actionPillHeight),
                shape = AbcTheme.shapes.xl
            )
            Spacer(Modifier.height(AbcTheme.spacing.space2))
        }
    }
}

@Composable
private fun rangeLabel(range: DateRange): String = stringResource(
    when (range) {
        DateRange.LAST_30_DAYS -> R.string.transactions_range_30
        DateRange.LAST_90_DAYS -> R.string.transactions_range_90
        DateRange.ALL -> R.string.transactions_range_all
    }
)

@Composable
private fun groupLabel(group: LedgerGroup): String = stringResource(
    when (group) {
        LedgerGroup.TODAY -> R.string.transactions_group_today
        LedgerGroup.YESTERDAY -> R.string.transactions_group_yesterday
        LedgerGroup.LAST_30_DAYS -> R.string.transactions_group_last_30
        LedgerGroup.EARLIER -> R.string.transactions_group_earlier
    }
)

private const val SKELETON_ROWS = 6

@Preview(widthDp = 360, heightDp = 1200)
@Composable
private fun TransactionsContentPreview() {
    AbcTheme { TransactionsScreen(TransactionsPreviewData.content(), TransactionsEvents()) }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun TransactionsNoMatchPreview() {
    AbcTheme { TransactionsScreen(TransactionsPreviewData.noMatch(), TransactionsEvents()) }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun TransactionsLoadingPreview() {
    AbcTheme { TransactionsScreen(TransactionsUiState.Loading, TransactionsEvents()) }
}
