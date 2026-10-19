package com.abcbank.mobile.core.di

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.SystemWallClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ClockModule {
    @Binds
    @Singleton
    fun bindClock(impl: SystemWallClock): Clock
}

@Module
@InstallIn(SingletonComponent::class)
object TimeZoneModule {
    // Not a singleton: the device time zone can change while the app runs.
    @Provides
    fun provideZoneId(): ZoneId = ZoneId.systemDefault()
}
