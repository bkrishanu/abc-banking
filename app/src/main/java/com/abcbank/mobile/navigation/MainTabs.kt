package com.abcbank.mobile.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.abcbank.mobile.core.designsystem.component.LocalTabBell
import com.abcbank.mobile.core.designsystem.component.MoreAction
import com.abcbank.mobile.core.designsystem.component.MoreActionsSheet
import com.abcbank.mobile.core.designsystem.component.TabBell
import com.abcbank.mobile.core.designsystem.component.TransferButton
import com.abcbank.mobile.core.designsystem.component.bottomBarClearance
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.cards.ui.CardsNavigation
import com.abcbank.mobile.feature.cards.ui.CardsRoute
import com.abcbank.mobile.feature.home.ui.HomeActions
import com.abcbank.mobile.feature.home.ui.HomeRoute
import com.abcbank.mobile.feature.insights.ui.InsightsRoute
import com.abcbank.mobile.feature.notifications.ui.TabBellUiState
import com.abcbank.mobile.feature.notifications.ui.TabBellViewModel
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
    val onDocuments: () -> Unit,
    val onSupport: () -> Unit,
    val onNotifications: () -> Unit,
    val onExploreProducts: () -> Unit,
    val onLoggedOut: () -> Unit
)

/**
 * The tabs after login (DESIGN_SYSTEM.md section 06, AbcBottomBar): Home, Cards, Insights and
 * Profile over the docked bar, with the floating Transfer button above it. Each tab keeps its
 * state when another is selected, and every tab's top bar shares one notifications bell.
 */
@Composable
fun MainTabs(
    navigation: MainTabsNavigation,
    modifier: Modifier = Modifier,
    bellViewModel: TabBellViewModel = hiltViewModel()
) {
    val tabs = rememberNavController()
    val current by tabs.currentBackStackEntryAsState()
    fun selected(tab: KClass<*>) = current?.destination?.hasRoute(tab) == true
    // One More Actions sheet for Home's grid pill and the Cards tab's More.
    var showMoreActions by rememberSaveable { mutableStateOf(false) }
    val bellState by bellViewModel.uiState.collectAsStateWithLifecycle()
    val bell = TabBell(
        unreadNotifications = (bellState as? TabBellUiState.Content)?.unreadNotifications ?: 0,
        onNotifications = navigation.onNotifications
    )

    Box(modifier) {
        CompositionLocalProvider(
            LocalBottomBarClearance provides bottomBarClearance(),
            LocalTabBell provides bell
        ) {
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
                            onDocuments = navigation.onDocuments,
                            onNotifications = navigation.onNotifications,
                            onExploreProducts = navigation.onExploreProducts,
                            onInsights = { tabs.selectTab(InsightsTab) },
                            onCards = { tabs.selectTab(CardsTab) },
                            onMoreActions = { showMoreActions = true }
                        )
                    )
                }
                composable<CardsTab> {
                    CardsRoute(
                        CardsNavigation(onAccount = navigation.onAccount)
                    )
                }
                composable<InsightsTab> { InsightsRoute() }
                composable<ProfileTab> {
                    ProfileRoute(
                        onBack = null,
                        onLoggedOut = navigation.onLoggedOut,
                        onDocuments = navigation.onDocuments,
                        onSupport = navigation.onSupport
                    )
                }
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TransferButton(onClick = navigation.onTransfer)
            Spacer(Modifier.height(AbcTheme.spacing.space3))
            TabBar(tabs = tabs, selected = ::selected)
        }
    }
    if (showMoreActions) {
        MoreActionsSheet(
            actions = listOf(
                MoreAction(
                    R.drawable.ic_arrow_up_right,
                    stringResource(R.string.more_send_money),
                    navigation.onTransfer
                ),
                MoreAction(
                    R.drawable.ic_arrow_down,
                    stringResource(R.string.more_deposit_check),
                    navigation.onDepositCheck
                ),
                MoreAction(
                    R.drawable.ic_file_text,
                    stringResource(R.string.more_statements),
                    navigation.onDocuments
                ),
                MoreAction(
                    R.drawable.ic_layout_grid,
                    stringResource(R.string.more_explore_products),
                    navigation.onExploreProducts
                ),
                MoreAction(R.drawable.ic_chart_column, stringResource(R.string.more_insights)) {
                    tabs.selectTab(InsightsTab)
                },
                MoreAction(R.drawable.ic_credit_card, stringResource(R.string.more_cards)) {
                    tabs.selectTab(CardsTab)
                }
            ),
            onDismiss = { showMoreActions = false }
        )
    }
}

@Composable
private fun TabBar(tabs: NavHostController, selected: (KClass<*>) -> Boolean) {
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
        )
    )
}

/** Standard tab switching: one copy of each tab, its state saved, Back returns to Home. */
private fun NavHostController.selectTab(tab: Any) {
    navigate(tab) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
