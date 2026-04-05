/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.shade.ui

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import com.android.internal.graphics.ColorUtils
import com.android.systemui.res.R

object ShadeColors {
    /**
     * Calculate notification shade panel color.
     *
     * Blur on: berry uses `shade_berry_qs_*`; classic (colored) uses **darker** tint for QS
     * (top) than notifications (bottom) by assigning the notification-base rail to QS and the
     * lighter accent/neutral blend to the notification stack.
     *
     * Blur off: classic uses the same darker-top / lighter-bottom pairing as blur-on
     * ({@link #notificationScrimFallback} for QS, {@link #shadePanelFallback} for notifications);
     * berry uses {@link #shadePanelFallbackBerry}.
     *
     * The returned color is always **fully opaque** (`0xFF` alpha). Scrim view alpha is
     * controlled separately by {@link ScrimController} and the user's
     * {@link Settings.Secure#QS_PANEL_SCRIM_ALPHA} setting, keeping tint-color and opacity
     * concerns independent.
     *
     * @param context Context to resolve colors.
     * @param blurSupported Whether blur is enabled (can be off due to battery saver)
     * @param withScrim Whether to composite a scrim when blur is enabled (used by legacy shade).
     * @return opaque color for the shade panel.
     */
    @JvmStatic
    fun shadePanel(context: Context, blurSupported: Boolean, withScrim: Boolean): Int {
        val raw = if (useBerryBlackShade(context)) {
            if (blurSupported) {
                val standard = compositeBerryQs(context)
                if (withScrim) {
                    ColorUtils.compositeColors(standard, berryScrimBehind(context))
                } else {
                    standard
                }
            } else {
                shadePanelFallbackBerry(context)
            }
        } else if (blurSupported) {
            // Classic + blur: QS (top) = darker (accent rail); notifications use the lighter blend.
            val panel = notificationScrimStandard(context)
            if (withScrim) {
                ColorUtils.compositeColors(panel, shadePanelScrimBehind(context))
            } else {
                panel
            }
        } else {
            // Classic + no blur: same darker/lighter split as blur-on (swapped vs stock naming).
            notificationScrimFallback(context)
        }
        return forceOpaque(raw)
    }

    /**
     * Notification stack scrim tint. Returned fully opaque; view-level alpha is applied by
     * {@link ScrimController}. When berry is off and the shade is Monet-colored, this uses the
     * lighter accent/neutral blend; the darker accent rail is reserved for {@link #shadePanel}
     * (QS zone).
     */
    @JvmStatic
    fun notificationScrim(context: Context, blurSupported: Boolean): Int {
        val raw = if (useBerryBlackShade(context)) {
            notificationScrimBerry(context)
        } else if (blurSupported) {
            // Lighter plate for notification stack; QS uses notificationScrimStandard (darker).
            shadePanelBlurOnClassic(context)
        } else {
            shadePanelFallback(context)
        }
        return forceOpaque(raw)
    }

    @JvmStatic
    fun shadePanelScrimBehind(context: Context): Int {
        val raw = if (useBerryBlackShade(context)) {
            berryScrimBehind(context)
        } else {
            context.resources.getColor(
                com.android.internal.R.color.shade_panel_scrim,
                context.theme,
            )
        }
        return forceOpaque(raw)
    }

    /** Strip embedded alpha — scrim opacity is controlled by ScrimController, not by tint. */
    private fun forceOpaque(color: Int): Int = color or 0xFF000000.toInt()

    private fun useBerryBlackShade(context: Context): Boolean {
        if (Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.BERRY_BLACK_THEME,
                0,
            ) != 1
        ) {
            return false
        }
        val night = Configuration.UI_MODE_NIGHT_YES
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == night
    }

    private fun compositeBerryQs(context: Context): Int {
        val fg = context.getColor(R.color.shade_berry_qs_fg)
        val bg = context.getColor(R.color.shade_berry_qs_bg)
        return ColorUtils.compositeColors(fg, bg)
    }

    private fun berryScrimBehind(context: Context): Int {
        return context.getColor(R.color.shade_berry_scrim_behind)
    }

    /**
     * QS plate when blur is on and berry is off: same accent/neutral blend shape as
     * [shadePanelFallback] (Monet / cr_colors), not framework neutral-only `shade_panel_fg/bg`.
     */
    private fun shadePanelBlurOnClassic(context: Context): Int {
        return ColorUtils.blendARGB(
            context.getColor(R.color.shade_panel_fallback_fg),
            context.getColor(R.color.shade_panel_fallback_bg),
            0.7f,
        )
    }

    @JvmStatic
    private fun shadePanelFallback(context: Context): Int {
        return ColorUtils.blendARGB(
            context.getColor(R.color.shade_panel_fallback_fg),
            context.getColor(R.color.shade_panel_fallback_bg),
            0.7f,
        )
    }

    @JvmStatic
    private fun shadePanelFallbackBerry(context: Context): Int {
        return ColorUtils.blendARGB(
            context.getColor(R.color.shade_panel_fallback_fg_berry),
            context.getColor(R.color.shade_panel_fallback_bg_berry),
            0.7f,
        )
    }

    @JvmStatic
    private fun notificationScrimStandard(context: Context): Int {
        return ColorUtils.setAlphaComponent(
            context.getColor(R.color.notification_scrim_base),
            (0.5f * 255).toInt(),
        )
    }

    @JvmStatic
    private fun notificationScrimStandardBerry(context: Context): Int {
        return ColorUtils.setAlphaComponent(
            context.getColor(R.color.notification_scrim_base_berry),
            (0.5f * 255).toInt(),
        )
    }

    /** Berry: one scrim tint for both blur and no-blur (classic differs). */
    private fun notificationScrimBerry(context: Context): Int {
        return notificationScrimStandardBerry(context)
    }

    @JvmStatic
    private fun notificationScrimFallback(context: Context): Int {
        return ColorUtils.blendARGB(
            context.getColor(R.color.notification_scrim_base),
            context.getColor(R.color.shade_panel_fallback_bg),
            0.7f,
        )
    }
}
