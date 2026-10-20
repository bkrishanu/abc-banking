package com.abcbank.mobile.feature.support

import app.cash.turbine.test
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.feature.support.domain.AskSupportUseCase
import com.abcbank.mobile.feature.support.domain.BotReply
import com.abcbank.mobile.feature.support.domain.CaseStatus
import com.abcbank.mobile.feature.support.domain.Faq
import com.abcbank.mobile.feature.support.domain.FaqSource
import com.abcbank.mobile.feature.support.domain.GetFaqsUseCase
import com.abcbank.mobile.feature.support.domain.ObserveSupportCasesUseCase
import com.abcbank.mobile.feature.support.domain.Party
import com.abcbank.mobile.feature.support.domain.SupportBot
import com.abcbank.mobile.feature.support.domain.SupportCase
import com.abcbank.mobile.feature.support.domain.SupportData
import com.abcbank.mobile.feature.support.domain.SupportRepository
import com.abcbank.mobile.feature.support.ui.ChatLine
import com.abcbank.mobile.feature.support.ui.SupportUiState
import com.abcbank.mobile.feature.support.ui.SupportViewModel
import com.abcbank.mobile.testing.MainDispatcherRule
import com.abcbank.mobile.testing.ManualClock
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val HAND_OFF =
    "I've passed this to our support team. They'll reply here within one business day."

/** The catalog's FAQs, as Firestore's catalog/faqs is created from them. */
private val FAQS: List<Faq> = Json.parseToJsonElement(
    File("src/main/assets/seed/catalog.json").readText()
)
    .jsonObject.getValue("faqs").jsonArray.map { element ->
        val faq = element.jsonObject
        Faq(
            id = faq.getValue("id").jsonPrimitive.content,
            question = faq.getValue("question").jsonPrimitive.content,
            keywords = faq.getValue("keywords").jsonArray.map { it.jsonPrimitive.content },
            answer = faq.getValue("answer").jsonPrimitive.content
        )
    }

class SupportBotTest {
    private fun answer(question: String) = SupportBot.match(question, FAQS)?.id

    @Test
    fun `every FAQ answers its own question`() {
        FAQS.forEach { assertEquals(it.question, it.id, answer(it.question)) }
    }

    @Test
    fun `keywords match at word starts and phrases as whole words`() {
        assertEquals("freeze-card", answer("I LOST my card yesterday"))
        assertEquals("statements", answer("where can I download statements?"))
        assertEquals("dispute-transaction", answer("I didn’t make this payment"))
        // "ach" inside "each" is not the ACH keyword.
        assertNull(answer("what about each of them"))
        assertNull(answer("Can I open a joint account?"))
    }

    @Test
    fun `an unanswered question becomes an open case with the scripted reply`() {
        val case = SupportBot.handOff(
            "  Can I open a joint account? ",
            HAND_OFF,
            "chat-1",
            ManualClock().now
        )
        assertEquals(CaseStatus.OPEN, case.status)
        assertEquals("Can I open a joint account?", case.topic)
        assertEquals(listOf(Party.CUSTOMER, Party.BANK), case.messages.map { it.from })
        assertEquals(HAND_OFF, case.messages.last().body)
    }
}

private class FakeSupportRepository : SupportRepository {
    val data = MutableStateFlow<DataResult<SupportData>?>(null)
    val opened = mutableListOf<SupportCase>()

    override fun observeCases(): Flow<DataResult<SupportData>> = data.filterNotNull()

    override suspend fun openCase(case: SupportCase): DataResult<Unit> {
        opened += case
        data.value = DataResult.Success(SupportData(opened.toList(), false))
        return DataResult.Success(Unit)
    }
}

private class FakeFaqs(var result: DataResult<List<Faq>> = DataResult.Success(FAQS)) : FaqSource {
    override suspend fun faqs(): DataResult<List<Faq>> = result
}

class AskSupportUseCaseTest {
    private val repository = FakeSupportRepository()

    @Test
    fun `answers write nothing, unanswered questions open a case, failures are reported`() =
        runTest {
            val faqs = FakeFaqs()
            val ask = AskSupportUseCase(faqs, repository, ManualClock())
            assertTrue(ask("How do I freeze my card?", HAND_OFF) is BotReply.Answer)
            assertTrue(repository.opened.isEmpty())
            assertTrue(ask("Can I open a joint account?", HAND_OFF) is BotReply.HandedOff)
            assertEquals(1, repository.opened.size)
            faqs.result = DataResult.Failure(DataError.OFFLINE)
            assertEquals(BotReply.Failed(DataError.OFFLINE), ask("anything", HAND_OFF))
        }
}

class SupportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSupportRepository()

    private fun viewModel(): SupportViewModel {
        val faqs = FakeFaqs()
        return SupportViewModel(
            observeCases = ObserveSupportCasesUseCase(repository),
            getFaqs = GetFaqsUseCase(faqs),
            askSupport = AskSupportUseCase(faqs, repository, ManualClock())
        )
    }

    @Test
    fun `the chat answers, hands off, and the new case shows in Your Cases`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(SupportUiState.Loading, awaitItem())
            repository.data.value = DataResult.Success(SupportData(emptyList(), false))
            testScheduler.runCurrent()
            assertEquals(FAQS.size, (expectMostRecentItem() as SupportUiState.Content).faqs.size)

            viewModel.onInputChange("How long does a standard transfer take?")
            viewModel.onAsk(HAND_OFF)
            testScheduler.runCurrent()
            viewModel.onAsk(HAND_OFF, "Can I open a joint account?")
            testScheduler.runCurrent()
            val content = expectMostRecentItem() as SupportUiState.Content
            assertEquals("", content.chat.input)
            val lines = content.chat.lines
            assertEquals(4, lines.size)
            assertEquals("standard-transfer-time", (lines[1] as ChatLine.Answer).faq.id)
            assertTrue(lines[3] is ChatLine.HandedOff)
            assertEquals(listOf("Can I open a joint account?"), content.cases.map { it.topic })

            viewModel.onSelectCase(content.cases.single().id)
            testScheduler.runCurrent()
            assertEquals(
                content.cases.single(),
                (expectMostRecentItem() as SupportUiState.Content).selectedCase
            )
        }
    }
}
