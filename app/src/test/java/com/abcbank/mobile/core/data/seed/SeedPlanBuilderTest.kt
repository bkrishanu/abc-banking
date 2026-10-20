package com.abcbank.mobile.core.data.seed

import com.abcbank.mobile.core.data.firebase.AccountDto
import com.abcbank.mobile.core.data.firebase.CardDto
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.SavingsGoalDto
import com.abcbank.mobile.core.data.firebase.TransactionDto
import com.abcbank.mobile.core.data.firebase.UserProfileDto
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.CreditCardAccount
import com.abcbank.mobile.core.model.DepositAccount
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.core.model.TransactionDirection
import com.abcbank.mobile.feature.insights.data.toSavingsGoal
import com.abcbank.mobile.feature.insights.domain.InsightsRules
import com.abcbank.mobile.feature.insights.domain.SpendingPeriod
import com.google.firebase.Timestamp
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Builds every persona from the real seed files and checks the rules in DEMO_DATA.md section 01. */
class SeedPlanBuilderTest {
    private val seedDir = File("src/main/assets/seed")
    private val catalog = parse(File(seedDir, "catalog.json"))
    private val personas = PersonaId.entries.associateWith {
        parse(File(seedDir, "personas/${it.key}.json"))
    }
    private val plans = personas.mapValues { (_, json) -> build(json) }

    private fun build(json: JsonObject, now: Instant = NOW) =
        SeedPlanBuilder(json, catalog, now, ZONE).build()

    @Test
    fun `the same persona always seeds the same data`() = forEachPersona { persona, plan ->
        assertEquals(persona.key, plan, build(personas.getValue(persona)))
    }

    @Test
    fun `every window's spending meets each category target exactly`() = forEachPersona {
            persona,
            plan
        ->
        val targets = personas.getValue(persona).getValue("fillerTargets").jsonObject
            .mapValues { (_, value) -> value.toString().toLong() }
        val spending = transactions(plan).filter {
            it.direction == TransactionDirection.DEBIT &&
                it.category.name !in SeedPlanBuilder.NOT_SPENDING
        }
        for (window in WINDOWS) {
            val byCategory = spending.filter { dayOffset(it.createdAt) in window }
                .groupBy { it.category.name }
                .mapValues { (_, items) -> items.sumOf { it.amount.cents } }
            assertEquals("${persona.key} days $window", targets, byCategory)
        }
    }

    @Test
    fun `insights show each persona's documented spending and savings goal`() = forEachPersona {
            persona,
            plan
        ->
        val profile =
            requireNotNull(UserProfileDto.fromMap(plan.profile.forFirestore()).toUserProfile())
        val today = NOW.atZone(ZONE).toLocalDate()
        val spending = InsightsRules.spending(
            transactions(plan),
            profile.monthlyBudget,
            SpendingPeriod.LAST_30_DAYS,
            today,
            ZONE
        )
        // DEMO_DATA.md: the last-30-day totals behind the Insights donut.
        val (total, percent) = when (persona) {
            PersonaId.SAMANTA -> 1_248_000L to 66
            PersonaId.MARCUS -> 275_500L to 92
            PersonaId.GRACE -> 369_200L to 74
            PersonaId.DANIEL -> 3_200_000L to 80
        }
        assertEquals(persona.key, total, spending.total.cents)
        assertEquals(persona.key, percent, spending.percentOfBudget)
        assertEquals(persona.key, total, spending.slices.sumOf { it.amount.cents })
        if (persona == PersonaId.SAMANTA) {
            assertEquals(
                listOf(
                    TransactionCategory.SHOPPING to 37,
                    TransactionCategory.ENTERTAINMENT to 21,
                    null to 42
                ),
                spending.slices.map { it.category to it.percentOfSpending }
            )
        }
        val goals = documents(plan, FirestorePaths.SAVINGS_GOALS).map { (id, fields) ->
            requireNotNull(SavingsGoalDto.fromMap(fields).toSavingsGoal(id)) { "$id does not map" }
        }
        val savings = InsightsRules.savings(goals.single(), ZONE)
        assertEquals(13, savings.points.size)
        assertEquals(savings.goal.saved, savings.points.last().saved)
        assertEquals(today.minusDays(90), savings.points.first().date)
    }

