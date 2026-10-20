package com.abcbank.mobile.feature.products.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abcbank.mobile.feature.products.domain.LoanProduct

/** Where the products flow goes; the nav graph supplies these. */
data class ProductsNavigation(
    val onBack: () -> Unit,
    val onCalculator: () -> Unit,
    val onIdentity: () -> Unit,
    val onTerms: () -> Unit,
    val onDecision: () -> Unit,
    val onComplete: () -> Unit,
    val onViewAccount: (String) -> Unit,
    val onDone: () -> Unit
)

@Composable
private fun rememberEvents(
    viewModel: ProductsViewModel,
    navigation: ProductsNavigation,
    onContinue: () -> Unit
): ProductsEvents = remember(viewModel, navigation, onContinue) {
    ProductsEvents(
        onBack = navigation.onBack,
        onRetry = viewModel::onRetry,
        onViewAccount = navigation.onViewAccount,
        onAmountChange = viewModel::onAmountChange,
        onTermChange = viewModel::onTermChange,
        onContinue = onContinue,
        onEmploymentSelect = viewModel::onEmploymentSelect,
        onIncomeChange = viewModel::onIncomeChange,
        onAgreeChange = viewModel::onAgreeChange,
        onStartCheck = viewModel::onStartCheck,
        onSubmit = viewModel::onSubmit,
        onDone = navigation.onDone
    )
}

@Composable
fun ProductsRoute(viewModel: ProductsViewModel, navigation: ProductsNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listings = (state as? ProductsUiState.Content)?.listings.orEmpty()
    val events = rememberEvents(viewModel, navigation, navigation.onIdentity).copy(
        onApply = { productId ->
            viewModel.onSelectProduct(productId)
            val isLoan = listings.firstOrNull { it.product.id == productId }?.product is LoanProduct
            if (isLoan) navigation.onCalculator() else navigation.onIdentity()
        }
    )
    ProductsScreen(state, events)
}

@Composable
fun LoanCalculatorRoute(viewModel: ProductsViewModel, navigation: ProductsNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoanCalculatorScreen(state, rememberEvents(viewModel, navigation, navigation.onIdentity))
}

@Composable
fun ApplicationIdentityRoute(viewModel: ProductsViewModel, navigation: ProductsNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ApplicationIdentityScreen(state, rememberEvents(viewModel, navigation, navigation.onTerms))
}

@Composable
fun ApplicationTermsRoute(viewModel: ProductsViewModel, navigation: ProductsNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ApplicationTermsScreen(state, rememberEvents(viewModel, navigation, navigation.onDecision))
}

@Composable
fun ApplicationDecisionRoute(viewModel: ProductsViewModel, navigation: ProductsNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val submitted = (state as? ProductsUiState.Content)?.submitted?.application?.id
    LaunchedEffect(submitted) { if (submitted != null) navigation.onComplete() }
    ApplicationDecisionScreen(state, rememberEvents(viewModel, navigation, navigation.onComplete))
}

@Composable
fun ApplicationCompleteRoute(viewModel: ProductsViewModel, navigation: ProductsNavigation) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ApplicationCompleteScreen(state, rememberEvents(viewModel, navigation, navigation.onDone))
}
