package com.abcbank.mobile.feature.payments.ui

import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.security.StepUpState
import com.abcbank.mobile.feature.payments.domain.Bill
import com.abcbank.mobile.feature.payments.domain.BillListing
import com.abcbank.mobile.feature.payments.domain.BillRules
import com.abcbank.mobile.feature.payments.domain.BillStatus
import com.abcbank.mobile.feature.payments.domain.Biller
import java.time.LocalDate
import java.time.ZoneOffset

/** Samanta's bills (DEMO_DATA.md section 03) and the biller directory, for previews and tests. */
internal object BillPayPreviewData {
    val today: LocalDate = LocalDate.of(2026, 10, 20)

    val directory = listOf(
        Biller("brightline-electric", "Brightline Electric", "Electricity"),
        Biller("city-water", "City Water & Sewer", "Water"),
        Biller("metro-gas", "Metro Gas Co.", "Gas"),
        Biller("streamnet", "StreamNet Fiber", "Internet"),
        Biller("volt-mobile", "Volt Mobile", "Mobile"),
        Biller("skyline-tv", "Skyline TV", "Cable TV"),
        Biller("harbor-mutual", "Harbor Mutual Insurance", "Insurance"),
        Biller("fairview-county", "Fairview County Treasurer", "Property tax")
    )

    private fun listing(
        billerId: String,
        cents: Long,
        dueIn: Long,
        autoPay: Boolean = false,
        scheduledIn: Long? = null,
        paid: Boolean = false
    ): BillListing {
        val due = today.plusDays(dueIn)
        val scheduled = scheduledIn?.let { today.plusDays(it) }
        val status = when {
            paid -> BillStatus.PAID
            scheduled != null -> BillStatus.SCHEDULED
            else -> BillStatus.DUE
        }
        val bill = Bill(
            id = billerId,
            billerId = billerId,
            amountDue = Money(cents),
            dueDate = due.atStartOfDay(ZoneOffset.UTC).toInstant(),
            autoPay = autoPay,
            scheduledFor = scheduled?.atStartOfDay(ZoneOffset.UTC)?.toInstant(),
            status = status,
            paidAt = if (paid) today.atStartOfDay(ZoneOffset.UTC).toInstant() else null,
            accountLast4 = null
        )
        return BillListing(
            bill = bill,
            biller = directory.first { it.id == billerId },
            dueDay = due,
            scheduledDay = scheduled,
            paidDay = if (paid) today else null,
            daysUntilDue = dueIn,
            dueSoon = BillRules.isDueSoon(bill, due, today)
        )
    }

    val bills = listOf(
        listing("volt-mobile", 9_500, 4),
        listing("brightline-electric", 19_275, 9),
        listing("streamnet", 7_999, 12, autoPay = true),
        listing("city-water", 5_430, 15, scheduledIn = 14)
    )

    /** Samanta's list right after paying Volt Mobile today: it moves to Recently Paid. */
    val billsAfterPaying = listOf(
        listing("brightline-electric", 19_275, 9),
        listing("streamnet", 7_999, 12, autoPay = true),
        listing("city-water", 5_430, 15, scheduledIn = 14),
        listing("volt-mobile", 9_500, 4, paid = true)
    )

    fun samanta(
        selected: String? = null,
        form: AddBillerForm = AddBillerForm(),
        bills: List<BillListing> = this.bills
    ) = BillPayUiState.Content(
        bills = bills,
        totalDue = BillRules.totalDue(bills.map { it.bill }),
        earliestDue = bills.filter {
            it.bill.status != BillStatus.PAID
        }.minOfOrNull { it.dueDay },
        directory = directory,
        addedBillerIds = bills.map { it.biller.id }.toSet(),
        today = today,
        sources = listOf(TransferPreviewData.checking, TransferPreviewData.savings),
        source = TransferPreviewData.checking,
        selected = bills.firstOrNull { it.bill.id == selected },
        form = form,
        stepUp = StepUpState(),
        paid = null,
        busy = false,
        failed = false,
        balancesHidden = false,
        isOffline = false
    )
}
