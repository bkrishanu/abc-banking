package com.abcbank.mobile.feature.home.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.common.LowBalanceRule
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.AdaptiveRow
import com.abcbank.mobile.core.designsystem.component.BalanceText
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.GreetingTopBar
import com.abcbank.mobile.core.designsystem.component.LocalBottomBarClearance
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.QuickActionButton
import com.abcbank.mobile.core.designsystem.component.QuickActionRow
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
    HomeScreen(
        state = state,
        actions = actions,
        onToggleBalances = viewModel::onToggleBalances,
        onRetry = viewModel::onRetry
    )
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
    if (state.balancesHidden) {
        Spacer(Modifier.height(spacing.space3))
        HiddenBalancesBanner(onShow = onToggleBalances)
    }
    state.travelNotice?.let { notice ->
        Spacer(Modifier.height(spacing.space3))
        TravelNoticeBanner(notice, onManage = actions.onCards)
    }
    Spacer(Modifier.height(spacing.space4))
    BalanceHeader(state, actions, onToggleBalances)
    Spacer(Modifier.height(spacing.space6))

    HomeFeatures(actions)
    Spacer(Modifier.height(spacing.space6))

    SectionHeader(title = stringResource(R.string.home_accounts))
    AccountCards(state.accounts, state.balancesHidden, actions.onAccount)
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
 * The sage wallet header, then the quick actions below it (DESIGN_SYSTEM.md sections 06 and 10).
 * Text is white on the deep teal card-sage gradient.
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
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AbcTheme.shapes.xl)
                .background(
                    Brush.linearGradient(listOf(colors.cardSage.start, colors.cardSage.end))
                )
                .padding(start = spacing.space5, end = spacing.space2, top = spacing.space2)
                .padding(bottom = spacing.space5)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_wallet_balance),
                    style = AbcTheme.typography.small,
                    color = colors.onPrimary,
                    modifier = Modifier.weight(1f)
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
                        tint = colors.onPrimary,
                        modifier = Modifier.size(AbcTheme.dimens.iconMedium)
                    )
                }
            }
            Column(Modifier.padding(end = spacing.space3)) {
                BalanceText(
                    money = state.walletBalance,
                    hidden = state.balancesHidden,
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.onPrimary,
                    textAlign = TextAlign.Start
                )
                if (state.walletName != null && state.walletLast4 != null) {
                    Text(
                        text = stringResource(
                            R.string.account_name_last4,
                            state.walletName,
                            state.walletLast4
                        ),
                        style = AbcTheme.typography.small,
                        color = colors.onPrimary
                    )
                }
                // The warning says the balance is low, so it hides with the balances.
                if (state.walletIsLow && !state.balancesHidden) {
                    Spacer(Modifier.height(spacing.space3))
                    LowBalanceWarning()
                }
                Spacer(Modifier.height(spacing.space3))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
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
        }
        Spacer(Modifier.height(spacing.space4))
        QuickActionRow {
            QuickActionButton(
                label = stringResource(R.string.quick_action_send),
                icon = R.drawable.ic_send,
                onClick = actions.onTransfer,
                emphasized = true,
                offline = state.isOffline
            )
            QuickActionButton(
                label = stringResource(R.string.home_action_pay_bills),
                icon = R.drawable.ic_receipt,
                onClick = actions.onPayBills,
                offline = state.isOffline
            )
            QuickActionButton(
                label = stringResource(R.string.quick_action_deposit),
                icon = R.drawable.ic_landmark,
                onClick = actions.onDepositCheck,
                offline = state.isOffline
            )
            QuickActionButton(
                label = stringResource(R.string.quick_action_more),
                icon = R.drawable.ic_layout_grid,
                onClick = actions.onMoreActions
            )
        }
    }
}

