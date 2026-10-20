package com.abcbank.mobile.feature.payments.ui

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Beneficiary
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.security.StepUpState
import com.abcbank.mobile.feature.payments.domain.AmountError
import com.abcbank.mobile.feature.payments.domain.PayeeError
import com.abcbank.mobile.feature.payments.domain.TransferSpeed

/** Send to someone (P2P, 5 steps) or move between own accounts (4 steps). */
enum class TransferMode { SEND, MOVE }

sealed interface TransferUiState {
    data object Loading : TransferUiState

    data class Error(val error: DataError) : TransferUiState

    data class Content(
        val mode: TransferMode,
        /** Recent payees first, in their Quick transfer order, then the rest by name. */
        val beneficiaries: List<Beneficiary>,
        val beneficiary: Beneficiary?,
        /** Accounts this transfer can leave from; for MOVE also [destinations]. */
        val sources: List<Account>,
        val destinations: List<Account>,
        val source: Account?,
        val destination: Account?,
        val amountText: String,
        val amount: Money,
        val fee: Money,
        val speed: TransferSpeed,
        val memo: String,
        val category: TransactionCategory?,
        val amountError: AmountError?,
        val balancesHidden: Boolean,
        val isOffline: Boolean,
        val stepUp: StepUpState,
        val payee: PayeeForm,
        val receipt: TransferReceipt?,
        /** A confirmed transfer is being written; the button shows progress and ignores taps. */
        val sending: Boolean,
        val failed: Boolean
    ) : TransferUiState {
        val total: Money get() = amount + fee
        val canContinueFromAmount: Boolean get() = amount.isPositive &&
            amountError == null &&
            source != null &&
            // A payee id from navigation that matches no saved payee leaves nobody to send to.
            (if (mode == TransferMode.SEND) beneficiary != null else destination != null)
    }
}

data class PayeeForm(
    val name: String = "",
    val routing: String = "",
    val accountNumber: String = "",
    /** The bank for a complete routing number, or null while typing or unknown. */
    val bankName: String? = null,
    val error: PayeeError? = null,
    /** Bumped when a payee is saved, so the screen can move on. */
    val added: Int = 0
)

/** What the receipt step shows (MobileBuild.md 3.2, step 5). */
data class TransferReceipt(
    val mode: TransferMode,
    val amount: Money,
    val fee: Money,
    val recipient: String,
    val source: String,
    val reference: String,
    val transactionId: String,
    val speed: TransferSpeed
)
