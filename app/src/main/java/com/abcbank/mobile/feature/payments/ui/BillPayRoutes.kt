package com.abcbank.mobile.feature.payments.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Where bill pay goes; the nav graph supplies these. */
data class BillPayNavigation(
    val onBack: () -> Unit,
    val onAddBiller: () -> Unit,
    val onBill: () -> Unit,
    val onBillAdded: () -> Unit,
    val onViewTransaction: (String) -> Unit
)

@Composable
private fun rememberEvents(
    viewModel: BillPayViewModel,
    navigation: BillPayNavigation
): BillPayEvents = remember(viewModel, navigation) {
    BillPayEvents(
        onBack = navigation.onBack,
        onRetry = viewModel::onRetry,
        onOpenBill = {
            viewModel.onSelectBill(it)
            navigation.onBill()
        },
        onAddBiller = {
            viewModel.onStartAddBiller()
            navigation.onAddBiller()
        },
        onSearch = viewModel::onSearch,
        onChooseBiller = viewModel::onChooseBiller,
        onAccountNumberChange = viewModel::onAccountNumberChange,
        onFetchBill = viewModel::onFetchBill,
        onSaveBiller = viewModel::onAddBiller,
        onSelectSource = viewModel::onSelectSource,
        onPay = viewModel::onPay,
        onSchedule = viewModel::onSchedule,
        canScheduleFor = viewModel::canScheduleFor,
        onAutoPayChange = viewModel::onAutoPayChange,
        onViewTransaction = navigation.onViewTransaction,
        onStepUpDigit = viewModel::onStepUpDigit,
        onStepUpDelete = viewModel::onStepUpDelete,
        onStepUpCancel = viewModel::onStepUpCancel
    )
}

@Composable
fun BillsRoute(viewModel: BillPayViewModel, navigation: BillPayNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BillsScreen(state, rememberEvents(viewModel, navigation))
}

@Composable
fun AddBillerRoute(viewModel: BillPayViewModel, navigation: BillPayNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // A saved biller opens its new bill.
    val added = (state as? BillPayUiState.Content)?.form?.added ?: 0
    val initial = remember { added }
    LaunchedEffect(added) { if (added > initial) navigation.onBillAdded() }
    AddBillerScreen(state, rememberEvents(viewModel, navigation))
}

@Composable
fun BillDetailRoute(viewModel: BillPayViewModel, navigation: BillPayNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BillDetailScreen(state, rememberEvents(viewModel, navigation))
}
