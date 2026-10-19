package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/** `ContentCard` from DESIGN_SYSTEM.md section 06: flat white, radius-xl, no border or shadow. */
@Composable
fun ContentCard(
    modifier: Modifier = Modifier,
    padding: Dp = AbcTheme.spacing.space4,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AbcTheme.shapes.xl)
            .background(AbcTheme.colors.surface)
            .padding(padding),
        content = content
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun ContentCardPreview() {
    AbcTheme {
        ContentCard(Modifier.padding(AbcTheme.spacing.space4)) {
            Text("Everyday Checking", style = AbcTheme.typography.body)
        }
    }
}
