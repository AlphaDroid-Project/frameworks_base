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
import androidx.compose.ui.unit.Density
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.sin
import com.android.systemui.alpha.style.common.AlphaColorScheme
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.alpha.style.themes.ColorParams
import com.android.systemui.alpha.style.themes.MetallicTheme
import com.android.systemui.alpha.style.themes.MetallicLightTheme
import com.android.systemui.alpha.style.themes.MetallicDarkTheme

class QSMetallicStyleRenderer(
    private val accentColor: Color,
    private val neutralColor: Color,
    private val isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings
) : QSTileStyleRenderer {

    override val id = "metallic"
    override val name = "Metallic"
    override val blendMode: BlendMode = BlendMode.Overlay

    private val theme: MetallicTheme = if (isDarkTheme) MetallicDarkTheme else MetallicLightTheme

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
        drawMetallicSheen(tileBounds)
    }

    override fun DrawScope.renderIconBackgroundOverlay(
        iconBackgroundBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        density: Density
    ) {
        drawMetallicSheen(iconBackgroundBounds)
    }

    private fun DrawScope.drawMetallicSheen(bounds: Rect) {
        val (start, end) = calculateGradientOffsets(bounds, theme.shineAngle + userSettings.angle)
        val intensity = theme.shineIntensity * userSettings.strength

        val shineBrush = Brush.linearGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.4f),
                Color.White.copy(alpha = 0.1f),
                Color.White.copy(alpha = intensity),
                Color.White.copy(alpha = 0.1f),
                Color.Black.copy(alpha = 0.3f)
            ),
            start = start,
            end = end
        )

        drawRect(
            brush = shineBrush,
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height)
        )
    }

    private fun calculateGradientOffsets(
        bounds: Rect,
        angleDegrees: Float
    ): Pair<Offset, Offset> {
        val rad = Math.toRadians(angleDegrees.toDouble())
        val cosAngle = cos(rad).toFloat()
        val sinAngle = sin(rad).toFloat()

        val centerX = bounds.left + bounds.width / 2f
        val centerY = bounds.top + bounds.height / 2f

        val dx = cosAngle * bounds.width / 2f
        val dy = sinAngle * bounds.height / 2f

        val startX = centerX - dx
        val startY = centerY - dy
        val endX = centerX + dx
        val endY = centerY + dy

        return Offset(startX, startY) to Offset(endX, endY)
    }
}
