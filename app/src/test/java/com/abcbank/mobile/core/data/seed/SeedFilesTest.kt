package com.abcbank.mobile.core.data.seed

import com.abcbank.mobile.core.data.firebase.AccountDto
import com.abcbank.mobile.core.data.firebase.BeneficiaryDto
import com.abcbank.mobile.core.data.firebase.CardDto
import com.abcbank.mobile.core.data.firebase.HoldingDto
import com.abcbank.mobile.core.data.firebase.TransactionDto
import com.abcbank.mobile.core.data.firebase.TravelNoticeDto
import com.abcbank.mobile.core.data.firebase.UserProfileDto
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.InvestmentAccount
import com.abcbank.mobile.core.model.LoanAccount
import com.abcbank.mobile.core.model.PersonaId
import com.google.firebase.Timestamp
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the bundled seed JSON (DEMO_DATA.md section 08) before any seeder reads it: every record
 * passes the Firestore mappers, references resolve, and the numbers agree with each other.
 */
class SeedFilesTest {
    private val seedDir = File("src/main/assets/seed")
    private val catalog = parse(File(seedDir, "catalog.json"))
    private val personas = PersonaId.entries.associateWith {
        parse(File(seedDir, "personas/${it.key}.json"))
    }

    private val bankNames = catalog.array("banks").associate {
        it.string("routingNumber") to
            it.string("name")
    }
    private val billerIds = catalog.array("billers").map { it.string("id") }.toSet()

    @Test
    fun `each persona file matches its customer ID and persona`() = forEachPersona {
            persona,
            seed
        ->
        val profile = seed.obj("profile")
        val customerId = requireNotNull(CustomerId.parse(profile.string("customerId")))

        assertEquals(persona, PersonaCatalog.personaFor(customerId))
        assertEquals(persona.key, profile.string("personaId"))
        assertEquals(persona.key, profile.string("avatarKey"))
        assertEquals(SEED_VERSION, seed.getValue("seedVersion").jsonPrimitive.long)

        val withSeedFields = profile.toFirestoreMap() +
            mapOf("seedVersion" to SEED_VERSION, "createdAt" to SEED_TIME)
        assertNotNull(UserProfileDto.fromMap(withSeedFields).toUserProfile())
    }

    @Test
    fun `accounts map and their balances agree`() = forEachPersona { persona, seed ->
        val accounts = seed.array("accounts").map { json ->
            requireNotNull(AccountDto.fromMap(json.toFirestoreMap()).toAccount(json.string("id"))) {
                "${persona.key}: account ${json.string("id")} does not map"
            }
        }
        val pendingDebits = seed.array("transactions")
            .filter { it.string("status") == "PENDING" && it.string("direction") == "DEBIT" }
            .groupBy { it.string("accountId") }
            .mapValues { (_, items) -> items.sumOf { it.long("amount") } }

        for (account in accounts) {
            when (account) {
                is DepositAccount -> assertEquals(
                    "${persona.key}: ${account.id} available = ledger - pending debits",
                    account.ledger.cents - (pendingDebits[account.id] ?: 0L),
                    account.available.cents
                )
                is CreditCardAccount -> assertEquals(
                    "${persona.key}: ${account.id} available = limit - balance",
                    account.creditLimit - account.currentBalance,
                    account.availableCredit
                )
                is LoanAccount -> account.creditLimit?.let { limit ->
                    assertEquals(limit - account.owed, account.availableToDraw)
                }
                is InvestmentAccount -> assertEquals(
                    "${persona.key}: holdings add up to the portfolio",
                    account.portfolioValue.cents,
                    seed.array("holdings").filter { it.string("accountId") == account.id }
                        .sumOf { it.long("value") }
                )
                else -> Unit
            }
        }
    }

    @Test
    fun `holdings, cards and travel notices map`() = forEachPersona { persona, seed ->
        val accountIds = idsOf(seed, "accounts")
        for (holding in seed.array("holdings")) {
            assertTrue(holding.string("accountId") in accountIds)
            assertNotNull(
                HoldingDto.fromMap(holding.toFirestoreMap()).toHolding(holding.string("id"))
            )
        }
        for (json in seed.array("cards")) {
            val map = json.toFirestoreMap() - "travelNotices"
            val dto = CardDto.fromMap(map)
            val card = requireNotNull(dto.toCard(json.string("id"))) {
                "${persona.key}: card ${json.string("id")} does not map"
            }
            assertTrue(card.accountId in accountIds)
            val pan = requireNotNull(dto.toCardSecrets()).pan
            assertTrue("${persona.key}: ${card.id} passes Luhn", passesLuhn(pan))
            for (notice in json.array("travelNotices")) {
                assertNotNull(
                    TravelNoticeDto.fromMap(
                        notice.toFirestoreMap()
                    ).toTravelNotice(notice.string("id"))
                )
            }
        }
        assertEquals(
            "${persona.key}: stack orders are 0, 1, 2...",
            seed.array("cards").indices.toList(),
            seed.array("cards").map { it.long("stackOrder").toInt() }
        )
    }

