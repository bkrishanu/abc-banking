package com.abcbank.mobile.feature.payments.domain

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.security.StepUpAction
import com.abcbank.mobile.core.security.StepUpGrant
import com.abcbank.mobile.core.security.StepUpManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Bill pay use cases (MobileBuild.md 3.3). Adding a biller is adding a payee and paying more
// than $1,000 is an outbound payment, so both check step-up here (MobileBuild.md 1.2).

/**
 * A bill with its biller and its dates as local days; bills whose biller is not in the directory
 * are left out.
 */
data class BillListing(
    val bill: Bill,
    val biller: Biller,
    val dueDay: LocalDate,
    val scheduledDay: LocalDate?,
    val paidDay: LocalDate?,
    /** Days from today to [dueDay]; negative once it has passed. */
    val daysUntilDue: Long = 0,
    /** Unpaid and due within a week (or overdue): the badge shows in warning. */
    val dueSoon: Boolean = false
)

data class BillsOverview(
    /** Unpaid first by due date, then paid ones, newest first. */
    val bills: List<BillListing>,
    val directory: List<Biller>,
    val accounts: List<Account>,
    val today: LocalDate,
    val isFromCache: Boolean,
    /** The unpaid bills' total and the first of their due days, for the summary card. */
    val totalDue: Money = Money.ZERO,
    val earliestDue: LocalDate? = null
)

class ObserveBillsUseCase @Inject constructor(
    private val repository: BillsRepository,
    private val directory: BillerDirectory,
    private val clock: Clock,
    private val zone: Provider<ZoneId>
) {
    operator fun invoke(): Flow<DataResult<BillsOverview>> =
        repository.observeBills().map { result ->
            when (result) {
                is DataResult.Failure -> result
                is DataResult.Success -> {
                    val billers = runCatching { directory.billers() }.getOrElse {
                        return@map DataResult.Failure(DataError.MALFORMED_DATA)
                    }
                    val byId = billers.associateBy { it.id }
                    val zoneId = zone.get()
                    fun Instant.day(): LocalDate = atZone(zoneId).toLocalDate()
                    val today = clock.now().day()
                    val listings = result.value.bills.mapNotNull { bill ->
                        byId[bill.billerId]?.let {
                            val due = bill.dueDate.day()
                            BillListing(
                                bill,
                                it,
                                due,
                                bill.scheduledFor?.day(),
                                bill.paidAt?.day(),
                                daysUntilDue = BillRules.daysUntil(due, today),
                                dueSoon = BillRules.isDueSoon(bill, due, today)
                            )
                        }
                    }
                    val (paid, open) = listings.partition { it.bill.status == BillStatus.PAID }
                    DataResult.Success(
                        BillsOverview(
                            bills =
                            open.sortedBy { it.bill.dueDate } +
                                paid.sortedByDescending { it.bill.paidAt },
                            directory = billers,
                            accounts = result.value.accounts,
                            today = today,
                            isFromCache = result.value.isFromCache,
                            totalDue = BillRules.totalDue(open.map { it.bill }),
                            earliestDue = open.minOfOrNull { it.dueDay }
                        )
                    )
                }
            }
        }
}

sealed interface FetchResult {
    data class Fetched(val bill: FetchedBill) : FetchResult

    data class Invalid(val error: BillAccountError) : FetchResult
}

class FetchBillUseCase @Inject constructor(
    private val clock: Clock,
    private val zone: Provider<ZoneId>
) {
    operator fun invoke(biller: Biller, accountNumber: String): FetchResult {
        BillRules.validateAccountNumber(accountNumber)?.let { return FetchResult.Invalid(it) }
        val zoneId = zone.get()
        return FetchResult.Fetched(
            BillRules.fetch(biller, accountNumber, clock.now().atZone(zoneId).toLocalDate(), zoneId)
        )
    }
}

sealed interface BillResult {
    data object Done : BillResult

    data class Paid(val plan: BillPaymentPlan) : BillResult

    data object StepUpRequired : BillResult

    /** More than the account has, the bill is already paid, or the date is out of range. */
    data object Invalid : BillResult

    data class Failed(val error: DataError) : BillResult
}

private fun DataResult<Unit>.toBillResult(): BillResult = when (this) {
    is DataResult.Success -> BillResult.Done
    is DataResult.Failure -> BillResult.Failed(error)
}

class AddBillerUseCase @Inject constructor(
    private val repository: BillsRepository,
    private val stepUp: StepUpManager
) {
    suspend operator fun invoke(bill: FetchedBill, grant: StepUpGrant?): BillResult {
        if (!stepUp.consume(grant, StepUpAction.AddBeneficiary)) return BillResult.StepUpRequired
        return repository.addBill(bill).toBillResult()
    }
}

class PayBillUseCase @Inject constructor(
    private val repository: BillsRepository,
    private val stepUp: StepUpManager,
    private val clock: Clock
) {
    suspend operator fun invoke(
        listing: BillListing,
        source: Account,
        grant: StepUpGrant?
    ): BillResult {
        val bill = listing.bill
        if (bill.status == BillStatus.PAID ||
            source !is DepositAccount ||
            !TransferRules.canSendFrom(source)
        ) {
            return BillResult.Invalid
        }
        if (TransferRules.validateSend(bill.amountDue, Money.ZERO, source) != null) {
            return BillResult.Invalid
        }
        if (!stepUp.consume(
                grant,
                StepUpAction.OutboundTransfer(bill.amountDue)
            )
        ) {
            return BillResult.StepUpRequired
        }
        val plan = BillRules.plan(
            source,
            bill,
            listing.biller,
            UUID.randomUUID().toString(),
            clock.now()
        )
        return when (val result = repository.payBill(plan)) {
            is DataResult.Success -> BillResult.Paid(plan)
            is DataResult.Failure -> BillResult.Failed(result.error)
        }
    }
}

class ScheduleBillUseCase @Inject constructor(
    private val repository: BillsRepository,
    private val clock: Clock,
    private val zone: Provider<ZoneId>
) {
    /** A null [date] cancels the scheduled payment. */
    suspend operator fun invoke(bill: Bill, date: LocalDate?): BillResult {
        if (bill.status == BillStatus.PAID) return BillResult.Invalid
        val zoneId = zone.get()
        if (date == null) return repository.schedule(bill.id, null).toBillResult()
        val today = clock.now().atZone(zoneId).toLocalDate()
        if (!BillRules.canScheduleFor(
                date,
                today,
                bill.dueDate.atZone(zoneId).toLocalDate()
            )
        ) {
            return BillResult.Invalid
        }
        return repository.schedule(bill.id, date.atStartOfDay(zoneId).toInstant()).toBillResult()
    }
}

class SetAutoPayUseCase @Inject constructor(private val repository: BillsRepository) {
    suspend operator fun invoke(bill: Bill, autoPay: Boolean): BillResult = if (bill.autoPay ==
        autoPay
    ) {
        BillResult.Done
    } else {
        repository.setAutoPay(bill.id, autoPay).toBillResult()
    }
}
