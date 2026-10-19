package com.abcbank.mobile.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.security.MpinAttemptStore
import com.abcbank.mobile.core.security.MpinAttempts
import com.abcbank.mobile.core.security.RememberedCustomerStore
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

/** Device-local settings in DataStore. Excluded from backup and device transfer (res/xml rules). */
@Singleton
class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    MpinAttemptStore,
    RememberedCustomerStore {

    override suspend fun readMpinAttempts(): MpinAttempts {
        val prefs = current()
        return MpinAttempts(
            failedCount = prefs[MPIN_FAILED_COUNT] ?: 0,
            lockedUntil = prefs[MPIN_LOCKED_UNTIL_MS]?.let(Instant::ofEpochMilli)
        )
    }

    override suspend fun writeMpinAttempts(attempts: MpinAttempts) {
        dataStore.edit { prefs ->
            prefs[MPIN_FAILED_COUNT] = attempts.failedCount
            val lockedUntil = attempts.lockedUntil
            if (lockedUntil != null) {
                prefs[MPIN_LOCKED_UNTIL_MS] = lockedUntil.toEpochMilli()
            } else {
                prefs.remove(MPIN_LOCKED_UNTIL_MS)
            }
        }
    }

    override suspend fun rememberCustomer(customerId: CustomerId) {
        dataStore.edit { prefs -> prefs[REMEMBERED_CUSTOMER_ID] = customerId.value }
    }

    override suspend fun rememberedCustomer(): CustomerId? =
        current()[REMEMBERED_CUSTOMER_ID]?.let(CustomerId::parse)

    override suspend fun forgetCustomer() {
        dataStore.edit { prefs -> prefs.remove(REMEMBERED_CUSTOMER_ID) }
    }

    // An unreadable file is treated as empty settings rather than crashing the app.
    private suspend fun current(): Preferences = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .first()

    private companion object {
        val MPIN_FAILED_COUNT = intPreferencesKey("mpin_failed_count")
        val MPIN_LOCKED_UNTIL_MS = longPreferencesKey("mpin_locked_until_ms")
        val REMEMBERED_CUSTOMER_ID = stringPreferencesKey("remembered_customer_id")
    }
}
