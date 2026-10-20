package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * `AmountKeypad` from DESIGN_SYSTEM.md section 06: the MPIN pad's 3 x 4 grid of 64dp keys with
 * H1 digits, and a decimal point and delete on the bottom row. Keys report characters; the
 * caller decides what a valid amount is.
 */
@Composable
fun AmountKeypad(onKey: (Char) -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = AbcTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                row.forEach { key -> Key(key, onKey) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
            Key('.', onKey, description = stringResource(R.string.cd_decimal_point))
            Key('0', onKey)
            val deleteDescription = stringResource(R.string.cd_delete)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(AbcTheme.dimens.keypadKeyHeight)
                    .clip(AbcTheme.shapes.md)
                    .clickable(role = Role.Button, onClick = onDelete)
                    .semantics { contentDescription = deleteDescription }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = AbcTheme.colors.text,
                    modifier = Modifier.size(AbcTheme.dimens.iconLarge)
                )
            }
        }
    }
}

@Composable
private fun RowScope.Key(key: Char, onKey: (Char) -> Unit, description: String? = null) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .height(AbcTheme.dimens.keypadKeyHeight)
            .clip(AbcTheme.shapes.md)
            .background(AbcTheme.colors.surface)
            .clickable(role = Role.Button) { onKey(key) }
            .then(
                if (description !=
                    null
                ) {
                    Modifier.semantics { contentDescription = description }
                } else {
                    Modifier
                }
            )
    ) {
        Text(text = key.toString(), style = AbcTheme.typography.h1, color = AbcTheme.colors.text)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun AmountKeypadPreview() {
    AbcTheme {
        AmountKeypad(onKey = {
        }, onDelete = {}, modifier = Modifier.padding(AbcTheme.spacing.space4))
    }
}
