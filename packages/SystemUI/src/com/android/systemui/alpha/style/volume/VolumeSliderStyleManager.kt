/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.volume

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.android.systemui.alpha.style.UiStyleRepository
import com.android.systemui.alpha.style.UiStyleState
import com.android.systemui.alpha.style.brightness.renderers.BSAerogelStyleRenderer
import com.android.systemui.alpha.style.brightness.renderers.BSBevelStyleRenderer
import com.android.systemui.alpha.style.brightness.renderers.BSGradientStyleRenderer
import com.android.systemui.alpha.style.brightness.renderers.BSMetallicStyleRenderer
import com.android.systemui.alpha.style.brightness.renderers.BSNeonStyleRenderer
import com.android.systemui.alpha.style.brightness.renderers.BSOutlineStyleRenderer
import com.android.systemui.alpha.style.brightness.renderers.BSReflectiveStyleRenderer
import com.android.systemui.alpha.style.brightness.renderers.BSSlashStyleRenderer
import com.android.systemui.alpha.style.brightness.renderers.BrightnessSliderStyleRenderer
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeSliderStyleManager @Inject constructor(
    private val context: Context,
    private val uiStyleRepository: UiStyleRepository,
) {
    val styleState: StateFlow<UiStyleState>
        get() = uiStyleRepository.styleState

    val currentStyleId: StateFlow<String>
        get() = uiStyleRepository.selectedStyleIdFlow

    fun getRenderer(
        accentColor: Color,
        neutralColor: Color,
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
                userSettings = settings,
            )

            "neon" -> BSNeonStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings,
            )

            "bevel" -> BSBevelStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings,
            )

            "gradient" -> BSGradientStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings,
            )

            "reflective" -> BSReflectiveStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings,
            )

            "slash" -> BSSlashStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings,
            )

            "aerogel" -> BSAerogelStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings,
            )

            "metallic" -> BSMetallicStyleRenderer(
                accentColor = accentColor,
                neutralColor = neutralColor,
                isDarkTheme = isDark,
                userSettings = settings,
            )

            else -> null
        }
    }
}