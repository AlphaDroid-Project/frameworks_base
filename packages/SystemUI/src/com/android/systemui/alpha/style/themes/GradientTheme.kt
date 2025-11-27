/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.themes

/**
 * Theme interface for Gradient style.
 */
interface GradientTheme {
    val angle: Float

    val activeParams: ColorParams
    val inactiveParams: ColorParams
    val thumbParams: ColorParams
}

/**
 * Light mode theme for Gradient style.
 */
object GradientLightTheme : GradientTheme {
    override val angle: Float = 45f

    override val activeParams = ColorParams(
        // Made vivid (was 0.80/0.90).
        // 0.65 lightness allows the color to be bright but saturated.
        baseLightness = 0.4f,
        baseSaturation = 1.3f,
        baseAlpha = 1.0f,
        forceLightContent = true
    )

    override val inactiveParams = ColorParams(
        baseLightness = 0.6f,
        baseSaturation = 0.6f,
        baseAlpha = 1.0f,
        forceLightContent = false
    )

    // Exact match with activeParams
    override val thumbParams = activeParams.copy()
}

/**
 * Dark mode theme for Gradient style.
 */
object GradientDarkTheme : GradientTheme {
    override val angle: Float = 45f

    override val activeParams = ColorParams(
        baseLightness = 0.25f,
        baseSaturation = 1.1f,
        baseAlpha = 1.0f,
        forceLightContent = true
    )

    override val inactiveParams = ColorParams(
        baseLightness = 0.12f,
        baseSaturation = 0.3f,
        baseAlpha = 1.0f,
        forceLightContent = true
    )

    override val thumbParams = activeParams.copy()
}
