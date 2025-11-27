/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.brightness

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.android.systemui.alpha.style.UiStyleRepository
import com.android.systemui.alpha.style.UiStyleState
import com.android.systemui.alpha.style.brightness.renderers.*
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrightnessSliderStyleManager @Inject constructor(
    private val context: Context,
    private val uiStyleRepository: UiStyleRepository
) {

    /**
     * Exposes the FULL style state for Compose observation.
     * Compose UI MUST observe this for live updates.
     */
    val styleState: StateFlow<UiStyleState>
        get() = uiStyleRepository.styleState

    /**
     * Legacy: Just the style ID.
     */
    val currentStyleId: StateFlow<String>
        get() = uiStyleRepository.selectedStyleIdFlow

    /**
     * Creates a renderer based on the CURRENT state.
     */
    fun getRenderer(
        accentColor: Color,
        neutralColor: Color
    ): BrightnessSliderStyleRenderer? {
        val state = uiStyleRepository.styleState.value
        val isDark = state.isNightMode
        val settings = state.settings

        return when (state.styleId) {
            "system_default" -> null

            "outline" -> BSOutlineStyleRenderer(
                context = context,
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "neon" -> BSNeonStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "bevel" -> BSBevelStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "gradient" -> BSGradientStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "reflective" -> BSReflectiveStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "slash" -> BSSlashStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "aerogel" -> BSAerogelStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings
            )

            "metallic" -> BSMetallicStyleRenderer(
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
