package com.abcbank.mobile.feature.cards

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.Card
import com.abcbank.mobile.core.model.CardArt
import com.abcbank.mobile.core.model.CardChannels
import com.abcbank.mobile.core.model.CardLimits
import com.abcbank.mobile.core.model.CardNetwork
import com.abcbank.mobile.core.model.CardSecrets
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import com.abcbank.mobile.core.model.TravelNotice
import com.abcbank.mobile.core.security.MpinVerifier
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.core.security.StepUpAction
import com.abcbank.mobile.core.security.StepUpControllerFactory
import com.abcbank.mobile.core.security.StepUpGrant
import com.abcbank.mobile.core.security.StepUpManager
import com.abcbank.mobile.feature.cards.domain.AddTravelNoticeUseCase
import com.abcbank.mobile.feature.cards.domain.CardRules
import com.abcbank.mobile.feature.cards.domain.CardWithNotices
import com.abcbank.mobile.feature.cards.domain.CardsData
import com.abcbank.mobile.feature.cards.domain.CardsRepository
import com.abcbank.mobile.feature.cards.domain.LimitError
import com.abcbank.mobile.feature.cards.domain.LimitsResult
import com.abcbank.mobile.feature.cards.domain.ObserveCardsUseCase
import com.abcbank.mobile.feature.cards.domain.RevealCardDetailsUseCase
import com.abcbank.mobile.feature.cards.domain.SetCardFrozenUseCase
import com.abcbank.mobile.feature.cards.domain.StepUpResult
import com.abcbank.mobile.feature.cards.domain.TravelNoticeError
import com.abcbank.mobile.feature.cards.domain.TravelNoticeResult
import com.abcbank.mobile.feature.cards.domain.UpdateCardChannelsUseCase
import com.abcbank.mobile.feature.cards.domain.UpdateCardLimitsUseCase
import com.abcbank.mobile.feature.cards.ui.CardsMessage
import com.abcbank.mobile.feature.cards.ui.CardsUiState
import com.abcbank.mobile.feature.cards.ui.CardsViewModel
import com.abcbank.mobile.testing.FakeMpinAttemptStore
import com.abcbank.mobile.testing.MainDispatcherRule
import com.abcbank.mobile.testing.ManualClock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val NOW: Instant = Instant.parse("2026-10-19T13:00:00Z")
private val TODAY: LocalDate = LocalDate.of(2026, 10, 19)
private val UTC = ZoneOffset.UTC

private fun card(id: String, debit: Boolean = true, stackOrder: Int = 0) = Card(
    id = id,
    accountId = if (debit) "checking" else "credit",
    tag = id,
    network = if (debit) CardNetwork.MASTERCARD else CardNetwork.VISA,
    art = CardArt.PEACH,
    holderName = "SAMANTA JOHNS",
    last4 = "3144",
    expiry = YearMonth.of(2029, 8),
    frozen = false,
    limits = if (debit) {
        CardLimits(Money(100_000), Money(250_000), Money(500_000), Money(1_000_000))
    } else {
        CardLimits(Money(50_000), Money(200_000), null, null)
    },
    channels = CardChannels(international = true, online = true, contactless = true),
    stackOrder = stackOrder
)

private val MAIN = card("main-card")
private val SIGNATURE = card("signature-card", debit = false, stackOrder = 1)
private val ACCOUNTS = listOf(
    DepositAccount(
        "checking",
        AccountType.CHECKING,
        "Everyday Checking",
        "3144",
        Money(15_583_200),
        Money(15_620_700),
        null
    ),
    CreditCardAccount(
        "credit", "Signature Credit Card", "5821",
        Money(
            5_000_000
        ),
        Money(1_176_500), Money(3_823_500),
        Money(1_176_500), null, NOW, false
    )
)

private class FakeCardsRepository : CardsRepository {
    val data = MutableStateFlow<DataResult<CardsData>?>(null)
    val calls = mutableListOf<String>()

