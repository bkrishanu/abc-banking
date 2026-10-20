package com.abcbank.mobile.feature.accounts.domain

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Transaction
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** The active persona's transactions and the accounts they belong to. */
data class TransactionsData(
    val transactions: List<Transaction>,
    val accounts: List<Account>,
    val isFromCache: Boolean
)

data class TransactionDetail(
    val transaction: Transaction,
    val account: Account?,
    val isFromCache: Boolean
)

interface TransactionsRepository {
    /** Live. Every transaction since [since] (all when null), newest first. */
    fun observeTransactions(since: Instant?): Flow<DataResult<TransactionsData>>

    /** Live. Success(null) when there is no such transaction. */
    fun observeTransaction(transactionId: String): Flow<DataResult<TransactionDetail?>>
}
