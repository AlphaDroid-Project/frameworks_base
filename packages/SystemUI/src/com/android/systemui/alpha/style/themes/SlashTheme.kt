/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.themes

interface SlashTheme {
    val slashAngle: Float
    val slashAlpha: Float
    val cutLineAlpha: Float

    val activeParams: ColorParams
    val inactiveParams: ColorParams
    // New: Specific params for the "little icon background" in dual tiles
    val iconBackgroundParams: ColorParams
    val thumbParams: ColorParams
}

object SlashLightTheme : SlashTheme {
    override val slashAngle: Float = 115f
    override val slashAlpha: Float = 0.1f
    override val cutLineAlpha: Float = 0.4f

    override val activeParams = ColorParams(
        baseLightness = 0.55f,
        baseSaturation = 0.9f,
        baseAlpha = 1.0f,
        forceLightContent = true
    )

    override val inactiveParams = ColorParams(
        baseLightness = 0.90f,
        baseSaturation = 0.05f,
        baseAlpha = 1.0f,
        forceLightContent = false
    )

    // More color (sat 0.2) and contrast (lightness 0.80) compared to inactive (0.90)
    override val iconBackgroundParams = ColorParams(
        baseLightness = 0.80f,
        baseSaturation = 0.2f,
        baseAlpha = 1.0f,
        forceLightContent = false
    )

    override val thumbParams = activeParams.copy()
}

object SlashDarkTheme : SlashTheme {
    override val slashAngle: Float = 115f
    override val slashAlpha: Float = 0.2f
    override val cutLineAlpha: Float = 0.2f

    override val activeParams = ColorParams(
        baseLightness = 0.30f,
        baseSaturation = 0.9f,
        baseAlpha = 1.0f,
        forceLightContent = true
    )

    override val inactiveParams = ColorParams(
        baseLightness = 0.12f,
        baseSaturation = 0.0f,
        baseAlpha = 1.0f,
        forceLightContent = true
    )

    // More color (sat 0.2) and contrast (lightness 0.25) compared to inactive (0.12)
    override val iconBackgroundParams = ColorParams(
        baseLightness = 0.25f,
        baseSaturation = 0.2f,
        baseAlpha = 1.0f,
        forceLightContent = true
    )

    override val thumbParams = activeParams.copy()
}
