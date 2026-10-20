package com.abcbank.mobile.feature.payments.domain

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Beneficiary
import kotlinx.coroutines.flow.Flow

data class PaymentsData(
    val accounts: List<Account>,
    val beneficiaries: List<Beneficiary>,
    val isFromCache: Boolean
)

/** A payee to save: only the last four digits of the account number are stored. */
data class NewPayee(
    val id: String,
    val name: String,
    val shortName: String,
    val bankName: String,
    val routingNumber: String,
    val accountLast4: String
)

interface PaymentsRepository {
    fun observePaymentsData(): Flow<DataResult<PaymentsData>>

    /**
     * Writes the whole transfer in one batch, applied to the cache at once and uploaded in the
     * background like every other write, so a transfer never waits on the network.
     */
    suspend fun applyTransfer(plan: TransferPlan): DataResult<Unit>

    suspend fun addPayee(payee: NewPayee): DataResult<Unit>
}

/** Looks up a bank by routing number (DEMO_DATA.md section 07, Banks). */
interface BankDirectory {
    suspend fun bankName(routingNumber: String): String?
}
