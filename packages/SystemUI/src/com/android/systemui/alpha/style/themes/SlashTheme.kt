/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.themes

interface SlashTheme {
    val slashAngle: Float
    val slashAlpha: Float
    val cutLineAlpha: Float

    // Defines where the slash starts on the x-axis (0.0 - 1.0).
    val slashStartRatio: Float

    val activeParams: ColorParams
    val inactiveParams: ColorParams
    val iconBackgroundParams: ColorParams
    val thumbParams: ColorParams
}

object SlashLightTheme : SlashTheme {
    override val slashAngle: Float = 115f
    override val slashAlpha: Float = 0.1f
    override val cutLineAlpha: Float = 0.4f
    override val slashStartRatio: Float = 0.88f

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
    override val slashStartRatio: Float = 0.88f

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

    override val iconBackgroundParams = ColorParams(
        baseLightness = 0.25f,
        baseSaturation = 0.2f,
        baseAlpha = 1.0f,
        forceLightContent = true
    )

    override val thumbParams = activeParams.copy()
}