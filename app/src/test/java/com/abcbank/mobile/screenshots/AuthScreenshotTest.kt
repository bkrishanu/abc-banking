package com.abcbank.mobile.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.auth.ui.CustomerIdError
import com.abcbank.mobile.feature.auth.ui.LoginScreen
import com.abcbank.mobile.feature.auth.ui.LoginUiState
import com.abcbank.mobile.feature.auth.ui.MpinMessage
import com.abcbank.mobile.feature.auth.ui.WelcomeScreen
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test

/**
 * Renders the auth screens on the JVM at the 360dp design width (DESIGN_SYSTEM.md section 08).
 * Record with `recordPaparazziDebug`, compare with `verifyPaparazziDebug`.
 */
class AuthScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = COMPACT_360)

    @Test
    fun welcome() = paparazzi.snapshot { AbcTheme { WelcomeScreen(onGetStarted = {}) } }

    @Test
    fun loginEmpty() = snapshotLogin(content(customerId = "", isMpinEnabled = false))

    @Test
    fun loginEnteringMpin() = snapshotLogin(content(mpinEntered = 3))

    @Test
    fun loginUnknownId() = snapshotLogin(
        content(
            customerId = "ABCDEF12",
            customerIdError = CustomerIdError.Unknown,
            isMpinEnabled = false
        )
    )

    @Test
    fun loginWrongMpin() =
        snapshotLogin(content(mpinMessage = MpinMessage.WrongMpin(attemptsLeft = 3)))

    @Test
    fun loginLockedOut() = snapshotLogin(
        content(isMpinEnabled = false, mpinMessage = MpinMessage.LockedOut(secondsLeft = 24))
    )

    private fun snapshotLogin(state: LoginUiState) = paparazzi.snapshot {
        AbcTheme {
            LoginScreen(
                state = state,
                onBack = {},
                onCustomerIdChange = {},
                onCustomerIdDone = {},
                onMpinDigit = {},
                onMpinDelete = {}
            )
        }
    }
}

/** Same screens at 200% font scale (AGENTS.md section 07). */
class AuthLargeFontScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = COMPACT_360.copy(fontScale = 2f))

    @Test
    fun welcome() = paparazzi.snapshot { AbcTheme { WelcomeScreen(onGetStarted = {}) } }

    @Test
    fun loginEnteringMpin() = paparazzi.snapshot {
        AbcTheme {
            LoginScreen(
                state = content(mpinEntered = 2),
                onBack = {},
                onCustomerIdChange = {},
                onCustomerIdDone = {},
                onMpinDigit = {},
                onMpinDelete = {}
            )
        }
    }
}

// 1080 px at 3x density = 360 x 780 dp.
private val COMPACT_360 = DeviceConfig.PIXEL_5.copy(
    screenWidth = 1080,
    screenHeight = 2340,
    density = Density.XXHIGH,
    xdpi = 480,
    ydpi = 480
)

private fun content(
    customerId: String = "SAMJOH01",
    customerIdError: CustomerIdError? = null,
    isMpinEnabled: Boolean = true,
    mpinEntered: Int = 0,
    mpinMessage: MpinMessage? = null
) = LoginUiState.Content(
    customerId = customerId,
    customerIdError = customerIdError,
    isMpinEnabled = isMpinEnabled,
    mpinEntered = mpinEntered,
    mpinLength = 6,
    mpinMessage = mpinMessage,
    isLoggedIn = false
)
