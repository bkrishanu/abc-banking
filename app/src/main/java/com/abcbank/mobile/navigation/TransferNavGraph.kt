package com.abcbank.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.abcbank.mobile.feature.payments.ui.AddPayeeRoute
import com.abcbank.mobile.feature.payments.ui.TransferAmountRoute
import com.abcbank.mobile.feature.payments.ui.TransferNavigation
import com.abcbank.mobile.feature.payments.ui.TransferReceiptRoute
import com.abcbank.mobile.feature.payments.ui.TransferRecipientRoute
import com.abcbank.mobile.feature.payments.ui.TransferReviewRoute
import com.abcbank.mobile.feature.payments.ui.TransferSpeedRoute
import com.abcbank.mobile.feature.payments.ui.TransferViewModel
import kotlinx.serialization.Serializable

/** The transfer flow (PRD features 7 and 8): one graph, so its steps share one ViewModel. */
/**
 * [beneficiaryId] starts a transfer to that payee (the Cards tab's Quick Transfer). The
 * ViewModel uses it only if it names a saved payee.
 */
@Serializable
data class TransferGraph(val beneficiaryId: String? = null)

@Serializable
data object TransferRecipientStep

@Serializable
data object AddPayeeStep

@Serializable
data object TransferAmountStep

@Serializable
data object TransferSpeedStep

@Serializable
data object TransferReviewStep

@Serializable
data object TransferReceiptStep

fun NavGraphBuilder.transferGraph(
    navController: NavController,
    onViewTransaction: (String) -> Unit
) {
    navigation<TransferGraph>(startDestination = TransferRecipientStep) {
        composable<TransferRecipientStep> {
            TransferRecipientRoute(
                transferViewModel(navController, it),
                navigation(navController, onViewTransaction)
            )
        }
        composable<AddPayeeStep> {
            AddPayeeRoute(
                transferViewModel(navController, it),
                navigation(navController, onViewTransaction)
            )
        }
        composable<TransferAmountStep> {
            TransferAmountRoute(
                transferViewModel(navController, it),
                navigation(navController, onViewTransaction)
            )
        }
        composable<TransferSpeedStep> {
            TransferSpeedRoute(
                transferViewModel(navController, it),
                navigation(navController, onViewTransaction)
            )
        }
        composable<TransferReviewStep> {
            TransferReviewRoute(
                transferViewModel(navController, it),
                navigation(navController, onViewTransaction)
            )
        }
        composable<TransferReceiptStep> {
            TransferReceiptRoute(
                transferViewModel(navController, it),
                navigation(navController, onViewTransaction)
            )
        }
    }
}

/** The ViewModel lives on the graph's back stack entry, so it survives moving between steps. */
@Composable
private fun transferViewModel(
    navController: NavController,
    entry: NavBackStackEntry
): TransferViewModel {
    val graphEntry = remember(entry) { navController.getBackStackEntry<TransferGraph>() }
    return hiltViewModel(graphEntry)
}

@Composable
private fun navigation(
    navController: NavController,
    onViewTransaction: (String) -> Unit
): TransferNavigation = remember(navController, onViewTransaction) {
    val leave: () -> Unit = { navController.popBackStack<TransferGraph>(inclusive = true) }
    TransferNavigation(
        onBack = { navController.popBackStack() },
        onAddPayee = { navController.navigate(AddPayeeStep) },
        onAmount = {
            // A saved payee replaces the form, so Back from the amount returns to the list.
            navController.navigate(TransferAmountStep) { popUpTo<TransferRecipientStep>() }
        },
        onSpeed = { navController.navigate(TransferSpeedStep) },
        onReview = { navController.navigate(TransferReviewStep) },
        onReceipt = {
            // Nothing before the receipt can be reached again, so a sent transfer is never resent.
            navController.navigate(TransferReceiptStep) {
                popUpTo<TransferRecipientStep> {
                    inclusive =
                        true
                }
            }
        },
        onViewTransaction = { id ->
            leave()
            onViewTransaction(id)
        },
        onDone = { leave() }
    )
}
