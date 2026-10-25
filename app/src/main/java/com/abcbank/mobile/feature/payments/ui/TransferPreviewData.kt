package com.abcbank.mobile.feature.payments.ui

import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.Beneficiary
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.security.StepUpState
import com.abcbank.mobile.feature.payments.domain.AmountInput
import com.abcbank.mobile.feature.payments.domain.TransferRules
import com.abcbank.mobile.feature.payments.domain.TransferSpeed

/** Samanta's accounts and payees from DEMO_DATA.md, for previews and screenshot tests. */
internal object TransferPreviewData {
    val checking = DepositAccount(
        id = "checking",
        type = AccountType.CHECKING,
        nickname = "Everyday Checking",
        last4 = "3144",
        available = Money(15_583_200),
        ledger = Money(15_620_700),
        apy = null
    )
    val savings = DepositAccount(
        id = "savings",
        type = AccountType.SAVINGS,
        nickname = "Travel Savings",
        last4 = "7720",
        available = Money(1_203_200),
        ledger = Money(1_203_200),
        apy = null
    )
    val mark =
        Beneficiary(
            "mark-robinson",
            "Mark Robinson",
            "Mark R.",
            "Northfield Bank",
            "500000021",
            "1180",
            0
        )
    val kris =
        Beneficiary("kris-patel", "Kris Patel", "Kris", "Harbor State Bank", "500000034", "2290", 1)
    val oakwood = Beneficiary(
        "oakwood-properties",
        "Oakwood Properties",
        "Oakwood Properties",
        "Summit National Bank",
        "500000050",
        "3305",
        null
    )

    val receipt = TransferReceipt(
        mode = TransferMode.SEND,
        amount = Money(25_000),
        fee = Money.ZERO,
        recipient = "Mark Robinson",
        source = "Everyday Checking ••3144",
        sourceAccountId = "checking",
        reference = "7c1e9a52-4b0d-4f3e-9d61-2a8f5b3c7e10",
        transactionId = "transfer-7c1e9a52-4b0d-4f3e-9d61-2a8f5b3c7e10",
        speed = TransferSpeed.STANDARD
    )

    fun content(
        amountText: String = "",
        speed: TransferSpeed = TransferSpeed.STANDARD,
        memo: String = ""
    ): TransferUiState.Content {
        val amount = AmountInput(amountText).money
        return TransferUiState.Content(
            mode = TransferMode.SEND,
            beneficiaries = listOf(mark, kris, oakwood),
            beneficiary = mark,
            sources = listOf(checking, savings),
            destinations = emptyList(),
            source = checking,
            destination = null,
            amountText = amountText,
            amount = amount,
            fee = TransferRules.fee(amount, speed),
            speed = speed,
            memo = memo,
            category = null,
            amountError = null,
            balancesHidden = false,
            isOffline = false,
            stepUp = StepUpState(),
            payee = PayeeForm(),
            receipt = null,
            sending = false,
            failed = false
        )
    }
}
