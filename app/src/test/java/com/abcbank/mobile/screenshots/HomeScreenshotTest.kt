package com.abcbank.mobile.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.model.TransactionCategory
import com.abcbank.mobile.feature.accounts.ui.TransactionDetailScreen
import com.abcbank.mobile.feature.accounts.ui.TransactionDetailUiState
import com.abcbank.mobile.feature.accounts.ui.TransactionsEvents
import com.abcbank.mobile.feature.accounts.ui.TransactionsPreviewData
import com.abcbank.mobile.feature.accounts.ui.TransactionsScreen
import com.abcbank.mobile.feature.home.ui.HomeActions
import com.abcbank.mobile.feature.home.ui.HomePreviewData
import com.abcbank.mobile.feature.home.ui.HomeScreen
import com.abcbank.mobile.feature.home.ui.HomeUiState
import com.abcbank.mobile.feature.profile.ui.ProfileScreen
import com.abcbank.mobile.feature.profile.ui.ProfileUiState
import com.android.resources.Density
import java.time.LocalDate
import java.time.LocalTime
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

/** Transaction ledger and detail (PRD feature 6). */
class TransactionsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = TALL_360)

    @Test
    fun ledger() = paparazzi.snapshot {
        AbcTheme { TransactionsScreen(TransactionsPreviewData.content(), TransactionsEvents()) }
    }

    @Test
    fun ledgerNoMatch() = paparazzi.snapshot {
        AbcTheme { TransactionsScreen(TransactionsPreviewData.noMatch(), TransactionsEvents()) }
    }

    @Test
    fun detailDisputed() = paparazzi.snapshot {
        AbcTheme {
            TransactionDetailScreen(
                state = TransactionDetailUiState.Content(
                    transaction = TransactionsPreviewData.transaction(
                        "tx-016",
                        "Melody Live Tickets",
                        TransactionCategory.ENTERTAINMENT,
                        -42_000,
                        disputed = true
                    ),
                    account = TransactionsPreviewData.checking,
                    date = LocalDate.of(2026, 10, 8),
                    time = LocalTime.of(22, 10),
                    balancesHidden = false,
                    isOffline = false,
                    canDispute = false
                ),
                onBack = {},
                onRetry = {}
            )
        }
    }
}

/** A charge that can be disputed: Download Receipt and Dispute Transaction. */
class TransactionActionsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = TALL_360)

    @Test
    fun detailCharge() = paparazzi.snapshot {
        AbcTheme {
            TransactionDetailScreen(
                state = TransactionDetailUiState.Content(
                    transaction = TransactionsPreviewData.transaction(
                        "tx-001",
                        "Aster & Vine Boutique",
                        TransactionCategory.SHOPPING,
                        -12_500
                    ),
                    account = TransactionsPreviewData.checking,
                    date = LocalDate.of(2026, 10, 19),
                    time = LocalTime.of(15, 14),
                    balancesHidden = false,
                    isOffline = false,
                    canDispute = true
                ),
                onBack = {},
                onRetry = {}
            )
        }
    }
}

/** 200% font scale (AGENTS.md section 07): balances shrink to fit, nothing clips. */
class HomeLargeFontScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = TALL_360.copy(fontScale = 2f, screenHeight = 3 * 2600))

    @Test
    fun ledger() = paparazzi.snapshot {
        AbcTheme { TransactionsScreen(TransactionsPreviewData.content(), TransactionsEvents()) }
    }

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
