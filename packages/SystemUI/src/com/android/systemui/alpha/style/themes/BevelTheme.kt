/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.themes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Theme interface for Bevel style.
 * Shared by both Brightness Slider and QS Tile renderers.
 */
interface BevelTheme {
    val bevelWidth: Dp
    val activeHighlight: Color
    val activeShadow: Color
    val inactiveHighlight: Color
    val inactiveShadow: Color
    val outlineWidth: Dp
    val outlineActiveAlpha: Float
    val outlineInactiveAlpha: Float
    val surfaceGradientAlpha: Float
    val tintAlpha: Float
    val bevelAngle: Float
    val surfaceGradientAngle: Float
}

/**
 * Light mode theme for Bevel style.
 */
object BevelLightTheme : BevelTheme {
    override val bevelWidth: Dp = 2.5.dp
    override val activeHighlight: Color = Color.White.copy(alpha = 0.80f)
    override val activeShadow: Color = Color.Black.copy(alpha = 0.95f)
    override val inactiveHighlight: Color = Color.White.copy(alpha = 0.30f)
    override val inactiveShadow: Color = Color.Black.copy(alpha = 0.55f)
    override val outlineWidth: Dp = 1.dp
    override val outlineActiveAlpha: Float = 0.12f
    override val outlineInactiveAlpha: Float = 0.08f
    override val surfaceGradientAlpha: Float = 0.08f
    override val tintAlpha: Float = 0.08f
    override val bevelAngle: Float = 90f
    override val surfaceGradientAngle: Float = 135f
}

/**
 * Dark mode theme for Bevel style.
 */
object BevelDarkTheme : BevelTheme {
    override val bevelWidth: Dp = 2.5.dp
    override val activeHighlight: Color = Color.White.copy(alpha = 0.50f)
    override val activeShadow: Color = Color.Black.copy(alpha = 0.70f)
    override val inactiveHighlight: Color = Color.White.copy(alpha = 0.20f)
    override val inactiveShadow: Color = Color.Black.copy(alpha = 0.40f)
    override val outlineWidth: Dp = 1.dp
    override val outlineActiveAlpha: Float = 0.08f
    override val outlineInactiveAlpha: Float = 0.05f
    override val surfaceGradientAlpha: Float = 0.05f
    override val tintAlpha: Float = 0.05f
    override val bevelAngle: Float = 90f
    override val surfaceGradientAngle: Float = 135f
}
