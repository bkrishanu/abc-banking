package com.abcbank.mobile

import android.app.Application
import android.os.Build
import com.abcbank.mobile.core.common.ApplicationScope
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.security.BindDeviceUseCase
import com.abcbank.mobile.core.security.SessionLockObserver
import com.abcbank.mobile.feature.payments.data.DepositClearingScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class AbcBankApp : Application() {
    @Inject
    lateinit var auth: AuthDataSource

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var depositClearing: DepositClearingScheduler

    @Inject
    lateinit var sessionLockObserver: SessionLockObserver

    @Inject
    lateinit var bindDevice: BindDeviceUseCase

    override fun onCreate() {
        super.onCreate()
        // Sign in while the presenter is still on Welcome and Login, so the persona's data can be
        // read and written by the time Home opens. Login itself never waits for this.
        applicationScope.launch {
            auth.signedInUid()
            // Device binding (MobileBuild.md 1.1): the Keystore key and its public key in Firestore.
            // A failure is retried on the next launch; nothing waits for it.
            bindDevice(Build.MODEL)
        }
        depositClearing.start()
        sessionLockObserver.start()
    }
}
