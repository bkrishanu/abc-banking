package com.abcbank.mobile.feature.home.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.ActionPill
import com.abcbank.mobile.core.designsystem.component.BalanceText
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.GreetingTopBar
import com.abcbank.mobile.core.designsystem.component.GridPill
import com.abcbank.mobile.core.designsystem.component.LocalBottomBarClearance
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.SectionHeader
import com.abcbank.mobile.core.designsystem.component.SkeletonBlock
import com.abcbank.mobile.core.designsystem.component.TransactionLeadingIcon
import com.abcbank.mobile.core.designsystem.component.TransactionRow
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.BANK_CARD_ASPECT_RATIO
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus

/** Where Home's actions go. Destinations that are not built yet are no-ops in the nav host. */
data class HomeActions(
    val onNotifications: () -> Unit = {},
    val onProfile: () -> Unit = {},
    val onTransfer: () -> Unit = {},
    val onPayBills: () -> Unit = {},
    val onMoreActions: () -> Unit = {},
    val onViewAllTransactions: () -> Unit = {},
    val onAccount: (accountId: String) -> Unit = {},
    val onTransaction: (transactionId: String) -> Unit = {},
    val onExploreProducts: () -> Unit = {},
    val onInsights: () -> Unit = {},
    val onDepositCheck: () -> Unit = {},
    val onDocuments: () -> Unit = {},
    val onCards: () -> Unit = {}
)

@Composable
fun HomeRoute(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showMoreActions by rememberSaveable { mutableStateOf(false) }
    HomeScreen(
        state = state,
        actions = actions.copy(onMoreActions = { showMoreActions = true }),
        onToggleBalances = viewModel::onToggleBalances,
        onRetry = viewModel::onRetry
    )
    if (showMoreActions) {
        MoreActionsSheet(
            actions = listOf(
                MoreAction(
                    R.drawable.ic_arrow_up_right,
                    stringResource(R.string.more_send_money),
                    actions.onTransfer
                ),
                MoreAction(
                    R.drawable.ic_arrow_down,
                    stringResource(R.string.more_deposit_check),
                    actions.onDepositCheck
                ),
                MoreAction(
                    R.drawable.ic_file_text,
                    stringResource(R.string.more_statements),
                    actions.onDocuments
                ),
                MoreAction(
                    R.drawable.ic_layout_grid,
                    stringResource(R.string.more_explore_products),
                    actions.onExploreProducts
                ),
                MoreAction(
                    R.drawable.ic_chart_column,
                    stringResource(R.string.more_insights),
                    actions.onInsights
                ),
                MoreAction(
                    R.drawable.ic_credit_card,
                    stringResource(R.string.more_cards),
                    actions.onCards
                )
            ),
            onDismiss = { showMoreActions = false }
        )
    }
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    onToggleBalances: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AbcTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AbcTheme.spacing.space4)
    ) {
        when (state) {
            HomeUiState.Loading -> HomeSkeleton()
            is HomeUiState.Error -> HomeError(state.error, onRetry)
            is HomeUiState.Empty -> {
                GreetingTopBar(
                    personaId = state.personaId,
                    greeting = stringResource(R.string.home_greeting, state.firstName),
                    unreadNotifications = 0,
                    onNotifications = actions.onNotifications,
                    onProfile = actions.onProfile
                )
                Spacer(Modifier.height(AbcTheme.spacing.space6))
                EmptyCard(stringResource(R.string.home_empty), actions.onExploreProducts)
            }
            is HomeUiState.Content -> HomeContent(state, actions, onToggleBalances)
        }
        Spacer(Modifier.height(AbcTheme.spacing.space6 + LocalBottomBarClearance.current))
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    actions: HomeActions,
    onToggleBalances: () -> Unit
) {
    val spacing = AbcTheme.spacing
    GreetingTopBar(
        personaId = state.personaId,
        greeting = stringResource(R.string.home_greeting, state.firstName),
        unreadNotifications = state.unreadNotifications,
        onNotifications = actions.onNotifications,
        onProfile = actions.onProfile
    )
    if (state.isOffline) {
        Spacer(Modifier.height(spacing.space2))
        OfflineBanner()
    }
    Spacer(Modifier.height(spacing.space4))
    BalanceHeader(state, actions, onToggleBalances)
    Spacer(Modifier.height(spacing.space6))

    SectionHeader(title = stringResource(R.string.home_accounts))
    AccountsCard(state.accounts, state.balancesHidden, actions.onAccount)
    if (state.showNoInvestments) {
        Spacer(Modifier.height(spacing.space4))
        EmptyCard(stringResource(R.string.home_no_investments), actions.onExploreProducts)
    }
    Spacer(Modifier.height(spacing.space6))

    SectionHeader(
        title = stringResource(R.string.home_latest_transactions),
        actionLabel = stringResource(R.string.action_view_all),
        onAction = actions.onViewAllTransactions
    )
    ContentCard(padding = spacing.space4) {
        if (state.latestTransactions.isEmpty()) {
            Text(
                text = stringResource(R.string.home_no_transactions),
                style = AbcTheme.typography.body,
                color = AbcTheme.colors.textMuted
            )
        }
        state.latestTransactions.forEach { item ->
            LatestTransaction(item, state.balancesHidden, actions.onTransaction)
        }
    }
}