    @Test
    fun `filler stays inside each pool's range`() = forEachPersona { persona, plan ->
        val pools = catalog.getValue("fillerMerchants").jsonObject
        val filler = transactions(plan).filter { it.id.startsWith("fx-") }
        assertTrue(filler.isNotEmpty())
        for (item in filler) {
            assertTrue(dayOffset(item.createdAt) in -122..-3)
            val pool = pools[item.category.name]?.jsonObject ?: continue
            val merchants = pool["merchants"]?.toString() ?: continue
            assertTrue("${persona.key} ${item.merchant}", "\"${item.merchant}\"" in merchants)
            val min = pool.getValue("min").toString().toLong()
            val max = pool.getValue("max").toString().toLong()
            val total = filler.filter { it.category == item.category }.sumOf { it.amount.cents }
            if (total >= min) {
                assertTrue(
                    "${persona.key} ${item.id} ${item.amount}",
                    item.amount.cents in min..max
                )
            }
        }
    }

    @Test
    fun `running balances end at the documented balances and never go negative`() =
        forEachPersona { persona, plan ->
            val accounts = accounts(plan).associateBy { it.id }
            for ((accountId, items) in transactions(plan).groupBy { it.accountId }) {
                val newestFirst = items.sortedWith(
                    compareByDescending<Transaction> { it.createdAt }.thenByDescending { it.id }
                )
                when (val account = accounts.getValue(accountId)) {
                    is DepositAccount -> {
                        assertEquals(account.ledger, newestFirst.first().runningBalance)
                        assertTrue(
                            "${persona.key} $accountId goes negative",
                            items.none { it.runningBalance.isNegative }
                        )
                    }
                    is CreditCardAccount -> {
                        assertEquals(account.currentBalance, newestFirst.first().runningBalance)
                        assertTrue(
                            "${persona.key} $accountId over its limit",
                            items.all {
                                !it.runningBalance.isNegative &&
                                    it.runningBalance <= account.creditLimit
                            }
                        )
                    }
                    else -> error("${persona.key}: transactions on $accountId")
                }
            }
        }

    @Test
    fun `references are unique UUIDs`() = forEachPersona { persona, plan ->
        val references = transactions(plan).map { it.reference }
        assertEquals(persona.key, references.size, references.toSet().size)
        references.forEach { assertEquals(4, java.util.UUID.fromString(it).version()) }
    }

    @Test
    fun `every document maps and every path is unique`() = forEachPersona { persona, plan ->
        assertNotNull(UserProfileDto.fromMap(plan.profile.forFirestore()).toUserProfile())
        assertEquals(3L, plan.seedVersion)
        documents(plan, FirestorePaths.CARDS).forEach { (id, fields) ->
            assertNotNull(CardDto.fromMap(fields).toCard(id))
        }
        val paths = plan.documents.map { it.path.joinToString("/") }
        assertEquals(persona.key, paths.size, paths.toSet().size)
        assertTrue(plan.documents.all { it.path.size % 2 == 0 })
    }

    @Test
    fun `income repeats every month so each statement shows a salary`() = forEachPersona {
            persona,
            plan
        ->
        val income = transactions(plan).filter { it.category.name == "INCOME" }
        val perWindow = WINDOWS.map { window -> income.count { dayOffset(it.createdAt) in window } }
        assertTrue(
            "${persona.key}: $perWindow",
            perWindow.all {
                it == perWindow.first() && it > 0
            }
        )
    }

