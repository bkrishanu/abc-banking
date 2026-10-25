package com.abcbank.mobile.feature.cards.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.AbcConfirmDialog
import com.abcbank.mobile.core.designsystem.component.AbcSliderRow
import com.abcbank.mobile.core.designsystem.component.AbcSwitchRow
import com.abcbank.mobile.core.designsystem.component.BankCardArt
import com.abcbank.mobile.core.designsystem.component.BankCardFace
import com.abcbank.mobile.core.designsystem.component.BankCardView
import com.abcbank.mobile.core.designsystem.component.CardArtPattern
import com.abcbank.mobile.core.designsystem.component.CardTag
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.FrozenCardBanner
import com.abcbank.mobile.core.designsystem.component.LocalBottomBarClearance
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.SkeletonBlock
import com.abcbank.mobile.core.designsystem.component.StatusBadge
import com.abcbank.mobile.core.designsystem.component.StepUpSheet
import com.abcbank.mobile.core.designsystem.component.TitleTopBar
import com.abcbank.mobile.core.designsystem.component.gradient
import com.abcbank.mobile.core.designsystem.theme.AbcMotion
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.BANK_CARD_ASPECT_RATIO
import com.abcbank.mobile.core.model.CardArt
import com.abcbank.mobile.core.model.CardChannels
import com.abcbank.mobile.core.model.CardSecrets
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.feature.cards.domain.CardRules
import java.time.LocalDate

/** Everything the Cards tab can ask its ViewModel to do. */
data class CardsEvents(
    val onSelectCard: (String) -> Unit = {},
    val onToggleDetails: () -> Unit = {},
    val onSetFrozen: (Boolean) -> Unit = {},
    val onChannelsChange: (CardChannels) -> Unit = {},
    val onAtmLimitChange: (Money) -> Unit = {},
    val onRetailLimitChange: (Money) -> Unit = {},
    val onSaveLimits: () -> Unit = {},
    val onAddTravelNotice: (String, LocalDate, LocalDate) -> Unit = { _, _, _ -> },
    val onTravelSheetClosed: () -> Unit = {},
    val onAddToWallet: () -> Unit = {},
    val onStepUpDigit: (Int) -> Unit = {},
    val onStepUpDelete: () -> Unit = {},
    val onStepUpCancel: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val navigation: CardsNavigation = CardsNavigation()
)

/** Where the Cards tab leads outside itself; MainTabs supplies these. */
data class CardsNavigation(
    val onDeposit: () -> Unit = {},
    val onSend: () -> Unit = {},
    val onMore: () -> Unit = {},
    val onQuickTransfer: (String) -> Unit = {},
    val onAddPayee: () -> Unit = {}
)

@Composable
fun CardsRoute(
    navigation: CardsNavigation = CardsNavigation(),
    viewModel: CardsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Revealed details never outlive the screen or survive the app going to the background
    // (AGENTS.md section 05).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onHideDetails() }
    DisposableEffect(Unit) { onDispose { viewModel.onHideDetails() } }
    CardsScreen(
        state = state,
        events = CardsEvents(
            onSelectCard = viewModel::onSelectCard,
            onToggleDetails = viewModel::onToggleDetails,
            onSetFrozen = viewModel::onSetFrozen,
            onChannelsChange = viewModel::onChannelsChange,
            onAtmLimitChange = viewModel::onAtmLimitChange,
            onRetailLimitChange = viewModel::onRetailLimitChange,
            onSaveLimits = viewModel::onSaveLimits,
            onAddTravelNotice = viewModel::onAddTravelNotice,
            onTravelSheetClosed = viewModel::onTravelSheetClosed,
            onAddToWallet = viewModel::onAddToWallet,
            onStepUpDigit = viewModel::onStepUpDigit,
            onStepUpDelete = viewModel::onStepUpDelete,
            onStepUpCancel = viewModel::onStepUpCancel,
            onRetry = viewModel::onRetry,
            navigation = navigation
        )
    )
}

/**
 * The Cards tab (MobileBuild.md 4.1 and 4.2, PRD feature 10): the card stack, then the selected
 * card's controls. Reveal and limit changes go through step-up; freezing is confirmed first.
 */
