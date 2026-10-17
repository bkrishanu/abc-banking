package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.designsystem.theme.BANK_CARD_ASPECT_RATIO
import com.abcbank.mobile.core.designsystem.theme.CardGradient

/**
 * A bank card face: gradient art plus a scrim that fades up from the bottom edge so white text
 * stays readable (DESIGN_SYSTEM.md section 02). BankCardView builds on this; the Welcome screen
 * uses it with only the wordmark. Decorative: put text and descriptions in [content].
 */
@Composable
fun BankCardArt(
    gradient: CardGradient,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .aspectRatio(BANK_CARD_ASPECT_RATIO)
            .clip(AbcTheme.shapes.lg)
            .background(Brush.linearGradient(listOf(gradient.start, gradient.end)))
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, AbcTheme.colors.cardScrim))
            ),
        content = content
    )
}

@Preview
@Composable
private fun BankCardArtPreview() {
    AbcTheme {
        BankCardArt(
            gradient = AbcTheme.colors.cardPeach,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AbcTheme.spacing.space4)
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
}
