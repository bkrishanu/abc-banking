package com.abcbank.mobile.feature.cards.domain

import com.abcbank.mobile.core.model.Card
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TravelNotice
import java.time.LocalDate
import java.time.ZoneId

enum class LimitError { BELOW_MINIMUM, ABOVE_MAXIMUM, NOT_ON_STEP }

enum class TravelNoticeError {
    DESTINATION_MISSING,
    DESTINATION_TOO_LONG,
    STARTS_IN_PAST,
    ENDS_BEFORE_START,
    TOO_LONG
}

/** Card control rules (MobileBuild.md 4.2; slider steps and notice limits decided 2026-10-20). */
object CardRules {
    private val SMALL_STEP = Money.ofDollars(100)
    private val LARGE_STEP = Money.ofDollars(500)
    private val LARGE_STEP_FROM = Money.ofDollars(10_000)
    const val MAX_DESTINATION_LENGTH = 40
    private const val MAX_NOTICE_DAYS = 365L

    /** Slider step for a limit with this maximum: $100, or $500 above $10,000 (daniel's cards). */
    fun step(max: Money): Money = if (max > LARGE_STEP_FROM) LARGE_STEP else SMALL_STEP

    fun validateLimit(value: Money, max: Money): LimitError? {
        val step = step(max)
        return when {
            value < step -> LimitError.BELOW_MINIMUM
            value > max -> LimitError.ABOVE_MAXIMUM
            value.cents % step.cents != 0L -> LimitError.NOT_ON_STEP
            else -> null
        }
    }

    /** Null when both limits are valid. Credit cards have no retail limit of their own. */
    fun validateLimits(card: Card, atmDaily: Money, retailDaily: Money?): LimitError? {
        validateLimit(atmDaily, card.limits.atmMax)?.let { return it }
        val retailMax = card.limits.retailMax
        if (retailDaily == null || retailMax == null) return null
        return validateLimit(retailDaily, retailMax)
    }

    fun validateTravelNotice(
        destination: String,
        start: LocalDate,
        end: LocalDate,
        today: LocalDate
    ): TravelNoticeError? {
        val trimmed = destination.trim()
        return when {
            trimmed.isEmpty() -> TravelNoticeError.DESTINATION_MISSING
            trimmed.length > MAX_DESTINATION_LENGTH -> TravelNoticeError.DESTINATION_TOO_LONG
            start < today -> TravelNoticeError.STARTS_IN_PAST
            end < start -> TravelNoticeError.ENDS_BEFORE_START
            end > start.plusDays(MAX_NOTICE_DAYS) -> TravelNoticeError.TOO_LONG
            else -> null
        }
    }

    /** A notice is active from its start date through its end date, inclusive. */
    fun isActive(notice: TravelNotice, today: LocalDate, zone: ZoneId): Boolean =
        notice.isActiveOn(today, zone)

    /** The newest [RECENT_COUNT] transactions on the card's account, newest first. */
    fun recentActivity(transactions: List<Transaction>, accountId: String): List<Transaction> =
        transactions
            .filter { it.accountId == accountId }
            .sortedWith(
                compareByDescending<Transaction> {
                    it.createdAt
                }.thenByDescending { it.id }
            )
            .take(RECENT_COUNT)

    const val RECENT_COUNT = 3
    const val RECENT_DAYS = 30L
}
