package com.abcbank.mobile.feature.profile.di

import com.abcbank.mobile.feature.profile.data.FirestoreProfileRepository
import com.abcbank.mobile.feature.profile.data.SeederDemoDataResetter
import com.abcbank.mobile.feature.profile.domain.DemoDataResetter
import com.abcbank.mobile.feature.profile.domain.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ProfileModule {
    @Binds
    fun bindProfileRepository(impl: FirestoreProfileRepository): ProfileRepository

    @Binds
    fun bindDemoDataResetter(impl: SeederDemoDataResetter): DemoDataResetter
}
