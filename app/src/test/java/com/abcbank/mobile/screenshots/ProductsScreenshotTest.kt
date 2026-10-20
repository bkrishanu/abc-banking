package com.abcbank.mobile.screenshots

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.products.ui.ApplicationCompleteScreen
import com.abcbank.mobile.feature.products.ui.ApplicationDecisionScreen
import com.abcbank.mobile.feature.products.ui.ApplicationIdentityScreen
import com.abcbank.mobile.feature.products.ui.ApplicationTermsScreen
import com.abcbank.mobile.feature.products.ui.CheckState
import com.abcbank.mobile.feature.products.ui.LoanCalculatorScreen
import com.abcbank.mobile.feature.products.ui.ProductsEvents
import com.abcbank.mobile.feature.products.ui.ProductsPreviewData
import com.abcbank.mobile.feature.products.ui.ProductsScreen
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test

/** Explore Products, the loan calculator and the four application steps at 360dp. */
class ProductsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PRODUCTS_360)

    @Test
    fun catalog() = snapshot { ProductsScreen(ProductsPreviewData.content(), ProductsEvents()) }

    @Test
    fun catalogWithOpenedCard() = snapshot {
        ProductsScreen(ProductsPreviewData.content(openedCard = true), ProductsEvents())
    }

    @Test
    fun loanCalculator() = snapshot {
        LoanCalculatorScreen(
            ProductsPreviewData.content(selected = "personal-loan"),
            ProductsEvents()
        )
    }

    @Test
    fun identity() = snapshot {
        ApplicationIdentityScreen(
            ProductsPreviewData.content(selected = "personal-loan"),
            ProductsEvents()
        )
    }

    @Test
    fun terms() = snapshot {
        ApplicationTermsScreen(
            ProductsPreviewData.content(selected = "signature-rewards").copy(agreed = true),
            ProductsEvents()
        )
    }

    @Test
    fun checking() = snapshot {
        ApplicationDecisionScreen(
            ProductsPreviewData.content(
                selected = "personal-loan"
            ).copy(check = CheckState.CHECKING),
            ProductsEvents()
        )
    }

    @Test
    fun approvedLoan() = snapshot {
        ApplicationDecisionScreen(
            ProductsPreviewData.content(
                selected = "personal-loan"
            ).copy(check = CheckState.DECIDED),
            ProductsEvents()
        )
    }

    @Test
    fun complete() = snapshot {
        ApplicationCompleteScreen(ProductsPreviewData.completedLoan(), ProductsEvents())
    }

    private fun snapshot(content: @Composable () -> Unit) = paparazzi.snapshot {
        AbcTheme { content() }
    }
}

class ProductsLargeFontScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = PRODUCTS_360.copy(
            fontScale = 2f,
            screenHeight =
            3 * 2400
        )
    )

    @Test
    fun catalog() = paparazzi.snapshot {
        AbcTheme { ProductsScreen(ProductsPreviewData.content(), ProductsEvents()) }
    }

    @Test
    fun loanCalculator() = paparazzi.snapshot {
        AbcTheme {
            LoanCalculatorScreen(
                ProductsPreviewData.content(selected = "personal-loan"),
                ProductsEvents()
            )
        }
    }
}

// 1080 px at 3x density = 360dp wide, 1300dp tall.
private val PRODUCTS_360 = DeviceConfig.PIXEL_5.copy(
    screenWidth = 1080,
    screenHeight = 3 * 1300,
    density = Density.XXHIGH,
    xdpi = 480,
    ydpi = 480
)
