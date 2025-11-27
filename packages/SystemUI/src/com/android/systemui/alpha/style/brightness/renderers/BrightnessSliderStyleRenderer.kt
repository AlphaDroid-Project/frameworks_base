/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.brightness.renderers

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import com.android.systemui.alpha.style.common.AlphaColorScheme

/**
 * Renderer interface for brightness slider styling.
 *
 * Effect application order:
 * 1.Background is drawn (solid color from AlphaColorScheme)
 * 2.Renderer overlay is applied on top
 * 3.Icons/content drawn last
 */
interface BrightnessSliderStyleRenderer {

    val id: String
    val name: String
    val blendMode: BlendMode
        get() = BlendMode.SrcOver

    /**
     * Allows the renderer to override the base color scheme.
     * This affects ALL components: track, thumb, auto-brightness button.
     */
    fun produceColorScheme(default: AlphaColorScheme): AlphaColorScheme = default

    /**
     * Render overlay effect on active track segment.
     */
    fun DrawScope.renderActiveSegmentOverlay(
        segmentBounds: Rect,
        shape: Shape,
        cornerRadii: SegmentCornerRadii,
        materialColor: Color,
        density: Density
    ) {}

    /**
     * Render overlay effect on inactive track segment.
     */
    fun DrawScope.renderInactiveSegmentOverlay(
        segmentBounds: Rect,
        shape: Shape,
        cornerRadii: SegmentCornerRadii,
        materialColor: Color,
        density: Density
    ) {}

    /**
     * Render overlay effect on auto-brightness button.
     *
     * @param buttonBounds The bounds of the button
     * @param shape The shape of the button
     * @param cornerRadius Corner radius in pixels
     * @param materialColor The background color
     * @param isActive True for active state (auto-brightness ON)
     * @param density Current density
     */
    fun DrawScope.renderButtonOverlay(
        buttonBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        isActive: Boolean,
        density: Density
    ) {}

    /**
     * Render overlay effect on thumb.
     * Default implementation uses the same effect as button overlay.
     *
     * @param thumbBounds The bounds of the thumb
     * @param shape The shape of the thumb
     * @param cornerRadius Corner radius in pixels
     * @param thumbColor The thumb color (from AlphaColorScheme.thumb or overridden)
     * @param density Current density
     */
    fun DrawScope.renderThumbOverlay(
        thumbBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        thumbColor: Color,
        density: Density
    ) {
        // Default: same effect as active button
        renderButtonOverlay(
            buttonBounds = thumbBounds,
            shape = shape,
            cornerRadius = cornerRadius,
            materialColor = thumbColor,
            isActive = true,
            density = density
        )
    }

    /**
     * Get the thumb color for this style.
     * Default returns null to use AlphaColorScheme.thumb (same as small active tile).
     * Override for special cases (e.g., Slash uses darker color).
     *
     * @param schemeThumbColor The thumb color from AlphaColorScheme
     * @param schemeAccentColor The accent color from AlphaColorScheme
     * @return Custom thumb color, or null to use schemeThumbColor
     */
    fun getThumbColor(schemeThumbColor: Color, schemeAccentColor: Color): Color? = null

    /**
     * Whether to skip the thumb overlay effect.
     * Default is false (apply the effect).
     * Override for special cases (e.g., Slash only uses solid color + inset).
     */
    fun skipThumbOverlay(): Boolean = false

    fun DrawScope.renderTrackIcon(density: Density) {}
    fun DrawScope.renderButtonIcon(density: Density) {}
}

data class SegmentCornerRadii(
    val topLeft: Float,
    val topRight: Float,
    val bottomLeft: Float,
    val bottomRight: Float
)