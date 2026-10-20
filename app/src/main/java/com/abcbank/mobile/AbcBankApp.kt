package com.abcbank.mobile

import android.app.Application
import com.abcbank.mobile.core.common.ApplicationScope
import com.abcbank.mobile.core.data.firebase.AuthDataSource
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

    override fun onCreate() {
        super.onCreate()
        // Sign in while the presenter is still on Welcome and Login, so the persona's data can be
        // read and written by the time Home opens. Login itself never waits for this.
        applicationScope.launch { auth.signedInUid() }
    }
}
