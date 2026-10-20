package com.abcbank.mobile.core.data.firebase

import com.google.firebase.Timestamp

/**
 * A `savingsGoals/{goalId}` document. Mapped to the Insights model in `feature/insights/data`,
 * so core/data does not depend on a feature. Amounts are cents.
 */
data class SavingsGoalDto(
    val name: String? = null,
    val target: Long? = null,
    val saved: Long? = null,
    val accountId: String? = null,
    val startedAt: Timestamp? = null,
    /** Null when the field is missing or any point is not a whole number. */
    val weeklyHistory: List<Long>? = null
) {
    companion object {
        const val NAME = "name"
        const val TARGET = "target"
        const val SAVED = "saved"
        const val ACCOUNT_ID = "accountId"
        const val STARTED_AT = "startedAt"
        const val WEEKLY_HISTORY = "weeklyHistory"

        fun fromMap(map: Map<String, Any?>): SavingsGoalDto = SavingsGoalDto(
            name = map.stringField(NAME),
            target = map.longField(TARGET),
            saved = map.longField(SAVED),
            accountId = map.stringField(ACCOUNT_ID),
            startedAt = map.timestampField(STARTED_AT),
            weeklyHistory = (map[WEEKLY_HISTORY] as? List<*>)?.let { points ->
                points.map { (it as? Long) ?: (it as? Int)?.toLong() ?: return@let null }
            }
        )
    }
}
