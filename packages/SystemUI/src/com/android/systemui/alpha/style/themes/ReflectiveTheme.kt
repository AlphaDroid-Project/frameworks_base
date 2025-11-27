/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.themes

interface ReflectiveTheme {
    // Height of the reflection relative to component height (0.0 - 1.0)
    val reflectionHeight: Float
    // Opacity of the white reflection at the top
    val reflectionAlpha: Float
    // Opacity of the glass rim/border
    val rimAlpha: Float

    val activeParams: ColorParams
    val inactiveParams: ColorParams
    val thumbParams: ColorParams
}

object ReflectiveLightTheme : ReflectiveTheme {
    override val reflectionHeight: Float = 0.55f
    override val reflectionAlpha: Float = 0.6f
    override val rimAlpha: Float = 0.5f

    override val activeParams = ColorParams(
        baseLightness = 0.45f,     // Significantly darker to pop against light bg
        baseSaturation = 1.2f,     // Increased vividness
        baseAlpha = 1.0f,
        forceLightContent = true   // White text
    )

    override val inactiveParams = ColorParams(
        baseLightness = 0.85f,     // Silver/Grey base
        baseSaturation = 0.0f,
        baseAlpha = 1.0f,
        forceLightContent = false
    )

    override val thumbParams = activeParams.copy()
}

object ReflectiveDarkTheme : ReflectiveTheme {
    override val reflectionHeight: Float = 0.55f
    override val reflectionAlpha: Float = 0.35f
    override val rimAlpha: Float = 0.4f

    override val activeParams = ColorParams(
        baseLightness = 0.20f,     // Deep dark base
        baseSaturation = 1.2f,
        baseAlpha = 1.0f,
        forceLightContent = true
    )

    override val inactiveParams = ColorParams(
        baseLightness = 0.15f,     // Deep neutral
        baseSaturation = 0.1f,
        baseAlpha = 1.0f,
        forceLightContent = true
    )

    override val thumbParams = activeParams.copy()
}
