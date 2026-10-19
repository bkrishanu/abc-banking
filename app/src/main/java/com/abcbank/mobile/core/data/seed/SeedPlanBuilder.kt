package com.abcbank.mobile.core.data.seed

import com.abcbank.mobile.core.data.firebase.AccountDto
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.TransactionDto
import com.abcbank.mobile.core.data.firebase.UserProfileDto
import java.nio.ByteBuffer
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.random.Random
import kotlinx.serialization.json.JsonObject

/** One document to write, with its path below `users/{uid}` (collection, id, collection, id...). */
internal data class SeedDocument(val path: List<String>, val fields: Map<String, Any>)

/** Everything a persona seed writes. Date fields hold [Instant]s; the writer converts them. */
internal data class SeedPlan(
    val seedVersion: Long,
    val profile: Map<String, Any>,
    val documents: List<SeedDocument>
)

/**
 * Turns a persona file and the catalog into the documents DemoDataSeeder writes: resolves dates,
 * adds the filler history, and computes each transaction's reference and running balance
 * (DEMO_DATA.md sections 01 and 08). Pure, so tests can check a whole persona without Firebase.
 */
internal class SeedPlanBuilder(
    private val persona: JsonObject,
    private val catalog: JsonObject,
    private val now: Instant,
    zone: ZoneId
) {
    private val dates = SeedDates(now, zone)
    private val resolve: (SeedDate) -> Any = dates::resolve

    // One generator for the whole persona, filler first, so the output never changes.
    private val random = Random(RANDOM_SEED)

    fun build(): SeedPlan {
        val seedVersion = persona.seedLong(SEED_VERSION)
        val profile = persona.seedObject(PROFILE).toFields(resolve) +
            mapOf(UserProfileDto.SEED_VERSION to seedVersion, UserProfileDto.CREATED_AT to now)
        val documents = buildList {
            addAll(simple(FirestorePaths.ACCOUNTS))
            addAll(holdings())
            addAll(cards())
            addAll(simple(FirestorePaths.BENEFICIARIES))
            addAll(transactions())
            addAll(simple(FirestorePaths.BILLS))
            addAll(simple(FirestorePaths.SAVINGS_GOALS))
            addAll(notifications())
            addAll(simple(FirestorePaths.SUPPORT_CASES))
        }
        return SeedPlan(seedVersion, profile, documents)
    }

    private fun simple(collection: String): List<SeedDocument> = persona.seedArray(collection).map {
        SeedDocument(
            listOf(collection, it.seedString(ID)),
            it.toFields(resolve, except = setOf(ID))
        )
    }

    private fun holdings(): List<SeedDocument> = persona.seedArray(HOLDINGS_KEY).map {
        SeedDocument(
            listOf(
                FirestorePaths.ACCOUNTS,
                it.seedString(ACCOUNT_ID),
                FirestorePaths.HOLDINGS,
                it.seedString(ID)
            ),
            it.toFields(resolve, except = setOf(ID, ACCOUNT_ID))
        )
    }

    private fun cards(): List<SeedDocument> =
        persona.seedArray(FirestorePaths.CARDS).flatMap { card ->
            val cardId = card.seedString(ID)
            val notices = card.seedArray(FirestorePaths.TRAVEL_NOTICES).map { notice ->
                SeedDocument(
                    listOf(
                        FirestorePaths.CARDS,
                        cardId,
                        FirestorePaths.TRAVEL_NOTICES,
                        notice.seedString(ID)
                    ),
                    notice.toFields(resolve, except = setOf(ID))
                )
            }
            val fields = card.toFields(resolve, except = setOf(ID, FirestorePaths.TRAVEL_NOTICES))
            listOf(SeedDocument(listOf(FirestorePaths.CARDS, cardId), fields)) + notices
        }

    private fun notifications(): List<SeedDocument> =
        simple(FirestorePaths.NOTIFICATIONS).map { document ->
            val body = document.fields[BODY] as? String ?: return@map document
            document.copy(fields = document.fields + (BODY to dates.replaceDateTokens(body)))
        }

    private fun transactions(): List<SeedDocument> {
        val explicit = persona.seedArray(FirestorePaths.TRANSACTIONS).map { json ->
            SeedTransaction(
                id = json.seedString(ID),
                accountId = json.seedString(ACCOUNT_ID),
                isDebit = json.seedString(TransactionDto.DIRECTION) == DEBIT,
                amount = json.seedLong(TransactionDto.AMOUNT),
                isPosted = json.seedString(TransactionDto.STATUS) == POSTED,
                createdAt = dates.resolve(json.seedDate(TransactionDto.CREATED_AT)),
                fields = json.toFields(resolve, except = setOf(ID))
            )
        }
        val all = explicit + filler()

        // References are drawn oldest first so they do not depend on how the list is shown.
        val chronological = all.sortedWith(compareBy({ it.createdAt }, { it.id }))
        val references = chronological.associate { it.id to randomUuid().toString() }
        val runningBalances = runningBalances(chronological)

        return chronological.map {
            SeedDocument(
                listOf(FirestorePaths.TRANSACTIONS, it.id),
                it.fields + mapOf(
                    TransactionDto.REFERENCE to references.getValue(it.id),
                    TransactionDto.RUNNING_BALANCE to runningBalances.getValue(it.id)
                )
            )
        }
    }

    private fun filler(): List<SeedTransaction> {
        val mainAccountId = persona.seedArray(FirestorePaths.CARDS)
            .firstOrNull { it.seedLong(STACK_ORDER) == 0L }
            ?.seedString(ACCOUNT_ID)
            ?: throw MalformedSeedException("No Main card")
        val explicitSpending = persona.seedArray(FirestorePaths.TRANSACTIONS)
            .filter {
                it.seedString(TransactionDto.DIRECTION) == DEBIT &&
                    it.seedString(TransactionDto.CATEGORY) !in NOT_SPENDING
            }
            .map {
                val date = it.seedDate(TransactionDto.CREATED_AT)
                ExplicitSpend(
                    category = it.seedString(TransactionDto.CATEGORY),
                    merchant = it.seedString(TransactionDto.MERCHANT),
                    amount = it.seedLong(TransactionDto.AMOUNT),
                    dayOffset = date.dayOffset,
                    time = date.time
                )
            }
        val targets = persona.seedObject(FILLER_TARGETS).keys.associateWith {
            persona.seedObject(FILLER_TARGETS).seedLong(it)
        }
        val items = FillerGenerator(random).generate(
            targets = targets,
            pools = pools(),
            utilityBills = utilityBills(),
            explicit = explicitSpending
        )
        return items
            .sortedWith(
                compareByDescending<FillerItem> {
                    it.dayOffset
                }.thenByDescending { it.time }
            )
            .mapIndexed { index, item ->
                val createdAt = dates.resolve(SeedDate(item.dayOffset, item.time))
                val id = FILLER_ID_PREFIX + (index + 1).toString().padStart(FILLER_ID_DIGITS, '0')
                SeedTransaction(
                    id = id,
                    accountId = mainAccountId,
                    isDebit = true,
                    amount = item.amount,
                    isPosted = true,
                    createdAt = createdAt,
                    fields = mapOf(
                        TransactionDto.ACCOUNT_ID to mainAccountId,
                        TransactionDto.DIRECTION to DEBIT,
                        TransactionDto.AMOUNT to item.amount,
                        TransactionDto.MERCHANT to item.merchant,
                        TransactionDto.CATEGORY to item.category,
                        TransactionDto.STATUS to POSTED,
                        TransactionDto.CREATED_AT to createdAt
                    )
                )
            }
    }

    private fun pools(): Map<String, FillerPool> {
        val merchants = catalog.seedObject(FILLER_MERCHANTS)
        return merchants.keys.filter { it != FillerGenerator.UTILITIES }.associateWith { category ->
            val pool = merchants.seedObject(category)
            FillerPool(
                merchants = pool.seedStrings(MERCHANTS),
                min = pool.seedLong(MIN),
                max = pool.seedLong(MAX)
            )
        }
    }

    private fun utilityBills(): List<UtilityBill> {
        val utilityBillerIds = catalog.seedObject(FILLER_MERCHANTS)
            .seedObject(FillerGenerator.UTILITIES)
            .seedStrings(BILLER_IDS)
            .toSet()
        val billerNames = catalog.seedArray(BILLERS).associate {
            it.seedString(ID) to
                it.seedString(NAME)
        }
        return persona.seedArray(FirestorePaths.BILLS)
            .filter { it.seedString(BILLER_ID) in utilityBillerIds }
            .map {
                UtilityBill(
                    billerName = billerNames[it.seedString(BILLER_ID)]
                        ?: throw MalformedSeedException("Unknown biller"),
                    amount = it.seedLong(AMOUNT_DUE)
                )
            }
    }

    /**
     * Works back from each account's closing ledger balance (DEMO_DATA.md lists balances after
     * all seeded transactions). The value stored on a transaction is the balance after it. For
     * credit cards and loans the balance is the amount owed, so a debit raises it.
     */
    private fun runningBalances(chronological: List<SeedTransaction>): Map<String, Long> {
        val accounts = persona.seedArray(FirestorePaths.ACCOUNTS).associateBy { it.seedString(ID) }
        val balances = mutableMapOf<String, Long>()
        for ((accountId, items) in chronological.groupBy { it.accountId }) {
            val account = accounts[accountId] ?: throw MalformedSeedException("Unknown account")
            val owedStyle = account.seedString(AccountDto.TYPE) in OWED_TYPES
            var balance = account.seedLong(AccountDto.LEDGER_BALANCE)
            for (item in items.asReversed()) {
                balances[item.id] = balance
                // Pending items are not in the ledger balance yet.
                if (!item.isPosted) continue
                val raisesBalance = item.isDebit == owedStyle
                balance = if (raisesBalance) balance - item.amount else balance + item.amount
            }
        }
        return balances
    }

    private fun randomUuid(): UUID {
        val bytes = random.nextBytes(UUID_BYTES)
        // Version 4 (random) and the IETF variant, like UUID.randomUUID().
        bytes[6] = (bytes[6].toInt() and 0x0f or 0x40).toByte()
        bytes[8] = (bytes[8].toInt() and 0x3f or 0x80).toByte()
        val buffer = ByteBuffer.wrap(bytes)
        return UUID(buffer.long, buffer.long)
    }

    private data class SeedTransaction(
        val id: String,
        val accountId: String,
        val isDebit: Boolean,
        val amount: Long,
        val isPosted: Boolean,
        val createdAt: Instant,
        val fields: Map<String, Any>
    )

    companion object {
        /** The fixed seed in DEMO_DATA.md section 01: every install of a persona looks the same. */
        const val RANDOM_SEED = 181019

        /** Transfers move money; they are not spending (DEMO_DATA.md section 01). */
        val NOT_SPENDING = setOf("TRANSFER", "TRANSFER_OUT")

        private const val SEED_VERSION = "seedVersion"
        private const val PROFILE = "profile"
        private const val HOLDINGS_KEY = "holdings"
        private const val FILLER_TARGETS = "fillerTargets"
        private const val FILLER_MERCHANTS = "fillerMerchants"
        private const val BILLERS = "billers"
        private const val MERCHANTS = "merchants"
        private const val BILLER_IDS = "billerIds"
        private const val BILLER_ID = "billerId"
        private const val AMOUNT_DUE = "amountDue"
        private const val STACK_ORDER = "stackOrder"
        private const val MIN = "min"
        private const val MAX = "max"
        private const val ID = "id"
        private const val NAME = "name"
        private const val ACCOUNT_ID = "accountId"
        private const val BODY = "body"
        private const val DEBIT = "DEBIT"
        private const val POSTED = "POSTED"
        private val OWED_TYPES = setOf("CREDIT", "LOAN")
        private const val FILLER_ID_PREFIX = "fx-"
        private const val FILLER_ID_DIGITS = 3
        private const val UUID_BYTES = 16
    }
}
