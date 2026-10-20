package com.abcbank.mobile.feature.insights.data

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.SavingsGoalDto
import com.abcbank.mobile.core.data.firebase.TransactionDto
import com.abcbank.mobile.core.data.firebase.UserProfileDto
import com.abcbank.mobile.core.data.firebase.snapshotFlow
import com.abcbank.mobile.core.data.firebase.toDataError
import com.abcbank.mobile.core.data.firebase.toTimestamp
import com.abcbank.mobile.core.data.seed.ActivePersonaSource
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.feature.insights.domain.InsightsData
import com.abcbank.mobile.feature.insights.domain.InsightsRepository
import com.abcbank.mobile.feature.insights.domain.SavingsGoal
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class FirestoreInsightsRepository @Inject constructor(
    private val auth: AuthDataSource,
    private val activePersona: ActivePersonaSource,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : InsightsRepository {

    override fun observeInsights(since: Instant): Flow<DataResult<InsightsData>> = flow {
        val uid = when (val result = auth.signedInUid()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> {
                emit(result)
                return@flow
            }
        }
        val personaId = activePersona.activePersona()?.key ?: run {
            emit(DataResult.Failure(DataError.UNKNOWN))
            return@flow
        }
        fun collection(name: String) =
            firestore.collection(FirestorePaths.personaCollection(uid, personaId, name))

        // A single-field filter, so no composite index is needed.
        val transactions = collection(FirestorePaths.TRANSACTIONS)
            .whereGreaterThanOrEqualTo(TransactionDto.CREATED_AT, since.toTimestamp())
        emitAll(
            combine(
                firestore.document(FirestorePaths.persona(uid, personaId)).snapshotFlow(),
                transactions.snapshotFlow(),
                collection(FirestorePaths.SAVINGS_GOALS).snapshotFlow(),
                ::toInsightsData
            ).filterNotNull()
        )
    }
        .catch { error -> emit(DataResult.Failure(error.toDataError())) }
        .flowOn(dispatchers.default)

    /** Null while the persona's tree is still being written after login: the tab stays loading. */
    private fun toInsightsData(
        persona: DocumentSnapshot,
        transactions: QuerySnapshot,
        goals: QuerySnapshot
    ): DataResult<InsightsData>? {
        val profile =
            persona.data?.let { UserProfileDto.fromMap(it).toUserProfile() ?: return malformed() }
                ?: return null
        return DataResult.Success(
            InsightsData(
                monthlyBudget = profile.monthlyBudget,
                transactions = transactions.documents.map {
                    TransactionDto.fromMap(it.data.orEmpty()).toTransaction(it.id)
                        ?: return malformed()
                },
                goals = goals.documents.map {
                    SavingsGoalDto.fromMap(it.data.orEmpty()).toSavingsGoal(it.id)
                        ?: return malformed()
                },
                isFromCache = persona.metadata.isFromCache ||
                    transactions.metadata.isFromCache ||
                    goals.metadata.isFromCache
            )
        )
    }

    // A document that does not map is reported, never shown half-read (AGENTS.md section 04).
    private fun malformed(): DataResult<InsightsData> = DataResult.Failure(DataError.MALFORMED_DATA)
}

/** Null when a field is missing, negative or blank. */
internal fun SavingsGoalDto.toSavingsGoal(id: String): SavingsGoal? {
    val history = weeklyHistory ?: return null
    if (history.any { it < 0 }) return null
    return SavingsGoal(
        id = id,
        name = name?.takeIf(String::isNotBlank) ?: return null,
        target = target?.takeIf { it > 0 }?.let(::Money) ?: return null,
        saved = saved?.takeIf { it >= 0 }?.let(::Money) ?: return null,
        accountId = accountId?.takeIf(String::isNotBlank) ?: return null,
        startedAt =
        startedAt?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) }
            ?: return null,
        weeklyHistory = history.map(::Money)
    )
}
