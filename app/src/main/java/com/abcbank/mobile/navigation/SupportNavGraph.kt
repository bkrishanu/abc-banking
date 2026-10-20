package com.abcbank.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.abcbank.mobile.R
import com.abcbank.mobile.feature.support.ui.SupportCaseScreen
import com.abcbank.mobile.feature.support.ui.SupportChatScreen
import com.abcbank.mobile.feature.support.ui.SupportEvents
import com.abcbank.mobile.feature.support.ui.SupportScreen
import com.abcbank.mobile.feature.support.ui.SupportViewModel
import java.time.ZoneId
import kotlinx.serialization.Serializable

/** The support hub (PRD feature 15): hub, chat and case share one ViewModel. */
@Serializable
data object SupportGraph

@Serializable
data object SupportHubStep

@Serializable
data object SupportChatStep

@Serializable
data object SupportCaseStep

fun NavGraphBuilder.supportGraph(navController: NavController, onDispute: () -> Unit) {
    navigation<SupportGraph>(startDestination = SupportHubStep) {
        composable<SupportHubStep> {
            val viewModel = supportViewModel(navController, it)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            SupportScreen(state, events(viewModel, navController, onDispute))
        }
        composable<SupportChatStep> {
            val viewModel = supportViewModel(navController, it)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            SupportChatScreen(state, events(viewModel, navController, onDispute))
        }
        composable<SupportCaseStep> {
            val viewModel = supportViewModel(navController, it)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            // Times show in the phone's own time zone (ARCHITECTURE.md section 04).
            SupportCaseScreen(
                state,
                events(viewModel, navController, onDispute),
                zone = ZoneId.systemDefault()
            )
        }
    }
}

@Composable
private fun events(
    viewModel: SupportViewModel,
    navController: NavController,
    onDispute: () -> Unit
): SupportEvents {
    val handOff = stringResource(R.string.support_hand_off)
    return remember(viewModel, navController, onDispute, handOff) {
        SupportEvents(
            onBack = { navController.popBackStack() },
            onRetry = viewModel::onRetry,
            onChat = { navController.navigate(SupportChatStep) },
            onDispute = onDispute,
            onOpenCase = { id ->
                viewModel.onSelectCase(id)
                navController.navigate(SupportCaseStep)
            },
            onInputChange = viewModel::onInputChange,
            onAsk = { question -> viewModel.onAsk(handOff, question) }
        )
    }
}

@Composable
private fun supportViewModel(
    navController: NavController,
    entry: NavBackStackEntry
): SupportViewModel {
    val graphEntry = remember(entry) { navController.getBackStackEntry(SupportGraph) }
    return hiltViewModel(graphEntry)
}