    var requestedSince: Instant? = null

    override fun observeCards(transactionsSince: Instant): Flow<DataResult<CardsData>> {
        requestedSince = transactionsSince
        return data.filterNotNull()
    }

    override suspend fun loadSecrets(cardId: String): DataResult<CardSecrets> {
        calls += "secrets $cardId"
        return DataResult.Success(CardSecrets("5399000000093144", "314"))
    }

    override suspend fun setFrozen(cardId: String, frozen: Boolean): DataResult<Unit> {
        calls += "frozen $cardId $frozen"
        return DataResult.Success(Unit)
    }

    override suspend fun setChannels(cardId: String, channels: CardChannels): DataResult<Unit> {
        calls += "channels $cardId ${channels.international}"
        return DataResult.Success(Unit)
    }

    override suspend fun setLimits(
        cardId: String,
        atmDaily: Money,
        retailDaily: Money?
    ): DataResult<Unit> {
        calls += "limits $cardId ${atmDaily.cents} ${retailDaily?.cents}"
        return DataResult.Success(Unit)
    }

    override suspend fun addTravelNotice(
        cardId: String,
        destination: String,
        start: Instant,
        end: Instant
    ): DataResult<Unit> {
        calls += "notice $cardId $destination"
        return DataResult.Success(Unit)
    }
}

class CardRulesTest {
    @Test
    fun `slider steps are $100, or $500 for limits above $10,000`() {
        assertEquals(Money.ofDollars(100), CardRules.step(Money.ofDollars(2_500)))
        assertEquals(Money.ofDollars(100), CardRules.step(Money.ofDollars(10_000)))
        assertEquals(Money.ofDollars(500), CardRules.step(Money.ofDollars(50_000)))
    }

    @Test
    fun `limits must be on a step, at least one step and at most the maximum`() {
        val max = Money.ofDollars(2_500)
        assertNull(CardRules.validateLimit(Money.ofDollars(1_200), max))
        assertEquals(LimitError.BELOW_MINIMUM, CardRules.validateLimit(Money.ofDollars(50), max))
        assertEquals(LimitError.ABOVE_MAXIMUM, CardRules.validateLimit(Money.ofDollars(2_600), max))
        assertEquals(LimitError.NOT_ON_STEP, CardRules.validateLimit(Money.ofDollars(1_250), max))
        assertNull(CardRules.validateLimits(SIGNATURE, Money.ofDollars(1_000), null))
    }

    @Test
    fun `recent activity is the card account's three newest transactions`() {
        fun tx(id: String, accountId: String, hoursAgo: Long) = Transaction(
            id = id,
            accountId = accountId,
            direction = TransactionDirection.DEBIT,
            amount = Money(1_000),
            merchant = id,
            category = TransactionCategory.SHOPPING,
            reference = id,
            runningBalance = Money.ZERO,
            status = TransactionStatus.POSTED,
            disputed = false,
            createdAt = NOW.minusSeconds(hoursAgo * 3_600),
            clearsAt = null
        )
        val transactions = listOf(
            tx("old", "checking", 50),
            tx("savings", "savings", 1),
            tx("newest", "checking", 2),
            tx("second", "checking", 5),
            tx("third", "checking", 9)
        )
        assertEquals(
            listOf("newest", "second", "third"),
            CardRules.recentActivity(transactions, "checking").map { it.id }
        )
    }

    @Test
    fun `cards ask for today and the 29 days before it`() {
        val repository = FakeCardsRepository()
        ObserveCardsUseCase(repository, ManualClock(NOW)) { UTC }()
        assertEquals(
            TODAY.minusDays(29).atStartOfDay(UTC).toInstant(),
            repository.requestedSince
        )
    }

