package com.abcbank.mobile.feature.accounts.domain

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Transaction
import javax.inject.Inject

/** A generated receipt, as a content URI the share sheet can open (kept as text: no Android here). */
data class ReceiptFile(val contentUri: String)

/** Builds receipt PDFs on the device (ARCHITECTURE.md section 02, PDFs). */
interface ReceiptGenerator {
    suspend fun createReceipt(transaction: Transaction, account: Account?): DataResult<ReceiptFile>
}

class CreateReceiptUseCase @Inject constructor(private val generator: ReceiptGenerator) {
    suspend operator fun invoke(
        transaction: Transaction,
        account: Account?
    ): DataResult<ReceiptFile> = generator.createReceipt(transaction, account)
}
