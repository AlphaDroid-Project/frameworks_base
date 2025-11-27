/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.themes

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.systemui.alpha.style.common.GradientHelper.ColorSpace
import com.android.systemui.alpha.style.common.GradientHelper.Tone
import com.android.systemui.alpha.style.common.GradientHelper.PaletteColor

/**
 * Theme interface for Outline style.
 * Uses gradient borders from Material You palette colors.
 */
interface OutlineTheme {
    val strokeWidth: Dp

    // Gradient border: Start and end colors
    val activeBorderStart: PaletteColor
    val activeBorderEnd: PaletteColor
    val inactiveBorderStart: PaletteColor
    val inactiveBorderEnd: PaletteColor

    // Gradient angle
    val borderGradientAngle: Float

    // Optional tint to add vibrancy (blends original accent/neutral into gradient)
    val activeTintAlpha: Float
    val inactiveTintAlpha: Float

    val blendMode: BlendMode
}

/**
 * Light mode theme for Outline style.
 */
object OutlineLightTheme : OutlineTheme {
    override val strokeWidth: Dp = 2.5.dp

    // Active border gradient: Medium to dark accent
    override val activeBorderStart = PaletteColor(
        colorSpace = ColorSpace.ACCENT1,
        tone = Tone.T800
    )
    override val activeBorderEnd = PaletteColor(
        colorSpace = ColorSpace.ACCENT1,
        tone = Tone.T800
    )

    // Inactive border gradient: Medium to dark neutral
    override val inactiveBorderStart = PaletteColor(
        colorSpace = ColorSpace.NEUTRAL2,
        tone = Tone.T600
    )
    override val inactiveBorderEnd = PaletteColor(
        colorSpace = ColorSpace.NEUTRAL2,
        tone = Tone.T600
    )

    // Gradient flows top-left to bottom-right
    override val borderGradientAngle: Float = 135f

    // Tint to make borders more vibrant
    override val activeTintAlpha: Float = 0.15f
    override val inactiveTintAlpha: Float = 0.08f

    override val blendMode: BlendMode = BlendMode.SrcOver
}

/**
 * Dark mode theme for Outline style.
 */
object OutlineDarkTheme : OutlineTheme {
    override val strokeWidth: Dp = 2.5.dp

    // Active border gradient: Light to medium accent
    override val activeBorderStart = PaletteColor(
        colorSpace = ColorSpace.ACCENT1,
        tone = Tone.T400
    )
    override val activeBorderEnd = PaletteColor(
        colorSpace = ColorSpace.ACCENT1,
        tone = Tone.T400
    )

    // Inactive border gradient: Light to medium neutral
    override val inactiveBorderStart = PaletteColor(
        colorSpace = ColorSpace.NEUTRAL2,
        tone = Tone.T300
    )
    override val inactiveBorderEnd = PaletteColor(
        colorSpace = ColorSpace.NEUTRAL2,
        tone = Tone.T300
    )

    override val borderGradientAngle: Float = 135f

    // Tint to make borders more vibrant
    override val activeTintAlpha: Float = 0.18f
    override val inactiveTintAlpha: Float = 0.10f

    override val blendMode: BlendMode = BlendMode.SrcOver
}