    @Test
    fun `travel notices need a destination and sensible dates`() {
        fun check(destination: String, start: Long, end: Long) = CardRules.validateTravelNotice(
            destination,
            TODAY.plusDays(start),
            TODAY.plusDays(end),
            TODAY
        )

        assertNull(check("Lisbon, Portugal", 0, 14))
        assertEquals(TravelNoticeError.DESTINATION_MISSING, check("  ", 1, 2))
        assertEquals(TravelNoticeError.DESTINATION_TOO_LONG, check("x".repeat(41), 1, 2))
        assertEquals(TravelNoticeError.STARTS_IN_PAST, check("Rome", -1, 2))
        assertEquals(TravelNoticeError.ENDS_BEFORE_START, check("Rome", 5, 4))
        assertEquals(TravelNoticeError.TOO_LONG, check("Rome", 1, 367))
    }

    @Test
    fun `a notice is active from its first through its last day`() {
        fun notice(start: Long, end: Long) = TravelNotice(
            "n",
            "Toronto",
            TODAY.plusDays(start).atStartOfDay(UTC).toInstant(),
            TODAY.plusDays(end).atStartOfDay(UTC).toInstant()
        )
        assertTrue(CardRules.isActive(notice(-3, 10), TODAY, UTC))
        assertTrue(CardRules.isActive(notice(0, 0), TODAY, UTC))
        assertFalse(CardRules.isActive(notice(21, 35), TODAY, UTC))
    }
}

class CardUseCasesTest {
    private val repository = FakeCardsRepository()
    private val stepUp = StepUpManager(ManualClock(NOW))

    @Test
    fun `revealing needs a grant for that card, used once`() = runTest {
        val reveal = RevealCardDetailsUseCase(repository, stepUp)
        assertEquals(StepUpResult.StepUpRequired, reveal("main-card", null))
        assertTrue(repository.calls.isEmpty())

        // A grant issued for one card does not reveal another.
        val controller = StepUpControllerFactory(
            MpinVerifier(FakeMpinAttemptStore(), ManualClock(NOW)),
            stepUp,
            ManualClock(NOW)
        )
            .create(this)
        var grant: StepUpGrant? = null
        controller.request(StepUpAction.RevealCardDetails("main-card")) { grant = it }
        testScheduler.runCurrent()
        "123456".forEach { controller.onDigit(it.digitToInt()) }
        testScheduler.runCurrent()

        assertEquals(StepUpResult.StepUpRequired, reveal("signature-card", grant))
        assertTrue(reveal("main-card", grant) is StepUpResult.Done)
        assertEquals(StepUpResult.StepUpRequired, reveal("main-card", grant))
    }

    @Test
    fun `limits are validated before step-up, then need it`() = runTest {
        val update = UpdateCardLimitsUseCase(repository, stepUp)
        assertEquals(
            LimitsResult.Invalid(LimitError.ABOVE_MAXIMUM),
            update(MAIN, Money.ofDollars(9_000), Money.ofDollars(5_000), null)
        )
        assertEquals(
            LimitsResult.StepUpRequired,
            update(MAIN, Money.ofDollars(1_200), Money.ofDollars(5_000), null)
        )
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `travel notices are validated and stored at the start of each local day`() = runTest {
        val add = AddTravelNoticeUseCase(repository, ManualClock(NOW)) { UTC }
        assertEquals(
            TravelNoticeResult.Invalid(TravelNoticeError.STARTS_IN_PAST),
            add("main-card", "Rome", TODAY.minusDays(1), TODAY)
        )
        assertEquals(
            TravelNoticeResult.Added,
            add("main-card", "  Rome ", TODAY, TODAY.plusDays(3))
        )
        assertEquals(listOf("notice main-card Rome"), repository.calls)
    }
}

class CardsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCardsRepository()
    private val clock = ManualClock(NOW)
    private val stepUp = StepUpManager(clock)
    private val session = SessionManager()

