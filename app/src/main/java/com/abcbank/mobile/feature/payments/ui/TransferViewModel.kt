package com.abcbank.mobile.feature.payments.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.Beneficiary
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.core.security.StepUpAction
import com.abcbank.mobile.core.security.StepUpControllerFactory
import com.abcbank.mobile.core.security.StepUpState
import com.abcbank.mobile.feature.payments.domain.AddPayeeUseCase
import com.abcbank.mobile.feature.payments.domain.AmountInput
import com.abcbank.mobile.feature.payments.domain.MoveMoneyUseCase
import com.abcbank.mobile.feature.payments.domain.ObservePaymentsDataUseCase
import com.abcbank.mobile.feature.payments.domain.PayeeResult
import com.abcbank.mobile.feature.payments.domain.PaymentsData
import com.abcbank.mobile.feature.payments.domain.SendTransferUseCase
import com.abcbank.mobile.feature.payments.domain.TransferPlanner
import com.abcbank.mobile.feature.payments.domain.TransferResult
import com.abcbank.mobile.feature.payments.domain.TransferRules
import com.abcbank.mobile.feature.payments.domain.TransferSpeed
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The draft being built across the steps. Saved in SavedStateHandle so process death does not
 * lose it (ARCHITECTURE.md section 04, State).
 */
private data class Draft(
    val mode: TransferMode = TransferMode.SEND,
    val beneficiaryId: String? = null,
    val sourceId: String? = null,
    val destinationId: String? = null,
    val amount: AmountInput = AmountInput(),
    val speed: TransferSpeed = TransferSpeed.STANDARD,
    val memo: String = "",
    val category: TransactionCategory? = null
)

private data class FlowState(
    val payee: PayeeForm = PayeeForm(),
    val receipt: TransferReceipt? = null,
    val sending: Boolean = false,
    val failed: Boolean = false
)

