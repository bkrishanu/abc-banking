package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/**
 * Single-line input from DESIGN_SYSTEM.md section 06. The label is always visible above the field;
 * the placeholder is only a hint. [errorText] turns the border danger and shows below the field.
 */
@Composable
fun AbcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val colors = AbcTheme.colors
    val spacing = AbcTheme.spacing
    val typography = AbcTheme.typography
    val shape = AbcTheme.shapes.sm

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
        Text(text = label, style = typography.small, color = colors.textMuted)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = typography.body.copy(color = colors.text),
            cursorBrush = SolidColor(colors.text),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (errorText != null) error(errorText)
                },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AbcTheme.dimens.textFieldHeight)
                        .clip(shape)
                        .background(if (enabled) colors.surface else colors.surfaceMuted)
                        .border(
                            width = AbcTheme.dimens.borderWidth,
                            color = if (errorText != null) colors.danger else colors.border,
                            shape = shape
                        )
                        .padding(horizontal = spacing.space3, vertical = spacing.space3),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(text = placeholder, style = typography.body, color = colors.textMuted)
                    }
                    innerTextField()
                }
            }
        )
        if (errorText != null) {
            Text(text = errorText, style = typography.small, color = colors.danger)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F2)
@Composable
private fun AbcTextFieldPreview() {
    AbcTheme {
        Column(
            modifier = Modifier.padding(AbcTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space4)
        ) {
            AbcTextField(value = "", onValueChange = {
            }, label = "Customer ID", placeholder = "8 letters or numbers")
            AbcTextField(value = "SAMJOH01", onValueChange = {}, label = "Customer ID")
            AbcTextField(
                value = "SAM.JOH",
                onValueChange = {},
                label = "Customer ID",
                errorText = "Customer ID is 8 letters or numbers."
            )
        }
    }
}
