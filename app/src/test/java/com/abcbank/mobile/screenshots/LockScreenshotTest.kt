package com.abcbank.mobile.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.security.MpinEntryState
import com.abcbank.mobile.core.security.StepUpMessage
import com.abcbank.mobile.feature.auth.domain.LockIdentity
import com.abcbank.mobile.feature.auth.ui.LockScreen
import com.abcbank.mobile.feature.auth.ui.LockUiState
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test

/** The lock screen at 360 x 800dp, and at 200% font. */
class LockScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = LOCK_360)

    @Test
    fun samanta() = snapshot(
        LockUiState.Content(
            LockIdentity(PersonaId.SAMANTA, "Samanta"),
            MpinEntryState(active = true, entered = 2)
        )
    )

    @Test
    fun wrongMpin() = snapshot(
        LockUiState.Content(
            LockIdentity(PersonaId.MARCUS, "Marcus"),
            MpinEntryState(active = true, message = StepUpMessage.WrongMpin(3))
        )
    )

    private fun snapshot(state: LockUiState) = paparazzi.snapshot {
        AbcTheme { LockScreen(state, onDigit = {}, onDelete = {}, onNotYou = {}) }
    }
}

class LockLargeFontScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = LOCK_360.copy(fontScale = 2f, screenHeight = 3 * 1300))

    @Test
    fun samanta() = paparazzi.snapshot {
        AbcTheme {
            LockScreen(
                LockUiState.Content(
                    LockIdentity(PersonaId.SAMANTA, "Samanta"),
                    MpinEntryState(active = true)
                ),
                onDigit = {},
                onDelete = {},
                onNotYou = {}
            )
        }
    }
}

// 1080 x 2400 px at 3x density = 360 x 800dp.
private val LOCK_360 = DeviceConfig.PIXEL_5.copy(
    screenWidth = 1080,
    screenHeight = 3 * 800,
    density = Density.XXHIGH,
    xdpi = 480,
    ydpi = 480
)
