package com.abcbank.mobile.feature.documents

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import com.abcbank.mobile.core.model.UserProfile
import com.abcbank.mobile.feature.documents.domain.BankDocument
import com.abcbank.mobile.feature.documents.domain.DocumentFile
import com.abcbank.mobile.feature.documents.domain.DocumentRenderer
import com.abcbank.mobile.feature.documents.domain.DocumentsData
import com.abcbank.mobile.feature.documents.domain.DocumentsRepository
import com.abcbank.mobile.feature.documents.domain.DocumentsSetup
import com.abcbank.mobile.feature.documents.domain.DocumentsSetupSource
import com.abcbank.mobile.feature.documents.domain.ObserveDocumentsUseCase
import com.abcbank.mobile.feature.documents.domain.StatementRules
import com.abcbank.mobile.feature.documents.ui.DocumentsUiState
import com.abcbank.mobile.feature.documents.ui.DocumentsViewModel
import com.abcbank.mobile.feature.documents.ui.ViewerState
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val UTC = ZoneOffset.UTC
private val TODAY = LocalDate.of(2026, 10, 20)
private val NOW: Instant = TODAY.atTime(15, 0).toInstant(UTC)
private val CHECKING =
    DepositAccount(
        "checking",
        AccountType.CHECKING,
        "Everyday Checking",
        "3144",
        Money(100_000),
        Money(100_000),
        null
    )
private val CARD = CreditCardAccount(
    "credit", "Signature Credit Card", "5821",
    Money(
        500_000
    ),
    Money(30_000), Money(470_000), Money(30_000), null, NOW, false
)
private val PROFILE = UserProfile(
    personaId = PersonaId.SAMANTA,
    customerId = requireNotNull(CustomerId.parse("SAMJOH01")),
    seedVersion = 3,
    name = "Samanta Johns",
    holderName = "SAMANTA JOHNS",
    email = "samanta.johns@example.com",
    phone = "+1 (555) 010-2231",
    employment = "Employed",
    annualIncome = Money.ofDollars(185_000),
    monthlyBudget = Money.ofDollars(18_900),
    creditScore = 792,
    checkDepositLimit = Money.ofDollars(10_000),
    createdAt = NOW
)

private fun tx(
    id: String,
    accountId: String,
    date: LocalDate,
    direction: TransactionDirection,
    cents: Long,
    status: TransactionStatus = TransactionStatus.POSTED
) = Transaction(
    id, accountId, direction,
    Money(
        cents
    ),
    id, TransactionCategory.SHOPPING, id, Money.ZERO, status, false,
    date.atTime(12, 0).toInstant(UTC), null
)

class StatementRulesTest {
    @Test
    fun `statements cover the 3 months before this one, from the first of the oldest`() {
        assertEquals(
            listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 8), YearMonth.of(2026, 7)),
            StatementRules.months(TODAY)
        )
        assertEquals(
            LocalDate.of(2026, 7, 1).atStartOfDay(UTC).toInstant(),
            StatementRules.since(TODAY, UTC)
        )
    }

    @Test
    fun `balances are worked back from today, posted items only, with running balances`() {
        val transactions = listOf(
            tx("salary", "checking", LocalDate.of(2026, 9, 5), TransactionDirection.CREDIT, 50_000),
            tx("rent", "checking", LocalDate.of(2026, 9, 10), TransactionDirection.DEBIT, 20_000),
            tx(
                "october",
                "checking",
                LocalDate.of(2026, 10, 3),
                TransactionDirection.DEBIT,
                10_000
            ),
            tx(
                "pending",
                "checking",
                LocalDate.of(2026, 9, 20),
                TransactionDirection.CREDIT,
                99_000,
                TransactionStatus.PENDING
            )
        )
        val september = StatementRules.statement(
            CHECKING,
            YearMonth.of(2026, 9),
            transactions,
            PROFILE,
            UTC
        )
        // Today 1,000.00; before October's 100.00 debit 1,100.00; before September 800.00.
        assertEquals(Money(110_000), september.closing)
        assertEquals(Money(80_000), september.opening)
        assertEquals(Money(50_000), september.moneyIn)
        assertEquals(Money(20_000), september.moneyOut)
        assertEquals(listOf(Money(130_000), Money(110_000)), september.lines.map { it.balance })
        val august = StatementRules.statement(
            CHECKING,
            YearMonth.of(2026, 8),
            transactions,
            PROFILE,
            UTC
        )
        assertEquals(september.opening, august.closing)
        assertTrue(august.lines.isEmpty())
    }

    @Test
    fun `card statements count what is owed, so purchases raise it`() {
        val transactions = listOf(
            tx("shoes", "credit", LocalDate.of(2026, 9, 2), TransactionDirection.DEBIT, 10_000),
            tx("payment", "credit", LocalDate.of(2026, 9, 15), TransactionDirection.CREDIT, 5_000)
        )
        val september = StatementRules.statement(
            CARD,
            YearMonth.of(2026, 9),
            transactions,
            PROFILE,
            UTC
        )
        assertTrue(september.owedStyle)
        assertEquals(Money(30_000), september.closing)
        assertEquals(Money(25_000), september.opening)
    }

    @Test
    fun `the 1099-INT is for last year, and only when there is one`() {
        assertEquals(
            2025,
            StatementRules.taxForm(DocumentsSetup(emptyList(), Money(41_286)), PROFILE, TODAY)?.year
        )
        assertNull(StatementRules.taxForm(DocumentsSetup(emptyList(), null), PROFILE, TODAY))
    }
}

