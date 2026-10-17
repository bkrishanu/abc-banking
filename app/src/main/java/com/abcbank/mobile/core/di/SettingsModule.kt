package com.abcbank.mobile.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.data.prefs.SettingsRepository
import com.abcbank.mobile.core.security.MpinAttemptStore
import com.abcbank.mobile.core.security.RememberedCustomerStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object SettingsDataStoreModule {
    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        dispatchers: DispatcherProvider
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(dispatchers.io + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile("settings") }
    )
}

@Module
@InstallIn(SingletonComponent::class)
interface SettingsBindingsModule {
    @Binds
    fun bindMpinAttemptStore(impl: SettingsRepository): MpinAttemptStore

    @Binds
    fun bindRememberedCustomerStore(impl: SettingsRepository): RememberedCustomerStore
}
