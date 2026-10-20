package com.abcbank.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.abcbank.mobile.R
import com.abcbank.mobile.core.designsystem.theme.AbcTheme

/** One row in the More Actions sheet. */
data class MoreAction(val icon: Int, val label: String, val onClick: () -> Unit)

/**
 * The More Actions sheet behind Home's grid pill and the Cards tab's More (DESIGN_SYSTEM.md
 * section 06). Only destinations that exist are listed; each feature adds its row when built.
 */
@Composable
fun MoreActionsSheet(actions: List<MoreAction>, onDismiss: () -> Unit) {
    val spacing = AbcTheme.spacing
    val colors = AbcTheme.colors
    AbcBottomSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.space4)
                .padding(bottom = spacing.space6)
        ) {
            Text(
                text = stringResource(R.string.more_actions_title),
                style = AbcTheme.typography.h2,
                color = colors.text,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(spacing.space3))
            actions.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AbcTheme.dimens.touchTarget)
                        .clip(AbcTheme.shapes.md)
                        .clickable(role = Role.Button) {
                            onDismiss()
                            action.onClick()
                        }
                        .padding(vertical = spacing.space2),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(AbcTheme.dimens.iconCircle)
                            .clip(AbcTheme.shapes.full)
                            .background(colors.surfaceMuted)
                    ) {
                        Icon(
                            painter = painterResource(action.icon),
                            contentDescription = null,
                            tint = colors.text,
                            modifier = Modifier.size(AbcTheme.dimens.iconMedium)
                        )
                    }
                    Spacer(Modifier.width(spacing.space3))
                    Text(action.label, style = AbcTheme.typography.body, color = colors.text)
                }
            }
        }
    }
}
