package com.abcbank.mobile.feature.support.domain

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import java.time.Instant
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

// Customer support hub (MobileBuild.md 6, PRD feature 15, DEMO_DATA.md section 07, FAQs).

data class Faq(val id: String, val question: String, val keywords: List<String>, val answer: String)

/** FAQs from Firestore's catalog/faqs (PRD feature 15). */
interface FaqSource {
    suspend fun faqs(): DataResult<List<Faq>>
}

enum class CaseStatus { OPEN, IN_REVIEW, RESOLVED }

enum class Party { CUSTOMER, BANK }

data class CaseMessage(val from: Party, val body: String, val sentAt: Instant)

data class SupportCase(
    val id: String,
    val transactionId: String?,
    val topic: String,
    val status: CaseStatus,
    val createdAt: Instant,
    val messages: List<CaseMessage>
)

data class SupportData(val cases: List<SupportCase>, val isFromCache: Boolean)

interface SupportRepository {
    fun observeCases(): Flow<DataResult<SupportData>>

    suspend fun openCase(case: SupportCase): DataResult<Unit>
}

object SupportBot {
    private const val MAX_TOPIC = 80

    /**
     * The FAQ whose keywords appear most often in [question], ignoring case; ties go to the one
     * listed first. A one-word keyword matches the start of a word ("statement" in "statements",
     * but "ach" not in "each"); a phrase matches whole words in order. Null when nothing matches.
     */
    fun match(question: String, faqs: List<Faq>): Faq? {
        val words = words(question)
        val padded = " ${words.joinToString(" ")} "
        fun matches(keyword: String): Boolean {
            val parts = words(keyword)
            return when (parts.size) {
                0 -> false
                1 -> words.any { it.startsWith(parts.single()) }
                else -> " ${parts.joinToString(" ")} " in padded
            }
        }
        val scored = faqs.map { faq -> faq to faq.keywords.count(::matches) }
        val best = scored.maxByOrNull { it.second } ?: return null
        return best.first.takeIf { best.second > 0 }
    }

    /** Lower-case words; curly apostrophes count as straight ones, so "didn’t" is "didn't". */
    private fun words(text: String): List<String> = text.lowercase(Locale.US)
        .replace(CURLY_APOSTROPHE, APOSTROPHE)
        .split(NOT_WORD)
        .filter(String::isNotEmpty)

    private const val CURLY_APOSTROPHE = '’'
    private const val APOSTROPHE = '\''
    private val NOT_WORD = Regex("[^a-z0-9']+")

    /** A case for a question the bot cannot answer: the question and the scripted hand-off. */
    fun handOff(question: String, handOffReply: String, id: String, now: Instant): SupportCase {
        val trimmed = question.trim()
        return SupportCase(
            id = id,
            transactionId = null,
            topic = trimmed.take(MAX_TOPIC),
            status = CaseStatus.OPEN,
            createdAt = now,
            messages = listOf(
                CaseMessage(Party.CUSTOMER, trimmed, now),
                CaseMessage(Party.BANK, handOffReply, now)
            )
        )
    }
}

sealed interface BotReply {
    data class Answer(val faq: Faq) : BotReply

    /** No FAQ matched: the question became a support case. */
    data class HandedOff(val case: SupportCase) : BotReply

    data class Failed(val error: DataError) : BotReply
}

class AskSupportUseCase @Inject constructor(
    private val faqs: FaqSource,
    private val repository: SupportRepository,
    private val clock: Clock
) {
    /** [handOffReply] is the scripted reply from string resources (DEMO_DATA.md section 07). */
    suspend operator fun invoke(question: String, handOffReply: String): BotReply {
        val list = when (val result = faqs.faqs()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> return BotReply.Failed(result.error)
        }
        SupportBot.match(question, list)?.let { return BotReply.Answer(it) }
        val case = SupportBot.handOff(
            question,
            handOffReply,
            "chat-${UUID.randomUUID()}",
            clock.now()
        )
        return when (val result = repository.openCase(case)) {
            is DataResult.Success -> BotReply.HandedOff(case)
            is DataResult.Failure -> BotReply.Failed(result.error)
        }
    }
}

class ObserveSupportCasesUseCase @Inject constructor(private val repository: SupportRepository) {
    operator fun invoke(): Flow<DataResult<SupportData>> = repository.observeCases()
}

class GetFaqsUseCase @Inject constructor(private val faqs: FaqSource) {
    suspend operator fun invoke(): DataResult<List<Faq>> = faqs.faqs()
}
