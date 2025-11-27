/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.qs.renderers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.core.graphics.ColorUtils
import com.android.systemui.alpha.style.common.AlphaColorScheme
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.alpha.style.themes.ColorParams
import com.android.systemui.alpha.style.themes.NeonTheme
import com.android.systemui.alpha.style.themes.NeonLightTheme
import com.android.systemui.alpha.style.themes.NeonDarkTheme

class QSNeonStyleRenderer(
    private val accentColor: Color,
    private val neutralColor: Color,
    private val isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings
) : QSTileStyleRenderer {

    override val id = "neon"
    override val name = "Neon"
    override val blendMode: BlendMode = if (isDarkTheme) BlendMode.Screen else BlendMode.SrcOver

    private val theme: NeonTheme = if (isDarkTheme) NeonDarkTheme else NeonLightTheme

    override fun produceColorScheme(default: AlphaColorScheme): AlphaColorScheme {
        val newAccent = applyParams(default.accent, theme.activeParams)
        val newNeutral = applyParams(default.neutral, theme.inactiveParams)
        val newNeutralVariant = applyParams(default.neutralVariant, theme.inactiveParams)
        val newThumb = applyParams(default.accent, theme.thumbParams)

        val newOnAccent = if (theme.activeParams.forceLightContent) Color.White else default.onAccent
        val newOnNeutral = if (theme.inactiveParams.forceLightContent) Color.White else default.onNeutral
        val newOnNeutralVariant = if (theme.inactiveParams.forceLightContent) Color.White else default.onNeutralVariant

        return default.copy(
            accent = newAccent,
            onAccent = newOnAccent,
            neutral = newNeutral,
            onNeutral = newOnNeutral,
            neutralVariant = newNeutralVariant,
            onNeutralVariant = newOnNeutralVariant,
            thumb = newThumb
        )
    }

    private fun applyParams(color: Color, params: ColorParams): Color {
        val argb = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)
        hsl[1] = (hsl[1] * params.baseSaturation * userSettings.saturation).coerceIn(0f, 1f)
        hsl[2] = (params.baseLightness * userSettings.lightness).coerceIn(0f, 1f)
        val newColor = Color(ColorUtils.HSLToColor(hsl))
        return newColor.copy(alpha = (params.baseAlpha * userSettings.opacity).coerceIn(0f, 1f))
    }

    override fun DrawScope.renderTileBackgroundOverlay(
        tileBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        isSmallTile: Boolean,
        density: Density
    ) {
        val isActive = state == TileState.STATE_ACTIVE
        val glowBase = if (isActive) accentColor else neutralColor
        val vividColor = makeVividGlow(glowBase, isActive)
        val intensity = (if (isActive) theme.glowIntensity else theme.glowIntensity * 0.5f) * userSettings.strength

        drawNeonEffect(tileBounds, vividColor, intensity)
    }

    override fun DrawScope.renderIconBackgroundOverlay(
        iconBackgroundBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        density: Density
    ) {
        val isActive = state == TileState.STATE_ACTIVE
        val glowBase = if (isActive) accentColor else neutralColor
        val vividColor = makeVividGlow(glowBase, isActive)
        val intensity = (if (isActive) theme.glowIntensity else theme.glowIntensity * 0.5f) * userSettings.strength

        drawNeonEffect(iconBackgroundBounds, vividColor, intensity)
    }

    private fun makeVividGlow(color: Color, isActive: Boolean): Color {
        val argb = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)

        val saturationMultiplier = if (isActive) theme.saturationBoost else theme.saturationBoost * 0.9f
        hsl[1] = (hsl[1] * saturationMultiplier).coerceIn(0f, 1f)

        val lightnessBoost = if (isActive) theme.lightnessBoost else theme.lightnessBoost * 0.8f
        hsl[2] = (hsl[2] + lightnessBoost).coerceIn(0f, 1f)

        return Color(ColorUtils.HSLToColor(hsl))
    }

    private fun DrawScope.drawNeonEffect(
        bounds: Rect,
        color: Color,
        intensity: Float
    ) {
        if (bounds.width <= 0 || bounds.height <= 0) return

        val center = Offset(
            bounds.left + bounds.width / 2f,
            bounds.top + bounds.height / 2f
        )

        val glowBrush = Brush.radialGradient(
            0.0f to Color.Transparent,
            0.2f to color.copy(alpha = intensity * 0.05f),
            0.6f to color.copy(alpha = intensity * 0.4f),
            1.0f to color.copy(alpha = intensity),
            center = center,
            radius = kotlin.math.max(bounds.width, bounds.height) / 2f
        )

        val yScale = if (bounds.width > 0) bounds.height / bounds.width else 1f

        scale(scaleX = 1f, scaleY = yScale, pivot = center) {
            drawRect(
                brush = glowBrush,
                topLeft = Offset(bounds.left, center.y - bounds.width / 2f),
                size = Size(bounds.width, bounds.width)
            )
        }
    }
}
