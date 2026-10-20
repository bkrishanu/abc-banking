package com.abcbank.mobile.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.MpinPad
import com.abcbank.mobile.core.designsystem.component.ProfileAvatar
import com.abcbank.mobile.core.designsystem.component.mpinMessageText
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.security.MpinEntryState
import com.abcbank.mobile.core.security.StepUpMessage
import com.abcbank.mobile.feature.auth.domain.LockIdentity

/**
 * `LockScreen` (DESIGN_SYSTEM.md section 10, no sample): `ProfileAvatar`, "Welcome back, Samanta"
 * in H1, `MpinPad`, Ghost "Not you?". Shown over the app when the session locks.
 */
@Composable
fun LockScreen(
    state: LockUiState,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onNotYou: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = AbcTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AbcTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(spacing.space12))
        val content = state as? LockUiState.Content
        val identity = content?.identity
        if (identity != null) {
            ProfileAvatar(identity.personaId, size = AbcTheme.dimens.avatarLarge)
            Spacer(Modifier.height(spacing.space4))
        }
        Text(
            text = identity?.let { stringResource(R.string.lock_welcome_back, it.firstName) }
                ?: stringResource(R.string.lock_title),
            style = AbcTheme.typography.h1,
            color = AbcTheme.colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(spacing.space2))
        Text(
            text = stringResource(R.string.lock_prompt),
            style = AbcTheme.typography.body,
            color = AbcTheme.colors.textMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(spacing.space8))
        val pad = content?.pad ?: MpinEntryState()
        MpinPad(
            enteredCount = pad.entered,
            length = pad.length,
            onDigit = onDigit,
            onDelete = onDelete,
            enabled = pad.isPadEnabled,
            supportingText = pad.message?.let { mpinMessageText(it) },
            isError = pad.message is StepUpMessage.WrongMpin,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(spacing.space4))
        AbcButton(
            text = stringResource(R.string.lock_not_you),
            onClick = onNotYou,
            variant = AbcButtonVariant.Ghost
        )
        Spacer(Modifier.height(spacing.space6))
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LockScreenPreview() {
    AbcTheme {
        LockScreen(
            state = LockUiState.Content(
                LockIdentity(PersonaId.SAMANTA, "Samanta"),
                MpinEntryState(active = true, entered = 2)
            ),
            onDigit = {},
            onDelete = {},
            onNotYou = {}
        )
    }
}
