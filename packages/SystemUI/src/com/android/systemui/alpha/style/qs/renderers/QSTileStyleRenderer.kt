/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.qs.renderers

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import com.android.systemui.alpha.style.common.AlphaColorScheme

interface QSTileStyleRenderer {

    val id: String
    val name: String
    val blendMode: BlendMode
        get() = BlendMode.SrcOver

    /**
     * Allows the renderer to override the base color scheme used by AOSP.
     * This runs BEFORE any drawing happens.
     *
     * @param default The standard AOSP colors (Material You)
     * @return The modified color scheme for this style
     */
    fun produceColorScheme(default: AlphaColorScheme): AlphaColorScheme = default

    // --- Drawing Overlays (Run AFTER background is drawn) ---

    fun DrawScope.renderTileBackgroundOverlay(
        tileBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        isSmallTile: Boolean,
        density: Density
    ) {
    }

    fun DrawScope.renderIconBackgroundOverlay(
        iconBackgroundBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        density: Density
    ) {
    }

    fun DrawScope.renderIcon(density: Density) {}
    fun DrawScope.renderPrimaryLabel(density: Density) {}
    fun DrawScope.renderSecondaryLabel(density: Density) {}
}

internal object TileState {
    const val STATE_UNAVAILABLE = 0
    const val STATE_INACTIVE = 1
    const val STATE_ACTIVE = 2
}
