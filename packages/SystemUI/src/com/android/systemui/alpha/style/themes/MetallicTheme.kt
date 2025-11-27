/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.themes

interface MetallicTheme {
    val shineAngle: Float
    val shineIntensity: Float

    val activeParams: ColorParams
    val inactiveParams: ColorParams
    val thumbParams: ColorParams
}

object MetallicLightTheme : MetallicTheme {
    override val shineAngle: Float = 60f
    override val shineIntensity: Float = 0.5f

    override val activeParams = ColorParams(
        baseLightness = 0.40f,     // Dark Anodized Metal (was too light)
        baseSaturation = 1.1f,     // More vivid color retention
        baseAlpha = 1.0f,
        forceLightContent = true   // White text
    )

    override val inactiveParams = ColorParams(
        baseLightness = 0.85f,     // Silver / Aluminum
        baseSaturation = 0.0f,     // No color
        baseAlpha = 1.0f,
        forceLightContent = false  // Dark text
    )

    // Thumb matches active anodized look
    override val thumbParams = activeParams.copy()
}

object MetallicDarkTheme : MetallicTheme {
    override val shineAngle: Float = 60f
    override val shineIntensity: Float = 0.3f

    override val activeParams = ColorParams(
        baseLightness = 0.25f,     // Dark Anodized Metal
        baseSaturation = 1.0f,
        baseAlpha = 1.0f,
        forceLightContent = true
    )

    override val inactiveParams = ColorParams(
        baseLightness = 0.18f,     // Gunmetal Grey
        baseSaturation = 0.1f,
        baseAlpha = 1.0f,
        forceLightContent = true
    )

    override val thumbParams = activeParams.copy()
}
