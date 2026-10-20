package com.abcbank.mobile.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.abcbank.mobile.R
import com.abcbank.mobile.feature.documents.ui.DocumentViewerScreen
import com.abcbank.mobile.feature.documents.ui.DocumentsScreen
import com.abcbank.mobile.feature.documents.ui.DocumentsUiState
import com.abcbank.mobile.feature.documents.ui.DocumentsViewModel
import com.abcbank.mobile.feature.documents.ui.ViewerState
import kotlinx.serialization.Serializable

/** Statements and documents (PRD feature 14): the list and the viewer share one ViewModel. */
@Serializable
data object DocumentsGraph

@Serializable
data object DocumentsListStep

@Serializable
data object DocumentViewerStep

fun NavGraphBuilder.documentsGraph(navController: NavController) {
    navigation<DocumentsGraph>(startDestination = DocumentsListStep) {
        composable<DocumentsListStep> {
            val viewModel = documentsViewModel(navController, it)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            DocumentsScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onOpen = { id ->
                    viewModel.onOpen(id)
                    navController.navigate(DocumentViewerStep)
                },
                onRetry = viewModel::onRetry
            )
        }
        composable<DocumentViewerStep> {
            val viewModel = documentsViewModel(navController, it)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val shareTitle = stringResource(R.string.documents_share_title)
            // Leaving the viewer deletes the generated file (ARCHITECTURE.md section 04).
            DisposableEffect(Unit) { onDispose { viewModel.onViewerClosed() } }
            DocumentViewerScreen(
                viewer = (state as? DocumentsUiState.Content)?.viewer ?: ViewerState.Closed,
                onBack = { navController.popBackStack() },
                onShare = { uri ->
                    // Handing a finished file to the share sheet is UI work; building it happened in data.
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = PDF_MIME_TYPE
                        putExtra(Intent.EXTRA_STREAM, uri.toUri())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, shareTitle))
                },
                onRetry = viewModel::onOpen
            )
        }
    }
}

@Composable
private fun documentsViewModel(
    navController: NavController,
    entry: NavBackStackEntry
): DocumentsViewModel {
    val graphEntry = remember(entry) { navController.getBackStackEntry(DocumentsGraph) }
    return hiltViewModel(graphEntry)
}

private const val PDF_MIME_TYPE = "application/pdf"
