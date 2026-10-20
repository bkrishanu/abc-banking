package com.abcbank.mobile.feature.insights.di

import com.abcbank.mobile.feature.insights.data.FirestoreInsightsRepository
import com.abcbank.mobile.feature.insights.domain.InsightsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface InsightsModule {
    @Binds
    fun bindInsightsRepository(impl: FirestoreInsightsRepository): InsightsRepository
}
