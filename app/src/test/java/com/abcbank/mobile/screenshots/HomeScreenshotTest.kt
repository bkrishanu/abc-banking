package com.abcbank.mobile.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.feature.home.ui.HomeActions
import com.abcbank.mobile.feature.home.ui.HomePreviewData
import com.abcbank.mobile.feature.home.ui.HomeScreen
import com.abcbank.mobile.feature.home.ui.HomeUiState
import com.abcbank.mobile.feature.profile.ui.ProfileScreen
import com.abcbank.mobile.feature.profile.ui.ProfileUiState
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test

/**
 * Home at the 360dp design width, tall enough to show the whole scrolling page
 * (DESIGN_SYSTEM.md section 08). Record with `recordPaparazziDebug`.
 */
class HomeScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = TALL_360)

    @Test
    fun samanta() = snapshot(HomePreviewData.samanta())

    @Test
    fun marcusLowBalanceNoInvestments() = snapshot(HomePreviewData.marcus())

    @Test
    fun balancesHiddenOffline() = snapshot(HomePreviewData.samanta(hidden = true, offline = true))

    @Test
    fun loading() = snapshot(HomeUiState.Loading)

    @Test
    fun error() = snapshot(HomeUiState.Error(DataError.UNKNOWN))

    private fun snapshot(state: HomeUiState) = paparazzi.snapshot {
        AbcTheme {
            HomeScreen(state = state, actions = HomeActions(), onToggleBalances = {}, onRetry = {})
        }
    }
}

/** Profile, for now the persona and Log Out. */
class ProfileScreenshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_5.copy(
                screenWidth = 1080,
                screenHeight = 2340,
                density = Density.XXHIGH,
                xdpi = 480,
                ydpi = 480
            )
        )

    @Test
    fun samanta() = snapshot(
        ProfileUiState.Content(PersonaId.SAMANTA, "Samanta Johns", "SAMJOH01", isOffline = false)
    )

    @Test
    fun error() = snapshot(ProfileUiState.Error(DataError.UNKNOWN))

    private fun snapshot(state: ProfileUiState) = paparazzi.snapshot {
        AbcTheme { ProfileScreen(state = state, onBack = {}, onLogOut = {}, onRetry = {}) }
    }
}

/** 200% font scale (AGENTS.md section 07): balances shrink to fit, nothing clips. */
class HomeLargeFontScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = TALL_360.copy(fontScale = 2f, screenHeight = 3 * 2600))

    @Test
    fun samanta() = paparazzi.snapshot {
        AbcTheme {
            HomeScreen(
                state = HomePreviewData.samanta(),
                actions = HomeActions(),
                onToggleBalances = {},
                onRetry = {}
            )
        }
    }
}

// 1080 px at 3x density = 360dp wide, 1500dp tall.
private val TALL_360 = DeviceConfig.PIXEL_5.copy(
    screenWidth = 1080,
    screenHeight = 3 * 1500,
    density = Density.XXHIGH,
    xdpi = 480,
    ydpi = 480
)
