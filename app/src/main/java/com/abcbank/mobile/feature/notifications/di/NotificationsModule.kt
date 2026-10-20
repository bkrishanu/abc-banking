package com.abcbank.mobile.feature.notifications.di

import com.abcbank.mobile.feature.notifications.data.FirestoreNotificationsRepository
import com.abcbank.mobile.feature.notifications.domain.NotificationsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface NotificationsModule {
    @Binds
    fun bindNotificationsRepository(impl: FirestoreNotificationsRepository): NotificationsRepository
}
