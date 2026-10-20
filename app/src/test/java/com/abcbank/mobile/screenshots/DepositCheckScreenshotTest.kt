package com.abcbank.mobile.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.Money
import com.abcbank.mobile.feature.payments.domain.DepositError
import com.abcbank.mobile.feature.payments.domain.DepositRules
import com.abcbank.mobile.feature.payments.ui.DepositCheckEvents
import com.abcbank.mobile.feature.payments.ui.DepositCheckScreen
import com.abcbank.mobile.feature.payments.ui.DepositCheckUiState
import com.abcbank.mobile.feature.payments.ui.TransferPreviewData
import com.android.resources.Density
import java.time.Instant
import org.junit.Rule
import org.junit.Test

/** Deposit check at 360dp, tall enough for the keypad and cheque tiles. */
class DepositCheckScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DEPOSIT_360)

    private val form = DepositCheckUiState.Content(
        accounts = listOf(TransferPreviewData.checking, TransferPreviewData.savings),
        account = TransferPreviewData.checking,
        amountText = "1250",
        amount = Money(125_000),
        limit = Money.ofDollars(10_000),
        amountError = null,
        frontImage = true,
        backImage = false,
        submitted = null,
        submitting = false,
        failed = false,
        balancesHidden = false,
        isOffline = false
    )

    @Test
    fun form() = snapshot(form)

    @Test
    fun overLimit() = snapshot(
        form.copy(
            amountText = "12000",
            amount = Money.ofDollars(12_000),
            amountError = DepositError.OVER_LIMIT
        )
    )

    @Test
    fun received() = snapshot(
        form.copy(
            submitted = DepositRules.pending(
                TransferPreviewData.checking,
                Money(125_000),
                "5b2e7c1a-93f0-4d6e-8a14-2c9b7e3f0d51",
                Instant.parse("2026-10-20T15:00:00Z"),
                front = true,
                back = false
            )
        )
    )

    private fun snapshot(state: DepositCheckUiState) = paparazzi.snapshot {
        AbcTheme { DepositCheckScreen(state, DepositCheckEvents()) }
    }
}

// 1080 px at 3x density = 360dp wide, 1250dp tall.
private val DEPOSIT_360 = DeviceConfig.PIXEL_5.copy(
    screenWidth = 1080,
    screenHeight = 3 * 1250,
    density = Density.XXHIGH,
    xdpi = 480,
    ydpi = 480
)
