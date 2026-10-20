package com.abcbank.mobile.feature.support.di

import com.abcbank.mobile.feature.support.data.FirestoreFaqSource
import com.abcbank.mobile.feature.support.data.FirestoreSupportRepository
import com.abcbank.mobile.feature.support.domain.FaqSource
import com.abcbank.mobile.feature.support.domain.SupportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface SupportModule {
    @Binds
    fun bindFaqSource(impl: FirestoreFaqSource): FaqSource

    @Binds
    fun bindSupportRepository(impl: FirestoreSupportRepository): SupportRepository
}
