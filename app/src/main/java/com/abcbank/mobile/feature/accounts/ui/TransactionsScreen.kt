package com.abcbank.mobile.feature.accounts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
 * The transaction ledger (MobileBuild.md 2.2, PRD feature 6): search, All / Spendings / Earnings
 * tabs with counts, and date range and category chips that combine, over one card per
 * transaction grouped Today, Yesterday, Last 30 Days and Earlier.
 */
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    events: TransactionsEvents,
    modifier: Modifier = Modifier
) {
    var openFilter by rememberSaveable { mutableStateOf<FilterPart?>(null) }
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
                filterControls(state.header, events) { openFilter = it }
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
                filterControls(state.header, events) { openFilter = it }
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
    val part = openFilter
    if (part != null && header != null) {
        FilterSheet(header, part, events, onDismiss = { openFilter = null })
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

/** Which filter chip's sheet is open. */
private enum class FilterPart { RANGE, CATEGORIES }

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.filterControls(
    header: LedgerHeader,
    events: TransactionsEvents,
    onOpenFilter: (FilterPart) -> Unit
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
                // Offline, search still works over what was saved; the hint says so.
                placeholder = stringResource(
                    if (header.isOffline) {
                        R.string.transactions_search_offline
                    } else {
                        R.string.transactions_search_placeholder
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(spacing.space4))
            FilterChipRow(
                options = TypeFilter.entries.map { typeLabel(it, header.typeCounts[it]) },
                selectedIndex = header.filter.type.ordinal,
                onSelect = { events.onTypeSelect(TypeFilter.entries[it]) }
            )
            Spacer(Modifier.height(spacing.space3))
            // Wraps instead of squeezing: a chip label never breaks inside a word.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                DropdownChip(
                    label = rangeLabel(header.filter.range),
                    active = header.filter.range != DateRange.LAST_30_DAYS,
                    onClick = { onOpenFilter(FilterPart.RANGE) }
                )
                DropdownChip(
                    label = categoriesLabel(header.filter.categories),
                    active = header.filter.categories.isNotEmpty(),
                    onClick = { onOpenFilter(FilterPart.CATEGORIES) }
                )
            }
        }
    }
}

/** A pill with a chevron that opens a filter's sheet; primary outline when it is not the default. */
@Composable
private fun DropdownChip(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = AbcTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = AbcTheme.dimens.touchTarget)
            .clip(AbcTheme.shapes.full)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = AbcTheme.dimens.chipHeight)
                .clip(AbcTheme.shapes.full)
                .background(colors.surface)
                .border(
                    if (active) AbcTheme.dimens.focusRingWidth else AbcTheme.dimens.borderWidth,
                    if (active) colors.primary else colors.border,
                    AbcTheme.shapes.full
                )
                .padding(horizontal = AbcTheme.spacing.space3)
        ) {
            Text(label, style = AbcTheme.typography.small, color = colors.text, softWrap = false)
            Spacer(Modifier.width(AbcTheme.spacing.space1))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(AbcTheme.dimens.iconSmall)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AbcTheme.spacing.space4, bottom = AbcTheme.spacing.space2)
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = groupLabel(section.group),
                style = AbcTheme.typography.small.copy(fontWeight = FontWeight.SemiBold),
                color = AbcTheme.colors.textMuted,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = pluralStringResource(
                    R.plurals.transactions_items,
                    section.rows.size,
                    section.rows.size
                ),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted
            )
        }
    }
    // One card per transaction (Stitch), each its own lazy item so long ledgers stay fast.
    items(section.rows, key = { it.transaction.id }) { row ->
        Column(
            Modifier
                .padding(bottom = AbcTheme.spacing.space2)
                .clip(AbcTheme.shapes.xl)
                .background(AbcTheme.colors.surface)
                .padding(horizontal = AbcTheme.spacing.space4)
        ) {
            LedgerTransaction(row, section.group, hidden, onTransaction)
        }
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
    val withCategory = stringResource(
        R.string.transaction_category_when,
        categoryLabel(transaction.category),
        whenText
    )
    val subtitle = if (transaction.status == TransactionStatus.PENDING) {
        stringResource(R.string.transaction_pending, withCategory)
    } else {
        withCategory
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

/**
 * The sheet behind one filter chip: the date range (one of three) or categories (any number).
 * Changes apply as they are made.
 */
@Composable
private fun FilterSheet(
    header: LedgerHeader,
    part: FilterPart,
    events: TransactionsEvents,
    onDismiss: () -> Unit
) {
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
                text = stringResource(
                    if (part == FilterPart.RANGE) {
                        R.string.transactions_date_range
                    } else {
                        R.string.transactions_categories
                    }
                ),
                style = AbcTheme.typography.h2,
                color = colors.text,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(spacing.space2))
            if (part == FilterPart.RANGE) {
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
                        Text(
                            rangeLabel(range),
                            style = AbcTheme.typography.body,
                            color = colors.text
                        )
                    }
                }
            } else {
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

/** "All 9", or just "All" before the counts are known. */
@Composable
private fun typeLabel(type: TypeFilter, count: Int?): String {
    val label = stringResource(
        when (type) {
            TypeFilter.ALL -> R.string.transactions_type_all
            TypeFilter.SPENDINGS -> R.string.transactions_type_spendings
            TypeFilter.EARNINGS -> R.string.transactions_type_earnings
        }
    )
    return if (count ==
        null
    ) {
        label
    } else {
        stringResource(R.string.transactions_type_count, label, count)
    }
}

/** "All Categories", the one category chosen, or "3 Categories". */
@Composable
private fun categoriesLabel(categories: Set<TransactionCategory>): String = when (categories.size) {
    0 -> stringResource(R.string.transactions_all_categories)
    1 -> categoryLabel(categories.first())
    else -> pluralStringResource(
        R.plurals.transactions_categories_count,
        categories.size,
        categories.size
    )
}

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
