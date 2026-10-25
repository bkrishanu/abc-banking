package com.abcbank.mobile.feature.payments.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Where the flow goes between steps; the nav graph supplies these. */
data class TransferNavigation(
    val onBack: () -> Unit,
    val onAddPayee: () -> Unit,
    val onAmount: () -> Unit,
    val onSpeed: () -> Unit,
    val onReview: () -> Unit,
    val onReceipt: () -> Unit,
    val onViewTransaction: (String) -> Unit,
    val onDone: () -> Unit,
    /** Back to the amount step from Review. */
    val onEdit: () -> Unit = {},
    /** Leave the flow without sending. */
    val onCancel: () -> Unit = {}
)

@Composable
private fun rememberEvents(
    viewModel: TransferViewModel,
    navigation: TransferNavigation,
    onContinue: () -> Unit
): TransferEvents = remember(viewModel, navigation, onContinue) {
    TransferEvents(
        onBack = navigation.onBack,
        onRetry = viewModel::onRetry,
        onSendTo = {
            viewModel.onSendTo(it)
            navigation.onAmount()
        },
        onMoveBetweenAccounts = {
            viewModel.onMoveBetweenAccounts()
            navigation.onAmount()
        },
        onAddPayee = navigation.onAddPayee,
        onPayeeNameChange = viewModel::onPayeeNameChange,
        onPayeeRoutingChange = viewModel::onPayeeRoutingChange,
        onPayeeAccountChange = viewModel::onPayeeAccountChange,
        onSavePayee = viewModel::onSavePayee,
        onSelectSource = viewModel::onSelectSource,
        onSelectDestination = viewModel::onSelectDestination,
        onSwapAccounts = viewModel::onSwapAccounts,
        onKey = viewModel::onKey,
        onDeleteKey = viewModel::onDeleteKey,
        onMemoChange = viewModel::onMemoChange,
        onCategorySelect = viewModel::onCategorySelect,
        onSpeedSelect = viewModel::onSpeedSelect,
        onContinue = onContinue,
        onConfirm = viewModel::onConfirm,
        onEdit = navigation.onEdit,
        onCancel = navigation.onCancel,
        onViewTransaction = navigation.onViewTransaction,
        onDone = navigation.onDone,
        onStepUpDigit = viewModel::onStepUpDigit,
        onStepUpDelete = viewModel::onStepUpDelete,
        onStepUpCancel = viewModel::onStepUpCancel
    )
}

@Composable
fun TransferRecipientRoute(viewModel: TransferViewModel, navigation: TransferNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransferRecipientScreen(state, rememberEvents(viewModel, navigation, navigation.onAmount))
}

@Composable
fun AddPayeeRoute(viewModel: TransferViewModel, navigation: TransferNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // A saved payee is selected in the draft; move on to the amount.
    val added = (state as? TransferUiState.Content)?.payee?.added ?: 0
    val initial = remember { added }
    LaunchedEffect(added) { if (added > initial) navigation.onAmount() }
    AddPayeeScreen(state, rememberEvents(viewModel, navigation, navigation.onAmount))
}

@Composable
fun TransferAmountRoute(viewModel: TransferViewModel, navigation: TransferNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val next = if ((state as? TransferUiState.Content)?.mode ==
        TransferMode.MOVE
    ) {
        navigation.onReview
    } else {
        navigation.onSpeed
    }
    TransferAmountScreen(state, rememberEvents(viewModel, navigation, next))
}

@Composable
fun TransferSpeedRoute(viewModel: TransferViewModel, navigation: TransferNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransferSpeedScreen(state, rememberEvents(viewModel, navigation, navigation.onReview))
}

@Composable
fun TransferReviewRoute(viewModel: TransferViewModel, navigation: TransferNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reference = (state as? TransferUiState.Content)?.receipt?.reference
    LaunchedEffect(reference) { if (reference != null) navigation.onReceipt() }
    TransferReviewScreen(state, rememberEvents(viewModel, navigation, navigation.onReceipt))
}

@Composable
fun TransferReceiptRoute(viewModel: TransferViewModel, navigation: TransferNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransferReceiptScreen(state, rememberEvents(viewModel, navigation, navigation.onDone))
}
