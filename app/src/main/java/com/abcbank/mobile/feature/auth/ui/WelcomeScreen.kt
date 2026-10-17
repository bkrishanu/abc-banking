package com.abcbank.mobile.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcWordmark
import com.abcbank.mobile.core.designsystem.component.BankCardArt
import com.abcbank.mobile.core.designsystem.component.WordmarkTone
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.CardGradient
import com.abcbank.mobile.core.designsystem.theme.WelcomeCardStack

/** First screen (Sample Screens 1, left). Static, so it has no ViewModel. */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = AbcTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AbcTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = spacing.space4)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(spacing.space4))
            AbcWordmark()
            Spacer(Modifier.height(spacing.space6))
            CardStack()
            Spacer(Modifier.height(spacing.space8))
            Text(
                text = stringResource(R.string.welcome_headline),
                style = AbcTheme.typography.display,
                color = AbcTheme.colors.text,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(spacing.space3))
            Text(
                text = stringResource(R.string.welcome_subtext),
                style = AbcTheme.typography.body,
                color = AbcTheme.colors.textMuted
            )
            Spacer(Modifier.height(spacing.space6))
        }
        AbcButton(
            text = stringResource(R.string.welcome_get_started),
            onClick = onGetStarted,
            modifier = Modifier.padding(vertical = spacing.space4)
        )
    }
}

/** Two tilted card faces, decorative only, so hidden from TalkBack. */
@Composable
private fun CardStack() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AbcTheme.spacing.space4)
            .clearAndSetSemantics {}
    ) {
        WelcomeCard(
            gradient = AbcTheme.colors.cardSage,
            rotation = WelcomeCardStack.BACK_ROTATION_DEGREES,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = AbcTheme.spacing.space12)
        )
        WelcomeCard(
            gradient = AbcTheme.colors.cardPeach,
            rotation = WelcomeCardStack.FRONT_ROTATION_DEGREES,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

@Composable
private fun WelcomeCard(gradient: CardGradient, rotation: Float, modifier: Modifier = Modifier) {
    BankCardArt(
        gradient = gradient,
        modifier = modifier
            .fillMaxWidth(WelcomeCardStack.CARD_WIDTH_FRACTION)
            .graphicsLayer { rotationZ = rotation }
    ) {
        AbcWordmark(
            tone = WordmarkTone.OnCard,
            style = AbcTheme.typography.h3,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AbcTheme.spacing.space4)
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun WelcomeScreenPreview() {
    AbcTheme { WelcomeScreen(onGetStarted = {}) }
}

@Preview(widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun WelcomeScreenLargeFontPreview() {
    AbcTheme { WelcomeScreen(onGetStarted = {}) }
}
