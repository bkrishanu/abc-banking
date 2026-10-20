package com.abcbank.mobile.feature.payments.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.security.SessionManager
import com.abcbank.mobile.core.security.StepUpAction
import com.abcbank.mobile.core.security.StepUpControllerFactory
import com.abcbank.mobile.core.security.StepUpState
import com.abcbank.mobile.feature.payments.domain.AddBillerUseCase
import com.abcbank.mobile.feature.payments.domain.BillAccountError
import com.abcbank.mobile.feature.payments.domain.BillListing
import com.abcbank.mobile.feature.payments.domain.BillPaymentPlan
import com.abcbank.mobile.feature.payments.domain.BillResult
import com.abcbank.mobile.feature.payments.domain.BillRules
import com.abcbank.mobile.feature.payments.domain.BillStatus
import com.abcbank.mobile.feature.payments.domain.Biller
import com.abcbank.mobile.feature.payments.domain.BillsOverview
import com.abcbank.mobile.feature.payments.domain.FetchBillUseCase
import com.abcbank.mobile.feature.payments.domain.FetchResult
import com.abcbank.mobile.feature.payments.domain.FetchedBill
import com.abcbank.mobile.feature.payments.domain.ObserveBillsUseCase
import com.abcbank.mobile.feature.payments.domain.PayBillUseCase
import com.abcbank.mobile.feature.payments.domain.ScheduleBillUseCase
import com.abcbank.mobile.feature.payments.domain.SetAutoPayUseCase
import com.abcbank.mobile.feature.payments.domain.TransferRules
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
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

/** The Add a Biller step: pick from the directory, enter the account number, fetch the bill. */
data class AddBillerForm(
    val query: String = "",
    val biller: Biller? = null,
    val accountNumber: String = "",
    val error: BillAccountError? = null,
    val fetched: FetchedBill? = null,
    /** Bumped when a biller is added, so the screen can open its bill. */
    val added: Int = 0
)

sealed interface BillPayUiState {
    data object Loading : BillPayUiState

    data class Error(val error: DataError) : BillPayUiState

    data class Content(
        val bills: List<BillListing>,
        /** The directory filtered by the search, with billers already added marked. */
        val directory: List<Biller>,
        val addedBillerIds: Set<String>,
        val today: LocalDate,
        val sources: List<Account>,
        val source: Account?,
        val selected: BillListing?,
        val form: AddBillerForm,
        val stepUp: StepUpState,
        /** The payment made in this visit, for its confirmation. */
        val paid: BillPaymentPlan?,
        val busy: Boolean,
        val failed: Boolean,
        val balancesHidden: Boolean,
        val isOffline: Boolean
    ) : BillPayUiState {
        /** Paying needs an unpaid bill and an account with enough available. */
        val canPay: Boolean
            get() {
                val bill = selected?.bill ?: return false
                val from = source ?: return false
                return bill.status != BillStatus.PAID &&
                    TransferRules.validateSend(bill.amountDue, Money.ZERO, from) == null
            }
    }
}

private data class BillFlowState(
    val form: AddBillerForm = AddBillerForm(),
    val paid: BillPaymentPlan? = null,
    val busy: Boolean = false,
    val failed: Boolean = false
)

