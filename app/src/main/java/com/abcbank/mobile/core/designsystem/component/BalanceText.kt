package com.abcbank.mobile.core.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.MoneyFormatter
import com.abcbank.mobile.core.designsystem.theme.AbcMotion
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.Money

/**
 * A big balance from DESIGN_SYSTEM.md section 06: whole dollars at [style] size, cents at 60%.
 * When [hidden], it shows dots and TalkBack hears "Balance hidden", never the amount. At large
 * font scales it shrinks to fit the width instead of clipping (section 03).
 */
@Composable
fun BalanceText(
    money: Money,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = AbcTheme.typography.balance,
    color: Color = AbcTheme.colors.text,
    textAlign: TextAlign = TextAlign.Center
) {
    val hiddenDescription = stringResource(R.string.cd_balance_hidden)
    val amount = MoneyFormatter.format(money)
    Crossfade(
        targetState = hidden,
        animationSpec = tween(AbcMotion.PRESS_MS),
        label = "balanceMask",
        modifier = modifier.clearAndSetSemantics {
            contentDescription = if (hidden) hiddenDescription else amount
        }
    ) { isHidden ->
        val text = if (isHidden) {
            buildAnnotatedString { append(MASK) }
        } else {
            val parts = MoneyFormatter.parts(money)
            buildAnnotatedString {
                append(parts.whole)
                withStyle(SpanStyle(fontSize = CENTS_SCALE.em)) { append(parts.fraction) }
            }
        }
        BasicText(
            text = text,
            style = style.copy(color = color, textAlign = textAlign),
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            autoSize = TextAutoSize.StepBased(
                minFontSize = MIN_FONT_SIZE.sp,
                maxFontSize = style.fontSize
            )
        )
    }
}

private const val MASK = "••••••"
private const val CENTS_SCALE = 0.6f
private const val MIN_FONT_SIZE = 16

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun BalanceTextPreview() {
    AbcTheme {
        Column(Modifier.padding(AbcTheme.spacing.space4)) {
            BalanceText(money = Money(15_583_200), hidden = false)
            BalanceText(money = Money(15_583_200), hidden = true)
        }
    }
}
