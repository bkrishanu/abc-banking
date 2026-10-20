package com.abcbank.mobile.feature.support.ui

import com.abcbank.mobile.feature.support.domain.CaseMessage
import com.abcbank.mobile.feature.support.domain.CaseStatus
import com.abcbank.mobile.feature.support.domain.Faq
import com.abcbank.mobile.feature.support.domain.Party
import com.abcbank.mobile.feature.support.domain.SupportCase
import java.time.Instant

/** Samanta's seeded dispute (DEMO_DATA.md section 03) and the catalog FAQs. */
internal object SupportPreviewData {
    private val OPENED: Instant = Instant.parse("2026-10-10T09:00:00Z")

    val faqs = listOf(
        Faq(
            "freeze-card",
            "How do I freeze my card?",
            listOf("freeze", "lock", "lost", "stolen"),
            "Go to Cards, choose the card and turn on Freeze Card. You can turn it off at any time."
        ),
        Faq(
            "instant-fee",
            "What is the instant transfer fee?",
            listOf("fee", "instant", "cost"),
            "1.5% of the amount, at least \$0.25 and at most \$15.00."
        ),
        Faq(
            "statements",
            "Where are my statements?",
            listOf("statement", "pdf", "tax", "1099"),
            "Go to Profile, then Statements and Documents."
        )
    )

    val dispute = SupportCase(
        id = "dispute-melody-live",
        transactionId = "tx-016",
        topic = "I didn't make this purchase",
        status = CaseStatus.IN_REVIEW,
        createdAt = OPENED,
        messages = listOf(
            CaseMessage(
                Party.CUSTOMER,
                "I didn't make this purchase at Melody Live Tickets.",
                OPENED
            ),
            CaseMessage(
                Party.BANK,
                "Thanks for letting us know. We're reviewing this charge and will update you here within 5 business days.",
                OPENED.plusSeconds(300)
            )
        )
    )

    fun samanta(chat: ChatState = ChatState(), selected: Boolean = false) = SupportUiState.Content(
        cases = listOf(dispute),
        faqs = faqs,
        chat = chat,
        selectedCase = dispute.takeIf { selected },
        isOffline = false
    )
}
