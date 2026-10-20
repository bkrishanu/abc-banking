package com.abcbank.mobile.feature.accounts.domain

import com.abcbank.mobile.core.common.DataResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTransactionUseCase @Inject constructor(
    private val repository: TransactionsRepository
) {
    operator fun invoke(transactionId: String): Flow<DataResult<TransactionDetail?>> =
        repository.observeTransaction(transactionId)
}
