package com.abcbank.mobile.feature.accounts.di

import com.abcbank.mobile.feature.accounts.data.FirestoreTransactionsRepository
import com.abcbank.mobile.feature.accounts.domain.TransactionsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AccountsModule {
    @Binds
    fun bindTransactionsRepository(impl: FirestoreTransactionsRepository): TransactionsRepository
}