    @Test
    fun `Marcus goes below $500 at the rent payment, where his alert is dated`() {
        val checking = transactions(plans.getValue(PersonaId.MARCUS))
            .filter { it.accountId == "checking" && dayOffset(it.createdAt) in -29..0 }
            .sortedWith(compareBy<Transaction> { it.createdAt }.thenBy { it.id })
        val crossings = checking.zipWithNext().filter { (before, after) ->
            before.runningBalance.cents >= LOW_BALANCE && after.runningBalance.cents < LOW_BALANCE
        }
        assertEquals(listOf("tx-003"), crossings.map { it.second.id })
        val alert = documents(plans.getValue(PersonaId.MARCUS), FirestorePaths.NOTIFICATIONS)
            .single { (_, fields) -> fields["title"] == "Low balance" }.second
        assertEquals(
            crossings.single().second.createdAt,
            (alert.getValue("createdAt") as Timestamp).toInstant()
        )
    }

    @Test
    fun `notification date tokens become short dates`() {
        val bodies = documents(plans.getValue(PersonaId.GRACE), FirestorePaths.NOTIFICATIONS)
            .map { (_, fields) -> fields.getValue("body") as String }
        assertTrue(bodies.contains("Your Main and Rewards cards are set for Canada until Oct 29."))
        assertFalse(plans.values.any { it.documents.toString().contains("{date") })
    }

    @Test
    fun `a later-today item moves to before now and keeps its order`() {
        val earlyMorning = LocalDate.of(2026, 10, 19).atTime(9, 0).toInstant(ZoneOffset.UTC)
        val plan = build(personas.getValue(PersonaId.SAMANTA), now = earlyMorning)
        val today = transactions(plan).filter { it.id in setOf("tx-001", "tx-002", "tx-003") }
            .associateBy { it.id }

        val boutique = today.getValue("tx-001").createdAt
        val furniture = today.getValue("tx-002").createdAt
        assertTrue(boutique.isBefore(earlyMorning.minusSeconds(599)))
        assertTrue(boutique.isAfter(earlyMorning.minusSeconds(630)))
        assertTrue(furniture.isBefore(boutique))
        assertEquals(earlyMorning.minus(51, ChronoUnit.MINUTES), today.getValue("tx-003").createdAt)
    }

    private fun forEachPersona(check: (PersonaId, SeedPlan) -> Unit) =
        plans.forEach { (persona, plan) -> check(persona, plan) }

    private fun documents(
        plan: SeedPlan,
        collection: String
    ): List<Pair<String, Map<String, Any?>>> =
        plan.documents.filter { it.path.size == 2 && it.path[0] == collection }
            .map { it.path[1] to it.fields.forFirestore() }

    private fun transactions(plan: SeedPlan): List<Transaction> =
        documents(plan, FirestorePaths.TRANSACTIONS).map { (id, fields) ->
            requireNotNull(TransactionDto.fromMap(fields).toTransaction(id)) { "$id does not map" }
        }

    private fun accounts(plan: SeedPlan): List<Account> =
        documents(plan, FirestorePaths.ACCOUNTS).map { (id, fields) ->
            requireNotNull(AccountDto.fromMap(fields).toAccount(id)) { "$id does not map" }
        }

    private companion object {
        val ZONE: ZoneId = ZoneOffset.UTC
        val NOW: Instant = LocalDate.of(2026, 10, 19).atTime(13, 0).toInstant(ZoneOffset.UTC)
        const val LOW_BALANCE = 50_000L
        val WINDOWS = listOf(-29..0, -59..-30, -89..-60, -122..-90)

        fun parse(file: File): JsonObject = Json.parseToJsonElement(file.readText()).jsonObject

        fun dayOffset(instant: Instant): Int = ChronoUnit.DAYS.between(
            NOW.atZone(ZONE).toLocalDate(),
            instant.atZone(ZONE).toLocalDate()
        ).toInt()

        /** What DemoDataSeeder hands to Firestore: Instants become Timestamps. */
        fun Map<String, Any>.forFirestore(): Map<String, Any?> = mapValues { (_, value) ->
            if (value is Instant) Timestamp(value.epochSecond, value.nano) else value
        }
    }
}
