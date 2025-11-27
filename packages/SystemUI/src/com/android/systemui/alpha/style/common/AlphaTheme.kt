/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.android.compose.theme.PlatformTheme

/**
 * A wrapper around [PlatformTheme] that also initializes the [LocalAlphaColorScheme].
 *
 * Use this instead of PlatformTheme if you aren't using the QSTileStyleManager
 * to dynamically override colors, but still need access to the Alpha schema.
 */
@Composable
fun AlphaTheme(
    content: @Composable () -> Unit
) {
    // Ensure the base PlatformTheme (AOSP) is applied first to resolve dynamic colors
    PlatformTheme {
        val defaultScheme = defaultAlphaColorScheme()

        CompositionLocalProvider(
            LocalAlphaColorScheme provides defaultScheme,
            content = content
        )
    }
}