/**
 * The sage wallet header with the action pills overlapping its bottom edge by 28dp
 * (DESIGN_SYSTEM.md sections 06 and 10). Text is dark: white fails contrast on the light end of
 * the sage gradient.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BalanceHeader(
    state: HomeUiState.Content,
    actions: HomeActions,
    onToggleBalances: () -> Unit
) {
    val colors = AbcTheme.colors
    val spacing = AbcTheme.spacing
    val overlap = AbcTheme.dimens.actionPillOverlap
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AbcTheme.shapes.xl)
                .background(
                    Brush.linearGradient(listOf(colors.cardSage.start, colors.cardSage.end))
                )
                .padding(start = spacing.space5, end = spacing.space5, top = spacing.space3)
                .padding(bottom = spacing.space5 + overlap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_wallet_balance),
                    style = AbcTheme.typography.small,
                    color = colors.text
                )
                IconButton(
                    onClick = onToggleBalances,
                    modifier = Modifier.size(AbcTheme.dimens.touchTarget)
                ) {
                    Icon(
                        painter = painterResource(
                            if (state.balancesHidden) R.drawable.ic_eye else R.drawable.ic_eye_off
                        ),
                        contentDescription = stringResource(
                            if (state.balancesHidden) {
                                R.string.cd_show_balances
                            } else {
                                R.string.cd_hide_balances
                            }
                        ),
                        tint = colors.text,
                        modifier = Modifier.size(AbcTheme.dimens.iconMedium)
                    )
                }
            }
            BalanceText(
                money = state.walletBalance,
                hidden = state.balancesHidden,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(spacing.space3))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(
                    spacing.space2,
                    Alignment.CenterHorizontally
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                FlowChip(
                    icon = R.drawable.ic_arrow_up,
                    money = state.moneyIn,
                    hidden = state.balancesHidden,
                    incoming = true
                )
                FlowChip(
                    icon = R.drawable.ic_arrow_down,
                    money = -state.moneyOut,
                    hidden = state.balancesHidden,
                    incoming = false
                )
            }
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .pullUp(overlap),
            horizontalArrangement = Arrangement.spacedBy(
                spacing.space2,
                Alignment.CenterHorizontally
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.space2)
        ) {
            ActionPill(
                label = stringResource(R.string.home_action_transfer),
                icon = R.drawable.ic_arrow_up_right,
                onClick = actions.onTransfer
            )
            ActionPill(
                label = stringResource(R.string.home_action_pay_bills),
                icon = R.drawable.ic_receipt,
                onClick = actions.onPayBills
            )
            GridPill(
                contentDescription = stringResource(R.string.cd_more_actions),
                onClick = actions.onMoreActions
            )
        }
    }
}

/**
 * Draws the content [distance] higher and gives that space back, so it overlaps whatever is above.
 * If the pills wrap onto a second row at narrow widths, that row grows downward, not over the header.
 */
private fun Modifier.pullUp(distance: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val offset = distance.roundToPx()
    layout(placeable.width, (placeable.height - offset).coerceAtLeast(0)) {
        placeable.place(0, -offset)
    }
}

/** Money in or out over the last 30 days: the arrow and sign carry the meaning, not colour. */
@Composable
private fun FlowChip(@DrawableRes icon: Int, money: Money, hidden: Boolean, incoming: Boolean) {
    val colors = AbcTheme.colors
    val description = if (hidden) {
        stringResource(if (incoming) R.string.cd_money_in_hidden else R.string.cd_money_out_hidden)
    } else {
        stringResource(
            if (incoming) R.string.cd_money_in else R.string.cd_money_out,
            MoneyFormatter.format(money.absoluteValue())
        )
    }
    val tint = if (incoming) colors.positiveText else colors.text
    Row(
        modifier = Modifier
            .clip(AbcTheme.shapes.full)
            .background(colors.surface)
            .padding(horizontal = AbcTheme.spacing.space3, vertical = AbcTheme.spacing.space1)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(AbcTheme.dimens.iconSmall)
        )
        Spacer(Modifier.width(AbcTheme.spacing.space1))
        Text(
            text = if (hidden) AMOUNT_MASK else MoneyFormatter.formatSigned(money),
            style = AbcTheme.typography.label,
            color = tint
        )
    }
}

