package com.abcbank.mobile.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abcbank.mobile.R
import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.designsystem.component.AbcButton
import com.abcbank.mobile.core.designsystem.component.AbcButtonVariant
import com.abcbank.mobile.core.designsystem.component.AbcConfirmDialog
import com.abcbank.mobile.core.designsystem.component.ContentCard
import com.abcbank.mobile.core.designsystem.component.ErrorState
import com.abcbank.mobile.core.designsystem.component.LocalBottomBarClearance
import com.abcbank.mobile.core.designsystem.component.OfflineBanner
import com.abcbank.mobile.core.designsystem.component.ProfileAvatar
import com.abcbank.mobile.core.designsystem.component.SkeletonBlock
import com.abcbank.mobile.core.designsystem.theme.AbcTheme
import com.abcbank.mobile.core.model.PersonaId

@Composable
fun ProfileRoute(
    onBack: (() -> Unit)?,
    onLoggedOut: () -> Unit,
    onDocuments: () -> Unit = {},
    onSupport: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        if (state == ProfileUiState.LoggedOut) onLoggedOut()
    }
    ProfileScreen(
        state = state,
        onBack = onBack,
        onLogOut = viewModel::onLogOut,
        onRetry = viewModel::onRetry,
        onResetDemoData = viewModel::onResetDemoData,
        menu = listOf(
            ProfileMenuItem(
                R.drawable.ic_file_text,
                stringResource(R.string.profile_documents),
                onDocuments
            ),
            ProfileMenuItem(
                R.drawable.ic_life_buoy,
                stringResource(R.string.profile_support),
                onSupport
            )
        )
    )
}

/** A Profile row that opens another screen. */
data class ProfileMenuItem(val icon: Int, val label: String, val onClick: () -> Unit)

/**
 * The Profile screen, for now the persona and Log Out (DESIGN_SYSTEM.md section 10). Settings,
 * documents, support and Reset Demo Data join it with their features.
 */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    /** Null when Profile is a tab: there is nothing to go back to. */
    onBack: (() -> Unit)?,
    onLogOut: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    menu: List<ProfileMenuItem> = emptyList(),
    onResetDemoData: () -> Unit = {}
) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val spacing = AbcTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AbcTheme.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(AbcTheme.dimens.touchTarget)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = stringResource(R.string.cd_back),
                    tint = AbcTheme.colors.text,
                    modifier = Modifier.size(AbcTheme.dimens.iconLarge)
                )
            }
        } else {
            Spacer(Modifier.height(spacing.space4))
        }
        Column(Modifier.padding(horizontal = spacing.space4)) {
            Text(
                text = stringResource(R.string.profile_title),
                style = AbcTheme.typography.h1,
                color = AbcTheme.colors.text,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(spacing.space6))
            when (state) {
                ProfileUiState.Loading, ProfileUiState.LoggedOut -> ProfileSkeleton()
                ProfileUiState.Empty -> Text(
                    text = stringResource(R.string.profile_empty),
                    style = AbcTheme.typography.body,
                    color = AbcTheme.colors.textMuted
                )
                is ProfileUiState.Error -> ErrorState(
                    message = stringResource(
                        if (state.error == DataError.OFFLINE) {
                            R.string.state_offline_no_data
                        } else {
                            R.string.profile_error
                        }
                    ),
                    onRetry = onRetry
                )
                is ProfileUiState.Content -> {
                    PersonaCard(state)
                    Spacer(Modifier.height(spacing.space4))
                    ProfileMenu(
                        menu + ProfileMenuItem(
                            R.drawable.ic_rotate_ccw,
                            stringResource(R.string.profile_reset_demo),
                            { confirmReset = true }
                        )
                    )
                    resetMessage(state.reset)?.let { (text, isError) ->
                        Spacer(Modifier.height(spacing.space2))
                        Text(
                            text = text,
                            style = AbcTheme.typography.small,
                            color = if (isError) {
                                AbcTheme.colors.danger
                            } else {
                                AbcTheme.colors.textMuted
                            },
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                }
            }
            if (state != ProfileUiState.LoggedOut) {
                Spacer(Modifier.height(spacing.space6))
                AbcButton(
                    text = stringResource(R.string.action_log_out),
                    onClick = onLogOut,
                    variant = AbcButtonVariant.Secondary
                )
            }
            Spacer(Modifier.height(spacing.space6 + LocalBottomBarClearance.current))
        }
    }
    if (confirmReset) {
        AbcConfirmDialog(
            title = stringResource(R.string.profile_reset_title),
            text = stringResource(R.string.profile_reset_text),
            confirmLabel = stringResource(R.string.action_reset),
            cancelLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                confirmReset = false
                onResetDemoData()
            },
            onDismiss = { confirmReset = false }
        )
    }
}

/** The line under the menu while and after Reset Demo Data runs; null when not asked. */
@Composable
private fun resetMessage(status: ResetStatus): Pair<String, Boolean>? = when (status) {
    ResetStatus.IDLE -> null
    ResetStatus.RESETTING -> stringResource(R.string.profile_resetting) to false
    ResetStatus.DONE -> stringResource(R.string.profile_reset_done) to false
    ResetStatus.FAILED -> stringResource(R.string.profile_reset_failed) to true
}

@Composable
private fun ProfileMenu(items: List<ProfileMenuItem>) {
    val spacing = AbcTheme.spacing
    ContentCard(padding = spacing.space2) {
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = AbcTheme.colors.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AbcTheme.dimens.touchTarget)
                    .clickable(role = Role.Button, onClick = item.onClick)
                    .padding(spacing.space2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(AbcTheme.dimens.iconCircle)
                        .clip(AbcTheme.shapes.full)
                        .background(AbcTheme.colors.surfaceMuted)
                ) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                        tint = AbcTheme.colors.text,
                        modifier = Modifier.size(AbcTheme.dimens.iconMedium)
                    )
                }
                Spacer(Modifier.width(spacing.space3))
                Text(item.label, style = AbcTheme.typography.body, color = AbcTheme.colors.text)
            }
        }
    }
}

@Composable
private fun PersonaCard(state: ProfileUiState.Content) {
    if (state.isOffline) {
        OfflineBanner()
        Spacer(Modifier.height(AbcTheme.spacing.space4))
    }
    ContentCard(padding = AbcTheme.spacing.space5) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AbcTheme.spacing.space2)
        ) {
            ProfileAvatar(state.personaId, size = AbcTheme.dimens.avatarLarge)
            Text(
                text = state.name,
                style = AbcTheme.typography.h2,
                color = AbcTheme.colors.text,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.profile_customer_id, state.customerId),
                style = AbcTheme.typography.small,
                color = AbcTheme.colors.textMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileSkeleton() {
    SkeletonBlock(
        Modifier
            .fillMaxWidth()
            .height(AbcTheme.dimens.avatarLarge + AbcTheme.spacing.space12 * 2),
        shape = AbcTheme.shapes.xl
    )
}

@Composable
private fun ProfilePreview(state: ProfileUiState) {
    AbcTheme { ProfileScreen(state = state, onBack = null, onLogOut = {}, onRetry = {}) }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ProfileContentPreview() = ProfilePreview(
    ProfileUiState.Content(PersonaId.SAMANTA, "Samanta Johns", "SAMJOH01", isOffline = false)
)

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ProfileErrorPreview() = ProfilePreview(ProfileUiState.Error(DataError.UNKNOWN))

@Preview(widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun ProfileLargeFontPreview() = ProfilePreview(
    ProfileUiState.Content(PersonaId.MARCUS, "Marcus Lee", "MARLEE01", isOffline = false)
)
