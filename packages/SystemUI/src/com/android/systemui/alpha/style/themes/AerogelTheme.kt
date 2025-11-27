/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.themes

interface AerogelTheme {
    val activeParams: ColorParams
    val inactiveParams: ColorParams
    val thumbParams: ColorParams
}

object AerogelLightTheme : AerogelTheme {
    override val activeParams = ColorParams(
        baseLightness = 0.35f,     // Darker for better contrast in Light Mode
        baseSaturation = 1.3f,     // Vivid
        baseAlpha = 0.60f,         // The requested Translucency
        forceLightContent = true   // White text on vivid background
    )

    override val inactiveParams = ColorParams(
        baseLightness = 0.90f,
        baseSaturation = 0.1f,
        baseAlpha = 0.50f,         // Translucent inactive
        forceLightContent = false
    )

    // Thumb needs to be solid to be visible/usable
    override val thumbParams = ColorParams(
        baseLightness = 0.50f,
        baseSaturation = 1.3f,
        baseAlpha = 1.0f,          // Solid thumb
        forceLightContent = true
    )
}

object AerogelDarkTheme : AerogelTheme {
    override val activeParams = ColorParams(
        baseLightness = 0.35f,     // "Dark Vivid" tone
        baseSaturation = 1.3f,     // Boost saturation
        baseAlpha = 0.60f,         // The requested Translucency
        forceLightContent = true
    )

    override val inactiveParams = ColorParams(
        baseLightness = 0.15f,
        baseSaturation = 0.1f,
        baseAlpha = 0.50f,
        forceLightContent = true
    )

    override val thumbParams = ColorParams(
        baseLightness = 0.50f,
        baseSaturation = 1.3f,
        baseAlpha = 1.0f,          // Solid thumb
        forceLightContent = true
    )
}