@Composable
private fun LatestTransaction(
    item: HomeTransactionItem,
    hidden: Boolean,
    onClick: (String) -> Unit
) {
    val transaction = item.transaction
    val time = DateFormatter.time(item.time)
    val whenText = when (item.day) {
        TransactionDay.TODAY -> stringResource(R.string.transaction_today, time)
        TransactionDay.YESTERDAY -> stringResource(R.string.transaction_yesterday, time)
        TransactionDay.EARLIER -> stringResource(
            R.string.transaction_on_date,
            DateFormatter.shortDate(item.date),
            time
        )
    }
    val subtitle = if (transaction.status == TransactionStatus.PENDING) {
        stringResource(R.string.transaction_pending, whenText)
    } else {
        whenText
    }
    val incoming = transaction.direction == TransactionDirection.CREDIT
    TransactionRow(
        title = transaction.merchant,
        subtitle = subtitle,
        amount = transaction.signedAmount,
        hidden = hidden,
        leading = {
            TransactionLeadingIcon(transaction.category, transaction.merchant, incoming)
        },
        modifier = Modifier.clickable(onClick = { onClick(transaction.id) })
    )
}

@Composable
private fun EmptyCard(message: String, onExploreProducts: () -> Unit) {
    ContentCard(padding = AbcTheme.spacing.space4) {
        Text(text = message, style = AbcTheme.typography.body, color = AbcTheme.colors.text)
        Spacer(Modifier.height(AbcTheme.spacing.space3))
        AbcButton(
            text = stringResource(R.string.home_explore_products),
            onClick = onExploreProducts,
            variant = AbcButtonVariant.Secondary
        )
    }
}

@Composable
private fun HomeError(error: DataError, onRetry: () -> Unit) {
    Spacer(Modifier.height(AbcTheme.spacing.space12))
    ErrorState(
        message = stringResource(
            if (error == DataError.OFFLINE) R.string.state_offline_no_data else R.string.home_error
        ),
        onRetry = onRetry
    )
}

/** Shaped like the loaded screen: top bar, header, then two cards (DESIGN_SYSTEM.md section 06). */
@Composable
private fun HomeSkeleton() {
    val spacing = AbcTheme.spacing
    val dimens = AbcTheme.dimens
    val loading = stringResource(R.string.button_loading)
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(spacing.space4)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(dimens.touchTarget)
        ) {
            SkeletonBlock(Modifier.size(dimens.avatarSmall), shape = AbcTheme.shapes.full)
            Spacer(Modifier.width(spacing.space2))
            SkeletonBlock(
                Modifier
                    .fillMaxWidth(SKELETON_TITLE_FRACTION)
                    .height(dimens.skeletonLine)
            )
        }
        SkeletonBlock(
            Modifier
                .fillMaxWidth()
                .aspectRatio(BANK_CARD_ASPECT_RATIO),
            shape = AbcTheme.shapes.xl
        )
        repeat(SKELETON_ROWS) {
            SkeletonBlock(
                Modifier
                    .fillMaxWidth()
                    .height(dimens.actionPillHeight),
                shape = AbcTheme.shapes.xl
            )
        }
    }
}

private const val SKELETON_TITLE_FRACTION = 0.4f
private const val SKELETON_ROWS = 3

@Composable
private fun HomePreview(state: HomeUiState) {
    AbcTheme {
        HomeScreen(state = state, actions = HomeActions(), onToggleBalances = {}, onRetry = {})
    }
}

@Preview(widthDp = 360, heightDp = 1400)
@Composable
private fun HomeSamantaPreview() = HomePreview(HomePreviewData.samanta())

@Preview(widthDp = 360, heightDp = 1100)
@Composable
private fun HomeMarcusPreview() = HomePreview(HomePreviewData.marcus())

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun HomeHiddenPreview() =
    HomePreview(HomePreviewData.samanta(hidden = true, offline = true))

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun HomeLoadingPreview() = HomePreview(HomeUiState.Loading)

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun HomeErrorPreview() = HomePreview(HomeUiState.Error(DataError.UNKNOWN))
