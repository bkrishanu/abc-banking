package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.PersonaId

/** The notifications bell the tabs share: the unread count for its dot and where it goes. */
data class TabBell(val unreadNotifications: Int, val onNotifications: () -> Unit)

/**
 * Provided by the tab host so every tab's [TitleTopBar] shows the same bell. Null outside the
 * tabs (Profile opened from elsewhere), where the bar shows the title only.
 */
val LocalTabBell = compositionLocalOf<TabBell?> { null }

/**
 * `AbcTopBar`, greeting form (DESIGN_SYSTEM.md section 06), used by Home: avatar and
 * "Hello, Samanta" in H3 on the left, the notifications bell with an unread dot on the right.
 */
@Composable
fun GreetingTopBar(
    personaId: PersonaId,
    greeting: String,
    unreadNotifications: Int,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = AbcTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimens.touchTarget),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val profileDescription = stringResource(R.string.cd_profile)
        // Avatar and greeting open Profile, as the Profile tab does.
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = dimens.touchTarget)
                .clickable(role = Role.Button, onClick = onProfile)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$greeting, $profileDescription"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(personaId)
            Spacer(Modifier.width(AbcTheme.spacing.space3))
            Text(text = greeting, style = AbcTheme.typography.h3, color = AbcTheme.colors.text)
        }
        NotificationsBell(TabBell(unreadNotifications, onNotifications))
    }
}

/**
 * `AbcTopBar`, title form (DESIGN_SYSTEM.md section 06), used by the Cards, Insights and Profile
 * tabs: the screen title in H1 as the heading on the left and, inside the tabs, the shared bell
 * from [LocalTabBell] on the right.
 */
@Composable
fun TitleTopBar(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AbcTheme.dimens.touchTarget),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = AbcTheme.typography.h1,
            color = AbcTheme.colors.text,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        LocalTabBell.current?.let { NotificationsBell(it) }
    }
}

/** The bell in a 40dp surface circle, with an 8dp accent dot while anything is unread. */
@Composable
private fun NotificationsBell(bell: TabBell) {
    val colors = AbcTheme.colors
    val dimens = AbcTheme.dimens
    val description = if (bell.unreadNotifications > 0) {
        pluralStringResource(
            R.plurals.cd_notifications_unread,
            bell.unreadNotifications,
            bell.unreadNotifications
        )
    } else {
        stringResource(R.string.cd_notifications)
    }
    IconButton(
        onClick = bell.onNotifications,
        modifier = Modifier
            .size(dimens.touchTarget)
            .semantics { contentDescription = description }
    ) {
        Box(Modifier.size(dimens.iconCircle)) {
            Box(
                modifier = Modifier
                    .size(dimens.iconCircle)
                    .clip(AbcTheme.shapes.full)
                    .background(colors.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bell),
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(dimens.iconMedium)
                )
            }
            if (bell.unreadNotifications > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AbcTheme.spacing.space1)
                        .size(dimens.unreadDot)
                        .clip(AbcTheme.shapes.full)
                        .background(colors.accent)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF8F5)
@Composable
private fun AbcTopBarPreview() {
    AbcTheme {
        Column(Modifier.padding(AbcTheme.spacing.space4)) {
            GreetingTopBar(
                personaId = PersonaId.SAMANTA,
                greeting = "Hello, Samanta",
                unreadNotifications = 3,
                onNotifications = {},
                onProfile = {}
            )
            CompositionLocalProvider(LocalTabBell provides TabBell(3) {}) {
                TitleTopBar(title = "Cards")
            }
            TitleTopBar(title = "Profile")
        }
    }
}
