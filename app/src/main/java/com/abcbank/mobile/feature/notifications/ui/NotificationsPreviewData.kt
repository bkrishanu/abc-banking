package com.abcbank.mobile.feature.notifications.ui

import com.abcbank.mobile.feature.notifications.domain.Notification
import com.abcbank.mobile.feature.notifications.domain.NotificationKind
import com.abcbank.mobile.feature.notifications.domain.NotificationRules
import com.abcbank.mobile.feature.notifications.domain.NotificationsData
import java.time.LocalDate
import java.time.ZoneOffset

/** Samanta's seeded alerts (DEMO_DATA.md section 03), for previews and screenshot tests. */
internal object NotificationsPreviewData {
    private val TODAY = LocalDate.of(2026, 10, 20)

    private fun at(daysAgo: Long, hour: Int, minute: Int) =
        TODAY.minusDays(daysAgo).atTime(hour, minute).toInstant(ZoneOffset.UTC)

    val notifications = listOf(
        Notification(
            "n1",
            NotificationKind.TRANSACTIONAL,
            "Money received",
            "Helen Torres sent you \$38.60.",
            false,
            at(0, 8, 9)
        ),
        Notification(
            "n2",
            NotificationKind.TRANSACTIONAL,
            "Card used",
            "\$125.00 at Aster & Vine Boutique with your Main card.",
            false,
            at(0, 15, 14)
        ),
        Notification(
            "n3",
            NotificationKind.TRANSACTIONAL,
            "Bill due soon",
            "Volt Mobile, \$95.00, due in 5 days.",
            false,
            at(1, 9, 0)
        ),
        Notification(
            "n4",
            NotificationKind.TRANSACTIONAL,
            "Salary received",
            "\$7,115.38 paid into Everyday Checking.",
            true,
            at(3, 7, 0)
        ),
        Notification(
            "n5",
            NotificationKind.PROMOTIONAL,
            "Grow your savings",
            "Earn 4.50% APY with ABC High-Yield Savings.",
            false,
            at(2, 9, 0)
        )
    )

    fun samanta(
        kind: NotificationKind = NotificationKind.TRANSACTIONAL
    ): NotificationsUiState.Content {
        val overview = NotificationRules.overview(
            NotificationsData(notifications, false),
            TODAY,
            ZoneOffset.UTC
        )
        return NotificationsUiState.Content(
            kind,
            overview.byKind.getValue(kind),
            overview.unread,
            isOffline = false
        )
    }
}
