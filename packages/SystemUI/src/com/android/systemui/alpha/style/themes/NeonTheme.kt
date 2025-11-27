/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.themes

/**
 * Theme interface for Neon style.
 */
interface NeonTheme {
    val glowIntensity: Float
    val saturationBoost: Float
    val lightnessBoost: Float

    // Configuration for Active State (Accent)
    val activeParams: ColorParams

    // Configuration for Inactive State (Neutral)
    val inactiveParams: ColorParams

    // Configuration for Slider Thumb (Needs high visibility)
    val thumbParams: ColorParams
}

/**
 * Light mode theme for Neon style.
 */
object NeonLightTheme : NeonTheme {
    override val glowIntensity: Float = 0.70f
    override val saturationBoost: Float = 1.4f
    override val lightnessBoost: Float = 0.10f

    override val activeParams = ColorParams(
        baseLightness = 0.30f,
        baseSaturation = 1.2f,
        baseAlpha = 0.95f,
        forceLightContent = true
    )

    override val inactiveParams = ColorParams(
        baseLightness = 0.30f,
        baseSaturation = 1.0f,
        baseAlpha = 0.90f,
        forceLightContent = true
    )

    override val thumbParams = ColorParams(
        baseLightness = 0.40f,
        baseSaturation = 1.4f,
        baseAlpha = 1.0f,
        forceLightContent = false
    )
}

/**
 * Dark mode theme for Neon style.
 */
object NeonDarkTheme : NeonTheme {
    override val glowIntensity: Float = 0.80f
    override val saturationBoost: Float = 2.0f
    override val lightnessBoost: Float = 0.10f

    override val activeParams = ColorParams(
        baseLightness = 0.15f,
        baseSaturation = 1.4f,
        baseAlpha = 0.90f,
        forceLightContent = true
    )

    override val inactiveParams = ColorParams(
        baseLightness = 0.10f,
        baseSaturation = 1.0f,
        baseAlpha = 0.80f,
        forceLightContent = true
    )

    override val thumbParams = ColorParams(
        baseLightness = 0.18f,
        baseSaturation = 1.4f,
        baseAlpha = 0.90f,
        forceLightContent = false
    )
}
