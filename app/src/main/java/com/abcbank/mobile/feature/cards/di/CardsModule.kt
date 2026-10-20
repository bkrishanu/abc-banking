package com.abcbank.mobile.feature.cards.di

import com.abcbank.mobile.feature.cards.data.FirestoreCardsRepository
import com.abcbank.mobile.feature.cards.domain.CardsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface CardsModule {
    @Binds
    fun bindCardsRepository(impl: FirestoreCardsRepository): CardsRepository
}