    @Test
    fun `beneficiaries map and their banks are in the catalog`() = forEachPersona { persona, seed ->
        val beneficiaries = seed.array("beneficiaries")
        for (json in beneficiaries) {
            val beneficiary = requireNotNull(
                BeneficiaryDto.fromMap(json.toFirestoreMap()).toBeneficiary(json.string("id"))
            ) { "${persona.key}: beneficiary ${json.string("id")} does not map" }
            assertTrue(passesAbaChecksum(beneficiary.routingNumber))
            assertEquals(bankNames[beneficiary.routingNumber], beneficiary.bankName)
        }
        val recentOrders = beneficiaries.mapNotNull { it.optionalLong("recentOrder")?.toInt() }
        assertEquals(recentOrders.indices.toList(), recentOrders)
    }

    @Test
    fun `transactions map and reference real accounts`() = forEachPersona { persona, seed ->
        val accountIds = idsOf(seed, "accounts")
        for (json in seed.array("transactions")) {
            // The seeder generates the reference and running balance; stand-ins let the mapper run.
            val map = json.toFirestoreMap() + mapOf("reference" to "ref", "runningBalance" to 0L)
            val transaction =
                requireNotNull(TransactionDto.fromMap(map).toTransaction(json.string("id"))) {
                    "${persona.key}: transaction ${json.string("id")} does not map"
                }
            assertTrue(transaction.accountId in accountIds)
        }
    }

    @Test
    fun `bills, goals, cases and documents reference what exists`() = forEachPersona {
            persona,
            seed
        ->
        val accountIds = idsOf(seed, "accounts")
        val transactionIds = idsOf(seed, "transactions")
        seed.array("bills").forEach { assertTrue(it.string("billerId") in billerIds) }
        for (goal in seed.array("savingsGoals")) {
            assertTrue(goal.string("accountId") in accountIds)
            val history = goal.array("weeklyHistory").map { it.jsonPrimitive.long }
            assertEquals("${persona.key}: 13 weekly points", 13, history.size)
            assertEquals(goal.long("saved"), history.last())
        }
        seed.array("supportCases").forEach {
            assertTrue(it.string("transactionId") in transactionIds)
        }
        seed.obj("documents").array("statementAccountIds")
            .forEach { assertTrue(it.jsonPrimitive.content in accountIds) }
    }

    @Test
    fun `explicit spending fits inside the filler targets`() = forEachPersona { persona, seed ->
        val targets = seed.obj("fillerTargets").mapValues { (_, value) -> value.jsonPrimitive.long }
        // Pending debits count (DEMO_DATA.md section 01); transfers are not spending.
        val explicitSpending = seed.array("transactions")
            .filter { it.string("direction") == "DEBIT" && it.string("category") !in NOT_SPENDING }
            .filter { it.obj("createdAt").long("dayOffset") >= LAST_30_DAYS_FIRST_OFFSET }
            .groupBy { it.string("category") }
            .mapValues { (_, items) -> items.sumOf { it.long("amount") } }

        for ((category, spent) in explicitSpending) {
            val target = requireNotNull(targets[category]) {
                "${persona.key}: $category has explicit debits but no target"
            }
            assertTrue(
                "${persona.key}: $category explicit debits exceed the target",
                spent <= target
            )
        }
    }

    @Test
    fun `interest is a month at the account's APY`() = forEachPersona { persona, seed ->
        val accounts = seed.array("accounts").associateBy { it.string("id") }
        for (interest in seed.array("transactions").filter {
            it.string("category") == "INTEREST"
        }) {
            val account = accounts.getValue(interest.string("accountId"))
            // Closing balance x APY x 30/365, as DEMO_DATA.md section 01 states.
            val expected = Math.round(
                account.long("ledgerBalance") * account.long("rate") / 10_000.0 * 30 / 365
            )
            assertEquals(
                "${persona.key}: ${interest.string("id")}",
                expected,
                interest.long("amount")
            )
        }
    }

    @Test
    fun `card balances follow the statement cycle`() = forEachPersona { persona, seed ->
        for (card in seed.array("accounts").filter { it.string("type") == "CREDIT" }) {
            // Statements close 25 days before the due date (DEMO_DATA.md section 01).
            val closesOn = card.obj("dueDate").long("dayOffset") - STATEMENT_TO_DUE_DAYS
            val sinceStatement = seed.array("transactions")
                .filter {
                    it.string("accountId") == card.string("id") &&
                        it.obj("createdAt").long("dayOffset") > closesOn
                }
                .sumOf {
                    if (it.string("direction") ==
                        "DEBIT"
                    ) {
                        it.long("amount")
                    } else {
                        -it.long("amount")
                    }
                }
            assertEquals(
                "${persona.key}: ${card.string("id")} current = statement + newer activity",
                card.long("statementBalance") + sinceStatement,
                card.long("ledgerBalance")
            )
        }
    }

