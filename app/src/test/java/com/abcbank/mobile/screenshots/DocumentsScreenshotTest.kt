package com.abcbank.mobile.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.documents.ui.DocumentViewerScreen
import com.abcbank.mobile.feature.documents.ui.DocumentsPreviewData
import com.abcbank.mobile.feature.documents.ui.DocumentsScreen
import com.abcbank.mobile.feature.documents.ui.ViewerState
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test

/** Statements and documents at 360dp. The viewer is shown while building: PdfRenderer needs a device. */
class DocumentsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DOCUMENTS_360)

    @Test
    fun samanta() = paparazzi.snapshot {
        AbcTheme {
            DocumentsScreen(DocumentsPreviewData.samanta(), onBack = {}, onOpen = {}, onRetry = {})
        }
    }

    @Test
    fun marcusNo1099() = paparazzi.snapshot {
        AbcTheme {
            DocumentsScreen(DocumentsPreviewData.samanta(withTaxForm = false), onBack = {
            }, onOpen = {}, onRetry = {})
        }
    }

    @Test
    fun viewerBuilding() = paparazzi.snapshot {
        AbcTheme {
            DocumentViewerScreen(
                viewer = ViewerState.Building(DocumentsPreviewData.overview().statements.first()),
                onBack = {},
                onShare = {},
                onRetry = {}
            )
        }
    }
}

// 1080 px at 3x density = 360dp wide, 1000dp tall.
private val DOCUMENTS_360 = DeviceConfig.PIXEL_5.copy(
    screenWidth = 1080,
    screenHeight = 3 * 1000,
    density = Density.XXHIGH,
    xdpi = 480,
    ydpi = 480
)
