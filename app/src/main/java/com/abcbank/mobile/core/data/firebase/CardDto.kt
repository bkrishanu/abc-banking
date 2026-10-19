package com.abcbank.mobile.core.data.firebase

import com.abcbank.mobile.core.model.Card
import com.abcbank.mobile.core.model.CardArt
import com.abcbank.mobile.core.model.CardChannels
import com.abcbank.mobile.core.model.CardLimits
import com.abcbank.mobile.core.model.CardNetwork
import com.abcbank.mobile.core.model.CardSecrets
import com.abcbank.mobile.core.model.TravelNotice
import com.google.firebase.Timestamp
import java.time.DateTimeException
import java.time.YearMonth

/**
 * A `users/{uid}/cards/{cardId}` document. [pan] and [cvv] are fake (DEMO_DATA.md section 01) and
 * leave this class only through [toCardSecrets], when the card is revealed. Limits are cents;
 * [expiry] is "MM/YY".
 */
data class CardDto(
    val accountId: String? = null,
    val tag: String? = null,
    val network: String? = null,
    val artKey: String? = null,
    val holderName: String? = null,
    val pan: String? = null,
    val cvv: String? = null,
    val last4: String? = null,
    val expiry: String? = null,
    val frozen: Boolean? = null,
    val atmDailyLimit: Long? = null,
    val atmMaxLimit: Long? = null,
    val retailDailyLimit: Long? = null,
    val retailMaxLimit: Long? = null,
    val international: Boolean? = null,
    val online: Boolean? = null,
    val contactless: Boolean? = null,
    val stackOrder: Long? = null
) {
    fun toMap(): Map<String, Any> = firestoreFields(
        ACCOUNT_ID to accountId,
        TAG to tag,
        NETWORK to network,
        ART_KEY to artKey,
        HOLDER_NAME to holderName,
        PAN to pan,
        CVV to cvv,
        LAST4 to last4,
        EXPIRY to expiry,
        FROZEN to frozen,
        ATM_DAILY_LIMIT to atmDailyLimit,
        ATM_MAX_LIMIT to atmMaxLimit,
        RETAIL_DAILY_LIMIT to retailDailyLimit,
        RETAIL_MAX_LIMIT to retailMaxLimit,
        INTERNATIONAL to international,
        ONLINE to online,
        CONTACTLESS to contactless,
        STACK_ORDER to stackOrder
    )

    /** The masked card. Null when a field is missing or invalid. */
    fun toCard(id: String): Card? {
        val atmDaily = money(atmDailyLimit) ?: return null
        val atmMax = money(atmMaxLimit)?.takeIf { it >= atmDaily } ?: return null
        val retailDaily = retailDailyLimit?.let { money(it) ?: return null }
        val retailMax = retailMaxLimit?.let { money(it) ?: return null }
        // Debit cards have both retail values, credit cards neither (DEMO_DATA.md section 08).
        if ((retailDaily == null) != (retailMax == null)) return null
        if (retailDaily != null && retailMax != null && retailDaily > retailMax) return null
        return Card(
            id = id,
            accountId = accountId?.takeIf(String::isNotBlank) ?: return null,
            tag = tag?.takeIf(String::isNotBlank),
            network = enumOrNull<CardNetwork>(network) ?: return null,
            art = enumOrNull<CardArt>(artKey) ?: return null,
            holderName = holderName?.takeIf(String::isNotBlank) ?: return null,
            last4 = last4?.takeIf(String::isLast4) ?: return null,
            expiry = parseExpiry(expiry) ?: return null,
            frozen = frozen ?: false,
            limits = CardLimits(atmDaily, atmMax, retailDaily, retailMax),
            channels = CardChannels(
                international = international ?: return null,
                online = online ?: return null,
                contactless = contactless ?: return null
            ),
            stackOrder = stackOrder?.toIntOrNull() ?: return null
        )
    }

    /** Null unless the number has 16 digits ending in [last4] and the CVV has 3 or 4. */
    fun toCardSecrets(): CardSecrets? {
        val pan =
            pan?.takeIf { it.length == PAN_LENGTH && it.all(Char::isAsciiDigit) } ?: return null
        val last4 = last4 ?: return null
        if (!pan.endsWith(last4)) return null
        val cvv =
            cvv?.takeIf { it.length in CVV_LENGTHS && it.all(Char::isAsciiDigit) } ?: return null
        return CardSecrets(pan = pan, cvv = cvv)
    }

    companion object {
        const val ACCOUNT_ID = "accountId"
        const val TAG = "tag"
        const val NETWORK = "network"
        const val ART_KEY = "artKey"
        const val HOLDER_NAME = "holderName"
        const val PAN = "pan"
        const val CVV = "cvv"
        const val LAST4 = "last4"
        const val EXPIRY = "expiry"
        const val FROZEN = "frozen"
        const val ATM_DAILY_LIMIT = "atmDailyLimit"
        const val ATM_MAX_LIMIT = "atmMaxLimit"
        const val RETAIL_DAILY_LIMIT = "retailDailyLimit"
        const val RETAIL_MAX_LIMIT = "retailMaxLimit"
        const val INTERNATIONAL = "international"
        const val ONLINE = "online"
        const val CONTACTLESS = "contactless"
        const val STACK_ORDER = "stackOrder"

        private const val PAN_LENGTH = 16
        private val CVV_LENGTHS = 3..4
        private val EXPIRY_PATTERN = Regex("""(\d{2})/(\d{2})""")
        private const val CENTURY = 2000

        fun fromMap(map: Map<String, Any?>): CardDto = CardDto(
            accountId = map.stringField(ACCOUNT_ID),
            tag = map.stringField(TAG),
            network = map.stringField(NETWORK),
            artKey = map.stringField(ART_KEY),
            holderName = map.stringField(HOLDER_NAME),
            pan = map.stringField(PAN),
            cvv = map.stringField(CVV),
            last4 = map.stringField(LAST4),
            expiry = map.stringField(EXPIRY),
            frozen = map.booleanField(FROZEN),
            atmDailyLimit = map.longField(ATM_DAILY_LIMIT),
            atmMaxLimit = map.longField(ATM_MAX_LIMIT),
            retailDailyLimit = map.longField(RETAIL_DAILY_LIMIT),
            retailMaxLimit = map.longField(RETAIL_MAX_LIMIT),
            international = map.booleanField(INTERNATIONAL),
            online = map.booleanField(ONLINE),
            contactless = map.booleanField(CONTACTLESS),
            stackOrder = map.longField(STACK_ORDER)
        )

        private fun parseExpiry(value: String?): YearMonth? {
            val match = value?.let(EXPIRY_PATTERN::matchEntire) ?: return null
            val (month, year) = match.destructured
            return try {
                YearMonth.of(CENTURY + year.toInt(), month.toInt())
            } catch (_: DateTimeException) {
                null
            }
        }
    }
}

/** A `users/{uid}/cards/{cardId}/travelNotices/{noticeId}` document. */
data class TravelNoticeDto(
    val destination: String? = null,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = firestoreFields(
        DESTINATION to destination,
        START_DATE to startDate,
        END_DATE to endDate
    )

    /** Null when a field is missing or the notice ends before it starts. */
    fun toTravelNotice(id: String): TravelNotice? {
        val start = startDate?.toInstantUtc() ?: return null
        val end = endDate?.toInstantUtc()?.takeIf { !it.isBefore(start) } ?: return null
        return TravelNotice(
            id = id,
            destination = destination?.takeIf(String::isNotBlank) ?: return null,
            startDate = start,
            endDate = end
        )
    }

    companion object {
        const val DESTINATION = "destination"
        const val START_DATE = "startDate"
        const val END_DATE = "endDate"

        fun fromMap(map: Map<String, Any?>): TravelNoticeDto = TravelNoticeDto(
            destination = map.stringField(DESTINATION),
            startDate = map.timestampField(START_DATE),
            endDate = map.timestampField(END_DATE)
        )
    }
}

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