@Composable
fun CardsScreen(state: CardsUiState, events: CardsEvents, modifier: Modifier = Modifier) {
    val spacing = AbcTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AbcTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.space4)
    ) {
        Spacer(Modifier.height(spacing.space4))
        TitleTopBar(title = stringResource(R.string.cards_title))
        Spacer(Modifier.height(spacing.space6))
        when (state) {
            CardsUiState.Loading -> SkeletonBlock(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(BANK_CARD_ASPECT_RATIO),
                shape = AbcTheme.shapes.lg
            )
            CardsUiState.Empty -> ContentCard {
                Text(
                    stringResource(R.string.cards_empty),
                    style = AbcTheme.typography.body,
                    color = AbcTheme.colors.text
                )
            }
            is CardsUiState.Error -> ErrorState(
                message = stringResource(
                    if (state.error ==
                        DataError.OFFLINE
                    ) {
                        R.string.state_offline_no_data
                    } else {
                        R.string.cards_error
                    }
                ),
                onRetry = events.onRetry
            )
            is CardsUiState.Content -> CardsContent(state, events)
        }
        Spacer(Modifier.height(spacing.space6 + LocalBottomBarClearance.current))
    }
    if (state is CardsUiState.Content) {
        StepUpSheet(
            state = state.stepUp,
            onDigit = events.onStepUpDigit,
            onDelete = events.onStepUpDelete,
            onCancel = events.onStepUpCancel
        )
    }
}

@Composable
private fun CardsContent(state: CardsUiState.Content, events: CardsEvents) {
    val spacing = AbcTheme.spacing
    val card = state.selected.card
    var confirmFreeze by rememberSaveable { mutableStateOf(false) }
    if (state.isOffline) {
        OfflineBanner()
        Spacer(Modifier.height(spacing.space4))
    }
    CardStack(state, events.onSelectCard)
    Spacer(Modifier.height(spacing.space4))
    if (card.frozen) {
        FrozenCardBanner(onUnfreeze = { events.onSetFrozen(false) })
        Spacer(Modifier.height(spacing.space4))
    }
    CardsQuickActions(state, events.navigation)
    Spacer(Modifier.height(spacing.space4))
    AbcButton(
        text = stringResource(
            if (state.revealed !=
                null
            ) {
                R.string.cards_hide_details
            } else {
                R.string.cards_show_details
            }
        ),
        onClick = events.onToggleDetails,
        variant = AbcButtonVariant.Secondary
    )
    state.message?.let { message ->
        Text(
            text = stringResource(
                if (message ==
                    CardsMessage.LIMITS_SAVED
                ) {
                    R.string.cards_limits_saved
                } else {
                    R.string.cards_action_failed
                }
            ),
            style = AbcTheme.typography.small,
            color = if (message ==
                CardsMessage.LIMITS_SAVED
            ) {
                AbcTheme.colors.positiveText
            } else {
                AbcTheme.colors.danger
            },
            modifier = Modifier.padding(top = spacing.space2)
        )
    }
    Spacer(Modifier.height(spacing.space4))
    ContentCard {
        AbcSwitchRow(
            title = stringResource(R.string.cards_freeze),
            subtitle = stringResource(R.string.cards_freeze_subtitle),
            checked = card.frozen,
            onCheckedChange = { freeze ->
                if (freeze) confirmFreeze = true else events.onSetFrozen(false)
            },
            icon = R.drawable.ic_snowflake
        )
    }
    Spacer(Modifier.height(spacing.space4))
    LimitsCard(state, events)
    Spacer(Modifier.height(spacing.space4))
    ChannelsCard(card.channels, events.onChannelsChange)
    Spacer(Modifier.height(spacing.space4))
    TravelNoticesCard(state, events)
    Spacer(Modifier.height(spacing.space4))
    WalletCard(state.walletAdded, events.onAddToWallet)
    if (confirmFreeze) {
        AbcConfirmDialog(
            title = stringResource(R.string.cards_freeze_dialog_title),
            text = stringResource(R.string.cards_freeze_dialog_text),
            confirmLabel = stringResource(R.string.action_freeze),
            cancelLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                confirmFreeze = false
                events.onSetFrozen(true)
            },
            onDismiss = { confirmFreeze = false }
        )
    }
}

/**
 * The selected card in front with its tag; the others peek 24dp below it (DESIGN_SYSTEM.md
 * section 06) with their tag and balance, and come to the front when tapped.
 */
