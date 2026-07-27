/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.qs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.theme.LocalAndroidColorScheme
import com.android.systemui.alpha.style.qs.renderers.QSTileStyleRenderer
import com.android.systemui.qs.composefragment.viewmodel.QSFragmentComposeViewModel

/**
 * CompositionLocal for accessing QSFragmentComposeViewModel from any composable.
 * This avoids having to pass qsTileStyleManager through every composable's parameters.
 *
 * Default is null: media (and other) surfaces can host outside [QSFragmentCompose]
 * (e.g. lockscreen). Callers that need the VM must handle null; [rememberQsTileStyleRenderer]
 * degrades to unstyled when absent.
 */
val LocalQSFragmentComposeViewModel =
    compositionLocalOf<QSFragmentComposeViewModel?> { null }

/**
 * Low-level function that creates a QS tile style renderer from an explicit style manager.
 * Observes the FULL style state (including settings and themeVersion) to ensure
 * recomposition when ANY style property changes.
 *
 * @param styleManager The QSTileStyleManager to get style from
 * @return QSTileStyleRenderer or null if style is "none" or "system_default"
 */
@Composable
fun rememberQSTileStyleRenderer(
    styleManager: QSTileStyleManager
): QSTileStyleRenderer? {
    // Observe the FULL state
    // This includes styleId, settings, themeVersion, and isNightMode
    val styleState by styleManager.styleState.collectAsStateWithLifecycle()

    // Get current Material Theme colors
    val accentColor = MaterialTheme.colorScheme.primary
    val neutralColor = LocalAndroidColorScheme.current.surfaceEffect2

    // Recompose when ANY of these change (including settings and themeVersion)
    return remember(
        styleState.styleId,
        styleState.settings,
        styleState.themeVersion,
        styleState.isNightMode,
        accentColor,
        neutralColor
    ) {
        styleManager.getRenderer(accentColor, neutralColor)
    }
}

/**
 * High-level convenience function that creates a QS tile style renderer.
 * Gets the style manager from CompositionLocal automatically.
 *
 * @return QSTileStyleRenderer or null if style is "none"/"system_default", or if
 *   [LocalQSFragmentComposeViewModel] is not provided (non-QS hosts such as lockscreen media)
 */
@Composable
fun rememberQsTileStyleRenderer(): QSTileStyleRenderer? {
    val viewModel = LocalQSFragmentComposeViewModel.current ?: return null
    return rememberQSTileStyleRenderer(viewModel.qsTileStyleManager)
}
