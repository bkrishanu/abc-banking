package com.abcbank.mobile.feature.cards.domain

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Card
import com.abcbank.mobile.core.model.CardChannels
import com.abcbank.mobile.core.model.CardSecrets
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.security.StepUpAction
import com.abcbank.mobile.core.security.StepUpGrant
import com.abcbank.mobile.core.security.StepUpManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.flow.Flow

// The Cards tab's use cases (MobileBuild.md 4.1 and 4.2). Small and closely related, so they
// share a file. Step-up is checked here, not in the screen, so no caller can skip it.

class ObserveCardsUseCase @Inject constructor(
    private val repository: CardsRepository,
    private val clock: Clock,
    private val zone: Provider<ZoneId>
) {
    /** Recent Activity covers today and the 29 days before it, as on Home. */
    operator fun invoke(): Flow<DataResult<CardsData>> {
        val zone = zone.get()
        val since = clock.now().atZone(zone).toLocalDate()
            .minusDays(CardRules.RECENT_DAYS - 1)
            .atStartOfDay(zone)
            .toInstant()
        return repository.observeCards(since)
    }
}

sealed interface StepUpResult<out T> {
    data class Done<T>(val value: T) : StepUpResult<T>

    /** The grant was missing, used, expired or for another action. */
    data object StepUpRequired : StepUpResult<Nothing>

    data class Failed(val error: DataError) : StepUpResult<Nothing>
}

/** Reveal card details: needs step-up for this card. */
class RevealCardDetailsUseCase @Inject constructor(
    private val repository: CardsRepository,
    private val stepUp: StepUpManager
) {
    suspend operator fun invoke(cardId: String, grant: StepUpGrant?): StepUpResult<CardSecrets> {
        if (!stepUp.consume(
                grant,
                StepUpAction.RevealCardDetails(cardId)
            )
        ) {
            return StepUpResult.StepUpRequired
        }
        return when (val result = repository.loadSecrets(cardId)) {
            is DataResult.Success -> StepUpResult.Done(result.value)
            is DataResult.Failure -> StepUpResult.Failed(result.error)
        }
    }
}

sealed interface LimitsResult {
    data object Saved : LimitsResult

    data object StepUpRequired : LimitsResult

    data class Invalid(val error: LimitError) : LimitsResult

    data class Failed(val error: DataError) : LimitsResult
}

/** Change daily limits: validated against the card's maximums, then step-up. */
class UpdateCardLimitsUseCase @Inject constructor(
    private val repository: CardsRepository,
    private val stepUp: StepUpManager
) {
    suspend operator fun invoke(
        card: Card,
        atmDaily: Money,
        retailDaily: Money?,
        grant: StepUpGrant?
    ): LimitsResult {
        CardRules.validateLimits(card, atmDaily, retailDaily)?.let {
            return LimitsResult.Invalid(it)
        }
        if (!stepUp.consume(
                grant,
                StepUpAction.ChangeCardLimits
            )
        ) {
            return LimitsResult.StepUpRequired
        }
        val retail = retailDaily.takeIf { card.limits.retailMax != null }
        return when (val result = repository.setLimits(card.id, atmDaily, retail)) {
            is DataResult.Success -> LimitsResult.Saved
            is DataResult.Failure -> LimitsResult.Failed(result.error)
        }
    }
}

/** Freeze and unfreeze apply at once; the screen confirms before freezing. */
class SetCardFrozenUseCase @Inject constructor(private val repository: CardsRepository) {
    suspend operator fun invoke(cardId: String, frozen: Boolean): DataResult<Unit> =
        repository.setFrozen(cardId, frozen)
}

class UpdateCardChannelsUseCase @Inject constructor(private val repository: CardsRepository) {
    suspend operator fun invoke(cardId: String, channels: CardChannels): DataResult<Unit> =
        repository.setChannels(cardId, channels)
}

sealed interface TravelNoticeResult {
    data object Added : TravelNoticeResult

    data class Invalid(val error: TravelNoticeError) : TravelNoticeResult

    data class Failed(val error: DataError) : TravelNoticeResult
}

/** Add a travel notice; dates are whole local days, stored as the start of each day. */
class AddTravelNoticeUseCase @Inject constructor(
    private val repository: CardsRepository,
    private val clock: Clock,
    private val zone: Provider<ZoneId>
) {
    suspend operator fun invoke(
        cardId: String,
        destination: String,
        start: LocalDate,
        end: LocalDate
    ): TravelNoticeResult {
        val zone = zone.get()
        val today = clock.now().atZone(zone).toLocalDate()
        CardRules.validateTravelNotice(destination, start, end, today)?.let {
            return TravelNoticeResult.Invalid(it)
        }
        val result = repository.addTravelNotice(
            cardId = cardId,
            destination = destination.trim(),
            start = start.atStartOfDay(zone).toInstant(),
            end = end.atStartOfDay(zone).toInstant()
        )
        return when (result) {
            is DataResult.Success -> TravelNoticeResult.Added
            is DataResult.Failure -> TravelNoticeResult.Failed(result.error)
        }
    }
}