@Composable
private fun CardStack(state: CardsUiState.Content, onSelect: (String) -> Unit) {
    val others = state.cards.filter { it.card.id != state.selected.card.id }
    // The strip must fit a line of Small text, which is far taller than 24dp at 200% font scale.
    val peek = maxOf(
        AbcTheme.dimens.cardPeek,
        with(LocalDensity.current) { AbcTheme.typography.small.lineHeight.toDp() } +
            AbcTheme.spacing.space1 * 2
    )
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            others.forEach { item ->
                PeekingCard(item = item, hidden = state.balancesHidden, onClick = {
                    onSelect(item.card.id)
                })
            }
            Crossfade(
                targetState = state.selected,
                animationSpec = tween(AbcMotion.SCREEN_MS),
                label = "cardSwap"
            ) { item ->
                Box {
                    BankCardView(
                        face = item.face(state.revealed),
                        balanceHidden = state.balancesHidden,
                        modifier = Modifier.fillMaxWidth()
                    )
                    item.card.tag?.let { tag ->
                        CardTag(
                            tag,
                            Modifier.align(Alignment.TopEnd).offset(y = -AbcTheme.spacing.space2)
                        )
                    }
                }
            }
        }
    ) { measurables, constraints ->
        // The front card can grow at large font scales, so the others are placed from its bottom
        // edge: each shows a strip [peek] tall, the deepest lowest.
        val loose = constraints.copy(minHeight = 0)
        val front = measurables.last().measure(loose)
        val behind = measurables.dropLast(1).map { it.measure(loose) }
        val peekPx = peek.roundToPx()
        layout(front.width, front.height + peekPx * behind.size) {
            behind.forEachIndexed { index, card ->
                val depth = behind.size - index
                card.place(0, front.height - card.height + peekPx * depth)
            }
            front.place(0, 0)
        }
    }
}

@Composable
private fun PeekingCard(
    item: CardItem,
    hidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val light = item.card.art == CardArt.PEACH
    val ink = if (light) AbcTheme.colors.text else AbcTheme.colors.onPrimary
    BankCardArt(
        gradient = item.card.art.gradient(),
        pattern = if (light) CardArtPattern.Ribbons else CardArtPattern.Waves,
        scrim = !light,
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = AbcTheme.spacing.space4, vertical = AbcTheme.spacing.space1),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.card.tag.orEmpty(),
                style = AbcTheme.typography.small,
                color = ink,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Text(
                text = if (hidden) AMOUNT_MASK else MoneyFormatter.format(item.balance),
                style = AbcTheme.typography.small,
                color = ink
            )
        }
    }
}

@Composable
private fun LimitsCard(state: CardsUiState.Content, events: CardsEvents) {
    val card = state.selected.card
    ContentCard {
        SectionTitle(stringResource(R.string.cards_limits))
        LimitSlider(
            label = stringResource(
                if (state.selected.isCredit) {
                    R.string.cards_limit_cash_advance
                } else {
                    R.string.cards_limit_atm
                }
            ),
            value = state.limits.atm,
            max = card.limits.atmMax,
            onChange = events.onAtmLimitChange
        )
        val retail = state.limits.retail
        val retailMax = card.limits.retailMax
        if (retail != null && retailMax != null) {
            LimitSlider(
                stringResource(R.string.cards_limit_retail),
                retail,
                retailMax,
                events.onRetailLimitChange
            )
        } else {
            Text(
                text = stringResource(
                    R.string.cards_limit_retail_credit,
                    MoneyFormatter.format(state.selected.balance)
                ),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted,
                modifier = Modifier.padding(vertical = AbcTheme.spacing.space2)
            )
        }
        Spacer(Modifier.height(AbcTheme.spacing.space2))
        AbcButton(
            text = stringResource(R.string.cards_save_limits),
            onClick = events.onSaveLimits,
            enabled = state.limitsChanged
        )
    }
}

/** Snaps to the card's step ($100, or $500 for large limits) between one step and the maximum. */
@Composable
private fun LimitSlider(label: String, value: Money, max: Money, onChange: (Money) -> Unit) {
    val step = CardRules.step(max).cents
    val stops = ((max.cents - step) / step).toInt()
    AbcSliderRow(
        label = label,
        valueText = stringResource(
            R.string.cards_limit_value,
            MoneyFormatter.format(value),
            MoneyFormatter.format(max)
        ),
        value = value.cents.toFloat(),
        onValueChange = { raw ->
            val snapped = (Math.round(raw / step) * step).coerceIn(step, max.cents)
            if (snapped != value.cents) onChange(Money(snapped))
        },
        valueRange = step.toFloat()..max.cents.toFloat(),
        steps = (stops - 1).coerceAtLeast(0)
    )
}

