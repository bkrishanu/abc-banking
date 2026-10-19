package com.abcbank.mobile.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.component.AbcTextField
import com.abcbank.mobile.core.designsystem.component.MpinPad
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

@Composable
fun LoginRoute(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val loggedIn = (state as? LoginUiState.Content)?.isLoggedIn == true
    LaunchedEffect(loggedIn) {
        if (loggedIn) onLoggedIn()
    }
    LoginScreen(
        state = state,
        onBack = onBack,
        onCustomerIdChange = viewModel::onCustomerIdChange,
        onCustomerIdDone = viewModel::onCustomerIdDone,
        onMpinDigit = viewModel::onMpinDigit,
        onMpinDelete = viewModel::onMpinDelete
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onBack: () -> Unit,
    onCustomerIdChange: (String) -> Unit,
    onCustomerIdDone: () -> Unit,
    onMpinDigit: (Int) -> Unit,
    onMpinDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AbcTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(AbcTheme.dimens.touchTarget)) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.cd_back),
                tint = AbcTheme.colors.text,
                modifier = Modifier.size(AbcTheme.dimens.iconLarge)
            )
        }
        when (state) {
            // Restoring a saved lockout takes a single local read; a skeleton would only flash.
            LoginUiState.Loading -> Box(Modifier.fillMaxSize())
            is LoginUiState.Content -> LoginContent(
                state = state,
                onCustomerIdChange = onCustomerIdChange,
                onCustomerIdDone = onCustomerIdDone,
                onMpinDigit = onMpinDigit,
                onMpinDelete = onMpinDelete
            )
        }
    }
}

@Composable
private fun LoginContent(
    state: LoginUiState.Content,
    onCustomerIdChange: (String) -> Unit,
    onCustomerIdDone: () -> Unit,
    onMpinDigit: (Int) -> Unit,
    onMpinDelete: () -> Unit
) {
    val spacing = AbcTheme.spacing
    val focusManager = LocalFocusManager.current
    // Once the ID is recognised, hide the keyboard so the MPIN pad is in view.
    LaunchedEffect(state.isMpinEnabled) {
        if (state.isMpinEnabled) focusManager.clearFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.space4)
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = AbcTheme.typography.h1,
            color = AbcTheme.colors.text,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(spacing.space6))
        AbcTextField(
            value = state.customerId,
            onValueChange = onCustomerIdChange,
            label = stringResource(R.string.login_customer_id_label),
            placeholder = stringResource(R.string.login_customer_id_placeholder),
            errorText = state.customerIdError?.let { customerIdErrorText(it) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                onCustomerIdDone()
                focusManager.clearFocus()
            }),
            modifier = Modifier.fillMaxWidth()
        )
        if (state.replacesDataOf != null && state.customerIdError == null) {
            Spacer(Modifier.height(spacing.space1))
            Text(
                text = stringResource(R.string.login_replaces_data, state.replacesDataOf),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.warning
            )
        }
        Spacer(Modifier.height(spacing.space6))
        Text(
            text = stringResource(R.string.login_mpin_label),
            style = AbcTheme.typography.h3,
            color = AbcTheme.colors.text
        )
        Spacer(Modifier.height(spacing.space4))
        MpinPad(
            enteredCount = state.mpinEntered,
            length = state.mpinLength,
            onDigit = onMpinDigit,
            onDelete = onMpinDelete,
            enabled = state.isMpinEnabled,
            supportingText = state.mpinMessage?.let { mpinMessageText(it) },
            isError = state.mpinMessage is MpinMessage.WrongMpin ||
                state.mpinMessage is MpinMessage.SetupFailed,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(spacing.space6))
    }
}

@Composable
private fun customerIdErrorText(error: CustomerIdError): String = when (error) {
    CustomerIdError.WrongFormat -> stringResource(R.string.login_customer_id_error_format)
    CustomerIdError.Unknown -> stringResource(R.string.login_customer_id_error_unknown)
}

@Composable
private fun mpinMessageText(message: MpinMessage): String = when (message) {
    is MpinMessage.WrongMpin -> pluralStringResource(
        R.plurals.mpin_wrong_attempts_left,
        message.attemptsLeft,
        message.attemptsLeft
    )
    is MpinMessage.LockedOut -> stringResource(R.string.mpin_locked_countdown, message.secondsLeft)
    MpinMessage.SettingUp -> stringResource(R.string.login_setting_up)
    MpinMessage.SetupFailed -> stringResource(R.string.login_setup_failed)
}

private fun previewContent(
    customerId: String = "SAMJOH01",
    customerIdError: CustomerIdError? = null,
    replacesDataOf: String? = null,
    isMpinEnabled: Boolean = true,
    mpinEntered: Int = 0,
    mpinMessage: MpinMessage? = null
) = LoginUiState.Content(
    customerId = customerId,
    customerIdError = customerIdError,
    replacesDataOf = replacesDataOf,
    isMpinEnabled = isMpinEnabled,
    mpinEntered = mpinEntered,
    mpinLength = 6,
    mpinMessage = mpinMessage,
    isLoggedIn = false
)

@Composable
private fun LoginPreview(state: LoginUiState) {
    AbcTheme {
        LoginScreen(
            state = state,
            onBack = {},
            onCustomerIdChange = {},
            onCustomerIdDone = {},
            onMpinDigit = {},
            onMpinDelete = {}
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LoginEmptyPreview() =
    LoginPreview(previewContent(customerId = "", isMpinEnabled = false))

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LoginEnteringMpinPreview() = LoginPreview(previewContent(mpinEntered = 3))

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LoginUnknownIdPreview() = LoginPreview(
    previewContent(
        customerId = "ABCDEF12",
        customerIdError = CustomerIdError.Unknown,
        isMpinEnabled = false
    )
)

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LoginWrongMpinPreview() =
    LoginPreview(previewContent(mpinMessage = MpinMessage.WrongMpin(attemptsLeft = 3)))

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LoginLockedPreview() = LoginPreview(
    previewContent(isMpinEnabled = false, mpinMessage = MpinMessage.LockedOut(secondsLeft = 24))
)

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LoginReplacesDataPreview() = LoginPreview(
    previewContent(customerId = "MARLEE01", replacesDataOf = "Samanta Johns", mpinEntered = 2)
)

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LoginSettingUpPreview() =
    LoginPreview(previewContent(isMpinEnabled = false, mpinMessage = MpinMessage.SettingUp))

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LoginSetupFailedPreview() =
    LoginPreview(previewContent(mpinMessage = MpinMessage.SetupFailed))

@Preview(widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun LoginLargeFontPreview() = LoginPreview(previewContent(mpinEntered = 2))
