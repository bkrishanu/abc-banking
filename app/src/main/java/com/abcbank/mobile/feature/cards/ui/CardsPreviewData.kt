package com.abcbank.mobile.feature.cards.ui

import com.abcbank.mobile.core.model.Card
import com.abcbank.mobile.core.model.CardArt
import com.abcbank.mobile.core.model.CardChannels
import com.abcbank.mobile.core.model.CardLimits
import com.abcbank.mobile.core.model.CardNetwork
import com.abcbank.mobile.core.model.CardSecrets
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import com.abcbank.mobile.core.security.StepUpState
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset

/** Samanta's cards (DEMO_DATA.md section 03) for previews and screenshot tests. */
internal object CardsPreviewData {
    private val TODAY: LocalDate = LocalDate.of(2026, 10, 19)

    private fun activity(
        id: String,
        merchant: String,
        category: TransactionCategory,
        signedCents: Long,
        hour: Int,
        minute: Int,
        pending: Boolean = false
    ) = CardActivityItem(
        Transaction(
            id = id,
            accountId = "main-card",
            direction = if (signedCents < 0) {
                TransactionDirection.DEBIT
            } else {
                TransactionDirection.CREDIT
            },
            amount = Money(kotlin.math.abs(signedCents)),
            merchant = merchant,
            category = category,
            reference = id,
            runningBalance = Money.ZERO,
            status = if (pending) TransactionStatus.PENDING else TransactionStatus.POSTED,
            disputed = false,
            createdAt = TODAY.atTime(hour, minute).toInstant(ZoneOffset.UTC),
            clearsAt = null
        ),
        ActivityDay.TODAY,
        TODAY,
        LocalTime.of(hour, minute)
    )

    private fun card(
        id: String,
        tag: String,
        network: CardNetwork,
        art: CardArt,
        last4: String,
        expiry: YearMonth,
        limits: CardLimits,
        frozen: Boolean = false
    ) = Card(
        id = id,
        accountId = id,
        tag = tag,
        network = network,
        art = art,
        holderName = "SAMANTA JOHNS",
        last4 = last4,
        expiry = expiry,
        frozen = frozen,
        limits = limits,
        channels = CardChannels(international = true, online = true, contactless = true),
        stackOrder = 0
    )

    private val main = CardItem(
        card(
            "main-card",
            "Main card",
            CardNetwork.MASTERCARD,
            CardArt.PEACH,
            "3144",
            YearMonth.of(2029, 8),
            CardLimits(Money(100_000), Money(250_000), Money(500_000), Money(1_000_000))
        ),
        balance = Money(15_583_200),
        isCredit = false,
        notices = emptyList(),
        // Samanta's three newest checking transactions (DEMO_DATA.md section 03).
        recentActivity = listOf(
            activity(
                "tx-001",
                "Aster & Vine Boutique",
                TransactionCategory.SHOPPING,
                -12_500,
                15,
                14
            ),
            activity(
                "tx-002",
                "Northwind Furniture",
                TransactionCategory.SHOPPING,
                -37_500,
                11,
                40,
                pending = true
            ),
            activity("tx-003", "Helen Torres", TransactionCategory.TRANSFER_IN, 3_860, 8, 9)
        )
    )

    private val signature = CardItem(
        card(
            "signature-card",
            "Signature Card",
            CardNetwork.VISA,
            CardArt.SAGE,
            "5821",
            YearMonth.of(2028, 11),
            CardLimits(Money(50_000), Money(200_000), null, null)
        ),
        balance = Money(3_823_500),
        isCredit = true,
        notices = emptyList()
    )

    private val travel = CardItem(
        card(
            "travel-card",
            "Travel card",
            CardNetwork.MASTERCARD,
            CardArt.GRAPHITE,
            "7720",
            YearMonth.of(2030, 3),
            CardLimits(Money(80_000), Money(250_000), Money(300_000), Money(1_000_000))
        ),
        balance = Money(1_203_200),
        isCredit = false,
        notices = listOf(
            NoticeItem(
                "lisbon",
                "Lisbon, Portugal",
                TODAY.plusDays(21),
                TODAY.plusDays(35),
                active = false
            )
        )
    )

    fun samanta(
        selected: CardItem = main,
        revealed: CardSecrets? = null,
        frozen: Boolean = false,
        walletAdded: Boolean = false
    ): CardsUiState.Content {
        val chosen = if (frozen) {
            selected.copy(
                card = selected.card.copy(frozen = true)
            )
        } else {
            selected
        }
        return CardsUiState.Content(
            cards = listOf(main, signature, travel).map { item ->
                if (item.card.id == chosen.card.id) chosen else item
            },
            selected = chosen,
            revealed = revealed,
            limits = LimitDraft(chosen.card.limits.atmDaily, chosen.card.limits.retailDaily),
            limitsChanged = false,
            walletAdded = walletAdded,
            balancesHidden = false,
            isOffline = false,
            stepUp = StepUpState(),
            message = null,
            noticeError = null,
            noticesAdded = 0
        )
    }

    fun travelRevealed() = samanta(
        selected = travel,
        revealed = CardSecrets(pan = "5399000000027720", cvv = "772"),
        walletAdded = true
    )

    fun signatureFrozen() = samanta(selected = signature, frozen = true)
}