@Composable
private fun ChannelsCard(channels: CardChannels, onChange: (CardChannels) -> Unit) {
    ContentCard {
        SectionTitle(stringResource(R.string.cards_channels))
        AbcSwitchRow(
            title = stringResource(R.string.cards_channel_international),
            checked = channels.international,
            onCheckedChange = { onChange(channels.copy(international = it)) },
            icon = R.drawable.ic_globe
        )
        AbcSwitchRow(
            title = stringResource(R.string.cards_channel_online),
            checked = channels.online,
            onCheckedChange = { onChange(channels.copy(online = it)) },
            icon = R.drawable.ic_monitor_smartphone
        )
        AbcSwitchRow(
            title = stringResource(R.string.cards_channel_contactless),
            checked = channels.contactless,
            onCheckedChange = { onChange(channels.copy(contactless = it)) },
            icon = R.drawable.ic_nfc
        )
    }
}

@Composable
private fun TravelNoticesCard(state: CardsUiState.Content, events: CardsEvents) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val notices = state.selected.notices
    ContentCard {
        SectionTitle(stringResource(R.string.cards_travel_notices))
        if (notices.isEmpty()) {
            Text(
                stringResource(R.string.cards_no_travel_notices),
                style = AbcTheme.typography.body,
                color = AbcTheme.colors.textMuted
            )
        }
        notices.forEachIndexed { index, notice ->
            if (index > 0) HorizontalDivider(color = AbcTheme.colors.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AbcTheme.spacing.space3)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plane),
                    contentDescription = null,
                    tint = AbcTheme.colors.text,
                    modifier = Modifier.size(AbcTheme.dimens.iconMedium)
                )
                Spacer(Modifier.width(AbcTheme.spacing.space3))
                Column(Modifier.weight(1f)) {
                    Text(
                        notice.destination,
                        style = AbcTheme.typography.body,
                        color = AbcTheme.colors.text
                    )
                    Text(
                        text = stringResource(
                            R.string.cards_travel_dates,
                            DateFormatter.shortDate(notice.start),
                            DateFormatter.fullDate(notice.end)
                        ),
                        style = AbcTheme.typography.small,
                        color = AbcTheme.colors.textMuted
                    )
                }
                if (notice.active) StatusBadge(stringResource(R.string.cards_travel_active))
            }
        }
        Spacer(Modifier.height(AbcTheme.spacing.space3))
        AbcButton(
            text = stringResource(R.string.cards_add_travel_notice),
            onClick = { showSheet = true },
            variant = AbcButtonVariant.Secondary
        )
    }
    if (showSheet) {
        TravelNoticeSheet(
            error = state.noticeError,
            noticesAdded = state.noticesAdded,
            onSave = events.onAddTravelNotice,
            onDismiss = {
                showSheet = false
                events.onTravelSheetClosed()
            }
        )
    }
}

@Composable
private fun WalletCard(added: Boolean, onAdd: () -> Unit) {
    if (!added) {
        AbcButton(
            text = stringResource(R.string.cards_add_to_wallet),
            onClick = onAdd,
            variant = AbcButtonVariant.Secondary
        )
        return
    }
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = AbcTheme.colors.positiveText,
                modifier = Modifier.size(AbcTheme.dimens.iconMedium)
            )
            Spacer(Modifier.width(AbcTheme.spacing.space2))
            Text(
                stringResource(R.string.cards_added_to_wallet),
                style = AbcTheme.typography.body,
                color = AbcTheme.colors.text
            )
        }
        Text(
            text = stringResource(R.string.cards_wallet_demo),
            style = AbcTheme.typography.small,
            color = AbcTheme.colors.textMuted,
            modifier = Modifier.padding(top = AbcTheme.spacing.space1)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = AbcTheme.typography.h3,
        color = AbcTheme.colors.text,
        modifier = Modifier
            .padding(bottom = AbcTheme.spacing.space2)
            .semantics { heading() }
    )
}

private fun CardItem.face(revealed: CardSecrets?) = BankCardFace(
    art = card.art,
    network = card.network,
    holderName = card.holderName,
    last4 = card.last4,
    expiry = card.expiry,
    balance = balance,
    frozen = card.frozen,
    secrets = revealed
)

private const val AMOUNT_MASK = "••••"

@Preview(widthDp = 360, heightDp = 1800)
@Composable
private fun CardsScreenPreview() {
    AbcTheme { CardsScreen(CardsPreviewData.samanta(), CardsEvents()) }
}
