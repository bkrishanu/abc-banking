package com.abcbank.mobile.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.feature.support.ui.ChatLine
import com.abcbank.mobile.feature.support.ui.ChatState
import com.abcbank.mobile.feature.support.ui.SupportCaseScreen
import com.abcbank.mobile.feature.support.ui.SupportChatScreen
import com.abcbank.mobile.feature.support.ui.SupportEvents
import com.abcbank.mobile.feature.support.ui.SupportPreviewData
import com.abcbank.mobile.feature.support.ui.SupportScreen
import com.android.resources.Density
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test

/** The support hub, chat and a case thread at 360dp. */
class SupportScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = SUPPORT_360)

    @Test
    fun hub() = paparazzi.snapshot {
        AbcTheme { SupportScreen(SupportPreviewData.samanta(), SupportEvents()) }
    }

    @Test
    fun chatSuggestions() = paparazzi.snapshot {
        AbcTheme { SupportChatScreen(SupportPreviewData.samanta(), SupportEvents()) }
    }

    @Test
    fun chatConversation() = paparazzi.snapshot {
        val faq = SupportPreviewData.faqs.first()
        val chat = ChatState(
            lines = listOf(
                ChatLine.Customer("I lost my card"),
                ChatLine.Answer(faq),
                ChatLine.Customer("Can I open a joint account?"),
                ChatLine.HandedOff(
                    "I've passed this to our support team. They'll reply here within one business day."
                )
            )
        )
        AbcTheme { SupportChatScreen(SupportPreviewData.samanta(chat = chat), SupportEvents()) }
    }

    @Test
    fun caseThread() = paparazzi.snapshot {
        AbcTheme {
            SupportCaseScreen(
                SupportPreviewData.samanta(selected = true),
                SupportEvents(),
                zone = ZoneOffset.UTC
            )
        }
    }
}

// 1080 px at 3x density = 360dp wide, 1000dp tall.
private val SUPPORT_360 = DeviceConfig.PIXEL_5.copy(
    screenWidth = 1080,
    screenHeight = 3 * 1000,
    density = Density.XXHIGH,
    xdpi = 480,
    ydpi = 480
)
