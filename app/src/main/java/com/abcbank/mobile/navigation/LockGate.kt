package com.abcbank.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abcbank.mobile.feature.auth.ui.LockScreen
import com.abcbank.mobile.feature.auth.ui.LockViewModel

/**
 * The lock screen over everything (ARCHITECTURE.md section 04, Auth and session). A full-screen
 * dialog, so it also covers any open sheet; it inherits FLAG_SECURE from the Activity. Back does
 * nothing: only the MPIN or "Not you?" leaves it.
 */
@Composable
fun LockOverlay(viewModel: LockViewModel, onLoggedOut: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onShown() }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        LockScreen(
            state = state,
            onDigit = viewModel::onDigit,
            onDelete = viewModel::onDelete,
            onNotYou = { viewModel.onNotYou(onLoggedOut) }
        )
    }
}

/**
 * The Activity's lifecycle, held at CREATED while [paused]. Given to the NavHost, it stops every
 * screen's `collectAsStateWithLifecycle` under the lock screen, so their Firestore listeners
 * pause (after the usual 5 s) and resume on unlock, with the back stack untouched.
 */
@Composable
fun rememberPausableLifecycleOwner(paused: Boolean): LifecycleOwner {
    val parent = LocalLifecycleOwner.current
    val owner = remember(parent) { PausableLifecycleOwner() }
    DisposableEffect(parent) {
        val observer = LifecycleEventObserver { _, _ ->
            owner.parentState =
                parent.lifecycle.currentState
        }
        parent.lifecycle.addObserver(observer)
        onDispose { parent.lifecycle.removeObserver(observer) }
    }
    owner.paused = paused
    return owner
}

private class PausableLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    var parentState: Lifecycle.State = Lifecycle.State.INITIALIZED
        set(value) {
            field = value
            sync()
        }

    var paused: Boolean = false
        set(value) {
            field = value
            sync()
        }

    private fun sync() {
        val target = if (paused &&
            parentState.isAtLeast(Lifecycle.State.CREATED)
        ) {
            Lifecycle.State.CREATED
        } else {
            parentState
        }
        // A destroyed lifecycle cannot come back; the Activity is gone by then anyway.
        if (registry.currentState != Lifecycle.State.DESTROYED &&
            target != Lifecycle.State.INITIALIZED
        ) {
            registry.currentState = target
        }
    }
}