/** Inside the balance card when the wallet account is under the threshold (Stitch, adopted). */
@Composable
private fun LowBalanceWarning() {
    val colors = AbcTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(AbcTheme.shapes.md)
            .background(colors.warningTint)
            .padding(horizontal = AbcTheme.spacing.space3, vertical = AbcTheme.spacing.space2)
    ) {
        Box(
            Modifier
                .size(AbcTheme.dimens.statusDot)
                .clip(AbcTheme.shapes.full)
                .background(colors.warning)
        )
        Spacer(Modifier.width(AbcTheme.spacing.space2))
        Text(
            text = stringResource(
                R.string.home_low_balance,
                MoneyFormatter.format(LowBalanceRule.THRESHOLD)
            ),
            style = AbcTheme.typography.small,
            color = colors.text
        )
    }
}

/** While balances are hidden: says so, with Show to bring them back (Stitch Discreet Mode). */
@Composable
private fun HiddenBalancesBanner(onShow: () -> Unit) {
    val colors = AbcTheme.colors
    AdaptiveRow(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AbcTheme.shapes.xl)
            .background(colors.surface)
            .padding(start = AbcTheme.spacing.space4, end = AbcTheme.spacing.space2)
            .padding(vertical = AbcTheme.spacing.space2),
        start = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_eye_off),
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(AbcTheme.dimens.iconMedium)
                )
                Spacer(Modifier.width(AbcTheme.spacing.space3))
                Column(Modifier.semantics(mergeDescendants = true) {}) {
                    Text(
                        text = stringResource(R.string.home_hidden_title),
                        style = AbcTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.text
                    )
                    Text(
                        text = stringResource(R.string.home_hidden_text),
                        style = AbcTheme.typography.small,
                        color = colors.textMuted
                    )
                }
            }
        }
    ) {
        PillAction(stringResource(R.string.action_show), onShow, onDark = false)
    }
}

/** The active travel notice, in the deep teal band from Stitch, with Manage (the Cards tab). */
@Composable
private fun TravelNoticeBanner(notice: HomeTravelNotice, onManage: () -> Unit) {
    val colors = AbcTheme.colors
    val dates = stringResource(
        R.string.home_travel_dates,
        DateFormatter.shortDate(notice.start),
        DateFormatter.shortDate(notice.end)
    )
    AdaptiveRow(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AbcTheme.shapes.xl)
            .background(colors.primary)
            .padding(start = AbcTheme.spacing.space4, end = AbcTheme.spacing.space2)
            .padding(vertical = AbcTheme.spacing.space2),
        start = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_plane),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(AbcTheme.dimens.iconMedium)
                )
                Spacer(Modifier.width(AbcTheme.spacing.space3))
                Column(Modifier.semantics(mergeDescendants = true) {}) {
                    Text(
                        text = stringResource(R.string.home_travel_title),
                        style = AbcTheme.typography.label,
                        color = colors.onPrimary
                    )
                    Text(
                        text = notice.destination,
                        style = AbcTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.onPrimary
                    )
                    Text(text = dates, style = AbcTheme.typography.small, color = colors.onPrimary)
                }
            }
        }
    ) {
        PillAction(stringResource(R.string.action_manage), onManage, onDark = true)
    }
}

/** A compact pill for a banner's one action, inside a 48dp touch target. */
@Composable
private fun PillAction(label: String, onClick: () -> Unit, onDark: Boolean) {
    val colors = AbcTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = AbcTheme.dimens.touchTarget)
            .clip(AbcTheme.shapes.full)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(AbcTheme.spacing.space1)
    ) {
        Text(
            text = label,
            style = AbcTheme.typography.small.copy(fontWeight = FontWeight.SemiBold),
            color = if (onDark) colors.primary else colors.onPrimary,
            modifier = Modifier
                .clip(AbcTheme.shapes.full)
                .background(if (onDark) colors.surface else colors.primary)
                .padding(horizontal = AbcTheme.spacing.space4, vertical = AbcTheme.spacing.space2)
        )
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

@Preview(widthDp = 360, heightDp = 1400)
@Composable
private fun HomeGracePreview() = HomePreview(HomePreviewData.grace())

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
