/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.themes

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Soft outer halo + thin inner rim light along the real shape outline.
 * Distinct from Bevel (hard dual edge) and Outline (single solid/gradient border).
 */
interface LumenTheme {
    val haloWidth: Dp
    val rimWidth: Dp
    val activeHaloAlpha: Float
    val inactiveHaloAlpha: Float
    val activeRimAlpha: Float
    val inactiveRimAlpha: Float
    /** How much accent tints the halo/rim (0 = pure white/neutral). */
    val activeAccentMix: Float
    val inactiveAccentMix: Float
    val blendMode: BlendMode
}

object LumenLightTheme : LumenTheme {
    override val haloWidth: Dp = 3.5.dp
    override val rimWidth: Dp = 1.25.dp
    override val activeHaloAlpha: Float = 0.28f
    override val inactiveHaloAlpha: Float = 0.14f
    override val activeRimAlpha: Float = 0.72f
    override val inactiveRimAlpha: Float = 0.32f
    override val activeAccentMix: Float = 0.45f
    override val inactiveAccentMix: Float = 0.12f
    override val blendMode: BlendMode = BlendMode.SrcOver
}

object LumenDarkTheme : LumenTheme {
    override val haloWidth: Dp = 3.5.dp
    override val rimWidth: Dp = 1.25.dp
    override val activeHaloAlpha: Float = 0.38f
    override val inactiveHaloAlpha: Float = 0.16f
    override val activeRimAlpha: Float = 0.85f
    override val inactiveRimAlpha: Float = 0.28f
    override val activeAccentMix: Float = 0.55f
    override val inactiveAccentMix: Float = 0.15f
    override val blendMode: BlendMode = BlendMode.SrcOver
}
