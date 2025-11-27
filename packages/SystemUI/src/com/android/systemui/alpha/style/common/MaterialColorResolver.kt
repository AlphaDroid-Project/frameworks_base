/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.common

import android.content.res.Resources
import androidx.compose.ui.graphics.Color
import android.util.Log

/**
 * Resolves Material You colors that match AOSP's actual usage.
 * Uses the same color tokens as Material3 Expressive surfaces.
 */
object MaterialColorResolver {
    private const val TAG = "MaterialColorResolver"

    /**
     * Overlay alpha constants for gradient effects.
     */
    object OverlayAlpha {
        const val ACTIVE = 0.45f
        const val INACTIVE = 0.30f
        const val UNAVAILABLE = 0.20f
    }

    /**
     * Get accent color for active tiles/segments.
     * Matches MaterialTheme.colorScheme.primary used by AOSP.
     */
    fun getAccentColor(resources: Resources, night: Boolean): Color {
        val resId = if (night) {
            android.R.color.system_accent1_200
        } else {
            android.R.color.system_accent1_600
        }
        return resolveColor(resources, resId, "accent")
    }

    /**
     * Get neutral/surface color for inactive tiles/segments.
     * Matches LocalAndroidColorScheme.surfaceEffect2 used by AOSP.
     * Uses neutral2 palette which has subtle color tinting.
     */
    fun getNeutralSurfaceColor(resources: Resources, night: Boolean): Color {
        val resId = if (night) {
            android.R.color.system_neutral2_800
        } else {
            android.R.color.system_neutral2_100
        }
        return resolveColor(resources, resId, "neutral2_surface")
    }

    /**
     * Get neutral/surface color for icon backgrounds in dual-target tiles.
     * Matches LocalAndroidColorScheme.surfaceEffect3 used by AOSP.
     */
    fun getNeutralIconBackgroundColor(resources: Resources, night: Boolean): Color {
        val resId = if (night) {
            android.R.color.system_neutral2_700
        } else {
            android.R.color.system_neutral2_200
        }
        return resolveColor(resources, resId, "neutral2_icon_bg")
    }

    /**
     * Get unavailable/disabled color.
     */
    fun getUnavailableColor(resources: Resources, night: Boolean): Color {
        val resId = if (night) {
            android.R.color.system_neutral2_900
        } else {
            android.R.color.system_neutral2_50
        }
        return resolveColor(resources, resId, "unavailable")
    }

    /**
     * Get outline color for borders (matches AOSP outline tokens).
     */
    fun getOutlineColor(resources: Resources, night: Boolean): Color {
        val resId = if (night) {
            android.R.color.system_outline_variant_dark
        } else {
            android.R.color.system_outline_light
        }
        return resolveColor(resources, resId, "outline")
    }

    /**
     * Get active outline/border color (stronger than neutral outline).
     */
    fun getActiveOutlineColor(resources: Resources, night: Boolean): Color {
        val resId = if (night) {
            android.R.color.system_accent1_200
        } else {
            android.R.color.system_accent1_800
        }
        val base = resolveColor(resources, resId, "active_outline")
        return GradientHelper.blend(base, Color.Black, 0.25f)
    }

    /**
     * Helper to resolve color from resource ID.
     */
    private fun resolveColor(resources: Resources, resId: Int, name: String): Color {
        return try {
            if (resId == 0) {
                Log.w(TAG, "Invalid resource ID for color: $name")
                return getFallbackColor(name)
            }
            Color(resources.getColor(resId, null))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve color: $name (resId=$resId)", e)
            getFallbackColor(name)
        }
    }

    private fun getFallbackColor(name: String): Color {
        return when {
            name.contains("accent") -> Color(0xFF1976D2)
            name.contains("unavailable") -> Color(0xFF9E9E9E)
            else -> Color(0xFFE0E0E0)
        }
    }
}
