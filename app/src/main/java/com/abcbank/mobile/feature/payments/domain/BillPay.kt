package com.abcbank.mobile.feature.payments.domain

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.LowBalanceRule
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.Flow

/** A catalog biller (DEMO_DATA.md section 07, Billers). [category] is the catalog's label. */
data class Biller(val id: String, val name: String, val category: String)

enum class BillStatus { DUE, SCHEDULED, PAID }

/** A `bills/{billId}` document. Seeded bills use the biller id as their id, one per biller. */
data class Bill(
    val id: String,
    val billerId: String,
    val amountDue: Money,
    val dueDate: Instant,
    val autoPay: Boolean,
    val scheduledFor: Instant?,
    val status: BillStatus,
    val paidAt: Instant?,
    /** The customer's account number with the biller, last 4; null for seeded bills. */
    val accountLast4: String?
)

data class BillsData(val bills: List<Bill>, val accounts: List<Account>, val isFromCache: Boolean)

/** The bundled biller directory, like the bank directory: works offline. */
interface BillerDirectory {
    suspend fun billers(): List<Biller>
}

/** A fetched bill for a biller the customer adds (MobileBuild.md 3.3, bill fetch simulation). */
data class FetchedBill(
    val biller: Biller,
    val accountLast4: String,
    val amountDue: Money,
    val dueDate: Instant,
    val dueDay: LocalDate
)

/** What paying a bill writes: the debit, its alerts, and the bill marked paid. */
data class BillPaymentPlan(val transfer: TransferPlan, val billId: String, val paidAt: Instant)

interface BillsRepository {
    fun observeBills(): Flow<DataResult<BillsData>>

    suspend fun addBill(bill: FetchedBill): DataResult<Unit>

    suspend fun payBill(plan: BillPaymentPlan): DataResult<Unit>

    /** Null clears the schedule. */
    suspend fun schedule(billId: String, date: Instant?): DataResult<Unit>

    suspend fun setAutoPay(billId: String, autoPay: Boolean): DataResult<Unit>
}

enum class BillAccountError { FORMAT }

/** Bill pay rules (MobileBuild.md 3.3, DEMO_DATA.md section 07). Pure, so each is unit-tested. */
object BillRules {
    private val FETCH_MIN = Money.ofDollars(20)
    private val FETCH_MAX = Money.ofDollars(250)
    private const val DUE_FIRST_DAY = 7
    private const val DUE_LAST_DAY = 20
    private val ACCOUNT_DIGITS = 6..17
    private const val LAST4 = 4
    private const val CATEGORY_PROPERTY_TAX = "Property tax"
    private const val CATEGORY_INSURANCE = "Insurance"

    /** An unpaid bill due within this many days shows its due badge in warning (2026-10-25). */
    const val DUE_SOON_DAYS = 7L

    fun validateAccountNumber(number: String): BillAccountError? =
        if (number.length in ACCOUNT_DIGITS &&
            number.all { it in '0'..'9' }
        ) {
            null
        } else {
            BillAccountError.FORMAT
        }

    /**
     * The simulated fetch: an amount from $20.00 to $250.00 and a due date 7 to 20 days ahead,
     * both from a hash of the account number, so the same number always fetches the same bill.
     */
    fun fetch(biller: Biller, accountNumber: String, today: LocalDate, zone: ZoneId): FetchedBill {
        val hash = "${biller.id}:$accountNumber".hashCode().toLong().absoluteValue
        val span = FETCH_MAX.cents - FETCH_MIN.cents + 1
        val amount = Money(FETCH_MIN.cents + hash % span)
        val days = DUE_FIRST_DAY + (hash / span % (DUE_LAST_DAY - DUE_FIRST_DAY + 1))
        val due = today.plusDays(days).atStartOfDay(zone).toInstant()
        return FetchedBill(biller, accountNumber.takeLast(LAST4), amount, due, today.plusDays(days))
    }

    /** Utility bills are Utilities spending; property tax and home insurance are Housing. */
    fun transactionCategory(biller: Biller): TransactionCategory = when (biller.category) {
        CATEGORY_PROPERTY_TAX, CATEGORY_INSURANCE -> TransactionCategory.HOUSING
        else -> TransactionCategory.UTILITIES
    }

    /** Days from today to the due day; negative once it has passed. */
    fun daysUntil(due: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(today, due)

    /**
     * Due within [DUE_SOON_DAYS] days, or already overdue. Only bills still to pay: paid and
     * scheduled ones are taken care of.
     */
    fun isDueSoon(bill: Bill, due: LocalDate, today: LocalDate): Boolean =
        bill.status == BillStatus.DUE && daysUntil(due, today) <= DUE_SOON_DAYS

    /** What the unpaid bills (due or scheduled) add up to, for the Pay Bills summary. */
    fun totalDue(bills: List<Bill>): Money =
        Money(bills.filter { it.status != BillStatus.PAID }.sumOf { it.amountDue.cents })

    /** A payment date from tomorrow up to the due date (DEMO_DATA.md: scheduled before it is due). */
    fun canScheduleFor(date: LocalDate, today: LocalDate, due: LocalDate): Boolean =
        date.isAfter(today) && !date.isAfter(due)

    /** The debit, the bill paid alert and a low balance alert on crossing (PRD section 08). */
    fun plan(
        source: DepositAccount,
        bill: Bill,
        biller: Biller,
        reference: String,
        now: Instant
    ): BillPaymentPlan {
        val amount = bill.amountDue
        val after = source.copy(
            available = source.available - amount,
            ledger =
            source.ledger - amount
        )
        val idPrefix = "bill-$reference"
        val notifications = buildList {
            add(
                NewNotification(
                    "$idPrefix-paid",
                    NotificationType.BILL_PAID,
                    listOf(amount.cents.toString(), biller.name),
                    now
                )
            )
            if (!LowBalanceRule.isLow(source) && LowBalanceRule.isLow(after)) {
                add(
                    NewNotification(
                        "$idPrefix-low",
                        NotificationType.LOW_BALANCE,
                        listOf(after.nickname),
                        now
                    )
                )
            }
        }
        val transfer = TransferPlan(
            accounts = listOf(AccountUpdate(source.id, after.available, after.ledger)),
            transactions = listOf(
                NewTransaction(
                    id = "$idPrefix-main",
                    accountId = source.id,
                    direction = TransactionDirection.DEBIT,
                    amount = amount,
                    merchant = biller.name,
                    category = transactionCategory(biller),
                    reference = reference,
                    runningBalance = after.ledger,
                    memo = null,
                    createdAt = now
                )
            ),
            notifications = notifications,
            mainTransactionId = "$idPrefix-main",
            reference = reference
        )
        return BillPaymentPlan(transfer, bill.id, now)
    }
}
