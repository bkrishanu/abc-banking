package com.abcbank.mobile.feature.payments.data

import android.content.Context
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.common.LowBalanceRule
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.data.firebase.AccountDto
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.firebase.BeneficiaryDto
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.NotificationDto
import com.abcbank.mobile.core.data.firebase.TransactionDto
import com.abcbank.mobile.core.data.firebase.snapshotFlow
import com.abcbank.mobile.core.data.firebase.toDataError
import com.abcbank.mobile.core.data.firebase.toTimestamp
import com.abcbank.mobile.core.data.seed.ActivePersonaSource
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.feature.payments.domain.NewNotification
import com.abcbank.mobile.feature.payments.domain.NewPayee
import com.abcbank.mobile.feature.payments.domain.NotificationType
import com.abcbank.mobile.feature.payments.domain.PaymentsData
import com.abcbank.mobile.feature.payments.domain.PaymentsRepository
import com.abcbank.mobile.feature.payments.domain.TransferPlan
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class FirestorePaymentsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: AuthDataSource,
    private val activePersona: ActivePersonaSource,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : PaymentsRepository {

    private data class Tree(val uid: String, val personaId: String)

    private suspend fun tree(): DataResult<Tree> {
        val uid = when (val result = auth.signedInUid()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> return result
        }
        val personaId =
            activePersona.activePersona()?.key ?: return DataResult.Failure(DataError.UNKNOWN)
        return DataResult.Success(Tree(uid, personaId))
    }

    private fun Tree.collection(name: String) =
        firestore.collection(FirestorePaths.personaCollection(uid, personaId, name))

    override fun observePaymentsData(): Flow<DataResult<PaymentsData>> = flow {
        val tree = when (val result = tree()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> {
                emit(result)
                return@flow
            }
        }
        emitAll(
            combine(
                tree.collection(FirestorePaths.ACCOUNTS).snapshotFlow(),
                tree.collection(FirestorePaths.BENEFICIARIES).snapshotFlow()
            ) { accountSnapshot, beneficiarySnapshot ->
                val accounts = accountSnapshot.documents.map {
                    AccountDto.fromMap(it.data.orEmpty()).toAccount(it.id)
                        ?: return@combine malformed()
                }
                val beneficiaries = beneficiarySnapshot.documents.map {
                    BeneficiaryDto.fromMap(it.data.orEmpty()).toBeneficiary(it.id)
                        ?: return@combine malformed()
                }
                DataResult.Success(
                    PaymentsData(
                        accounts = accounts,
                        beneficiaries = beneficiaries,
                        isFromCache = accountSnapshot.metadata.isFromCache ||
                            beneficiarySnapshot.metadata.isFromCache
                    )
                )
            }
        )
    }
        .catch { error -> emit(DataResult.Failure(error.toDataError())) }
        .flowOn(dispatchers.default)

    override suspend fun applyTransfer(plan: TransferPlan): DataResult<Unit> = call { tree ->
        val batch = firestore.batch()
        plan.accounts.forEach { update ->
            batch.update(
                tree.collection(FirestorePaths.ACCOUNTS).document(update.accountId),
                mapOf(
                    AccountDto.AVAILABLE_BALANCE to update.available.cents,
                    AccountDto.LEDGER_BALANCE to update.ledger.cents
                )
            )
        }
        plan.transactions.forEach { transaction ->
            val dto = TransactionDto(
                accountId = transaction.accountId,
                direction = transaction.direction.name,
                amount = transaction.amount.cents,
                merchant = transaction.merchant,
                category = transaction.category.name,
                reference = transaction.reference,
                runningBalance = transaction.runningBalance.cents,
                status = POSTED,
                disputed = false,
                createdAt = transaction.createdAt.toTimestamp(),
                memo = transaction.memo
            )
            batch.set(
                tree.collection(FirestorePaths.TRANSACTIONS).document(transaction.id),
                dto.toMap()
            )
        }
        plan.notifications.forEach { notification ->
            batch.set(
                tree.collection(FirestorePaths.NOTIFICATIONS).document(notification.id),
                notification.toFields()
            )
        }
        // Not awaited, like every write: applied to the cache now, uploaded when possible.
        batch.commit()
        DataResult.Success(Unit)
    }

    override suspend fun addPayee(payee: NewPayee): DataResult<Unit> = call { tree ->
        val dto = BeneficiaryDto(
            name = payee.name,
            shortName = payee.shortName,
            bankName = payee.bankName,
            routingNumber = payee.routingNumber,
            accountLast4 = payee.accountLast4,
            isRecent = false
        )
        tree.collection(FirestorePaths.BENEFICIARIES).document(payee.id).set(dto.toMap())
        DataResult.Success(Unit)
    }

    /** Alert texts come from string resources, worded like the seeded alerts (DEMO_DATA.md 03). */
    private fun NewNotification.toFields(): Map<String, Any> {
        val (title, body) = when (type) {
            NotificationType.MONEY_SENT -> context.getString(
                R.string.notification_money_sent_title
            ) to
                context.getString(R.string.notification_money_sent_body, money(args[0]), args[1])
            NotificationType.MONEY_MOVED -> context.getString(
                R.string.notification_money_moved_title
            ) to
                context.getString(
                    R.string.notification_money_moved_body,
                    money(args[0]),
                    args[1],
                    args[2]
                )
            NotificationType.LOW_BALANCE -> context.getString(
                R.string.notification_low_balance_title
            ) to
                context.getString(
                    R.string.notification_low_balance_body,
                    args[0],
                    MoneyFormatter.format(LowBalanceRule.THRESHOLD)
                )
        }
        return NotificationDto.transactional(title, body, createdAt.toTimestamp()).toMap()
    }

    private fun money(cents: String): String = MoneyFormatter.format(Money(cents.toLong()))

    private suspend fun <T> call(block: suspend (Tree) -> DataResult<T>): DataResult<T> = try {
        when (val result = tree()) {
            is DataResult.Success -> block(result.value)
            is DataResult.Failure -> result
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        DataResult.Failure(error.toDataError())
    }

    // A document that does not map is reported, never shown half-read (AGENTS.md section 04).
    private fun malformed(): DataResult.Failure = DataResult.Failure(DataError.MALFORMED_DATA)

    private companion object {
        const val POSTED = "POSTED"
    }
}
