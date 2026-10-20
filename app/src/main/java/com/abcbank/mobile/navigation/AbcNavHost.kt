package com.abcbank.mobile.navigation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abcbank.mobile.core.designsystem.theme.AbcMotion
import com.abcbank.mobile.core.security.SessionState
import com.abcbank.mobile.feature.accounts.ui.TransactionDetailRoute
import com.abcbank.mobile.feature.accounts.ui.TransactionsRoute
import com.abcbank.mobile.feature.auth.ui.LockViewModel
import com.abcbank.mobile.feature.auth.ui.LoginRoute
import com.abcbank.mobile.feature.auth.ui.WelcomeScreen
import com.abcbank.mobile.feature.notifications.ui.NotificationsRoute
import com.abcbank.mobile.feature.payments.ui.DepositCheckRoute
import kotlinx.serialization.Serializable

@Serializable
data object WelcomeDestination

@Serializable
data object LoginDestination

/** The tabs after login: Home, Cards and Profile (MainTabs). */
@Serializable
data object MainDestination

/** Property names match TransactionsViewModel.ARG_ACCOUNT_ID and the detail screen's argument. */
@Serializable
data class TransactionsDestination(val accountId: String? = null)

@Serializable
data class TransactionDetailDestination(val transactionId: String)

@Serializable
data object NotificationsDestination

@Serializable
data object DepositCheckDestination

@Composable
fun AbcNavHost(modifier: Modifier = Modifier, lockViewModel: LockViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val session by lockViewModel.sessionState.collectAsStateWithLifecycle()
    val locked = session == SessionState.LOCKED

    // Back from the login screen goes to Welcome, never to the logged-out tabs.
    fun showLoginAfterLogOut() {
        navController.navigate(WelcomeDestination) {
            popUpTo<MainDestination> { inclusive = true }
        }
        navController.navigate(LoginDestination)
    }

    // Under the lock screen the screens stop collecting, so their listeners pause.
    CompositionLocalProvider(LocalLifecycleOwner provides rememberPausableLifecycleOwner(locked)) {
        AbcNavGraph(navController, modifier, ::showLoginAfterLogOut)
    }
    if (locked) LockOverlay(lockViewModel, onLoggedOut = ::showLoginAfterLogOut)
}

@Composable
private fun AbcNavGraph(
    navController: NavHostController,
    modifier: Modifier,
    showLoginAfterLogOut: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = WelcomeDestination,
        modifier = modifier,
        enterTransition = { fadeIn(tween(AbcMotion.SCREEN_MS, easing = FastOutSlowInEasing)) },
        exitTransition = { fadeOut(tween(AbcMotion.SCREEN_MS, easing = FastOutLinearInEasing)) },
        popEnterTransition = { fadeIn(tween(AbcMotion.SCREEN_MS, easing = FastOutSlowInEasing)) },
        popExitTransition = { fadeOut(tween(AbcMotion.SCREEN_MS, easing = FastOutLinearInEasing)) }
    ) {
        composable<WelcomeDestination> {
            WelcomeScreen(onGetStarted = { navController.navigate(LoginDestination) })
        }
        composable<LoginDestination> {
            LoginRoute(
                onBack = { navController.popBackStack() },
                onLoggedIn = {
                    // Back from the tabs leaves the app instead of returning to login.
                    navController.navigate(MainDestination) {
                        popUpTo<WelcomeDestination> { inclusive = true }
                    }
                }
            )
        }
        composable<MainDestination> {
            MainTabs(
                MainTabsNavigation(
                    onViewAllTransactions = { navController.navigate(TransactionsDestination()) },
                    onAccount = { navController.navigate(TransactionsDestination(accountId = it)) },
                    onTransaction = { navController.navigate(TransactionDetailDestination(it)) },
                    onTransfer = { navController.navigate(TransferGraph) },
                    onPayBills = { navController.navigate(BillPayGraph) },
                    onDepositCheck = { navController.navigate(DepositCheckDestination) },
                    onNotifications = { navController.navigate(NotificationsDestination) },
                    onExploreProducts = { navController.navigate(ProductsGraph) },
                    onLoggedOut = showLoginAfterLogOut
                )
            )
        }
        composable<TransactionsDestination> {
            TransactionsRoute(
                onBack = { navController.popBackStack() },
                onTransaction = { navController.navigate(TransactionDetailDestination(it)) }
            )
        }
        composable<TransactionDetailDestination> {
            TransactionDetailRoute(onBack = { navController.popBackStack() })
        }
        composable<DepositCheckDestination> {
            DepositCheckRoute(
                onBack = { navController.popBackStack() },
                onViewTransactions = { accountId ->
                    navController.popBackStack()
                    navController.navigate(TransactionsDestination(accountId = accountId))
                }
            )
        }
        composable<NotificationsDestination> {
            NotificationsRoute(onBack = { navController.popBackStack() })
        }
        billPayGraph(navController) { navController.navigate(TransactionDetailDestination(it)) }
        transferGraph(navController) { navController.navigate(TransactionDetailDestination(it)) }
        productsGraph(navController) {
            navController.navigate(TransactionsDestination(accountId = it))
        }
    }
}
