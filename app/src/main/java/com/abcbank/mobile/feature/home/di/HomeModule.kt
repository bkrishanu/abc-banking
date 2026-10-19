package com.abcbank.mobile.feature.home.di

import com.abcbank.mobile.feature.home.data.FirestoreHomeRepository
import com.abcbank.mobile.feature.home.domain.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface HomeModule {
    @Binds
    fun bindHomeRepository(impl: FirestoreHomeRepository): HomeRepository
}
