package com.abcbank.mobile.feature.payments.di

import com.abcbank.mobile.feature.payments.data.AssetBankDirectory
import com.abcbank.mobile.feature.payments.data.AssetBillerDirectory
import com.abcbank.mobile.feature.payments.data.FirestorePaymentsRepository
import com.abcbank.mobile.feature.payments.domain.BankDirectory
import com.abcbank.mobile.feature.payments.domain.BillerDirectory
import com.abcbank.mobile.feature.payments.domain.BillsRepository
import com.abcbank.mobile.feature.payments.domain.PaymentsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface PaymentsModule {
    @Binds
    fun bindPaymentsRepository(impl: FirestorePaymentsRepository): PaymentsRepository

    @Binds
    fun bindBankDirectory(impl: AssetBankDirectory): BankDirectory

    @Binds
    fun bindBillsRepository(impl: FirestorePaymentsRepository): BillsRepository

    @Binds
    fun bindBillerDirectory(impl: AssetBillerDirectory): BillerDirectory
}
