package com.abcbank.mobile.core.data.firebase

import com.abcbank.mobile.core.model.AccountType
import com.abcbank.mobile.core.model.CardArt
import com.abcbank.mobile.core.model.CardNetwork
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.InvestmentAccount
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.model.Rate
import com.abcbank.mobile.core.model.TermDepositAccount
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.core.model.TransactionStatus
import com.google.firebase.Timestamp
import java.time.Instant
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Maps as Firestore returns them (integers as Long), built from Samanta's data in DEMO_DATA.md. */
class FirestoreMappersTest {
    private val day0: Instant = Instant.parse("2026-10-17T15:14:00Z")
    private val timestamp = Timestamp(day0.epochSecond, 0)

    private val checking = mapOf<String, Any?>(
        "type" to "CHECKING",
        "nickname" to "Everyday Checking",
        "last4" to "3144",
        "availableBalance" to 15_583_200L,
        "ledgerBalance" to 15_620_700L
    )

    private val debitCard = mapOf<String, Any?>(
        "accountId" to "checking",
        "tag" to "Main card",
        "network" to "MASTERCARD",
        "artKey" to "PEACH",
        "holderName" to "SAMANTA JOHNS",
        "pan" to "5399000000093144",
        "cvv" to "314",
        "last4" to "3144",
        "expiry" to "08/29",
        "frozen" to false,
        "atmDailyLimit" to 100_000L,
        "atmMaxLimit" to 250_000L,
        "retailDailyLimit" to 500_000L,
        "retailMaxLimit" to 1_000_000L,
        "international" to true,
        "online" to true,
        "contactless" to true,
        "stackOrder" to 0L
    )

    private val transaction = mapOf<String, Any?>(
        "accountId" to "checking",
        "direction" to "DEBIT",
        "amount" to 12_500L,
        "merchant" to "Aster & Vine Boutique",
        "category" to "SHOPPING",
        "reference" to "6f1c2a8e-0b7d-4c55-9a1e-3d2b7c9e4f10",
        "runningBalance" to 15_583_200L,
        "status" to "POSTED",
        "createdAt" to timestamp
    )

    @Test
    fun `checking account maps with available and ledger balances`() {
        val account = AccountDto.fromMap(checking).toAccount("checking") as DepositAccount

        assertEquals(AccountType.CHECKING, account.type)
        assertEquals(Money(15_583_200), account.available)
        assertEquals(Money(15_620_700), account.ledger)
        assertNull(account.apy)
    }

    @Test
    fun `every account type maps from its own fields`() {
        val credit = AccountDto.fromMap(
            mapOf(
                "type" to "CREDIT", "nickname" to "Signature Credit Card", "last4" to "5821",
                "creditLimit" to 5_000_000L, "ledgerBalance" to 1_176_500L,
                "availableBalance" to 3_823_500L, "statementBalance" to 948_025L,
                "minimumDue" to 28_500L, "dueDate" to timestamp
            )
        ).toAccount("credit") as CreditCardAccount
        assertEquals(Money(1_176_500), credit.currentBalance)
        assertEquals(Money(3_823_500), credit.availableCredit)
        assertFalse(credit.autoPayFull)

        val loan = AccountDto.fromMap(
            mapOf(
                "type" to "LOAN",
                "nickname" to "Personal Line of Credit",
                "last4" to "6608",
                "creditLimit" to 1_500_000L,
                "ledgerBalance" to 250_000L,
                "availableBalance" to 1_250_000L,
                "rate" to 925L
            )
        ).toAccount("loc") as LoanAccount
        assertEquals(Rate(925), loan.apr)
        assertEquals(Money(1_250_000), loan.availableToDraw)

        val term = AccountDto.fromMap(
            mapOf(
                "type" to "TERM_DEPOSIT",
                "nickname" to "12-Month Term Deposit",
                "last4" to "9034",
                "principal" to 2_500_000L,
                "rate" to 435L,
                "maturesAt" to timestamp
            )
        ).toAccount("term") as TermDepositAccount
        assertEquals(day0, term.maturesAt)
        assertNull(term.openedAt)

        val investment = AccountDto.fromMap(
            mapOf(
                "type" to "INVESTMENT",
                "nickname" to "Investment Portfolio",
                "last4" to "4410",
                "portfolioValue" to 8_461_240L
            )
        ).toAccount("inv") as InvestmentAccount
        assertEquals(Money(8_461_240), investment.portfolioValue)
    }

