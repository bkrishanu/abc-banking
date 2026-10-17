package com.abcbank.mobile.feature.auth.di

import com.abcbank.mobile.feature.auth.data.CatalogPersonaDirectory
import com.abcbank.mobile.feature.auth.domain.PersonaDirectory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AuthModule {
    @Binds
    fun bindPersonaDirectory(impl: CatalogPersonaDirectory): PersonaDirectory
}
