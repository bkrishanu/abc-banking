package com.abcbank.mobile.screenshots

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.payments.domain.BillRules
import com.abcbank.mobile.feature.payments.ui.AddBillerForm
import com.abcbank.mobile.feature.payments.ui.AddBillerScreen
import com.abcbank.mobile.feature.payments.ui.BillDetailScreen
import com.abcbank.mobile.feature.payments.ui.BillPayEvents
import com.abcbank.mobile.feature.payments.ui.BillPayPreviewData
import com.abcbank.mobile.feature.payments.ui.BillsScreen
import com.android.resources.Density
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test

/** Bill pay at 360dp x 800dp. */
class BillPayScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = BILLS_360)

    @Test
    fun bills() = snapshot { BillsScreen(BillPayPreviewData.samanta(), BillPayEvents()) }

    @Test
    fun directory() = snapshot { AddBillerScreen(BillPayPreviewData.samanta(), BillPayEvents()) }

    @Test
    fun fetchedBill() = snapshot {
        val biller = BillPayPreviewData.directory.last()
        val fetched = BillRules.fetch(biller, "40021187", BillPayPreviewData.today, ZoneOffset.UTC)
        AddBillerScreen(
            BillPayPreviewData.samanta(
                form = AddBillerForm(biller = biller, accountNumber = "40021187", fetched = fetched)
            ),
            BillPayEvents()
        )
    }

    @Test
    fun billDetail() = snapshot {
        BillDetailScreen(
            BillPayPreviewData.samanta(selected = "brightline-electric"),
            BillPayEvents()
        )
    }

    @Test
    fun scheduledBill() = snapshot {
        BillDetailScreen(BillPayPreviewData.samanta(selected = "city-water"), BillPayEvents())
    }

    private fun snapshot(content: @Composable () -> Unit) = paparazzi.snapshot {
        AbcTheme { content() }
    }
}

// 1080 x 2400 px at 3x density = 360 x 800dp.
private val BILLS_360 = DeviceConfig.PIXEL_5.copy(
    screenWidth = 1080,
    screenHeight = 3 * 800,
    density = Density.XXHIGH,
    xdpi = 480,
    ydpi = 480
)
