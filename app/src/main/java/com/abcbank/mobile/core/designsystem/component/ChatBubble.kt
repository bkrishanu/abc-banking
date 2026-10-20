package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * `ChatBubble` from DESIGN_SYSTEM.md section 06: the customer's messages on the right in primary
 * with onPrimary text, the bank's on the left in surface, radius-lg, Body text, at most 80% of the
 * width, with an optional Small muted line below (time or the matched question).
 */
@Composable
fun ChatBubble(
    text: String,
    fromCustomer: Boolean,
    modifier: Modifier = Modifier,
    caption: String? = null
) {
    val colors = AbcTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (fromCustomer) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (fromCustomer) Alignment.End else Alignment.Start,
            modifier = Modifier
                .fillMaxWidth(BUBBLE_WIDTH)
                .semantics(mergeDescendants = true) {}
        ) {
            Text(
                text = text,
                style = AbcTheme.typography.body,
                color = if (fromCustomer) colors.onPrimary else colors.text,
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .clip(AbcTheme.shapes.lg)
                    .background(if (fromCustomer) colors.primary else colors.surface)
                    .padding(
                        horizontal = AbcTheme.spacing.space4,
                        vertical = AbcTheme.spacing.space3
                    )
            )
            caption?.let {
                Text(
                    text = it,
                    style = AbcTheme.typography.small,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = AbcTheme.spacing.space1)
                )
            }
        }
    }
}

private const val BUBBLE_WIDTH = 0.8f

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2, widthDp = 360)
@Composable
private fun ChatBubblePreview() {
    AbcTheme {
        Column(
            Modifier.padding(AbcTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space3)
        ) {
            ChatBubble("How do I freeze my card?", fromCustomer = true)
            ChatBubble(
                "Go to Cards, choose the card and turn on Freeze Card.",
                fromCustomer = false,
                caption = "How do I freeze my card?"
            )
        }
    }
}
