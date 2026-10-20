package com.abcbank.mobile.feature.payments.domain

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Beneficiary
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.security.StepUpAction
import com.abcbank.mobile.core.security.StepUpGrant
import com.abcbank.mobile.core.security.StepUpManager
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

// Money movement use cases (MobileBuild.md 3.1 and 3.2). Step-up is checked here, not in the
// screens, so no caller can skip it (ARCHITECTURE.md section 04, path of every write).

class ObservePaymentsDataUseCase @Inject constructor(private val repository: PaymentsRepository) {
    operator fun invoke(): Flow<DataResult<PaymentsData>> = repository.observePaymentsData()
}

sealed interface TransferResult {
    data class Sent(val plan: TransferPlan) : TransferResult

    data class Invalid(val error: AmountError) : TransferResult

    /** Over $1,000 and the grant was missing, used or for another amount. */
    data object StepUpRequired : TransferResult

    data class Failed(val error: DataError) : TransferResult
}

/** P2P and domestic transfer: validate, add the fee, check step-up, write (PRD feature 8). */
class SendTransferUseCase @Inject constructor(
    private val repository: PaymentsRepository,
    private val stepUp: StepUpManager,
    private val clock: Clock
) {
    suspend operator fun invoke(
        source: Account,
        beneficiary: Beneficiary,
        amount: Money,
        speed: TransferSpeed,
        memo: String?,
        category: TransactionCategory,
        grant: StepUpGrant?
    ): TransferResult {
        if (source !is DepositAccount ||
            !TransferRules.canSendFrom(source)
        ) {
            return TransferResult.Invalid(AmountError.OVER_AVAILABLE)
        }
        val fee = TransferRules.fee(amount, speed)
        TransferRules.validateSend(amount, fee, source)?.let { return TransferResult.Invalid(it) }
        if (!stepUp.consume(
                grant,
                StepUpAction.OutboundTransfer(amount)
            )
        ) {
            return TransferResult.StepUpRequired
        }
        val reference = UUID.randomUUID().toString()
        val plan = TransferPlanner.send(
            source = source,
            beneficiary = beneficiary,
            amount = amount,
            fee = fee,
            memo = memo?.trim()?.takeIf { it.isNotEmpty() },
            category = category,
            reference = reference,
            idPrefix = "transfer-$reference",
            now = clock.now()
        )
        return when (val result = repository.applyTransfer(plan)) {
            is DataResult.Success -> TransferResult.Sent(plan)
            is DataResult.Failure -> TransferResult.Failed(result.error)
        }
    }
}

/** Between the customer's own accounts (PRD feature 7). Not outbound, so no step-up. */
class MoveMoneyUseCase @Inject constructor(
    private val repository: PaymentsRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(
        source: Account,
        destination: Account,
        amount: Money
    ): TransferResult {
        if (!TransferRules.canMoveBetween(source) || !TransferRules.canMoveBetween(destination)) {
            return TransferResult.Invalid(AmountError.SAME_ACCOUNT)
        }
        TransferRules.validateMove(amount, source, destination)?.let {
            return TransferResult.Invalid(it)
        }
        val reference = UUID.randomUUID().toString()
        val plan = TransferPlanner.move(
            source,
            destination,
            amount,
            reference,
            "move-$reference",
            clock.now()
        )
        return when (val result = repository.applyTransfer(plan)) {
            is DataResult.Success -> TransferResult.Sent(plan)
            is DataResult.Failure -> TransferResult.Failed(result.error)
        }
    }
}

sealed interface PayeeResult {
    data class Added(val payeeId: String) : PayeeResult

    data class Invalid(val error: PayeeError) : PayeeResult

    data object StepUpRequired : PayeeResult

    data class Failed(val error: DataError) : PayeeResult
}

/** Add Beneficiary (MobileBuild.md 3.2): routing lookup, account check, then step-up. */
class AddPayeeUseCase @Inject constructor(
    private val repository: PaymentsRepository,
    private val banks: BankDirectory,
    private val stepUp: StepUpManager
) {
    suspend operator fun invoke(
        name: String,
        routing: String,
        accountNumber: String,
        grant: StepUpGrant?
    ): PayeeResult {
        val bankName = banks.bankName(routing)
        TransferRules.validatePayee(name, routing, accountNumber, bankName)?.let {
            return PayeeResult.Invalid(it)
        }
        if (!stepUp.consume(grant, StepUpAction.AddBeneficiary)) return PayeeResult.StepUpRequired
        val payee = NewPayee(
            id = "payee-${UUID.randomUUID()}",
            name = name.trim(),
            shortName = TransferRules.shortName(name),
            bankName = requireNotNull(bankName) { "Checked by validatePayee" },
            routingNumber = routing,
            accountLast4 = accountNumber.takeLast(ACCOUNT_LAST4)
        )
        return when (val result = repository.addPayee(payee)) {
            is DataResult.Success -> PayeeResult.Added(payee.id)
            is DataResult.Failure -> PayeeResult.Failed(result.error)
        }
    }

    /** For checking the form before asking for step-up. */
    suspend fun check(name: String, routing: String, accountNumber: String): PayeeError? =
        TransferRules.validatePayee(name, routing, accountNumber, banks.bankName(routing))

    suspend fun bankName(routing: String): String? = banks.bankName(routing)

    private companion object {
        const val ACCOUNT_LAST4 = 4
    }
}
