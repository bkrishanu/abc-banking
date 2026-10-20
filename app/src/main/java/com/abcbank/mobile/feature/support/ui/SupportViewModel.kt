package com.abcbank.mobile.feature.support.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.feature.support.domain.AskSupportUseCase
import com.abcbank.mobile.feature.support.domain.BotReply
import com.abcbank.mobile.feature.support.domain.Faq
import com.abcbank.mobile.feature.support.domain.GetFaqsUseCase
import com.abcbank.mobile.feature.support.domain.ObserveSupportCasesUseCase
import com.abcbank.mobile.feature.support.domain.SupportCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One line of this visit's chat. The bot's answers carry the question they matched. */
sealed interface ChatLine {
    data class Customer(val text: String) : ChatLine

    data class Answer(val faq: Faq) : ChatLine

    /** No answer: the question went to the team as a case. */
    data class HandedOff(val reply: String) : ChatLine

    data object Failed : ChatLine
}

data class ChatState(
    val lines: List<ChatLine> = emptyList(),
    val input: String = "",
    val waiting: Boolean = false
)

sealed interface SupportUiState {
    data object Loading : SupportUiState

    data class Error(val error: DataError) : SupportUiState

    /** The hub always has content (chat and FAQs); cases may be empty. */
    data class Content(
        /** Newest first. */
        val cases: List<SupportCase>,
        val faqs: List<Faq>,
        val chat: ChatState,
        val selectedCase: SupportCase?,
        val isOffline: Boolean
    ) : SupportUiState
}

/** The support hub (MobileBuild.md 6, PRD feature 15): chat, cases and FAQs. */
@HiltViewModel
class SupportViewModel @Inject constructor(
    observeCases: ObserveSupportCasesUseCase,
    private val getFaqs: GetFaqsUseCase,
    private val askSupport: AskSupportUseCase
) : ViewModel() {
    private val faqs = MutableStateFlow<List<Faq>>(emptyList())
    private val chat = MutableStateFlow(ChatState())
    private val selectedCaseId = MutableStateFlow<String?>(null)
    private val attempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SupportUiState> = combine(
        attempt.flatMapLatest { observeCases() },
        faqs,
        chat,
        selectedCaseId
    ) { result, faqList, chatState, caseId ->
        when (result) {
            is DataResult.Failure -> SupportUiState.Error(result.error)
            is DataResult.Success -> {
                val cases = result.value.cases.sortedByDescending { it.createdAt }
                SupportUiState.Content(
                    cases = cases,
                    faqs = faqList,
                    chat = chatState,
                    selectedCase = cases.firstOrNull { it.id == caseId },
                    isOffline = result.value.isFromCache
                )
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        SupportUiState.Loading
    )

    init {
        loadFaqs()
    }

    fun onInputChange(text: String) = chat.update { it.copy(input = text.take(MAX_QUESTION)) }

    /** Sends [question] from a suggestion, or the typed question when it is null. */
    fun onAsk(handOffReply: String, question: String? = null) {
        val text = (question ?: chat.value.input).trim()
        if (text.isEmpty() || chat.value.waiting) return
        chat.update {
            it.copy(lines = it.lines + ChatLine.Customer(text), input = "", waiting = true)
        }
        viewModelScope.launch {
            val line = when (val reply = askSupport(text, handOffReply)) {
                is BotReply.Answer -> ChatLine.Answer(reply.faq)
                is BotReply.HandedOff -> ChatLine.HandedOff(handOffReply)
                is BotReply.Failed -> ChatLine.Failed
            }
            chat.update { it.copy(lines = it.lines + line, waiting = false) }
        }
    }

    fun onSelectCase(caseId: String) {
        selectedCaseId.value = caseId
    }

    fun onRetry() {
        attempt.value++
        loadFaqs()
    }

    private fun loadFaqs() {
        viewModelScope.launch {
            (getFaqs() as? DataResult.Success)?.let { faqs.value = it.value }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val MAX_QUESTION = 300
    }
}
