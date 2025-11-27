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
import com.android.systemui.alpha.style.themes.GradientTheme
import com.android.systemui.alpha.style.themes.GradientLightTheme
import com.android.systemui.alpha.style.themes.GradientDarkTheme

class QSGradientStyleRenderer(
    private val accentColor: Color,
    private val neutralColor: Color,
    private val isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings
) : QSTileStyleRenderer {

    override val id = "gradient"
    override val name = "Gradient"
    override val blendMode: BlendMode = BlendMode.SrcAtop

    private val theme: GradientTheme = if (isDarkTheme) GradientDarkTheme else GradientLightTheme

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
        val baseColor = if (isActive) accentColor else neutralColor
        drawNiceGradient(tileBounds, baseColor, isActive)
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
        val baseColor = if (isActive) accentColor else neutralColor
        drawNiceGradient(iconBackgroundBounds, baseColor, isActive)
    }

    private fun DrawScope.drawNiceGradient(
        bounds: Rect,
        overlayColor: Color,
        isActive: Boolean
    ) {
        val (start, end) = calculateGradientOffsets(bounds, theme.angle + userSettings.angle)

        val startAlpha = (if (isDarkTheme) 0.5f else 0.4f) * userSettings.strength
        val endAlpha = 0.0f

        val gradientBrush = Brush.linearGradient(
            colors = listOf(
                overlayColor.copy(alpha = startAlpha),
                overlayColor.copy(alpha = endAlpha)
            ),
            start = start,
            end = end
        )

        drawRect(
            brush = gradientBrush,
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height)
        )

        if (isActive) {
             val shineAlpha = (if (isDarkTheme) 0.15f else 0.25f) * userSettings.strength
             val shineBrush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = shineAlpha), Color.Transparent),
                center = start,
                radius = bounds.width * 0.8f
            )
            drawRect(
                brush = shineBrush,
                topLeft = Offset(bounds.left, bounds.top),
                size = Size(bounds.width, bounds.height)
            )
        }
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