private class FakeDocumentsRepository : DocumentsRepository {
    val data = MutableStateFlow<DataResult<DocumentsData>?>(null)

    override fun observeDocumentsData(since: Instant): Flow<DataResult<DocumentsData>> =
        data.filterNotNull()
}

private class FakeRenderer : DocumentRenderer {
    val rendered = mutableListOf<String>()
    val deleted = mutableListOf<String>()

    override suspend fun render(document: BankDocument): DataResult<DocumentFile> {
        rendered += document.id
        return DataResult.Success(
            DocumentFile("/cache/${document.id}.pdf", "content://files/${document.id}")
        )
    }

    override suspend fun delete(file: DocumentFile) {
        deleted += file.path
    }
}

class DocumentsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeDocumentsRepository()
    private val renderer = FakeRenderer()

    private fun viewModel(
        setup: DocumentsSetup = DocumentsSetup(
            listOf("checking", "credit", "missing"),
            Money(41_286)
        )
    ) = DocumentsViewModel(
        savedStateHandle = SavedStateHandle(),
        observeDocuments = ObserveDocumentsUseCase(
            repository,
            object : DocumentsSetupSource {
                override suspend fun setup() = setup
            },
            ManualClock(NOW)
        ) { UTC },
        renderer = renderer
    )

    @Test
    fun `lists 3 months of statements per account, opens one, and deletes it on close`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(DocumentsUiState.Loading, awaitItem())
            repository.data.value =
                DataResult.Success(
                    DocumentsData(PROFILE, listOf(CHECKING, CARD), emptyList(), false)
                )
            val content = awaitItem() as DocumentsUiState.Content
            assertEquals(3, content.statementsByMonth.size)
            assertEquals(
                listOf("checking", "credit"),
                content.statementsByMonth.first().second.map {
                    it.account.id
                }
            )
            assertEquals("1099-int-2025", content.overview.taxForm?.id)

            viewModel.onOpen("statement-checking-2026-09")
            testScheduler.runCurrent()
            assertTrue(
                (expectMostRecentItem() as DocumentsUiState.Content).viewer is ViewerState.Ready
            )
            viewModel.onViewerClosed()
            testScheduler.runCurrent()
            assertEquals(listOf("/cache/statement-checking-2026-09.pdf"), renderer.deleted)
            assertEquals(
                ViewerState.Closed,
                (expectMostRecentItem() as DocumentsUiState.Content).viewer
            )
        }
    }

    @Test
    fun `no statement accounts and no 1099-INT is empty`() = runTest {
        val viewModel = viewModel(DocumentsSetup(emptyList(), null))
        viewModel.uiState.test {
            awaitItem()
            repository.data.value =
                DataResult.Success(DocumentsData(PROFILE, listOf(CHECKING), emptyList(), false))
            assertEquals(DocumentsUiState.Empty, awaitItem())
        }
    }
}
