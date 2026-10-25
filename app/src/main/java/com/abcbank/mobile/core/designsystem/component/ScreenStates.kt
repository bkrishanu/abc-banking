package com.abcbank.mobile.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcMotion
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

// Loading, offline and error states shared by every data-driven screen (DESIGN_SYSTEM.md
// section 06, States).

/** A shimmering placeholder block in surfaceMuted, shaped like the content it stands in for. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = AbcTheme.shapes.sm) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = SKELETON_ALPHA_LOW,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(AbcMotion.SCREEN_MS * 2), RepeatMode.Reverse),
        label = "skeletonAlpha"
    )
    Box(
        modifier = modifier
            .alpha(alpha)
            .clip(shape)
            .background(AbcTheme.colors.surfaceMuted)
            .clearAndSetSemantics {}
    )
}

/**
 * A wifi-off icon and "You're offline. Showing saved data." in Small, above content read from the
 * cache. With [syncedAt] (for example "3:14 pm") it says when that data was saved, as in Stitch.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier, syncedAt: String? = null) {
    val colors = AbcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AbcTheme.shapes.full)
            .background(colors.surface)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = AbcTheme.spacing.space4, vertical = AbcTheme.spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_wifi_off),
            contentDescription = null,
            tint = colors.accentDeep,
            modifier = Modifier.size(AbcTheme.dimens.iconSmall)
        )
        Text(
            text = if (syncedAt != null) {
                stringResource(R.string.state_offline_synced, syncedAt)
            } else {
                stringResource(R.string.state_offline)
            },
            style = AbcTheme.typography.small,
            color = colors.textMuted
        )
    }
}

/** One plain line and a Retry button. Never vendor or Firebase error text. */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space4)
    ) {
        Text(
            text = message,
            style = AbcTheme.typography.body,
            color = AbcTheme.colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        AbcButton(text = stringResource(R.string.action_retry), onClick = onRetry)
    }
}

private const val SKELETON_ALPHA_LOW = 0.4f

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun ScreenStatesPreview() {
    AbcTheme {
        Column(
            modifier = Modifier.padding(AbcTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space4)
        ) {
            SkeletonBlock(
                Modifier
                    .fillMaxWidth()
                    .height(AbcTheme.dimens.skeletonLine)
            )
            OfflineBanner()
            OfflineBanner(syncedAt = "3:14 pm")
            ErrorState(message = "We couldn't load your accounts.", onRetry = {})
        }
    }
}