    @Test
    fun `account with a missing, wrongly typed or invalid field is rejected`() {
        assertNull(AccountDto.fromMap(checking - "ledgerBalance").toAccount("a"))
        assertNull(AccountDto.fromMap(checking + ("availableBalance" to 155_832.0)).toAccount("a"))
        assertNull(AccountDto.fromMap(checking + ("availableBalance" to -1L)).toAccount("a"))
        assertNull(AccountDto.fromMap(checking + ("type" to "WALLET")).toAccount("a"))
        assertNull(AccountDto.fromMap(checking + ("last4" to "31a4")).toAccount("a"))
        assertNull(AccountDto.fromMap(checking + ("rate" to -5L)).toAccount("a"))
    }

    @Test
    fun `account round-trips through its write map`() {
        val dto = AccountDto.fromMap(checking)

        assertEquals(checking, dto.toMap())
    }

    @Test
    fun `transaction maps and signs debits`() {
        val mapped = requireNotNull(TransactionDto.fromMap(transaction).toTransaction("tx1"))

        assertEquals(TransactionDirection.DEBIT, mapped.direction)
        assertEquals(TransactionCategory.SHOPPING, mapped.category)
        assertEquals(TransactionStatus.POSTED, mapped.status)
        assertEquals(Money(-12_500), mapped.signedAmount)
        assertEquals(day0, mapped.createdAt)
        assertFalse(mapped.disputed)
        assertNull(mapped.clearsAt)
    }

    @Test
    fun `transaction with a zero amount or unknown category is rejected`() {
        assertNull(TransactionDto.fromMap(transaction + ("amount" to 0L)).toTransaction("tx"))
        assertNull(
            TransactionDto.fromMap(transaction + ("category" to "Shopping")).toTransaction("tx")
        )
        assertNull(TransactionDto.fromMap(transaction - "createdAt").toTransaction("tx"))
    }

    @Test
    fun `card maps without its number or CVV`() {
        val card = requireNotNull(CardDto.fromMap(debitCard).toCard("main"))

        assertEquals(CardNetwork.MASTERCARD, card.network)
        assertEquals(CardArt.PEACH, card.art)
        assertEquals(YearMonth.of(2029, 8), card.expiry)
        assertEquals(Money(500_000), card.limits.retailDaily)
        assertTrue(card.channels.contactless)
        assertFalse(card.toString().contains("5399000000093144"))
    }

    @Test
    fun `card secrets are checked and never printed`() {
        val secrets = requireNotNull(CardDto.fromMap(debitCard).toCardSecrets())

        assertEquals("5399000000093144", secrets.pan)
        assertEquals("314", secrets.cvv)
        assertFalse(secrets.toString().contains("5399"))
        assertNull(CardDto.fromMap(debitCard + ("pan" to "5399000000091111")).toCardSecrets())
        assertNull(CardDto.fromMap(debitCard + ("cvv" to "31")).toCardSecrets())
    }

    @Test
    fun `credit card has no retail limits`() {
        val credit = debitCard - "retailDailyLimit" - "retailMaxLimit"

        val card = requireNotNull(CardDto.fromMap(credit).toCard("sig"))

        assertNull(card.limits.retailDaily)
        assertNull(card.limits.retailMax)
    }