/** One ViewModel for the whole transfer flow, scoped to its navigation graph. */
@HiltViewModel
class TransferViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observePaymentsData: ObservePaymentsDataUseCase,
    private val sendTransfer: SendTransferUseCase,
    private val moveMoney: MoveMoneyUseCase,
    private val addPayee: AddPayeeUseCase,
    sessionManager: SessionManager,
    stepUpFactory: StepUpControllerFactory
) : ViewModel() {

    private val stepUp = stepUpFactory.create(viewModelScope)
    private val draft = MutableStateFlow(restoreDraft())
    private val flowState = MutableStateFlow(FlowState())
    private val attempt = MutableStateFlow(0)
    private var latest: TransferUiState.Content? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransferUiState> = combine(
        attempt.flatMapLatest { observePaymentsData() },
        draft,
        flowState,
        sessionManager.balancesHidden,
        stepUp.state
    ) { result, currentDraft, currentFlow, hidden, stepUpState ->
        when (result) {
            is DataResult.Failure -> TransferUiState.Error(result.error)
            is DataResult.Success -> toUiState(
                result.value,
                currentDraft,
                currentFlow,
                hidden,
                stepUpState
            )
        }.also { latest = it as? TransferUiState.Content }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        TransferUiState.Loading
    )

    fun onSendTo(beneficiaryId: String) = editDraft {
        it.copy(mode = TransferMode.SEND, beneficiaryId = beneficiaryId)
    }

    fun onMoveBetweenAccounts() = editDraft {
        it.copy(mode = TransferMode.MOVE, beneficiaryId = null, speed = TransferSpeed.STANDARD)
    }

    fun onSelectSource(accountId: String) = editDraft { it.copy(sourceId = accountId) }

    fun onSelectDestination(accountId: String) = editDraft { it.copy(destinationId = accountId) }

    /** Between My Accounts: From and To trade places. Every account here can do both. */
    fun onSwapAccounts() {
        val content = latest ?: return
        val source = content.source ?: return
        val destination = content.destination ?: return
        editDraft { it.copy(sourceId = destination.id, destinationId = source.id) }
    }

    fun onKey(key: Char) = editDraft { it.copy(amount = it.amount.press(key)) }

    fun onDeleteKey() = editDraft { it.copy(amount = it.amount.delete()) }

    fun onMemoChange(memo: String) = editDraft {
        it.copy(memo = memo.take(TransferRules.MAX_MEMO_LENGTH))
    }

    fun onCategorySelect(category: TransactionCategory?) = editDraft {
        it.copy(category = category)
    }

    fun onSpeedSelect(speed: TransferSpeed) = editDraft { it.copy(speed = speed) }

    /** After the disclaimer is accepted: step-up when the amount needs it, then send. */
    fun onConfirm() {
        val content = latest ?: return
        if (isBusy()) return
        when (content.mode) {
            TransferMode.MOVE -> send(content) { move(content) }
            TransferMode.SEND -> stepUp.request(
                StepUpAction.OutboundTransfer(content.amount)
            ) { grant ->
                send(content) {
                    val source =
                        content.source ?: return@send TransferResult.Failed(DataError.UNKNOWN)
                    val beneficiary =
                        content.beneficiary ?: return@send TransferResult.Failed(DataError.UNKNOWN)
                    sendTransfer(
                        source = source,
                        beneficiary = beneficiary,
                        amount = content.amount,
                        speed = content.speed,
                        memo = content.memo,
                        category = content.category ?: TransactionCategory.TRANSFER_OUT,
                        grant = grant
                    )
                }
            }
        }
    }

    /**
     * Reads the live flow state, not the last emitted UI state, so a quick double tap on Confirm
     * or Agree and Send can never send the money twice.
     */
    private fun isBusy(): Boolean = flowState.value.let { it.sending || it.receipt != null }

    private fun send(content: TransferUiState.Content, block: suspend () -> TransferResult) {
        if (isBusy()) return
        flowState.update { it.copy(sending = true, failed = false) }
        viewModelScope.launch { finish(content, block()) }
    }

    fun onPayeeNameChange(name: String) = editPayee {
        it.copy(
            name = name.take(
                TransferRules.MAX_NAME_LENGTH + 1
            ),
            error = null
        )
    }

    fun onPayeeRoutingChange(routing: String) {
        val digits = routing.filter { it in '0'..'9' }.take(ROUTING_LENGTH)
        editPayee { it.copy(routing = digits, bankName = null, error = null) }
        if (digits.length == ROUTING_LENGTH) {
            viewModelScope.launch {
                val bank = addPayee.bankName(digits)
                editPayee { if (it.routing == digits) it.copy(bankName = bank) else it }
            }
        }
    }

    fun onPayeeAccountChange(number: String) = editPayee {
        it.copy(
            accountNumber = number.filter { c ->
                c in '0'..'9'
            }.take(MAX_ACCOUNT_DIGITS),
            error = null
        )
    }

    /** Checks the form first, so a typo never costs an MPIN entry; then step-up, then save. */
    fun onSavePayee() {
        val form = flowState.value.payee
        viewModelScope.launch {
            addPayee.check(form.name, form.routing, form.accountNumber)?.let { error ->
                editPayee { it.copy(error = error) }
                return@launch
            }
            stepUp.request(StepUpAction.AddBeneficiary) { grant ->
                viewModelScope.launch {
                    when (
                        val result = addPayee(
                            form.name,
                            form.routing,
                            form.accountNumber,
                            grant
                        )
                    ) {
                        is PayeeResult.Added -> {
                            editDraft {
                                it.copy(mode = TransferMode.SEND, beneficiaryId = result.payeeId)
                            }
                            flowState.update {
                                it.copy(payee = PayeeForm(added = it.payee.added + 1))
                            }
                        }
                        is PayeeResult.Invalid -> editPayee { it.copy(error = result.error) }
                        PayeeResult.StepUpRequired, is PayeeResult.Failed -> flowState.update {
                            it.copy(failed = true)
                        }
                    }
                }
            }
        }
    }

    fun onStepUpDigit(digit: Int) = stepUp.onDigit(digit)

    fun onStepUpDelete() = stepUp.onDelete()

    fun onStepUpCancel() = stepUp.cancel()

    fun onRetry() {
        attempt.value++
    }

    private suspend fun move(content: TransferUiState.Content): TransferResult {
        val source = content.source ?: return TransferResult.Failed(DataError.UNKNOWN)
        val destination = content.destination ?: return TransferResult.Failed(DataError.UNKNOWN)
        return moveMoney(source, destination, content.amount)
    }

    private fun finish(content: TransferUiState.Content, result: TransferResult) {
        when (result) {
            is TransferResult.Sent -> flowState.update {
                it.copy(
                    sending = false,
                    failed = false,
                    receipt = TransferReceipt(
                        mode = content.mode,
                        amount = content.amount,
                        fee = content.fee,
                        recipient = content.beneficiary?.name
                            ?: content.destination?.let(TransferPlanner::label).orEmpty(),
                        source = content.source?.let(TransferPlanner::label).orEmpty(),
                        sourceAccountId = content.source?.id,
                        reference = result.plan.reference,
                        transactionId = result.plan.mainTransactionId,
                        speed = content.speed
                    )
                )
            }
            else -> flowState.update { it.copy(sending = false, failed = true) }
        }
    }

    private fun editDraft(change: (Draft) -> Draft) {
        draft.update(change)
        saveDraft(draft.value)
        flowState.update { it.copy(failed = false) }
    }

    private fun editPayee(change: (PayeeForm) -> PayeeForm) = flowState.update {
        it.copy(payee = change(it.payee))
    }

    private fun toUiState(
        data: PaymentsData,
        d: Draft,
        f: FlowState,
        hidden: Boolean,
        stepUpState: StepUpState
    ): TransferUiState.Content {
        val accounts = data.accounts
        val sources = accounts.filter {
            if (d.mode ==
                TransferMode.SEND
            ) {
                TransferRules.canSendFrom(it)
            } else {
                TransferRules.canMoveBetween(it)
            }
        }.sortedBy { ORDER.indexOf(it.type) }
        val source = sources.firstOrNull { it.id == d.sourceId } ?: sources.firstOrNull()
        val destinations = if (d.mode ==
            TransferMode.MOVE
        ) {
            sources.filter { it.id != source?.id }
        } else {
            emptyList()
        }
        val destination =
            destinations.firstOrNull { it.id == d.destinationId } ?: destinations.firstOrNull()
        val amount = d.amount.money
        val fee = if (d.mode ==
            TransferMode.SEND
        ) {
            TransferRules.fee(amount, d.speed)
        } else {
            Money.ZERO
        }
        val amountError = when {
            !amount.isPositive || source == null -> null
            d.mode == TransferMode.SEND -> TransferRules.validateSend(amount, fee, source)
            destination != null -> TransferRules.validateMove(amount, source, destination)
            else -> null
        }
        val beneficiaries = data.beneficiaries.sortedWith(
            compareBy<Beneficiary> { it.recentOrder ?: Int.MAX_VALUE }.thenBy { it.name }
        )
        return TransferUiState.Content(
            mode = d.mode,
            beneficiaries = beneficiaries,
            beneficiary = beneficiaries.firstOrNull { it.id == d.beneficiaryId },
            sources = sources,
            destinations = destinations,
            source = source,
            destination = destination,
            amountText = d.amount.text,
            amount = amount,
            fee = fee,
            speed = d.speed,
            memo = d.memo,
            category = d.category,
            amountError = amountError,
            balancesHidden = hidden,
            isOffline = data.isFromCache,
            stepUp = stepUpState,
            payee = f.payee,
            receipt = f.receipt,
            sending = f.sending,
            failed = f.failed
        )
    }

    private fun restoreDraft(): Draft = Draft(
        mode = savedStateHandle.get<String>(KEY_MODE)?.let {
            runCatching { TransferMode.valueOf(it) }.getOrNull()
        }
            ?: TransferMode.SEND,
        // A payee chosen before the flow opened (Quick Transfer) arrives as the graph's argument.
        beneficiaryId =
        savedStateHandle[KEY_BENEFICIARY] ?: savedStateHandle.get<String>(ARG_BENEFICIARY_ID),
        sourceId = savedStateHandle[KEY_SOURCE],
        destinationId = savedStateHandle[KEY_DESTINATION],
        amount = AmountInput(
            savedStateHandle.get<String>(KEY_AMOUNT).orEmpty().filter {
                it.isDigit() ||
                    it == AmountInput.DECIMAL
            }
        ),
        speed = savedStateHandle.get<String>(KEY_SPEED)?.let {
            runCatching { TransferSpeed.valueOf(it) }.getOrNull()
        }
            ?: TransferSpeed.STANDARD,
        memo = savedStateHandle.get<String>(KEY_MEMO).orEmpty().take(TransferRules.MAX_MEMO_LENGTH),
        category = savedStateHandle.get<String>(KEY_CATEGORY)?.let {
            runCatching { TransactionCategory.valueOf(it) }.getOrNull()
        }
    )

    private fun saveDraft(d: Draft) {
        savedStateHandle[KEY_MODE] = d.mode.name
        savedStateHandle[KEY_BENEFICIARY] = d.beneficiaryId
        savedStateHandle[KEY_SOURCE] = d.sourceId
        savedStateHandle[KEY_DESTINATION] = d.destinationId
        savedStateHandle[KEY_AMOUNT] = d.amount.text
        savedStateHandle[KEY_SPEED] = d.speed.name
        savedStateHandle[KEY_MEMO] = d.memo
        savedStateHandle[KEY_CATEGORY] = d.category?.name
    }

    override fun onCleared() = stepUp.cancel()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val ROUTING_LENGTH = 9
        const val MAX_ACCOUNT_DIGITS = 17
        val ORDER = listOf(AccountType.CHECKING, AccountType.SAVINGS, AccountType.LOAN)
        const val KEY_MODE = "transferMode"
        const val KEY_BENEFICIARY = "transferBeneficiary"

        /** Matches TransferGraph.beneficiaryId. */
        const val ARG_BENEFICIARY_ID = "beneficiaryId"
        const val KEY_SOURCE = "transferSource"
        const val KEY_DESTINATION = "transferDestination"
        const val KEY_AMOUNT = "transferAmount"
        const val KEY_SPEED = "transferSpeed"
        const val KEY_MEMO = "transferMemo"
        const val KEY_CATEGORY = "transferCategory"
    }
}
