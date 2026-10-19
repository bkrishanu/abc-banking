package com.abcbank.mobile.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.PersonaId

/**
 * `ProfileAvatar` from DESIGN_SYSTEM.md section 06: the persona's bundled photo in a circle.
 * Decorative here, because it always sits next to the name, so TalkBack skips it.
 */
@Composable
fun ProfileAvatar(
    personaId: PersonaId,
    modifier: Modifier = Modifier,
    size: Dp = AbcTheme.dimens.avatarSmall
) {
    Image(
        painter = painterResource(personaId.avatarRes()),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(AbcTheme.shapes.full)
            .background(AbcTheme.colors.surfaceMuted)
    )
}

/** Initials on surfaceMuted, for contacts and payees, which have no photos (DEMO_DATA.md 01). */
@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = AbcTheme.dimens.iconCircle
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(AbcTheme.shapes.full)
            .background(AbcTheme.colors.surfaceMuted)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialsOf(name),
            style = AbcTheme.typography.label,
            color = AbcTheme.colors.text
        )
    }
}

@DrawableRes
fun PersonaId.avatarRes(): Int = when (this) {
    PersonaId.SAMANTA -> R.drawable.avatar_samanta
    PersonaId.MARCUS -> R.drawable.avatar_marcus
    PersonaId.GRACE -> R.drawable.avatar_grace
    PersonaId.DANIEL -> R.drawable.avatar_daniel
}

/** "Helen Torres" becomes "HT"; a single word gives one letter. */
internal fun initialsOf(name: String): String = name.split(' ')
    .mapNotNull { word -> word.firstOrNull { it.isLetter() }?.uppercaseChar() }
    .take(2)
    .joinToString("")

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun ProfileAvatarPreview() {
    AbcTheme {
        Row(
            modifier = Modifier.padding(AbcTheme.spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(PersonaId.SAMANTA)
            ProfileAvatar(PersonaId.MARCUS, size = AbcTheme.dimens.iconCircle)
            InitialsAvatar("Helen Torres")
        }
    }
}
