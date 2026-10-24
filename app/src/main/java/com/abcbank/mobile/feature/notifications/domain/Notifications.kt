package com.abcbank.mobile.feature.notifications.domain

import com.abcbank.mobile.core.common.Clock
import com.abcbank.mobile.core.common.DataResult
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** MobileBuild.md 6: transactional alerts (debits, low balance) and promotional messages. */
enum class NotificationKind { TRANSACTIONAL, PROMOTIONAL }

data class Notification(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val read: Boolean,
    val createdAt: Instant
)

data class NotificationsData(val notifications: List<Notification>, val isFromCache: Boolean)

interface NotificationsRepository {
    fun observeNotifications(): Flow<DataResult<NotificationsData>>

    /** Marks these as read; applied to the cache at once like every write. */
    suspend fun markRead(ids: List<String>): DataResult<Unit>
}

enum class NotificationDay { TODAY, YESTERDAY, EARLIER }

/** A notification with the local day it belongs to, for its time label. */
data class DatedNotification(
    val notification: Notification,
    val day: NotificationDay,
    val date: LocalDate,
    val time: LocalTime
)

data class NotificationsOverview(
    /** Newest first, per kind. */
    val byKind: Map<NotificationKind, List<DatedNotification>>,
    val unread: Map<NotificationKind, Int>,
    val isFromCache: Boolean
)

object NotificationRules {
    fun day(createdAt: Instant, today: LocalDate, zone: ZoneId): NotificationDay {
        val date = createdAt.atZone(zone).toLocalDate()
        return when (date) {
            today -> NotificationDay.TODAY
            today.minusDays(1) -> NotificationDay.YESTERDAY
            else -> NotificationDay.EARLIER
        }
    }

    fun overview(data: NotificationsData, today: LocalDate, zone: ZoneId): NotificationsOverview {
        val sorted = data.notifications.sortedWith(
            compareByDescending<Notification> {
                it.createdAt
            }.thenBy { it.id }
        )
        return NotificationsOverview(
            byKind = NotificationKind.entries.associateWith { kind ->
                sorted.filter { it.kind == kind }.map {
                    val local = it.createdAt.atZone(zone)
                    DatedNotification(
                        it,
                        day(it.createdAt, today, zone),
                        local.toLocalDate(),
                        local.toLocalTime()
                    )
                }
            },
            unread = NotificationKind.entries.associateWith { kind ->
                sorted.count {
                    it.kind == kind &&
                        !it.read
                }
            },
            isFromCache = data.isFromCache
        )
    }
}

class ObserveNotificationsUseCase @Inject constructor(
    private val repository: NotificationsRepository,
    private val clock: Clock,
    private val zone: Provider<ZoneId>
) {
    operator fun invoke(): Flow<DataResult<NotificationsOverview>> =
        repository.observeNotifications().map { result ->
            when (result) {
                is DataResult.Failure -> result
                is DataResult.Success -> {
                    val zoneId = zone.get()
                    DataResult.Success(
                        NotificationRules.overview(
                            result.value,
                            clock.now().atZone(zoneId).toLocalDate(),
                            zoneId
                        )
                    )
                }
            }
        }
}

/** The unread count for the bell on every tab (DESIGN_SYSTEM.md section 06, `TitleTopBar`). */
class ObserveUnreadCountUseCase @Inject constructor(
    private val repository: NotificationsRepository
) {
    /**
     * A failure counts as nothing unread: the bell still opens the notification center, which
     * shows the error itself.
     */
    operator fun invoke(): Flow<Int> = repository.observeNotifications().map { result ->
        when (result) {
            is DataResult.Failure -> 0
            is DataResult.Success -> result.value.notifications.count { !it.read }
        }
    }
}

class MarkNotificationsReadUseCase @Inject constructor(
    private val repository: NotificationsRepository
) {
    /** Only unread ones are written, so opening a read alert costs nothing. */
    suspend operator fun invoke(notifications: List<Notification>): DataResult<Unit> {
        val unread = notifications.filterNot { it.read }.map { it.id }
        return if (unread.isEmpty()) DataResult.Success(Unit) else repository.markRead(unread)
    }
}
