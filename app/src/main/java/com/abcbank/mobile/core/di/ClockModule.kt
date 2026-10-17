package com.abcbank.mobile.core.di

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.SystemWallClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ClockModule {
    @Binds
    @Singleton
    fun bindClock(impl: SystemWallClock): Clock
}
