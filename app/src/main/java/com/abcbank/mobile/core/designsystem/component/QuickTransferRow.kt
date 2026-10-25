package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.Beneficiary

/**
 * `QuickTransferRow` from DESIGN_SYSTEM.md section 06, shared by Home and Cards: a `ContentCard`
 * with a scrolling row of recent payees (initials and short name; a tap opens the transfer to
 * them) and an Add button for a new payee.
 */
@Composable
fun QuickTransferRow(
    payees: List<Beneficiary>,
    onPayee: (String) -> Unit,
    onAddPayee: () -> Unit,
    modifier: Modifier = Modifier
) {
    ContentCard(padding = AbcTheme.spacing.space3, modifier = modifier) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space3)
        ) {
            payees.forEach { payee ->
                QuickPayee(payee) { onPayee(payee.id) }
            }
            AddPayee(onAddPayee)
        }
    }
}

@Composable
private fun QuickPayee(payee: Beneficiary, onClick: () -> Unit) {
    val description = stringResource(R.string.cd_quick_transfer_to, payee.name)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = AbcTheme.dimens.avatarLarge, max = AbcTheme.dimens.avatarLarge)
            .clip(AbcTheme.shapes.md)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
            }
            .padding(vertical = AbcTheme.spacing.space1)
    ) {
        InitialsAvatar(payee.name, size = AbcTheme.dimens.iconCircle)
        Spacer(Modifier.height(AbcTheme.spacing.space1))
        Text(
            text = payee.shortName,
            style = AbcTheme.typography.small,
            color = AbcTheme.colors.text,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddPayee(onClick: () -> Unit) {
    val description = stringResource(R.string.cd_add_payee)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = AbcTheme.dimens.avatarLarge)
            .clip(AbcTheme.shapes.md)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
            }
            .padding(vertical = AbcTheme.spacing.space1)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AbcTheme.dimens.iconCircle)
                .clip(AbcTheme.shapes.full)
                .background(AbcTheme.colors.accent)
        ) {
            Icon(
                painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = AbcTheme.colors.text,
                modifier = Modifier.size(AbcTheme.dimens.iconLarge)
            )
        }
        Spacer(Modifier.height(AbcTheme.spacing.space1))
        Text(
            stringResource(R.string.quick_transfer_add),
            style = AbcTheme.typography.small,
            color = AbcTheme.colors.text
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF8F5)
@Composable
private fun QuickTransferRowPreview() {
    AbcTheme {
        QuickTransferRow(
            payees = listOf(
                Beneficiary(
                    "mark",
                    "Mark Robinson",
                    "Mark R.",
                    "Northfield Bank",
                    "500000021",
                    "1180",
                    1
                ),
                Beneficiary("helen", "Helen Torres", "Helen T.", "ABC Bank", "500000018", "5532", 2)
            ),
            onPayee = {},
            onAddPayee = {},
            modifier = Modifier.padding(AbcTheme.spacing.space4)
        )
    }
}