    @Test
    fun `day counts in notifications are counted from the notification date`() = forEachPersona {
            persona,
            seed
        ->
        val billerNames = catalog.array("billers").associate {
            it.string("id") to it.string("name")
        }
        val dueDays = seed.array("bills").associate {
            billerNames.getValue(it.string("billerId")) to it.obj("dueDate").long("dayOffset")
        } + seed.array("accounts").filter { "dueDate" in it.jsonObject }.associate {
            it.string("nickname") to it.obj("dueDate").long("dayOffset")
        }
        val maturityDays = seed.array("accounts").filter { "maturesAt" in it.jsonObject }
            .associate { it.string("nickname") to it.obj("maturesAt").long("dayOffset") }
        for (notification in seed.array("notifications")) {
            val body = notification.string("body")
            val sentOn = notification.obj("createdAt").long("dayOffset")
            DUE_IN.matchEntire(body)?.let { match ->
                val (name, days) = match.destructured
                assertEquals(
                    "${persona.key}: $body",
                    dueDays.getValue(name) - sentOn,
                    days.toLong()
                )
            }
            MATURES_IN.matchEntire(body)?.let { match ->
                val (name, days) = match.destructured
                assertEquals(
                    "${persona.key}: $body",
                    maturityDays.getValue(name) - sentOn,
                    days.toLong()
                )
            }
        }
    }

    @Test
    fun `ids are unique within each collection`() = forEachPersona { persona, seed ->
        for (collection in ID_COLLECTIONS) {
            val ids = seed.array(collection).map { it.string("id") }
            assertEquals("${persona.key}: duplicate id in $collection", ids.size, ids.toSet().size)
        }
    }

    @Test
    fun `catalog routing numbers pass the ABA checksum`() {
        bankNames.keys.forEach { assertTrue("$it fails the checksum", passesAbaChecksum(it)) }
    }

    private fun forEachPersona(check: (PersonaId, JsonObject) -> Unit) {
        personas.forEach { (persona, seed) -> check(persona, seed) }
    }

    private fun idsOf(seed: JsonObject, collection: String): Set<String> =
        seed.array(collection).map { it.string("id") }.toSet()

    private companion object {
        const val SEED_VERSION = 3L
        val SEED_TIME = Timestamp(1_792_000_000L, 0)

        // Day 0 and the 29 days before it. Every explicit transaction is newer than day -23,
        // so this check does not depend on the exact window edge.
        const val LAST_30_DAYS_FIRST_OFFSET = -29L
        val NOT_SPENDING = setOf("TRANSFER", "TRANSFER_OUT")
        const val STATEMENT_TO_DUE_DAYS = 25L
        val DUE_IN = Regex("""(.+?), \$[\d,.]+,? (?:minimum )?due in (\d+) days\.""")
        val MATURES_IN = Regex("""Your (.+) matures in (\d+) days\.""")
        val ID_COLLECTIONS = listOf(
            "accounts",
            "holdings",
            "cards",
            "beneficiaries",
            "transactions",
            "bills",
            "savingsGoals",
            "notifications",
            "supportCases"
        )

        fun parse(file: File): JsonObject = Json.parseToJsonElement(file.readText()).jsonObject

        fun JsonElement.string(name: String): String =
            jsonObject.getValue(name).jsonPrimitive.content

        fun JsonElement.long(name: String): Long = jsonObject.getValue(name).jsonPrimitive.long

        fun JsonElement.optionalLong(name: String): Long? =
            jsonObject[name]?.jsonPrimitive?.longOrNull

        fun JsonElement.obj(name: String): JsonObject = jsonObject.getValue(name).jsonObject

        fun JsonElement.array(name: String): JsonArray = jsonObject.getValue(name).jsonArray

        /**
         * Shapes a seed record like a Firestore snapshot map: integers as Long, and every
         * `{ dayOffset, time }` date as a Timestamp (the seeder resolves the real date).
         */
        fun JsonElement.toFirestoreMap(): Map<String, Any?> =
            jsonObject.mapValues { (_, value) -> value.toFirestoreValue() }

        fun JsonElement.toFirestoreValue(): Any? = when (this) {
            is JsonNull -> null
            is JsonPrimitive -> when {
                isString -> content
                booleanOrNull != null -> boolean
                else -> requireNotNull(longOrNull) { "Seed numbers are whole cents: $content" }
            }
            is JsonObject -> if ("dayOffset" in this) SEED_TIME else toFirestoreMap()
            is JsonArray -> map { it.toFirestoreValue() }
        }

        fun passesLuhn(number: String): Boolean = number.reversed().mapIndexed { index, char ->
            val digit = char - '0'
            if (index % 2 == 1) (digit * 2).let { if (it > 9) it - 9 else it } else digit
        }.sum() % 10 == 0

        fun passesAbaChecksum(routing: String): Boolean {
            val digits = routing.map { it - '0' }
            val weights = listOf(3, 7, 1, 3, 7, 1, 3, 7, 1)
            return digits.size == 9 && digits.zip(weights).sumOf { (d, w) -> d * w } % 10 == 0
        }
    }
}
