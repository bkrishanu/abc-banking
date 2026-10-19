package com.abcbank.mobile.navigation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abcbank.mobile.core.designsystem.theme.AbcMotion
import com.abcbank.mobile.feature.auth.ui.LoginRoute
import com.abcbank.mobile.feature.auth.ui.WelcomeScreen
import com.abcbank.mobile.feature.home.ui.HomeActions
import com.abcbank.mobile.feature.home.ui.HomeRoute
import com.abcbank.mobile.feature.profile.ui.ProfileRoute
import kotlinx.serialization.Serializable

@Serializable
data object WelcomeDestination

@Serializable
data object LoginDestination

@Serializable
data object HomeDestination

@Serializable
data object ProfileDestination

@Composable
fun AbcNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
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
                    // Back from Home leaves the app instead of returning to login.
                    navController.navigate(HomeDestination) {
                        popUpTo<WelcomeDestination> { inclusive = true }
                    }
                }
            )
        }
        composable<HomeDestination> {
            // Notifications, transfer, bill pay and products are not built yet; their actions
            // are no-ops until each destination exists.
            HomeRoute(
                actions = HomeActions(onProfile = {
                    navController.navigate(ProfileDestination)
                })
            )
        }
        composable<ProfileDestination> {
            ProfileRoute(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    // Back from the login screen goes to Welcome, never to the logged-out Home.
                    navController.navigate(WelcomeDestination) {
                        popUpTo<HomeDestination> { inclusive = true }
                    }
                    navController.navigate(LoginDestination)
                }
            )
        }
    }
}
