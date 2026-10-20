package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.security.StepUpAction
import com.abcbank.mobile.core.security.StepUpMessage
import com.abcbank.mobile.core.security.StepUpState

/**
 * `StepUpSheet` from DESIGN_SYSTEM.md section 06: title "Enter Your MPIN", a reason line, the MPIN
 * pad and Cancel, in a bottom sheet. Driven by a StepUpController's state; shows nothing while
 * closed. No OTP or biometric (PRD section 08).
 */
@Composable
fun StepUpSheet(
    state: StepUpState,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    val action = state.action ?: return
    AbcBottomSheet(onDismiss = onCancel) {
        StepUpContent(state, action, onDigit, onDelete, onCancel)
    }
}

@Composable
private fun StepUpContent(
    state: StepUpState,
    action: StepUpAction,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    val spacing = AbcTheme.spacing
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space4)
            .padding(bottom = spacing.space6)
    ) {
        Text(
            text = stringResource(R.string.step_up_title),
            style = AbcTheme.typography.h2,
            color = AbcTheme.colors.text,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(spacing.space1))
        Text(
            text = reasonText(action),
            style = AbcTheme.typography.body,
            color = AbcTheme.colors.textMuted
        )
        Spacer(Modifier.height(spacing.space6))
        MpinPad(
            enteredCount = state.entered,
            length = state.length,
            onDigit = onDigit,
            onDelete = onDelete,
            enabled = state.isPadEnabled,
            supportingText = state.message?.let { mpinMessageText(it) },
            isError = state.message is StepUpMessage.WrongMpin,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(spacing.space4))
        AbcButton(
            text = stringResource(R.string.action_cancel),
            onClick = onCancel,
            variant = AbcButtonVariant.Ghost
        )
    }
}

@Composable
private fun reasonText(action: StepUpAction): String = when (action) {
    StepUpAction.AddBeneficiary -> stringResource(R.string.step_up_reason_add_beneficiary)
    is StepUpAction.OutboundTransfer ->
        stringResource(R.string.step_up_reason_transfer, MoneyFormatter.format(action.amount))
    StepUpAction.ChangeCardLimits -> stringResource(R.string.step_up_reason_card_limits)
    is StepUpAction.RevealCardDetails -> stringResource(R.string.step_up_reason_reveal_card)
}

/** The pad's message outside login: attempts left after a wrong MPIN, or the lockout countdown. */
@Composable
fun mpinMessageText(message: StepUpMessage): String = when (message) {
    is StepUpMessage.WrongMpin ->
        pluralStringResource(
            R.plurals.mpin_wrong_attempts_left,
            message.attemptsLeft,
            message.attemptsLeft
        )
    is StepUpMessage.LockedOut -> stringResource(
        R.string.mpin_locked_countdown,
        message.secondsLeft
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, heightDp = 700)
@Composable
private fun StepUpSheetPreview() {
    AbcTheme {
        StepUpContent(
            state = StepUpState(
                action = StepUpAction.RevealCardDetails("main-card"),
                entered = 2,
                message = StepUpMessage.WrongMpin(attemptsLeft = 3)
            ),
            action = StepUpAction.RevealCardDetails("main-card"),
            onDigit = {},
            onDelete = {},
            onCancel = {}
        )
    }
}
