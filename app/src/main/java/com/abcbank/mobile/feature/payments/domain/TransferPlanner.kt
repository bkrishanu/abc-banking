package com.abcbank.mobile.feature.payments.domain

import com.abcbank.mobile.core.common.LowBalanceRule
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Beneficiary
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import java.time.Instant

/** New balances for one account. For a line of credit, [ledger] is the amount owed. */
data class AccountUpdate(val accountId: String, val available: Money, val ledger: Money)

data class NewTransaction(
    val id: String,
    val accountId: String,
    val direction: TransactionDirection,
    val amount: Money,
    val merchant: String,
    val category: TransactionCategory,
    val reference: String,
    val runningBalance: Money,
    val memo: String?,
    val createdAt: Instant
)

enum class NotificationType { MONEY_SENT, MONEY_MOVED, LOW_BALANCE, BILL_PAID }

/** A transactional alert; the data layer turns it into text from string resources. */
data class NewNotification(
    val id: String,
    val type: NotificationType,
    val args: List<String>,
    val createdAt: Instant
)

/** Everything one transfer writes, as one batch. */
data class TransferPlan(
    val accounts: List<AccountUpdate>,
    val transactions: List<NewTransaction>,
    val notifications: List<NewNotification>,
    /** The transaction the receipt shows. */
    val mainTransactionId: String,
    val reference: String
)

/**
 * Works out a transfer's effects (ARCHITECTURE.md section 04, path of every write): debit the
 * source, credit the destination for own-account moves, one transaction per leg plus the instant
 * fee, the transactional alert, and a low balance alert when the source crosses below $500.00
 * (PRD section 08: once, on crossing). Pure, so every rule is unit-tested.
 */
object TransferPlanner {
    fun send(
        source: DepositAccount,
        beneficiary: Beneficiary,
        amount: Money,
        fee: Money,
        memo: String?,
        category: TransactionCategory,
        reference: String,
        idPrefix: String,
        now: Instant
    ): TransferPlan {
        val afterAmount = source.copy(
            available = source.available - amount,
            ledger = source.ledger - amount
        )
        val afterFee = afterAmount.copy(
            available = afterAmount.available - fee,
            ledger = afterAmount.ledger - fee
        )
        val mainId = "$idPrefix-main"
        val transactions = buildList {
            add(
                NewTransaction(
                    id = mainId,
                    accountId = source.id,
                    direction = TransactionDirection.DEBIT,
                    amount = amount,
                    merchant = beneficiary.name,
                    category = category,
                    reference = reference,
                    runningBalance = afterAmount.ledger,
                    memo = memo,
                    createdAt = now
                )
            )
            if (fee.isPositive) {
                add(
                    // A millisecond later, so the ledger lists the fee right after its transfer.
                    NewTransaction(
                        id = "$idPrefix-fee",
                        accountId = source.id,
                        direction = TransactionDirection.DEBIT,
                        amount = fee,
                        merchant = INSTANT_FEE_MERCHANT,
                        category = TransactionCategory.TRANSFER,
                        reference = "$reference-fee",
                        runningBalance = afterFee.ledger,
                        memo = null,
                        createdAt = now.plusMillis(1)
                    )
                )
            }
        }
        val notifications = buildList {
            add(
                NewNotification(
                    "$idPrefix-sent",
                    NotificationType.MONEY_SENT,
                    listOf(amount.cents.toString(), beneficiary.name),
                    now
                )
            )
            lowBalance(source, afterFee, "$idPrefix-low", now)?.let(::add)
        }
        return TransferPlan(
            accounts = listOf(AccountUpdate(source.id, afterFee.available, afterFee.ledger)),
            transactions = transactions,
            notifications = notifications,
            mainTransactionId = mainId,
            reference = reference
        )
    }

    fun move(
        source: Account,
        destination: Account,
        amount: Money,
        reference: String,
        idPrefix: String,
        now: Instant
    ): TransferPlan {
        val sourceLabel = label(source)
        val destinationLabel = label(destination)
        val sourceAfter = withdraw(source, amount)
        val destinationAfter = deposit(destination, amount)
        val outId = "$idPrefix-out"
        return TransferPlan(
            accounts = listOf(sourceAfter.update(), destinationAfter.update()),
            transactions = listOf(
                NewTransaction(
                    id = outId,
                    accountId = source.id,
                    direction = TransactionDirection.DEBIT,
                    amount = amount,
                    merchant = "$TO $destinationLabel",
                    category = TransactionCategory.TRANSFER,
                    reference = reference,
                    runningBalance = sourceAfter.ledgerOrOwed(),
                    memo = null,
                    createdAt = now
                ),
                NewTransaction(
                    id = "$idPrefix-in",
                    accountId = destination.id,
                    direction = TransactionDirection.CREDIT,
                    amount = amount,
                    merchant = "$FROM $sourceLabel",
                    category = TransactionCategory.TRANSFER,
                    reference = "$reference-in",
                    runningBalance = destinationAfter.ledgerOrOwed(),
                    memo = null,
                    createdAt = now
                )
            ),
            notifications = listOfNotNull(
                NewNotification(
                    "$idPrefix-moved",
                    NotificationType.MONEY_MOVED,
                    listOf(amount.cents.toString(), sourceLabel, destinationLabel),
                    now
                ),
                lowBalance(source, sourceAfter, "$idPrefix-low", now)
            ),
            mainTransactionId = outId,
            reference = reference
        )
    }

    /** "Everyday Checking ••3144", the account naming the seed data uses in transfer titles. */
    fun label(account: Account): String = "${account.nickname} ••${account.last4}"

    private fun withdraw(account: Account, amount: Money): Account = when (account) {
        is DepositAccount -> account.copy(
            available = account.available - amount,
            ledger = account.ledger - amount
        )
        // Drawing on a line of credit: owe more, less left to draw.
        is LoanAccount -> account.copy(
            owed = account.owed + amount,
            availableToDraw = account.availableToDraw - amount
        )
        else -> error("Money cannot move out of ${account.type}")
    }

    private fun deposit(account: Account, amount: Money): Account = when (account) {
        is DepositAccount -> account.copy(
            available = account.available + amount,
            ledger = account.ledger + amount
        )
        // Paying a line of credit: owe less, more left to draw.
        is LoanAccount -> account.copy(
            owed = account.owed - amount,
            availableToDraw = account.availableToDraw + amount
        )
        else -> error("Money cannot move into ${account.type}")
    }

    private fun Account.update(): AccountUpdate = when (this) {
        is DepositAccount -> AccountUpdate(id, available, ledger)
        is LoanAccount -> AccountUpdate(id, availableToDraw, owed)
        else -> error("No balances to update on $type")
    }

    private fun Account.ledgerOrOwed(): Money = when (this) {
        is DepositAccount -> ledger
        is LoanAccount -> owed
        else -> Money.ZERO
    }

    private fun lowBalance(
        before: Account,
        after: Account,
        id: String,
        now: Instant
    ): NewNotification? {
        val crossed = !LowBalanceRule.isLow(before) && LowBalanceRule.isLow(after)
        return if (crossed) {
            NewNotification(
                id,
                NotificationType.LOW_BALANCE,
                listOf(after.nickname),
                now
            )
        } else {
            null
        }
    }

    // Seed data uses these words for transfer titles (DEMO_DATA.md section 08).
    private const val TO = "To"
    private const val FROM = "From"
    const val INSTANT_FEE_MERCHANT = "Instant Transfer Fee"
}
