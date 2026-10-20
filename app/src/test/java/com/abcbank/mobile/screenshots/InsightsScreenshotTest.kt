package com.abcbank.mobile.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.designsystem.component.LocalChartEntrance
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.insights.ui.InsightsPreviewData
import com.abcbank.mobile.feature.insights.ui.InsightsScreen
import com.abcbank.mobile.feature.insights.ui.InsightsUiState
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test

/** The Insights tab at 360dp, tall enough for both cards (Sample 1 right, Sample 2 right top). */
class InsightsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = INSIGHTS_360)

    @Test
    fun samanta() = snapshot(InsightsPreviewData.samanta())

    @Test
    fun hiddenOfflineFirstPoint() =
        snapshot(InsightsPreviewData.samanta(hidden = true, offline = true, selectedPoint = 0))

    @Test
    fun loading() = snapshot(InsightsUiState.Loading)

    @Test
    fun error() = snapshot(InsightsUiState.Error(DataError.UNKNOWN))

    private fun snapshot(state: InsightsUiState) = paparazzi.snapshot {
        StillCharts {
            InsightsScreen(state = state, onPeriodSelect = {}, onPointSelect = {}, onRetry = {})
        }
    }
}

class InsightsLargeFontScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = INSIGHTS_360.copy(
            fontScale = 2f,
            screenHeight =
            3 * 2000
        )
    )

    @Test
    fun samanta() = paparazzi.snapshot {
        StillCharts {
            InsightsScreen(state = InsightsPreviewData.samanta(), onPeriodSelect = {
            }, onPointSelect = {}, onRetry = {})
        }
    }
}

/** Paparazzi captures the first frame, so charts are drawn finished. */
@Composable
private fun StillCharts(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalChartEntrance provides false) { AbcTheme { content() } }
}

// 1080 px at 3x density = 360dp wide, 1300dp tall.
private val INSIGHTS_360 = DeviceConfig.PIXEL_5.copy(
    screenWidth = 1080,
    screenHeight = 3 * 1300,
    density = Density.XXHIGH,
    xdpi = 480,
    ydpi = 480
)
