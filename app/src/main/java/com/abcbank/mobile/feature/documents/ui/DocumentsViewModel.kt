package com.abcbank.mobile.feature.documents.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.feature.documents.domain.BankDocument
import com.abcbank.mobile.feature.documents.domain.DocumentFile
import com.abcbank.mobile.feature.documents.domain.DocumentRenderer
import com.abcbank.mobile.feature.documents.domain.DocumentsOverview
import com.abcbank.mobile.feature.documents.domain.ObserveDocumentsUseCase
import com.abcbank.mobile.feature.documents.domain.Statement
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the viewer is: building the PDF, showing it, or failed. */
sealed interface ViewerState {
    data object Closed : ViewerState

    data class Building(val document: BankDocument) : ViewerState

    data class Ready(val document: BankDocument, val file: DocumentFile) : ViewerState

    data class Failed(val document: BankDocument) : ViewerState
}

sealed interface DocumentsUiState {
    data object Loading : DocumentsUiState

    /** No statement accounts and no 1099-INT. */
    data object Empty : DocumentsUiState

    data class Error(val error: DataError) : DocumentsUiState

    data class Content(
        /** Newest month first. */
        val statementsByMonth: List<Pair<YearMonth, List<Statement>>>,
        val overview: DocumentsOverview,
        val viewer: ViewerState,
        val isOffline: Boolean
    ) : DocumentsUiState
}

/** Statements and documents (MobileBuild.md 6, PRD feature 14): the list and the viewer. */
@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeDocuments: ObserveDocumentsUseCase,
    private val renderer: DocumentRenderer
) : ViewModel() {
    private val viewer = MutableStateFlow<ViewerState>(ViewerState.Closed)
    private val attempt = MutableStateFlow(0)
    private var latest: DocumentsOverview? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DocumentsUiState> = combine(
        attempt.flatMapLatest {
            observeDocuments()
        },
        viewer
    ) { result, view ->
        when (result) {
            is DataResult.Failure -> DocumentsUiState.Error(result.error)
            is DataResult.Success -> {
                val overview = result.value.also { latest = it }
                if (overview.statements.isEmpty() && overview.taxForm == null) {
                    DocumentsUiState.Empty
                } else {
                    DocumentsUiState.Content(
                        statementsByMonth = overview.statements.groupBy { it.month }.toList(),
                        overview = overview,
                        viewer = view,
                        isOffline = overview.isFromCache
                    )
                }
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        DocumentsUiState.Loading
    )

    /** Builds the PDF for [documentId]; the viewer shows it when ready. */
    fun onOpen(documentId: String) {
        val overview = latest ?: return
        val document =
            (overview.statements + listOfNotNull(overview.taxForm)).firstOrNull {
                it.id ==
                    documentId
            }
                ?: return
        savedStateHandle[KEY_OPEN] = documentId
        viewer.value = ViewerState.Building(document)
        viewModelScope.launch {
            viewer.value = when (val result = renderer.render(document)) {
                is DataResult.Success -> ViewerState.Ready(document, result.value)
                is DataResult.Failure -> ViewerState.Failed(document)
            }
        }
    }

    /** The viewer closed: the generated file is deleted, so documents never pile up. */
    fun onViewerClosed() {
        val ready = viewer.value as? ViewerState.Ready
        viewer.update { ViewerState.Closed }
        savedStateHandle[KEY_OPEN] = null
        if (ready != null) {
            viewModelScope.launch { withContext(NonCancellable) { renderer.delete(ready.file) } }
        }
    }

    fun onRetry() {
        attempt.value++
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val KEY_OPEN = "documentsOpen"
    }
}