/** Bill pay (MobileBuild.md 3.3, PRD feature 9): bills, Add a Biller and each bill's detail. */
@HiltViewModel
class BillPayViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeBills: ObserveBillsUseCase,
    private val fetchBill: FetchBillUseCase,
    private val addBiller: AddBillerUseCase,
    private val payBill: PayBillUseCase,
    private val scheduleBill: ScheduleBillUseCase,
    private val setAutoPay: SetAutoPayUseCase,
    sessionManager: SessionManager,
    stepUpFactory: StepUpControllerFactory
) : ViewModel() {
    private val stepUp = stepUpFactory.create(viewModelScope)
    private val selectedBillId = MutableStateFlow(savedStateHandle.get<String>(KEY_BILL))
    private val sourceId = MutableStateFlow(savedStateHandle.get<String>(KEY_SOURCE))
    private val flowState = MutableStateFlow(BillFlowState())
    private val attempt = MutableStateFlow(0)
    private var latest: BillPayUiState.Content? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BillPayUiState> = combine(
        attempt.flatMapLatest { observeBills() },
        combine(selectedBillId, sourceId, ::Pair),
        flowState,
        sessionManager.balancesHidden,
        stepUp.state
    ) { result, (billId, accountId), flow, hidden, stepUpState ->
        when (result) {
            is DataResult.Failure -> BillPayUiState.Error(result.error)
            is DataResult.Success -> toContent(
                result.value,
                billId,
                accountId,
                flow,
                hidden,
                stepUpState
            )
        }.also { latest = it as? BillPayUiState.Content }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        BillPayUiState.Loading
    )

    fun onSelectBill(billId: String) {
        selectedBillId.value = billId
        savedStateHandle[KEY_BILL] = billId
        flowState.update { it.copy(paid = null, failed = false) }
    }

    fun onSelectSource(accountId: String) {
        sourceId.value = accountId
        savedStateHandle[KEY_SOURCE] = accountId
    }

    fun onStartAddBiller() = flowState.update {
        it.copy(form = AddBillerForm(added = it.form.added))
    }

    fun onSearch(query: String) = editForm { it.copy(query = query.take(MAX_QUERY)) }

    fun onChooseBiller(biller: Biller?) = editForm {
        AddBillerForm(query = it.query, biller = biller, added = it.added)
    }

    fun onAccountNumberChange(number: String) = editForm {
        it.copy(
            accountNumber = number.filter { c ->
                c in '0'..'9'
            }.take(MAX_ACCOUNT_DIGITS),
            error = null,
            fetched = null
        )
    }

    fun onFetchBill() {
        val form = flowState.value.form
        val biller = form.biller ?: return
        when (val result = fetchBill(biller, form.accountNumber)) {
            is FetchResult.Fetched -> editForm { it.copy(fetched = result.bill, error = null) }
            is FetchResult.Invalid -> editForm { it.copy(error = result.error, fetched = null) }
        }
    }

    /** Adding a biller is adding a payee, so it asks for the MPIN (MobileBuild.md 1.2). */
    fun onAddBiller() {
        val fetched = flowState.value.form.fetched ?: return
        if (flowState.value.busy) return
        stepUp.request(StepUpAction.AddBeneficiary) { grant ->
            flowState.update { it.copy(busy = true, failed = false) }
            viewModelScope.launch {
                when (addBiller(fetched, grant)) {
                    BillResult.Done -> {
                        onSelectBill(fetched.biller.id)
                        flowState.update {
                            it.copy(
                                busy = false,
                                form = AddBillerForm(
                                    added =
                                    it.form.added + 1
                                )
                            )
                        }
                    }
                    else -> flowState.update { it.copy(busy = false, failed = true) }
                }
            }
        }
    }

    /** Pays the amount due now; more than $1,000 asks for the MPIN first. */
    fun onPay() {
        val content = latest ?: return
        val listing = content.selected ?: return
        val source = content.source ?: return
        if (isBusy()) return
        stepUp.request(StepUpAction.OutboundTransfer(listing.bill.amountDue)) { grant ->
            if (isBusy()) return@request
            flowState.update { it.copy(busy = true, failed = false) }
            viewModelScope.launch {
                when (val result = payBill(listing, source, grant)) {
                    is BillResult.Paid -> flowState.update {
                        it.copy(busy = false, paid = result.plan)
                    }
                    else -> flowState.update { it.copy(busy = false, failed = true) }
                }
            }
        }
    }

    fun onSchedule(date: LocalDate?) {
        val bill = latest?.selected?.bill ?: return
        viewModelScope.launch {
            if (scheduleBill(bill, date) !=
                BillResult.Done
            ) {
                flowState.update { it.copy(failed = true) }
            }
        }
    }

    fun onAutoPayChange(autoPay: Boolean) {
        val bill = latest?.selected?.bill ?: return
        viewModelScope.launch {
            if (setAutoPay(bill, autoPay) !=
                BillResult.Done
            ) {
                flowState.update { it.copy(failed = true) }
            }
        }
    }

    /** True for days a payment can be scheduled on, for the date picker. */
    fun canScheduleFor(date: LocalDate): Boolean {
        val content = latest ?: return false
        val listing = content.selected ?: return false
        return BillRules.canScheduleFor(date, content.today, listing.dueDay)
    }

    fun onStepUpDigit(digit: Int) = stepUp.onDigit(digit)

    fun onStepUpDelete() = stepUp.onDelete()

    fun onStepUpCancel() = stepUp.cancel()

    fun onRetry() {
        attempt.value++
    }

    private fun isBusy(): Boolean = flowState.value.busy || flowState.value.paid != null

    private fun editForm(change: (AddBillerForm) -> AddBillerForm) =
        flowState.update { it.copy(form = change(it.form), failed = false) }

    private fun toContent(
        overview: BillsOverview,
        billId: String?,
        accountId: String?,
        flow: BillFlowState,
        hidden: Boolean,
        stepUpState: StepUpState
    ): BillPayUiState.Content {
        val sources = overview.accounts.filter(TransferRules::canSendFrom).sortedBy {
            SOURCE_ORDER.indexOf(it.type)
        }
        val query = flow.form.query.trim()
        return BillPayUiState.Content(
            bills = overview.bills,
            directory = overview.directory.filter {
                query.isEmpty() ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
            },
            addedBillerIds = overview.bills.map { it.biller.id }.toSet(),
            today = overview.today,
            sources = sources,
            source = sources.firstOrNull { it.id == accountId } ?: sources.firstOrNull(),
            selected = overview.bills.firstOrNull { it.bill.id == billId },
            form = flow.form,
            stepUp = stepUpState,
            paid = flow.paid,
            busy = flow.busy,
            failed = flow.failed,
            balancesHidden = hidden,
            isOffline = overview.isFromCache
        )
    }

    override fun onCleared() = stepUp.cancel()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val MAX_QUERY = 40
        const val MAX_ACCOUNT_DIGITS = 17
        const val KEY_BILL = "billPayBill"
        const val KEY_SOURCE = "billPaySource"
        val SOURCE_ORDER = listOf(AccountType.CHECKING, AccountType.SAVINGS)
    }
}
