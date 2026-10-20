package com.abcbank.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.abcbank.mobile.feature.products.ui.ApplicationCompleteRoute
import com.abcbank.mobile.feature.products.ui.ApplicationDecisionRoute
import com.abcbank.mobile.feature.products.ui.ApplicationIdentityRoute
import com.abcbank.mobile.feature.products.ui.ApplicationTermsRoute
import com.abcbank.mobile.feature.products.ui.LoanCalculatorRoute
import com.abcbank.mobile.feature.products.ui.ProductsNavigation
import com.abcbank.mobile.feature.products.ui.ProductsRoute
import com.abcbank.mobile.feature.products.ui.ProductsViewModel
import kotlinx.serialization.Serializable

/** Explore Products and applications (PRD feature 12): one graph, one shared ViewModel. */
@Serializable
data object ProductsGraph

@Serializable
data object ProductsCatalogStep

@Serializable
data object LoanCalculatorStep

@Serializable
data object ApplicationIdentityStep

@Serializable
data object ApplicationTermsStep

@Serializable
data object ApplicationDecisionStep

@Serializable
data object ApplicationCompleteStep

fun NavGraphBuilder.productsGraph(navController: NavController, onViewAccount: (String) -> Unit) {
    navigation<ProductsGraph>(startDestination = ProductsCatalogStep) {
        composable<ProductsCatalogStep> {
            ProductsRoute(
                productsViewModel(navController, it),
                navigation(navController, onViewAccount)
            )
        }
        composable<LoanCalculatorStep> {
            LoanCalculatorRoute(
                productsViewModel(navController, it),
                navigation(navController, onViewAccount)
            )
        }
        composable<ApplicationIdentityStep> {
            ApplicationIdentityRoute(
                productsViewModel(navController, it),
                navigation(navController, onViewAccount)
            )
        }
        composable<ApplicationTermsStep> {
            ApplicationTermsRoute(
                productsViewModel(navController, it),
                navigation(navController, onViewAccount)
            )
        }
        composable<ApplicationDecisionStep> {
            ApplicationDecisionRoute(
                productsViewModel(navController, it),
                navigation(navController, onViewAccount)
            )
        }
        composable<ApplicationCompleteStep> {
            ApplicationCompleteRoute(
                productsViewModel(navController, it),
                navigation(navController, onViewAccount)
            )
        }
    }
}

@Composable
private fun productsViewModel(
    navController: NavController,
    entry: NavBackStackEntry
): ProductsViewModel {
    val graphEntry = remember(entry) { navController.getBackStackEntry(ProductsGraph) }
    return hiltViewModel(graphEntry)
}

@Composable
private fun navigation(
    navController: NavController,
    onViewAccount: (String) -> Unit
): ProductsNavigation = remember(navController, onViewAccount) {
    val leave: () -> Unit = { navController.popBackStack(ProductsGraph, inclusive = true) }
    ProductsNavigation(
        onBack = { navController.popBackStack() },
        onCalculator = { navController.navigate(LoanCalculatorStep) },
        onIdentity = { navController.navigate(ApplicationIdentityStep) },
        onTerms = { navController.navigate(ApplicationTermsStep) },
        onDecision = { navController.navigate(ApplicationDecisionStep) },
        onComplete = {
            // The submitted steps cannot be reached again, so nothing is applied for twice.
            navController.navigate(ApplicationCompleteStep) {
                popUpTo<ProductsCatalogStep>()
            }
        },
        onViewAccount = { accountId ->
            leave()
            onViewAccount(accountId)
        },
        onDone = {
            // Back to the catalog, which now shows the product as opened.
            navController.popBackStack(ProductsCatalogStep, inclusive = false)
        }
    )
}