    @Test
    fun `card with inconsistent limits or a bad expiry is rejected`() {
        assertNull(CardDto.fromMap(debitCard + ("atmDailyLimit" to 300_000L)).toCard("c"))
        assertNull(CardDto.fromMap(debitCard - "retailMaxLimit").toCard("c"))
        assertNull(CardDto.fromMap(debitCard + ("expiry" to "13/29")).toCard("c"))
        assertNull(CardDto.fromMap(debitCard + ("expiry" to "8/29")).toCard("c"))
        assertNull(CardDto.fromMap(debitCard + ("artKey" to "card-peach")).toCard("c"))
    }

    @Test
    fun `travel notice must end on or after its start`() {
        val notice = mapOf<String, Any?>(
            "destination" to "Lisbon, Portugal",
            "startDate" to timestamp,
            "endDate" to Timestamp(day0.plusSeconds(86_400).epochSecond, 0)
        )

        assertEquals(
            "Lisbon, Portugal",
            TravelNoticeDto.fromMap(notice).toTravelNotice("n")?.destination
        )
        val reversed = notice + ("endDate" to Timestamp(day0.minusSeconds(1).epochSecond, 0))
        assertNull(TravelNoticeDto.fromMap(reversed).toTravelNotice("n"))
    }

    @Test
    fun `beneficiary maps recent order and falls back to the full name`() {
        val beneficiary = mapOf<String, Any?>(
            "name" to "Mark Robinson",
            "shortName" to "Mark R.",
            "bankName" to "Northfield Bank",
            "routingNumber" to "500000021",
            "accountLast4" to "1180",
            "isRecent" to true,
            "recentOrder" to 0L
        )

        val recent = requireNotNull(BeneficiaryDto.fromMap(beneficiary).toBeneficiary("b1"))
        assertEquals(0, recent.recentOrder)
        assertTrue(recent.isRecent)

        val saved = requireNotNull(
            BeneficiaryDto.fromMap(
                beneficiary - "shortName" - "recentOrder" + ("isRecent" to false)
            )
                .toBeneficiary("b2")
        )
        assertEquals("Mark Robinson", saved.shortName)
        assertFalse(saved.isRecent)

        assertNull(BeneficiaryDto.fromMap(beneficiary - "recentOrder").toBeneficiary("b3"))
        assertNull(
            BeneficiaryDto.fromMap(
                beneficiary + ("routingNumber" to "50000002")
            ).toBeneficiary("b4")
        )
    }

    @Test
    fun `user profile maps persona and customer ID`() {
        val profile = mapOf<String, Any?>(
            "personaId" to "samanta",
            "customerId" to "SAMJOH01",
            "seedVersion" to 1L,
            "name" to "Samanta Johns",
            "holderName" to "SAMANTA JOHNS",
            "email" to "samanta.johns@example.com",
            "phone" to "+1 (555) 010-2231",
            "avatarKey" to "samanta",
            "employment" to "Employed, Product Design Lead at Lumen Studio",
            "annualIncome" to 18_500_000L,
            "monthlyBudget" to 1_890_000L,
            "creditScore" to 792L,
            "checkDepositLimit" to 1_000_000L,
            "createdAt" to timestamp
        )

        val mapped = requireNotNull(UserProfileDto.fromMap(profile).toUserProfile())

        assertEquals(PersonaId.SAMANTA, mapped.personaId)
        assertEquals(CustomerId.parse("SAMJOH01"), mapped.customerId)
        assertEquals("Samanta", mapped.firstName)
        assertEquals(Money(1_000_000), mapped.checkDepositLimit)
        assertNull(UserProfileDto.fromMap(profile + ("personaId" to "SAMANTA")).toUserProfile())
        assertNull(UserProfileDto.fromMap(profile + ("creditScore" to 900L)).toUserProfile())
        assertNull(UserProfileDto.fromMap(profile + ("customerId" to "SAM")).toUserProfile())
    }
}
