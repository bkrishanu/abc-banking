package com.abcbank.mobile.feature.products.domain

import com.abcbank.mobile.core.model.Money
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.math.absoluteValue

/** The card an approved card application issues (DEMO_DATA.md section 07, new accounts). */
data class NewCard(
    val id: String,
    val accountId: String,
    val tag: String,
    val holderName: String,
    val pan: String,
    val cvv: String,
    val last4: String,
    val expiry: YearMonth,
    val cashAdvanceDaily: Money,
    val cashAdvanceMax: Money,
    val stackOrder: Int
)

/**
 * Issues the fake card for a new card account, following the seeded cards (agreed 2026-10-20):
 * a Mastercard test-range number 5399 0000 00xx ending in the account's last 4, with one digit
 * chosen so it passes the Luhn check; expiry 4 years from opening; a CVV from the application id;
 * cash advance at 10% of the limit a day, up to 20%; placed after the seeded cards. Not real.
 */
object CardIssuer {
    private const val PREFIX = "5399000000"
    private const val CVV_MIN = 100
    private const val CVV_SPAN = 900
    private const val EXPIRY_YEARS = 4L
    private const val DAILY_PERCENT = 10
    private const val MAX_PERCENT = 20
    private const val PERCENT = 100
    private const val STEP_CENTS = 10_000L
    private const val STACK_AFTER_SEEDED = 9
    private const val DIGITS = 10

    fun issue(
        account: NewCardAccount,
        tag: String,
        holderName: String,
        applicationId: String
    ): NewCard = NewCard(
        id = "card-${account.id}",
        accountId = account.id,
        tag = tag,
        holderName = holderName.uppercase(),
        pan = pan(account.last4),
        cvv = cvv(applicationId),
        last4 = account.last4,
        expiry = YearMonth.from(account.openedAt.atZone(ZoneOffset.UTC)).plusYears(EXPIRY_YEARS),
        cashAdvanceDaily = share(account.creditLimit, DAILY_PERCENT),
        cashAdvanceMax = share(account.creditLimit, MAX_PERCENT),
        stackOrder = STACK_AFTER_SEEDED
    )

    private fun cvv(applicationId: String): String =
        (CVV_MIN + applicationId.reversed().hashCode().toLong().absoluteValue % CVV_SPAN).toString()

    /** 5399 0000 00, one check-making digit, a zero, then [last4]: 16 digits that pass Luhn. */
    fun pan(last4: String): String {
        require(last4.length == 4 && last4.all(Char::isDigit)) { "last4 must be four digits" }
        return (0 until DIGITS).map { "$PREFIX${it}0$last4" }.first(::passesLuhn)
    }

    fun passesLuhn(number: String): Boolean = number.reversed().mapIndexed { index, char ->
        val digit = char - '0'
        if (index % 2 == 1) (digit * 2).let { if (it > 9) it - 9 else it } else digit
    }.sum() % DIGITS == 0

    /** [percent] of [limit], rounded down to the $100 slider step (MobileBuild.md 4.2). */
    private fun share(limit: Money, percent: Int): Money =
        Money(limit.cents * percent / PERCENT / STEP_CENTS * STEP_CENTS)
}
