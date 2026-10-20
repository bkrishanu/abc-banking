package com.abcbank.mobile.screenshots

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.payments.domain.AmountError
import com.abcbank.mobile.feature.payments.domain.PayeeError
import com.abcbank.mobile.feature.payments.domain.TransferSpeed
import com.abcbank.mobile.feature.payments.ui.AddPayeeScreen
import com.abcbank.mobile.feature.payments.ui.PayeeForm
import com.abcbank.mobile.feature.payments.ui.TransferAmountScreen
import com.abcbank.mobile.feature.payments.ui.TransferEvents
import com.abcbank.mobile.feature.payments.ui.TransferMode
import com.abcbank.mobile.feature.payments.ui.TransferPreviewData
import com.abcbank.mobile.feature.payments.ui.TransferReceiptScreen
import com.abcbank.mobile.feature.payments.ui.TransferRecipientScreen
import com.abcbank.mobile.feature.payments.ui.TransferReviewScreen
import com.abcbank.mobile.feature.payments.ui.TransferSpeedScreen
import com.abcbank.mobile.feature.payments.ui.TransferUiState
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test

/** The transfer steps at 360dp, on a phone-height screen so the pinned button shows. */
class TransferScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PHONE_360)

    @Test
    fun recipient() = snapshot {
        TransferRecipientScreen(TransferPreviewData.content(), TransferEvents())
    }

    @Test
    fun recipientError() = snapshot {
        TransferRecipientScreen(TransferUiState.Error(DataError.OFFLINE), TransferEvents())
    }

    @Test
    fun addPayeeNotVerified() = snapshot {
        AddPayeeScreen(
            TransferPreviewData.content().copy(
                payee = PayeeForm(
                    name = "Jordan Ellis",
                    routing = "500000021",
                    accountNumber = "40020000",
                    bankName = "Northfield Bank",
                    error = PayeeError.ACCOUNT_NOT_VERIFIED
                )
            ),
            TransferEvents()
        )
    }

    @Test
    fun speed() = snapshot {
        TransferSpeedScreen(
            TransferPreviewData.content(amountText = "250", speed = TransferSpeed.INSTANT),
            TransferEvents()
        )
    }

    @Test
    fun review() = snapshot {
        TransferReviewScreen(
            TransferPreviewData.content(
                amountText = "1250",
                speed = TransferSpeed.INSTANT,
                memo = "Rent share"
            ),
            TransferEvents()
        )
    }

    @Test
    fun receipt() = snapshot {
        TransferReceiptScreen(
            TransferPreviewData.content().copy(receipt = TransferPreviewData.receipt),
            TransferEvents()
        )
    }

    private fun snapshot(content: @Composable () -> Unit) = paparazzi.snapshot {
        AbcTheme { content() }
    }
}

/** The amount step scrolls, so it is recorded tall enough to show everything. */
class TransferAmountScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PHONE_360.copy(screenHeight = 3 * 1300))

    @Test
    fun send() = paparazzi.snapshot {
        AbcTheme {
            TransferAmountScreen(
                TransferPreviewData.content(amountText = "1234.5"),
                TransferEvents()
            )
        }
    }

    @Test
    fun moveOverAvailable() = paparazzi.snapshot {
        AbcTheme {
            TransferAmountScreen(
                TransferPreviewData.content(amountText = "20000").copy(
                    mode = TransferMode.MOVE,
                    source = TransferPreviewData.savings,
                    destination = TransferPreviewData.checking,
                    destinations = listOf(TransferPreviewData.checking),
                    beneficiary = null,
                    amountError = AmountError.OVER_AVAILABLE
                ),
                TransferEvents()
            )
        }
    }
}

class TransferLargeFontScreenshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(deviceConfig = PHONE_360.copy(fontScale = 2f, screenHeight = 3 * 1600))

    @Test
    fun review() = paparazzi.snapshot {
        AbcTheme {
            TransferReviewScreen(
                TransferPreviewData.content(
                    amountText = "1250",
                    speed = TransferSpeed.INSTANT,
                    memo = "Rent share"
                ),
                TransferEvents()
            )
        }
    }

    @Test
    fun receipt() = paparazzi.snapshot {
        AbcTheme {
            TransferReceiptScreen(
                TransferPreviewData.content().copy(receipt = TransferPreviewData.receipt),
                TransferEvents()
            )
        }
    }
}

// 1080 x 2400 px at 3x density = 360 x 800dp.
private val PHONE_360 = DeviceConfig.PIXEL_5.copy(
    screenWidth = 1080,
    screenHeight = 3 * 800,
    density = Density.XXHIGH,
    xdpi = 480,
    ydpi = 480
)
