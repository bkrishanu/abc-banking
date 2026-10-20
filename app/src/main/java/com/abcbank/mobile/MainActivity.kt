package com.abcbank.mobile

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.security.SessionLock
import com.abcbank.mobile.navigation.AbcNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sessionLock: SessionLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Whole app, not only sensitive screens: blank app-switcher preview and no screenshots
        // (MobileBuild.md 1.1, ARCHITECTURE.md section 06).
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()
        setContent {
            AbcTheme {
                AbcNavHost()
            }
        }
    }

    /** Every touch or key resets the 30-minute idle lock (MobileBuild.md 1.1). */
    override fun onUserInteraction() {
        super.onUserInteraction()
        sessionLock.onUserInteraction()
    }
}
