package com.abcbank.mobile.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.component.AbcBottomBar
import com.abcbank.mobile.core.designsystem.component.BottomBarItem
import com.abcbank.mobile.core.designsystem.component.LocalBottomBarClearance
import com.abcbank.mobile.core.designsystem.component.bottomBarClearance
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.cards.ui.CardsRoute
import com.abcbank.mobile.feature.home.ui.HomeActions
import com.abcbank.mobile.feature.home.ui.HomeRoute
import com.abcbank.mobile.feature.insights.ui.InsightsRoute
import com.abcbank.mobile.feature.profile.ui.ProfileRoute
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

@Serializable
data object HomeTab

@Serializable
data object CardsTab

@Serializable
data object InsightsTab

@Serializable
data object ProfileTab

/** Where the tabs send the app outside themselves (screens that cover the bottom bar). */
data class MainTabsNavigation(
    val onViewAllTransactions: () -> Unit,
    val onAccount: (String) -> Unit,
    val onTransaction: (String) -> Unit,
    val onTransfer: () -> Unit,
    val onPayBills: () -> Unit,
    val onDepositCheck: () -> Unit,
    val onNotifications: () -> Unit,
    val onExploreProducts: () -> Unit,
    val onLoggedOut: () -> Unit
)

/**
 * The tabs after login (DESIGN_SYSTEM.md section 06, AbcBottomBar): Home, Cards, Insights and
 * Profile, with the floating bar over them. Each tab keeps its state when another is selected.
 */
@Composable
fun MainTabs(navigation: MainTabsNavigation, modifier: Modifier = Modifier) {
    val tabs = rememberNavController()
    val current by tabs.currentBackStackEntryAsState()
    fun selected(tab: KClass<*>) = current?.destination?.hasRoute(tab) == true

    Box(modifier) {
        CompositionLocalProvider(LocalBottomBarClearance provides bottomBarClearance()) {
            NavHost(navController = tabs, startDestination = HomeTab) {
                composable<HomeTab> {
                    HomeRoute(
                        actions = HomeActions(
                            onProfile = { tabs.selectTab(ProfileTab) },
                            onViewAllTransactions = navigation.onViewAllTransactions,
                            onAccount = navigation.onAccount,
                            onTransaction = navigation.onTransaction,
                            onTransfer = navigation.onTransfer,
                            onPayBills = navigation.onPayBills,
                            onDepositCheck = navigation.onDepositCheck,
                            onNotifications = navigation.onNotifications,
                            onExploreProducts = navigation.onExploreProducts,
                            onInsights = { tabs.selectTab(InsightsTab) },
                            onCards = { tabs.selectTab(CardsTab) }
                        )
                    )
                }
                composable<CardsTab> { CardsRoute() }
                composable<InsightsTab> { InsightsRoute() }
                composable<ProfileTab> {
                    ProfileRoute(onBack = null, onLoggedOut = navigation.onLoggedOut)
                }
            }
        }
        AbcBottomBar(
            items = listOf(
                BottomBarItem(
                    R.drawable.ic_house,
                    stringResource(R.string.tab_home),
                    selected(HomeTab::class)
                ) {
                    tabs.selectTab(HomeTab)
                },
                BottomBarItem(
                    R.drawable.ic_credit_card,
                    stringResource(R.string.tab_cards),
                    selected(CardsTab::class)
                ) {
                    tabs.selectTab(CardsTab)
                },
                BottomBarItem(
                    R.drawable.ic_chart_column,
                    stringResource(R.string.tab_insights),
                    selected(InsightsTab::class)
                ) {
                    tabs.selectTab(InsightsTab)
                },
                BottomBarItem(
                    R.drawable.ic_user,
                    stringResource(R.string.tab_profile),
                    selected(ProfileTab::class)
                ) {
                    tabs.selectTab(ProfileTab)
                }
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = AbcTheme.spacing.space4)
                .padding(bottom = AbcTheme.spacing.space3)
        )
    }
}

/** Standard tab switching: one copy of each tab, its state saved, Back returns to Home. */
private fun NavHostController.selectTab(tab: Any) {
    navigate(tab) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
