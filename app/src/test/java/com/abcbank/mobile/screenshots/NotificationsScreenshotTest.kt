package com.abcbank.mobile.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.notifications.domain.NotificationKind
import com.abcbank.mobile.feature.notifications.ui.NotificationsPreviewData
import com.abcbank.mobile.feature.notifications.ui.NotificationsScreen
import com.abcbank.mobile.feature.notifications.ui.NotificationsUiState
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test

/** The notification center at 360dp x 800dp. */
class NotificationsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = NOTIFICATIONS_360)

    @Test
    fun transactional() = snapshot(NotificationsPreviewData.samanta())

    @Test
    fun promotional() = snapshot(NotificationsPreviewData.samanta(NotificationKind.PROMOTIONAL))

    @Test
    fun emptyPromotional() = snapshot(
        NotificationsPreviewData.samanta(NotificationKind.PROMOTIONAL).copy(items = emptyList())
    )

    @Test
    fun loading() = snapshot(NotificationsUiState.Loading)

    private fun snapshot(state: NotificationsUiState) = paparazzi.snapshot {
        AbcTheme {
            NotificationsScreen(state, onBack = {
            }, onKindSelect = {}, onOpen = {}, onMarkAllRead = {}, onRetry = {})
        }
    }
}

// 1080 x 2400 px at 3x density = 360 x 800dp.
private val NOTIFICATIONS_360 = DeviceConfig.PIXEL_5.copy(
    screenWidth = 1080,
    screenHeight = 3 * 800,
    density = Density.XXHIGH,
    xdpi = 480,
    ydpi = 480
)
