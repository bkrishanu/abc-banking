package com.abcbank.mobile.feature.support.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DateFormatter
import com.abcbank.mobile.core.designsystem.component.AbcTextField
import com.abcbank.mobile.core.designsystem.component.BadgeTone
import com.abcbank.mobile.core.designsystem.component.ChatBubble
import com.abcbank.mobile.core.designsystem.component.ChoiceChip
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.FlowScaffold
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.SectionHeader
import com.abcbank.mobile.core.designsystem.component.SkeletonBlock
import com.abcbank.mobile.core.designsystem.component.StatusBadge
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.support.domain.CaseStatus
import com.abcbank.mobile.feature.support.domain.Faq
import com.abcbank.mobile.feature.support.domain.Party
import com.abcbank.mobile.feature.support.domain.SupportCase
import java.time.ZoneId

data class SupportEvents(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onChat: () -> Unit = {},
    val onDispute: () -> Unit = {},
    val onOpenCase: (String) -> Unit = {},
    val onInputChange: (String) -> Unit = {},
    val onAsk: (String?) -> Unit = {}
)

/**
 * Help and Support (MobileBuild.md 6): chat with the support bot, dispute a transaction, the
 * customer's cases, and common questions that open in place.
 */
@Composable
fun SupportScreen(state: SupportUiState, events: SupportEvents, modifier: Modifier = Modifier) {
    val spacing = AbcTheme.spacing
    FlowScaffold(
        step = null,
        totalSteps = null,
        title = stringResource(R.string.support_title),
        onBack = events.onBack,
        modifier = modifier
    ) {
        when (state) {
            SupportUiState.Loading -> SkeletonBlock(
                Modifier
                    .fillMaxWidth()
                    .height(AbcTheme.dimens.avatarLarge * 2),
                shape = AbcTheme.shapes.xl
            )
            is SupportUiState.Error -> ErrorState(
                message = stringResource(
                    if (state.error ==
                        DataError.OFFLINE
                    ) {
                        R.string.state_offline_no_data
                    } else {
                        R.string.support_error
                    }
                ),
                onRetry = events.onRetry
            )
            is SupportUiState.Content -> {
                if (state.isOffline) {
                    OfflineBanner()
                    Spacer(Modifier.height(spacing.space4))
                }
                ContentCard(padding = spacing.space2) {
                    LinkRow(
                        R.drawable.ic_message_circle,
                        stringResource(R.string.support_chat),
                        stringResource(R.string.support_chat_hint),
                        events.onChat
                    )
                    HorizontalDivider(color = AbcTheme.colors.border)
                    LinkRow(
                        R.drawable.ic_receipt,
                        stringResource(R.string.support_dispute),
                        stringResource(R.string.support_dispute_hint),
                        events.onDispute
                    )
                }
                Spacer(Modifier.height(spacing.space4))
                SectionHeader(title = stringResource(R.string.support_cases))
                if (state.cases.isEmpty()) {
                    Text(
                        stringResource(R.string.support_no_cases),
                        style = AbcTheme.typography.body,
                        color = AbcTheme.colors.textMuted
                    )
                } else {
                    ContentCard(padding = spacing.space2) {
                        state.cases.forEachIndexed { index, case ->
                            if (index > 0) HorizontalDivider(color = AbcTheme.colors.border)
                            CaseRow(case) { events.onOpenCase(case.id) }
                        }
                    }
                }
                Spacer(Modifier.height(spacing.space4))
                SectionHeader(title = stringResource(R.string.support_faqs))
                if (state.faqs.isNotEmpty()) {
                    ContentCard(padding = spacing.space2) {
                        state.faqs.forEachIndexed { index, faq ->
                            if (index > 0) HorizontalDivider(color = AbcTheme.colors.border)
                            FaqRow(faq)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkRow(icon: Int, title: String, detail: String, onClick: () -> Unit) {
    val spacing = AbcTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AbcTheme.dimens.touchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(spacing.space2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AbcTheme.dimens.iconCircle)
                .clip(AbcTheme.shapes.full)
                .background(AbcTheme.colors.surfaceMuted)
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = AbcTheme.colors.text,
                modifier = Modifier.size(AbcTheme.dimens.iconMedium)
            )
        }
        Spacer(Modifier.width(spacing.space3))
        Column(Modifier.weight(1f)) {
            Text(title, style = AbcTheme.typography.body, color = AbcTheme.colors.text)
            Text(detail, style = AbcTheme.typography.small, color = AbcTheme.colors.textMuted)
        }
    }
}

@Composable
private fun CaseRow(case: SupportCase, onClick: () -> Unit) {
    val spacing = AbcTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space1)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                case.topic,
                style = AbcTheme.typography.body,
                color = AbcTheme.colors.text,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(spacing.space2))
            StatusBadge(
                statusLabel(case.status),
                tone = if (case.status ==
                    CaseStatus.RESOLVED
                ) {
                    BadgeTone.Neutral
                } else {
                    BadgeTone.Warning
                }
            )
        }
        case.messages.lastOrNull()?.let {
            Text(
                it.body,
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FaqRow(faq: Faq) {
    var open by rememberSaveable(faq.id) { mutableStateOf(false) }
    val spacing = AbcTheme.spacing
    val stateText = stringResource(if (open) R.string.cd_expanded else R.string.cd_collapsed)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AbcTheme.dimens.touchTarget)
            .clickable(role = Role.Button) { open = !open }
            .semantics { stateDescription = stateText }
            .padding(spacing.space2)
            .animateContentSize()
    ) {
        Text(faq.question, style = AbcTheme.typography.body, color = AbcTheme.colors.text)
        if (open) {
            Spacer(Modifier.height(spacing.space1))
            Text(faq.answer, style = AbcTheme.typography.small, color = AbcTheme.colors.textMuted)
        }
    }
}

@Composable
private fun statusLabel(status: CaseStatus): String = stringResource(
    when (status) {
        CaseStatus.OPEN -> R.string.support_status_open
        CaseStatus.IN_REVIEW -> R.string.support_status_in_review
        CaseStatus.RESOLVED -> R.string.support_status_resolved
    }
)

private const val SUGGESTIONS = 4

/**
 * The support bot (MobileBuild.md 6): answers from the FAQs; a question it cannot match goes to
 * the team as a case with the scripted hand-off reply.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupportChatScreen(state: SupportUiState, events: SupportEvents, modifier: Modifier = Modifier) {
    val content = state as? SupportUiState.Content
    val chat = content?.chat ?: ChatState()
    val spacing = AbcTheme.spacing
    FlowScaffold(
        step = null,
        totalSteps = null,
        title = stringResource(R.string.support_chat),
        onBack = events.onBack,
        modifier = modifier,
        bottom = {
            Row(verticalAlignment = Alignment.Bottom) {
                AbcTextField(
                    value = chat.input,
                    onValueChange = events.onInputChange,
                    label = stringResource(R.string.support_question),
                    placeholder = stringResource(R.string.support_question_placeholder),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { events.onAsk(null) }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(spacing.space2))
                IconButton(
                    onClick = { events.onAsk(null) },
                    enabled = chat.input.isNotBlank() && !chat.waiting,
                    modifier = Modifier
                        .size(AbcTheme.dimens.textFieldHeight)
                        .clip(AbcTheme.shapes.sm)
                        .background(AbcTheme.colors.primary)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_send),
                        contentDescription = stringResource(R.string.cd_send),
                        tint = AbcTheme.colors.onPrimary,
                        modifier = Modifier.size(AbcTheme.dimens.iconMedium)
                    )
                }
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            ChatBubble(stringResource(R.string.support_greeting), fromCustomer = false)
            chat.lines.forEach { line ->
                when (line) {
                    is ChatLine.Customer -> ChatBubble(line.text, fromCustomer = true)
                    is ChatLine.Answer -> ChatBubble(
                        line.faq.answer,
                        fromCustomer = false,
                        caption = line.faq.question
                    )
                    is ChatLine.HandedOff -> ChatBubble(
                        line.reply,
                        fromCustomer = false,
                        caption = stringResource(R.string.support_case_opened)
                    )
                    ChatLine.Failed -> ChatBubble(
                        stringResource(R.string.support_failed),
                        fromCustomer = false
                    )
                }
            }
            if (chat.waiting) {
                ChatBubble(
                    stringResource(R.string.support_typing),
                    fromCustomer = false
                )
            }
        }
        if (chat.lines.isEmpty() && content != null && content.faqs.isNotEmpty()) {
            Spacer(Modifier.height(spacing.space4))
            Text(
                stringResource(R.string.support_suggestions),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted
            )
            Spacer(Modifier.height(spacing.space2))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                content.faqs.take(SUGGESTIONS).forEach { faq ->
                    ChoiceChip(label = faq.question, selected = false, onClick = {
                        events.onAsk(faq.question)
                    })
                }
            }
        }
    }
}

/** A case's messages as a thread, with its status. */
@Composable
fun SupportCaseScreen(
    state: SupportUiState,
    events: SupportEvents,
    modifier: Modifier = Modifier,
    zone: ZoneId
) {
    val case = (state as? SupportUiState.Content)?.selectedCase
    val spacing = AbcTheme.spacing
    FlowScaffold(
        step = null,
        totalSteps = null,
        title = case?.topic.orEmpty(),
        onBack = events.onBack,
        modifier = modifier
    ) {
        if (case == null) return@FlowScaffold
        StatusBadge(
            statusLabel(case.status),
            tone = if (case.status ==
                CaseStatus.RESOLVED
            ) {
                BadgeTone.Neutral
            } else {
                BadgeTone.Warning
            }
        )
        Spacer(Modifier.height(spacing.space4))
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            case.messages.forEach { message ->
                val local = message.sentAt.atZone(zone)
                ChatBubble(
                    text = message.body,
                    fromCustomer = message.from == Party.CUSTOMER,
                    caption = stringResource(
                        R.string.support_message_time,
                        DateFormatter.shortDate(local.toLocalDate()),
                        DateFormatter.time(local.toLocalTime())
                    )
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 1000)
@Composable
private fun SupportPreview() {
    AbcTheme { SupportScreen(SupportPreviewData.samanta(), SupportEvents()) }
}
