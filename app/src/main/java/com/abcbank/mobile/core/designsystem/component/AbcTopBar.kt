package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.PersonaId

/**
 * `AbcTopBar`, greeting form (DESIGN_SYSTEM.md section 06): avatar and "Hello, Samanta" on the
 * left, the notifications bell with an unread dot on the right.
 */
@Composable
fun GreetingTopBar(
    personaId: PersonaId,
    greeting: String,
    unreadNotifications: Int,
    onNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AbcTheme.colors
    val dimens = AbcTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimens.touchTarget),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(personaId)
        Spacer(Modifier.width(AbcTheme.spacing.space2))
        Text(
            text = greeting,
            style = AbcTheme.typography.body,
            color = colors.text,
            modifier = Modifier.weight(1f)
        )
        val bellDescription = if (unreadNotifications > 0) {
            pluralStringResource(
                R.plurals.cd_notifications_unread,
                unreadNotifications,
                unreadNotifications
            )
        } else {
            stringResource(R.string.cd_notifications)
        }
        IconButton(
            onClick = onNotifications,
            modifier = Modifier
                .size(dimens.touchTarget)
                .semantics { contentDescription = bellDescription }
        ) {
            Box {
                Icon(
                    painter = painterResource(R.drawable.ic_bell),
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(dimens.iconLarge)
                )
                if (unreadNotifications > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(dimens.unreadDot)
                            .clip(AbcTheme.shapes.full)
                            .background(colors.accent)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun GreetingTopBarPreview() {
    AbcTheme {
        GreetingTopBar(
            personaId = PersonaId.SAMANTA,
            greeting = "Hello, Samanta",
            unreadNotifications = 3,
            onNotifications = {},
            modifier = Modifier.padding(AbcTheme.spacing.space4)
        )
    }
}
