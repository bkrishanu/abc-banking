package com.abcbank.mobile.feature.accounts.domain

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import javax.inject.Inject

/** The texts a new dispute case starts with; they come from string resources, via the screen. */
data class DisputeMessages(val topic: String, val customerMessage: String, val bankReply: String)

sealed interface DisputeResult {
    data object Submitted : DisputeResult

    data object NotDisputable : DisputeResult

    data class Failed(val error: DataError) : DisputeResult
}

/**
 * Which transactions can be disputed (MobileBuild.md 2.2, PRD feature 6): posted charges. Money
 * coming in, transfers the customer made themselves, pending items (not charged yet) and charges
 * already under dispute cannot.
 */
object DisputeRules {
    private val NOT_CHARGES = setOf(
        TransactionCategory.TRANSFER,
        TransactionCategory.TRANSFER_OUT,
        TransactionCategory.TRANSFER_IN
    )

    fun canDispute(transaction: Transaction): Boolean =
        transaction.direction == TransactionDirection.DEBIT &&
            transaction.status == TransactionStatus.POSTED &&
            transaction.category !in NOT_CHARGES &&
            !transaction.disputed
}

/** Opens a support case for the charge and marks it disputed. */
class DisputeTransactionUseCase @Inject constructor(
    private val repository: TransactionsRepository
) {
    suspend operator fun invoke(
        transaction: Transaction,
        messages: DisputeMessages
    ): DisputeResult {
        if (!DisputeRules.canDispute(transaction)) return DisputeResult.NotDisputable
        return when (val result = repository.disputeTransaction(transaction, messages)) {
            is DataResult.Success -> DisputeResult.Submitted
            is DataResult.Failure -> DisputeResult.Failed(result.error)
        }
    }
}
