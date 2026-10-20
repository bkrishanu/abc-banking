package com.abcbank.mobile.core.security

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.model.Money
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/** Actions that can require step-up (MobileBuild.md 1.2). */
sealed interface StepUpAction {
    data object AddBeneficiary : StepUpAction

    data class OutboundTransfer(val amount: Money) : StepUpAction

    data object ChangeCardLimits : StepUpAction

    data class RevealCardDetails(val cardId: String) : StepUpAction
}

/**
 * Proof that the MPIN was re-entered for one [action]. Only StepUpManager makes one, and a use
 * case spends it with [StepUpManager.consume]: it works once, for that action, for a short time.
 */
class StepUpGrant internal constructor(
    internal val id: Long,
    val action: StepUpAction,
    internal val issuedAt: Instant
)

/**
 * The step-up rules (MobileBuild.md 1.2, PRD section 08): MPIN re-entry before adding a payee,
 * sending more than $1,000, changing card limits or revealing card details. No OTP, no biometrics.
 */
@Singleton
class StepUpManager @Inject constructor(private val clock: Clock) {
    private val nextId = AtomicLong()
    private val unused = mutableSetOf<Long>()

    fun isRequired(action: StepUpAction): Boolean = when (action) {
        is StepUpAction.OutboundTransfer -> action.amount > TRANSFER_THRESHOLD
        StepUpAction.AddBeneficiary,
        StepUpAction.ChangeCardLimits,
        is StepUpAction.RevealCardDetails -> true
    }

    /** Called by StepUpController after a correct MPIN, or straight away when not required. */
    internal fun issue(action: StepUpAction): StepUpGrant = synchronized(unused) {
        val grant = StepUpGrant(nextId.incrementAndGet(), action, clock.now())
        unused += grant.id
        grant
    }

    /**
     * True, once, when [grant] was issued for exactly [action] within [GRANT_LIFETIME]. Use cases
     * call this before acting, so an action cannot skip step-up or reuse an old approval.
     */
    fun consume(grant: StepUpGrant?, action: StepUpAction): Boolean = synchronized(unused) {
        if (!isRequired(action)) return true
        if (grant == null || grant.action != action || !unused.remove(grant.id)) return false
        !clock.now().isAfter(grant.issuedAt.plus(GRANT_LIFETIME))
    }

    companion object {
        /** Transfers above this need step-up, for every persona (PRD section 08). */
        val TRANSFER_THRESHOLD: Money = Money.ofDollars(1_000)
        val GRANT_LIFETIME: Duration = Duration.ofMinutes(2)
    }
}
