package com.abcbank.mobile.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class CardNetwork {
    MASTERCARD,
    VISA
}

/** Card face gradients (DESIGN_SYSTEM.md section 02: card-peach, card-sage, card-graphite). */
enum class CardArt {
    PEACH,
    SAGE,
    GRAPHITE
}

/**
 * Daily limits and their slider maximums (MobileBuild.md 4.2). On a credit card the ATM pair is the
 * cash advance limit and the retail pair is null: retail is the linked account's available credit
 * (DEMO_DATA.md section 08).
 */
data class CardLimits(
    val atmDaily: Money,
    val atmMax: Money,
    val retailDaily: Money?,
    val retailMax: Money?
)

data class CardChannels(val international: Boolean, val online: Boolean, val contactless: Boolean)

/**
 * A card as the Cards tab shows it while masked. The full number and CVV are not here on purpose:
 * they live in [CardSecrets], loaded only while revealed (AGENTS.md section 05).
 */
data class Card(
    val id: String,
    val accountId: String,
    val tag: String?,
    val network: CardNetwork,
    val art: CardArt,
    val holderName: String,
    val last4: String,
    val expiry: YearMonth,
    val frozen: Boolean,
    val limits: CardLimits,
    val channels: CardChannels,
    val stackOrder: Int
)

/**
 * The full card number and CVV, held in memory only while revealed. Not a data class, so neither
 * value can reach a log through toString.
 */
class CardSecrets(val pan: String, val cvv: String) {
    override fun toString(): String = "CardSecrets(hidden)"
}

data class TravelNotice(
    val id: String,
    val destination: String,
    val startDate: Instant,
    val endDate: Instant
) {
    /** Active from its start date through its end date, inclusive, in [zone] (MobileBuild.md 4.2). */
    fun isActiveOn(today: LocalDate, zone: ZoneId): Boolean =
        today in startDate.atZone(zone).toLocalDate()..endDate.atZone(zone).toLocalDate()
}
