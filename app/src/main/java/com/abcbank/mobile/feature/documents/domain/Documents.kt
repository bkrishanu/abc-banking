package com.abcbank.mobile.feature.documents.domain

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import com.abcbank.mobile.core.model.UserProfile
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Statements and documents (MobileBuild.md 6, PRD feature 14, DEMO_DATA.md section 01).

/** The persona file's `documents` block: which accounts get statements, and the 1099-INT. */
data class DocumentsSetup(val statementAccountIds: List<String>, val interest1099: Money?)

/** Read from the bundled persona file, like the persona's name on the lock screen. */
interface DocumentsSetupSource {
    suspend fun setup(): DocumentsSetup?
}

data class DocumentsData(
    val profile: UserProfile,
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val isFromCache: Boolean
)

interface DocumentsRepository {
    /** The profile, accounts and transactions created at or after [since]. */
    fun observeDocumentsData(since: Instant): Flow<DataResult<DocumentsData>>
}

/** One posted line on a statement, with the balance after it. */
data class StatementLine(
    val date: LocalDate,
    val description: String,
    val moneyIn: Money?,
    val moneyOut: Money?,
    val balance: Money
)

sealed interface BankDocument {
    val id: String
}

data class Statement(
    val account: Account,
    val month: YearMonth,
    val customerName: String,
    val customerId: String,
    /** For cards and loans the balance is the amount owed. */
    val owedStyle: Boolean,
    val opening: Money,
    val closing: Money,
    val moneyIn: Money,
    val moneyOut: Money,
    val lines: List<StatementLine>
) : BankDocument {
    override val id: String get() = "statement-${account.id}-$month"
}

data class TaxForm(
    val year: Int,
    val interest: Money,
    val recipientName: String,
    val customerId: String
) : BankDocument {
    override val id: String get() = "1099-int-$year"
}

/** A generated PDF in the cache, shared through the FileProvider. */
data class DocumentFile(val path: String, val contentUri: String)

interface DocumentRenderer {
    suspend fun render(document: BankDocument): DataResult<DocumentFile>

    /** Generated files are deleted when the viewer closes (ARCHITECTURE.md section 04). */
    suspend fun delete(file: DocumentFile)
}

object StatementRules {
    private const val MONTHS = 3L

    /** The 3 calendar months before this one, newest first (DEMO_DATA.md section 01). */
    fun months(today: LocalDate): List<YearMonth> {
        val current = YearMonth.from(today)
        return (1..MONTHS).map { current.minusMonths(it) }
    }

    fun since(today: LocalDate, zone: ZoneId): Instant =
        YearMonth.from(today).minusMonths(MONTHS).atDay(1).atStartOfDay(zone).toInstant()

    fun canHaveStatement(account: Account): Boolean =
        account is DepositAccount || account is CreditCardAccount || account is LoanAccount

    private fun currentBalance(account: Account): Money = when (account) {
        is DepositAccount -> account.ledger
        is CreditCardAccount -> account.currentBalance
        is LoanAccount -> account.owed
        else -> Money.ZERO
    }

    private fun isOwedStyle(account: Account) =
        account is CreditCardAccount || account is LoanAccount

    /** What a posted transaction did to the balance: credits raise a deposit balance, debits raise what is owed. */
    private fun effect(transaction: Transaction, owedStyle: Boolean): Money {
        val raises = (transaction.direction == TransactionDirection.DEBIT) == owedStyle
        return if (raises) transaction.amount else -transaction.amount
    }

    /**
     * A month's statement, worked back from the account's balance today: the balance at any moment
     * is today's balance less everything posted since. Pending items are not on statements.
     */
    fun statement(
        account: Account,
        month: YearMonth,
        transactions: List<Transaction>,
        profile: UserProfile,
        zone: ZoneId
    ): Statement {
        val owed = isOwedStyle(account)
        val posted = transactions
            .filter { it.accountId == account.id && it.status == TransactionStatus.POSTED }
            .sortedWith(compareBy<Transaction> { it.createdAt }.thenBy { it.id })
        val start = month.atDay(1).atStartOfDay(zone).toInstant()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        fun balanceAt(instant: Instant): Money = posted.filter {
            !it.createdAt.isBefore(instant)
        }.fold(currentBalance(account)) { balance, tx ->
            balance -
                effect(tx, owed)
        }
        val opening = balanceAt(start)
        var running = opening
        val inMonth = posted.filter { !it.createdAt.isBefore(start) && it.createdAt.isBefore(end) }
        val lines = inMonth.map { tx ->
            running += effect(tx, owed)
            val credit = tx.direction == TransactionDirection.CREDIT
            StatementLine(
                date = tx.createdAt.atZone(zone).toLocalDate(),
                description = tx.merchant,
                moneyIn = tx.amount.takeIf { credit },
                moneyOut = tx.amount.takeUnless { credit },
                balance = running
            )
        }
        return Statement(
            account = account,
            month = month,
            customerName = profile.name,
            customerId = profile.customerId.value,
            owedStyle = owed,
            opening = opening,
            closing = running,
            moneyIn = Money(
                inMonth.filter {
                    it.direction == TransactionDirection.CREDIT
                }.sumOf { it.amount.cents }
            ),
            moneyOut = Money(
                inMonth.filter {
                    it.direction == TransactionDirection.DEBIT
                }.sumOf { it.amount.cents }
            ),
            lines = lines
        )
    }

    /** The 1099-INT for the previous tax year, or null when the persona has none (under $10). */
    fun taxForm(setup: DocumentsSetup, profile: UserProfile, today: LocalDate): TaxForm? =
        setup.interest1099?.let {
            TaxForm(today.year - 1, it, profile.name, profile.customerId.value)
        }
}

data class DocumentsOverview(
    /** Newest month first, then in the persona file's account order. */
    val statements: List<Statement>,
    val taxForm: TaxForm?,
    val taxYear: Int,
    val isFromCache: Boolean
)

class ObserveDocumentsUseCase @Inject constructor(
    private val repository: DocumentsRepository,
    private val setupSource: DocumentsSetupSource,
    private val clock: Clock,
    private val zone: Provider<ZoneId>
) {
    operator fun invoke(): Flow<DataResult<DocumentsOverview>> {
        val zoneId = zone.get()
        val today = clock.now().atZone(zoneId).toLocalDate()
        return repository.observeDocumentsData(StatementRules.since(today, zoneId)).map { result ->
            when (result) {
                is DataResult.Failure -> result
                is DataResult.Success -> {
                    val setup =
                        setupSource.setup()
                            ?: return@map DataResult.Failure(DataError.MALFORMED_DATA)
                    val data = result.value
                    val accounts = setup.statementAccountIds.mapNotNull { id ->
                        data.accounts.firstOrNull {
                            it.id == id
                        }?.takeIf(StatementRules::canHaveStatement)
                    }
                    DataResult.Success(
                        DocumentsOverview(
                            statements = StatementRules.months(today).flatMap { month ->
                                accounts.map {
                                    StatementRules.statement(
                                        it,
                                        month,
                                        data.transactions,
                                        data.profile,
                                        zoneId
                                    )
                                }
                            },
                            taxForm = StatementRules.taxForm(setup, data.profile, today),
                            taxYear = today.year - 1,
                            isFromCache = data.isFromCache
                        )
                    )
                }
            }
        }
    }
}