    private fun viewModel() = CardsViewModel(
        observeCards = ObserveCardsUseCase(repository, clock) { UTC },
        revealCardDetails = RevealCardDetailsUseCase(repository, stepUp),
        setCardFrozen = SetCardFrozenUseCase(repository),
        updateChannels = UpdateCardChannelsUseCase(repository),
        updateLimits = UpdateCardLimitsUseCase(repository, stepUp),
        addTravelNotice = AddTravelNoticeUseCase(repository, clock) { UTC },
        sessionManager = session,
        stepUpFactory = StepUpControllerFactory(
            MpinVerifier(FakeMpinAttemptStore(), clock),
            stepUp,
            clock
        ),
        clock = clock,
        zone = { UTC }
    )

    private fun load() {
        repository.data.value = DataResult.Success(
            CardsData(
                listOf(CardWithNotices(MAIN, emptyList()), CardWithNotices(SIGNATURE, emptyList())),
                ACCOUNTS,
                false
            )
        )
    }

    private suspend fun ReceiveTurbine<CardsUiState>.content(): CardsUiState.Content =
        awaitItem() as CardsUiState.Content

    private fun CardsViewModel.enterMpin() = "123456".forEach { onStepUpDigit(it.digitToInt()) }

    @Test
    fun `shows the stack with each card's spendable balance, Main card selected`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(CardsUiState.Loading, awaitItem())
            load()
            val content = content()
            assertEquals("main-card", content.selected.card.id)
            assertEquals(Money(15_583_200), content.cards[0].balance)
            assertEquals(Money(3_823_500), content.cards[1].balance)
            assertTrue(content.cards[1].isCredit)
        }
    }

    @Test
    fun `details are revealed only after step-up, and switching card hides them`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            skipItems(1)
            load()
            skipItems(1)

            viewModel.onToggleDetails()
            assertTrue(content().stepUp.isVisible)
            assertTrue(repository.calls.isEmpty())

            viewModel.enterMpin()
            // The MPIN check runs as queued work; run it before reading the latest state.
            testScheduler.runCurrent()
            val revealed = expectMostRecentItem() as CardsUiState.Content
            assertEquals("314", revealed.revealed?.cvv)
            assertFalse(revealed.stepUp.isVisible)

            viewModel.onSelectCard("signature-card")
            val switched = content()
            assertEquals("signature-card", switched.selected.card.id)
            assertNull(switched.revealed)
        }
    }

    @Test
    fun `leaving the screen wipes revealed details`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            skipItems(1)
            load()
            skipItems(1)
            viewModel.onToggleDetails()
            viewModel.enterMpin()
            // The MPIN check runs as queued work; run it before reading the latest state.
            testScheduler.runCurrent()
            assertEquals("314", (expectMostRecentItem() as CardsUiState.Content).revealed?.cvv)

            viewModel.onHideDetails()
            assertNull(content().revealed)
        }
    }

    @Test
    fun `limit changes are drafts until saved with step-up`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            skipItems(1)
            load()
            skipItems(1)

            viewModel.onAtmLimitChange(Money.ofDollars(1_500))
            assertTrue(content().limitsChanged)

            viewModel.onSaveLimits()
            assertTrue(content().stepUp.isVisible)
            viewModel.enterMpin()
            // The MPIN check runs as queued work; run it before reading the latest state.
            testScheduler.runCurrent()
            val saved = expectMostRecentItem() as CardsUiState.Content
            assertEquals(CardsMessage.LIMITS_SAVED, saved.message)
            assertEquals(listOf("limits main-card 150000 500000"), repository.calls)
        }
    }

    @Test
    fun `freeze, channels and wallet act on the selected card`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            skipItems(1)
            load()
            skipItems(1)

            viewModel.onSetFrozen(true)
            viewModel.onChannelsChange(MAIN.channels.copy(international = false))
            viewModel.onAddToWallet()
            assertTrue(content().walletAdded)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("frozen main-card true", "channels main-card false"), repository.calls)
    }
}
