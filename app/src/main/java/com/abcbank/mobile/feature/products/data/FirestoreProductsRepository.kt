package com.abcbank.mobile.feature.products.data

import android.content.Context
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.data.firebase.AccountDto
import com.abcbank.mobile.core.data.firebase.ApplicationDto
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.UserProfileDto
import com.abcbank.mobile.core.data.firebase.snapshotFlow
import com.abcbank.mobile.core.data.firebase.toDataError
import com.abcbank.mobile.core.data.firebase.toTimestamp
import com.abcbank.mobile.core.data.seed.ActivePersonaSource
import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.feature.products.domain.Application
import com.abcbank.mobile.feature.products.domain.ApplicationPlan
import com.abcbank.mobile.feature.products.domain.Decision
import com.abcbank.mobile.feature.products.domain.NewAccount
import com.abcbank.mobile.feature.products.domain.NewCardAccount
import com.abcbank.mobile.feature.products.domain.NewLoanAccount
import com.abcbank.mobile.feature.products.domain.NewSavingsAccount
import com.abcbank.mobile.feature.products.domain.ProductsData
import com.abcbank.mobile.feature.products.domain.ProductsRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class FirestoreProductsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: AuthDataSource,
    private val activePersona: ActivePersonaSource,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : ProductsRepository {

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

    override fun observeProductsData(): Flow<DataResult<ProductsData>> = flow {
        val tree = when (val result = tree()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> {
                emit(result)
                return@flow
            }
        }
        emitAll(
            combine(
                firestore.document(FirestorePaths.persona(tree.uid, tree.personaId)).snapshotFlow(),
                tree.collection(FirestorePaths.ACCOUNTS).snapshotFlow(),
                tree.collection(FirestorePaths.APPLICATIONS).snapshotFlow(),
                ::toProductsData
            ).filterNotNull()
        )
    }
        .catch { error -> emit(DataResult.Failure(error.toDataError())) }
        .flowOn(dispatchers.default)

    /** Null while the persona's tree is still being written after login. */
    private fun toProductsData(
        persona: DocumentSnapshot,
        accounts: QuerySnapshot,
        applications: QuerySnapshot
    ): DataResult<ProductsData>? {
        val fields = persona.data ?: return null
        return DataResult.Success(
            ProductsData(
                profile = UserProfileDto.fromMap(fields).toUserProfile() ?: return malformed(),
                accounts = accounts.documents.map {
                    AccountDto.fromMap(it.data.orEmpty()).toAccount(it.id) ?: return malformed()
                },
                applications = applications.documents.map {
                    ApplicationDto.fromMap(it.data.orEmpty()).toApplication(it.id)
                        ?: return malformed()
                },
                isFromCache = persona.metadata.isFromCache ||
                    accounts.metadata.isFromCache ||
                    applications.metadata.isFromCache
            )
        )
    }

    override suspend fun submit(plan: ApplicationPlan): DataResult<Unit> = try {
        when (val result = tree()) {
            is DataResult.Failure -> result
            is DataResult.Success -> {
                val tree = result.value
                val application = plan.application
                val batch = firestore.batch()
                val answers = buildMap<String, Any> {
                    put(ApplicationDto.ANSWER_EMPLOYMENT, application.answers.employment.name)
                    put(ApplicationDto.ANSWER_ANNUAL_INCOME, application.answers.annualIncome.cents)
                    application.answers.loanAmount?.let {
                        put(ApplicationDto.ANSWER_AMOUNT, it.cents)
                    }
                    application.answers.termMonths?.let {
                        put(ApplicationDto.ANSWER_TERM_MONTHS, it.toLong())
                    }
                }
                batch.set(
                    tree.collection(FirestorePaths.APPLICATIONS).document(application.id),
                    ApplicationDto(
                        productId = application.productId,
                        step = ApplicationDto.STEP_COMPLETE,
                        answers = answers,
                        decision = application.decision.name,
                        accountId = application.accountId,
                        createdAt = application.createdAt.toTimestamp()
                    ).toMap()
                )
                plan.account?.let { account ->
                    batch.set(
                        tree.collection(FirestorePaths.ACCOUNTS).document(account.id),
                        account.toDto().toMap()
                    )
                }
                batch.set(
                    tree.collection(
                        FirestorePaths.NOTIFICATIONS
                    ).document("${application.id}-decision"),
                    notification(plan)
                )
                // Not awaited, like every write: applied to the cache now, uploaded when possible.
                batch.commit()
                DataResult.Success(Unit)
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        DataResult.Failure(error.toDataError())
    }

    private fun NewAccount.toDto(): AccountDto = when (this) {
        is NewSavingsAccount -> AccountDto(
            type = AccountType.SAVINGS.name,
            nickname = nickname,
            last4 = last4,
            availableBalance = 0,
            ledgerBalance = 0,
            rate = apy.basisPoints.toLong(),
            openedAt = openedAt.toTimestamp()
        )
        is NewCardAccount -> AccountDto(
            type = AccountType.CREDIT.name,
            nickname = nickname,
            last4 = last4,
            availableBalance = creditLimit.cents,
            ledgerBalance = 0,
            rate = apr.basisPoints.toLong(),
            creditLimit = creditLimit.cents,
            statementBalance = 0,
            dueDate = firstDueDate.toTimestamp(),
            autoPayFull = false,
            openedAt = openedAt.toTimestamp()
        )
        // What is owed starts at the full amount; nothing is left to draw on an installment loan.
        is NewLoanAccount -> AccountDto(
            type = AccountType.LOAN.name,
            nickname = nickname,
            last4 = last4,
            availableBalance = 0,
            ledgerBalance = principal.cents,
            rate = apr.basisPoints.toLong(),
            principal = principal.cents,
            openedAt = openedAt.toTimestamp()
        )
    }

    /** The decision alert, worded like the seeded alerts (DEMO_DATA.md section 03). */
    private fun notification(plan: ApplicationPlan): Map<String, Any> {
        val application = plan.application
        val approved = application.decision == Decision.APPROVED
        val title = context.getString(
            if (approved) {
                R.string.notification_application_approved_title
            } else {
                R.string.notification_application_referred_title
            }
        )
        val body = if (approved) {
            context.getString(
                R.string.notification_application_approved_body,
                application.productName
            )
        } else {
            context.getString(
                R.string.notification_application_referred_body,
                application.productName
            )
        }
        return mapOf(
            NOTIFICATION_KIND to TRANSACTIONAL,
            NOTIFICATION_TITLE to title,
            NOTIFICATION_BODY to body,
            NOTIFICATION_READ to false,
            NOTIFICATION_CREATED_AT to application.createdAt.toTimestamp()
        )
    }

    // A document that does not map is reported, never shown half-read (AGENTS.md section 04).
    private fun malformed(): DataResult<ProductsData> = DataResult.Failure(DataError.MALFORMED_DATA)

    private companion object {
        const val NOTIFICATION_KIND = "kind"
        const val NOTIFICATION_TITLE = "title"
        const val NOTIFICATION_BODY = "body"
        const val NOTIFICATION_READ = "read"
        const val NOTIFICATION_CREATED_AT = "createdAt"
        const val TRANSACTIONAL = "TRANSACTIONAL"
    }
}

/** Null when a field is missing or invalid. */
internal fun ApplicationDto.toApplication(id: String): Application? {
    val decision =
        decision?.let { value -> Decision.entries.firstOrNull { it.name == value } } ?: return null
    return Application(
        id = id,
        productId = productId?.takeIf(String::isNotBlank) ?: return null,
        decision = decision,
        accountId = accountId?.takeIf(String::isNotBlank),
        createdAt =
        createdAt?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) }
            ?: return null
    )
}
