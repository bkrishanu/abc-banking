package com.abcbank.mobile.feature.auth.di

import com.abcbank.mobile.feature.auth.data.AssetPersonaNames
import com.abcbank.mobile.feature.auth.data.CatalogPersonaDirectory
import com.abcbank.mobile.feature.auth.data.FirestoreDemoDataRepository
import com.abcbank.mobile.feature.auth.domain.DemoDataRepository
import com.abcbank.mobile.feature.auth.domain.PersonaDirectory
import com.abcbank.mobile.feature.auth.domain.PersonaNames
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AuthModule {
    @Binds
    fun bindPersonaDirectory(impl: CatalogPersonaDirectory): PersonaDirectory

    @Binds
    fun bindDemoDataRepository(impl: FirestoreDemoDataRepository): DemoDataRepository

    @Binds
    fun bindPersonaNames(impl: AssetPersonaNames): PersonaNames
}
