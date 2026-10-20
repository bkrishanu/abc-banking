package com.abcbank.mobile.core.di

import com.abcbank.mobile.core.data.firebase.FirestoreDeviceRegistry
import com.abcbank.mobile.core.security.AndroidDeviceKeyStore
import com.abcbank.mobile.core.security.DeviceKeyStore
import com.abcbank.mobile.core.security.DeviceRegistry
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DeviceBindingModule {
    @Binds
    fun bindDeviceKeyStore(impl: AndroidDeviceKeyStore): DeviceKeyStore

    @Binds
    fun bindDeviceRegistry(impl: FirestoreDeviceRegistry): DeviceRegistry
}
