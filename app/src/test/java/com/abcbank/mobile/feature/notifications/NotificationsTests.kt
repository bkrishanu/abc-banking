package com.abcbank.mobile.feature.notifications

import app.cash.turbine.test
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.data.firebase.NotificationDto
import com.abcbank.mobile.feature.notifications.data.toNotification
import com.abcbank.mobile.feature.notifications.domain.MarkNotificationsReadUseCase
import com.abcbank.mobile.feature.notifications.domain.Notification
import com.abcbank.mobile.feature.notifications.domain.NotificationDay
import com.abcbank.mobile.feature.notifications.domain.NotificationKind
import com.abcbank.mobile.feature.notifications.domain.NotificationRules
import com.abcbank.mobile.feature.notifications.domain.NotificationsData
import com.abcbank.mobile.feature.notifications.domain.NotificationsRepository
import com.abcbank.mobile.feature.notifications.domain.ObserveNotificationsUseCase
import com.abcbank.mobile.feature.notifications.ui.NotificationsUiState
import com.abcbank.mobile.feature.notifications.ui.NotificationsViewModel
import com.abcbank.mobile.testing.MainDispatcherRule
import com.abcbank.mobile.testing.ManualClock
import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val UTC = ZoneOffset.UTC
private val TODAY = LocalDate.of(2026, 10, 20)
private val NOW: Instant = TODAY.atTime(15, 0).toInstant(UTC)

private fun notification(
    id: String,
    kind: NotificationKind,
    daysAgo: Long,
    read: Boolean = false,
    hour: Int = 9
) = Notification(
    id,
    kind,
    "Title $id",
    "Body $id",
    read,
    TODAY.minusDays(daysAgo).atTime(hour, 0).toInstant(UTC)
)

private val ALERTS = listOf(
    notification("old", NotificationKind.TRANSACTIONAL, 3, read = true),
    notification("today-early", NotificationKind.TRANSACTIONAL, 0, hour = 8),
    notification("today-late", NotificationKind.TRANSACTIONAL, 0, hour = 14),
    notification("yesterday", NotificationKind.TRANSACTIONAL, 1),
    notification("offer", NotificationKind.PROMOTIONAL, 2)
)

class NotificationRulesTest {
    @Test
    fun `alerts are split by kind, newest first, with unread counts`() {
        val overview = NotificationRules.overview(NotificationsData(ALERTS, false), TODAY, UTC)
        val alerts = overview.byKind.getValue(NotificationKind.TRANSACTIONAL)
        assertEquals(
            listOf("today-late", "today-early", "yesterday", "old"),
            alerts.map {
                it.notification.id
            }
        )
        assertEquals(
            listOf(
                NotificationDay.TODAY,
                NotificationDay.TODAY,
                NotificationDay.YESTERDAY,
                NotificationDay.EARLIER
            ),
            alerts.map { it.day }
        )
        assertEquals(3, overview.unread[NotificationKind.TRANSACTIONAL])
        assertEquals(1, overview.unread[NotificationKind.PROMOTIONAL])
    }

    @Test
    fun `documents with an unknown kind or no title do not map`() {
        val time = Timestamp(NOW.epochSecond, 0)
        assertEquals(
            NotificationKind.PROMOTIONAL,
            NotificationDto(
                "PROMOTIONAL",
                "Grow your savings",
                "Earn 4.50% APY.",
                null,
                time
            ).toNotification("p")?.kind
        )
        assertNull(NotificationDto("MARKETING", "Hi", "Body", false, time).toNotification("x"))
        assertNull(NotificationDto("TRANSACTIONAL", " ", "Body", false, time).toNotification("y"))
        assertNull(NotificationDto("TRANSACTIONAL", "Hi", "Body", false, null).toNotification("z"))
    }
}

private class FakeNotificationsRepository : NotificationsRepository {
    val data = MutableStateFlow<DataResult<NotificationsData>?>(null)
    val marked = mutableListOf<List<String>>()

    override fun observeNotifications(): Flow<DataResult<NotificationsData>> = data.filterNotNull()

    override suspend fun markRead(ids: List<String>): DataResult<Unit> {
        marked += ids
        val current = (data.value as? DataResult.Success)?.value ?: return DataResult.Success(Unit)
        data.value = DataResult.Success(
            current.copy(
                notifications = current.notifications.map {
                    if (it.id in
                        ids
                    ) {
                        it.copy(read = true)
                    } else {
                        it
                    }
                }
            )
        )
        return DataResult.Success(Unit)
    }
}

class NotificationsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeNotificationsRepository()

    private fun viewModel() = NotificationsViewModel(
        observeNotifications = ObserveNotificationsUseCase(repository, ManualClock(NOW)) { UTC },
        markRead = MarkNotificationsReadUseCase(repository)
    )

    @Test
    fun `opening an alert marks only that one, and read alerts are not written again`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(NotificationsUiState.Loading, awaitItem())
            repository.data.value = DataResult.Success(NotificationsData(ALERTS, false))
            val content = awaitItem() as NotificationsUiState.Content
            assertEquals(NotificationKind.TRANSACTIONAL, content.kind)
            viewModel.onOpen(content.items.first())
            viewModel.onOpen(content.items.last())
            testScheduler.runCurrent()
            assertEquals(listOf(listOf("today-late")), repository.marked)
            val after = expectMostRecentItem() as NotificationsUiState.Content
            assertEquals(2, after.unread[NotificationKind.TRANSACTIONAL])
        }
    }

    @Test
    fun `Mark All as Read clears the selected kind only`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            repository.data.value = DataResult.Success(NotificationsData(ALERTS, false))
            awaitItem()
            viewModel.onMarkAllRead()
            testScheduler.runCurrent()
            val after = expectMostRecentItem() as NotificationsUiState.Content
            assertEquals(0, after.unread[NotificationKind.TRANSACTIONAL])
            assertEquals(1, after.unread[NotificationKind.PROMOTIONAL])
            viewModel.onKindSelect(NotificationKind.PROMOTIONAL)
            testScheduler.runCurrent()
            val offers = expectMostRecentItem() as NotificationsUiState.Content
            assertEquals(listOf("offer"), offers.items.map { it.notification.id })
            assertTrue(offers.hasUnread)
        }
    }

    @Test
    fun `an empty kind is still content, so the tabs stay`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            repository.data.value = DataResult.Success(NotificationsData(emptyList(), false))
            val content = awaitItem() as NotificationsUiState.Content
            assertTrue(content.items.isEmpty())
        }
    }
}
