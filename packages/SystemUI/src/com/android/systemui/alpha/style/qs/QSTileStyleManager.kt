/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.qs

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.android.systemui.alpha.style.UiStyleRepository
import com.android.systemui.alpha.style.UiStyleState
import com.android.systemui.alpha.style.qs.renderers.QSBevelStyleRenderer
import com.android.systemui.alpha.style.qs.renderers.QSAerogelStyleRenderer
import com.android.systemui.alpha.style.qs.renderers.QSGradientStyleRenderer
import com.android.systemui.alpha.style.qs.renderers.QSMetallicStyleRenderer
import com.android.systemui.alpha.style.qs.renderers.QSNeonStyleRenderer
import com.android.systemui.alpha.style.qs.renderers.QSOutlineStyleRenderer
import com.android.systemui.alpha.style.qs.renderers.QSReflectiveStyleRenderer
import com.android.systemui.alpha.style.qs.renderers.QSSlashStyleRenderer
import com.android.systemui.alpha.style.qs.renderers.QSTileStyleRenderer
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QSTileStyleManager @Inject constructor(
    private val context: Context,
    private val uiStyleRepository: UiStyleRepository
) {

    /**
     * Exposes the FULL style state for Compose observation.
     * Compose UI MUST observe this (not just currentStyleId) for live updates.
     */
    val styleState: StateFlow<UiStyleState>
        get() = uiStyleRepository.styleState

    /**
     * Legacy: Just the style ID. Use styleState instead for full reactivity.
     */
    val currentStyleId: StateFlow<String>
        get() = uiStyleRepository.selectedStyleIdFlow

    /**
     * Creates a renderer based on the CURRENT state.
     * Call this inside a remember() block that depends on styleState fields.
     */
    fun getRenderer(
        accentColor: Color,
        neutralColor: Color
    ): QSTileStyleRenderer? {
        val state = uiStyleRepository.styleState.value
        val isDark = state.isNightMode
        val settings = state.settings

        return when (state.styleId) {
            "system_default" -> null

            "outline" -> QSOutlineStyleRenderer(
                context = context,
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "neon" -> QSNeonStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "bevel" -> QSBevelStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "gradient" -> QSGradientStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "reflective" -> QSReflectiveStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "slash" -> QSSlashStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "aerogel" -> QSAerogelStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "metallic" -> QSMetallicStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            else -> null
        }
    }

    fun getAllStyleIds(): List<String> {
        return UiStyleRepository.SUPPORTED_STYLE_IDS
    }

    fun getStyleName(styleId: String): String {
        return when (styleId) {
            "system_default" -> "System Default"
            "outline" -> "Outline"
            "neon" -> "Neon"
            "bevel" -> "Bevel"
            "gradient" -> "Gradient"
            "reflective" -> "Reflective"
            "slash" -> "Slash"
            "aerogel" -> "Aerogel"
            "metallic" -> "Metallic"
            else -> "Unknown"
        }
    }
}
