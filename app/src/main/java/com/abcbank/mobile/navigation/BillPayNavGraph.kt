package com.abcbank.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.abcbank.mobile.feature.payments.ui.AddBillerRoute
import com.abcbank.mobile.feature.payments.ui.BillDetailRoute
import com.abcbank.mobile.feature.payments.ui.BillPayNavigation
import com.abcbank.mobile.feature.payments.ui.BillPayViewModel
import com.abcbank.mobile.feature.payments.ui.BillsRoute
import kotlinx.serialization.Serializable

/** Bill pay (PRD feature 9): bills, Add a Biller and a bill's detail share one ViewModel. */
@Serializable
data object BillPayGraph

@Serializable
data object BillsStep

@Serializable
data object AddBillerStep

@Serializable
data object BillDetailStep

fun NavGraphBuilder.billPayGraph(
    navController: NavController,
    onViewTransaction: (String) -> Unit
) {
    navigation<BillPayGraph>(startDestination = BillsStep) {
        composable<BillsStep> {
            BillsRoute(
                billPayViewModel(navController, it),
                navigation(navController, onViewTransaction)
            )
        }
        composable<AddBillerStep> {
            AddBillerRoute(
                billPayViewModel(navController, it),
                navigation(navController, onViewTransaction)
            )
        }
        composable<BillDetailStep> {
            BillDetailRoute(
                billPayViewModel(navController, it),
                navigation(navController, onViewTransaction)
            )
        }
    }
}

@Composable
private fun billPayViewModel(
    navController: NavController,
    entry: NavBackStackEntry
): BillPayViewModel {
    val graphEntry = remember(entry) { navController.getBackStackEntry(BillPayGraph) }
    return hiltViewModel(graphEntry)
}

@Composable
private fun navigation(
    navController: NavController,
    onViewTransaction: (String) -> Unit
): BillPayNavigation = remember(navController, onViewTransaction) {
    BillPayNavigation(
        onBack = { navController.popBackStack() },
        onAddBiller = { navController.navigate(AddBillerStep) },
        onBill = { navController.navigate(BillDetailStep) },
        // The new bill replaces Add a Biller, so Back returns to the list.
        onBillAdded = { navController.navigate(BillDetailStep) { popUpTo<BillsStep>() } },
        onViewTransaction = { id ->
            navController.popBackStack(BillPayGraph, inclusive = true)
            onViewTransaction(id)
        }
    )
}
