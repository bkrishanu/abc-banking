package com.abcbank.mobile.feature.payments.domain

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Deposit check (MobileBuild.md 2.3, DEMO_DATA.md section 07, check deposits).

enum class DepositError { BELOW_MINIMUM, OVER_LIMIT, ACCOUNT }

data class DepositData(
    /** The profile's `checkDepositLimit`: $10,000.00, or $100,000.00 for daniel. */
    val limit: Money,
    val accounts: List<Account>,
    val isFromCache: Boolean
)

/** A new check deposit: a PENDING credit that clears at [clearsAt]; balances do not move yet. */
data class PendingDeposit(
    val id: String,
    val accountId: String,
    val amount: Money,
    val reference: String,
    /** The ledger as it stands; it becomes the balance after this deposit when it clears. */
    val runningBalance: Money,
    val createdAt: Instant,
    val clearsAt: Instant,
    val frontImage: Boolean,
    val backImage: Boolean
)

/** What clearing one deposit writes: Posted, the credit, and the "Deposit cleared" alert. */
data class ClearedDeposit(
    val transactionId: String,
    val account: AccountUpdate,
    val runningBalance: Money,
    val notification: NewNotification
)

data class PendingDeposits(val deposits: List<Transaction>, val accounts: List<Account>)

interface DepositRepository {
    fun observeDepositData(): Flow<DataResult<DepositData>>

    suspend fun submitDeposit(deposit: PendingDeposit): DataResult<Unit>

    /** The active persona's pending deposits and accounts, read once. */
    suspend fun pendingDeposits(): DataResult<PendingDeposits>

    suspend fun clearDeposit(cleared: ClearedDeposit): DataResult<Unit>
}

/** Told when a deposit is made, so clearing can be scheduled while the app is open. */
fun interface DepositClearingTrigger {
    fun onDepositSubmitted()
}

object DepositRules {
    val MINIMUM: Money = Money(100)
    val CLEARING_TIME: Duration = Duration.ofSeconds(30)
    const val MERCHANT = "Check deposit"

    fun canDepositInto(account: Account): Boolean = account is DepositAccount &&
        (account.type == AccountType.CHECKING || account.type == AccountType.SAVINGS)

    fun validate(amount: Money, limit: Money): DepositError? = when {
        amount < MINIMUM -> DepositError.BELOW_MINIMUM
        amount > limit -> DepositError.OVER_LIMIT
        else -> null
    }

    fun pending(
        account: DepositAccount,
        amount: Money,
        reference: String,
        now: Instant,
        front: Boolean,
        back: Boolean
    ) = PendingDeposit(
        id = "deposit-$reference",
        accountId = account.id,
        amount = amount,
        reference = reference,
        runningBalance = account.ledger,
        createdAt = now,
        clearsAt = now.plus(CLEARING_TIME),
        frontImage = front,
        backImage = back
    )

    fun isDue(transaction: Transaction, now: Instant): Boolean {
        val clearsAt = transaction.clearsAt ?: return false
        return transaction.status == TransactionStatus.PENDING &&
            transaction.direction == TransactionDirection.CREDIT &&
            transaction.category == TransactionCategory.DEPOSIT &&
            !now.isBefore(clearsAt)
    }

    fun clear(deposit: Transaction, account: DepositAccount, now: Instant): ClearedDeposit {
        val available = account.available + deposit.amount
        val ledger = account.ledger + deposit.amount
        return ClearedDeposit(
            transactionId = deposit.id,
            account = AccountUpdate(account.id, available, ledger),
            runningBalance = ledger,
            notification = NewNotification(
                id = "${deposit.id}-cleared",
                type = NotificationType.DEPOSIT_CLEARED,
                args = listOf(deposit.amount.cents.toString(), account.nickname),
                createdAt = now
            )
        )
    }
}

class ObserveDepositDataUseCase @Inject constructor(private val repository: DepositRepository) {
    operator fun invoke(): Flow<DataResult<DepositData>> = repository.observeDepositData()
}

sealed interface DepositResult {
    data class Submitted(val deposit: PendingDeposit) : DepositResult

    data class Invalid(val error: DepositError) : DepositResult

    data class Failed(val error: DataError) : DepositResult
}

class SubmitDepositUseCase @Inject constructor(
    private val repository: DepositRepository,
    private val trigger: DepositClearingTrigger,
    private val clock: Clock
) {
    suspend operator fun invoke(
        account: Account,
        amount: Money,
        limit: Money,
        front: Boolean,
        back: Boolean
    ): DepositResult {
        if (account !is DepositAccount ||
            !DepositRules.canDepositInto(account)
        ) {
            return DepositResult.Invalid(DepositError.ACCOUNT)
        }
        DepositRules.validate(amount, limit)?.let { return DepositResult.Invalid(it) }
        val deposit = DepositRules.pending(
            account,
            amount,
            UUID.randomUUID().toString(),
            clock.now(),
            front,
            back
        )
        return when (val result = repository.submitDeposit(deposit)) {
            is DataResult.Success -> {
                trigger.onDepositSubmitted()
                DepositResult.Submitted(deposit)
            }
            is DataResult.Failure -> DepositResult.Failed(result.error)
        }
    }
}

/**
 * Posts every deposit whose 30 seconds are up and returns when the next one is due, or null.
 * One at a time, so a timer and a return to the foreground never credit the same deposit twice.
 */
@Singleton
class ClearDueDepositsUseCase @Inject constructor(
    private val repository: DepositRepository,
    private val clock: Clock
) {
    private val mutex = Mutex()

    suspend operator fun invoke(): Instant? = mutex.withLock {
        val pending =
            (repository.pendingDeposits() as? DataResult.Success)?.value ?: return@withLock null
        val now = clock.now()
        // Balances move as each deposit clears, so later deposits on the same account build on it.
        val accounts = pending.accounts.associateBy { it.id }.toMutableMap()
        var next: Instant? = null
        pending.deposits.sortedBy { it.clearsAt }.forEach { deposit ->
            val account = accounts[deposit.accountId] as? DepositAccount ?: return@forEach
            if (DepositRules.isDue(deposit, now)) {
                val cleared = DepositRules.clear(deposit, account, now)
                if (repository.clearDeposit(cleared) is DataResult.Success) {
                    accounts[account.id] =
                        account.copy(
                            available = cleared.account.available,
                            ledger = cleared.account.ledger
                        )
                }
            } else if (deposit.status == TransactionStatus.PENDING) {
                deposit.clearsAt?.let { clearsAt ->
                    next =
                        next?.let { minOf(it, clearsAt) } ?: clearsAt
                }
            }
        }
        next
    }
}
