/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.qs.renderers

import android.content.Context
import android.service.quicksettings.Tile
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.core.graphics.ColorUtils
import com.android.systemui.alpha.style.common.GradientHelper
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.alpha.style.themes.OutlineTheme
import com.android.systemui.alpha.style.themes.OutlineLightTheme
import com.android.systemui.alpha.style.themes.OutlineDarkTheme
import kotlin.math.cos
import kotlin.math.sin

class QSOutlineStyleRenderer(
    private val context: Context,
    private val accentColor: Color,
    private val neutralColor: Color,
    isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings
) : QSTileStyleRenderer {

    override val id = "outline"
    override val name = "Outline"
    override val blendMode: BlendMode get() = theme.blendMode

    private val theme: OutlineTheme = if (isDarkTheme) OutlineDarkTheme else OutlineLightTheme

    override fun DrawScope.renderTileBackgroundOverlay(
        tileBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        isSmallTile: Boolean,
        density: Density
    ) {
        val strokeWidthPx = with(density) { theme.strokeWidth.toPx() } * userSettings.strength
        val halfStroke = strokeWidthPx / 2f

        val isActive = state == Tile.STATE_ACTIVE

        val borderStart = if (isActive) theme.activeBorderStart else theme.inactiveBorderStart
        val borderEnd = if (isActive) theme.activeBorderEnd else theme.inactiveBorderEnd
        val tintColor = if (isActive) accentColor else neutralColor
        val tintAlpha = if (isActive) theme.activeTintAlpha else theme.inactiveTintAlpha

        var startColor = tuneColor(GradientHelper.paletteColor(context.resources, borderStart))
        var endColor = tuneColor(GradientHelper.paletteColor(context.resources, borderEnd))

        if (tintAlpha > 0f) {
            startColor = blendColors(startColor, tintColor, tintAlpha)
            endColor = blendColors(endColor, tintColor, tintAlpha)
        }

        if (state == Tile.STATE_UNAVAILABLE) {
            startColor = startColor.copy(alpha = 0.38f)
            endColor = endColor.copy(alpha = 0.38f)
        }

        val (gradStart, gradEnd) = calculateGradientOffsets(tileBounds, theme.borderGradientAngle + userSettings.angle)
        val gradientBrush = Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = gradStart,
            end = gradEnd
        )

        drawRoundRect(
            brush = gradientBrush,
            topLeft = Offset(tileBounds.left + halfStroke, tileBounds.top + halfStroke),
            size = Size(tileBounds.width - strokeWidthPx, tileBounds.height - strokeWidthPx),
            cornerRadius = CornerRadius((cornerRadius - halfStroke).coerceAtLeast(0f)),
            style = Stroke(width = strokeWidthPx)
        )
    }

    override fun DrawScope.renderIconBackgroundOverlay(
        iconBackgroundBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        density: Density
    ) {
        val strokeWidthPx = with(density) { theme.strokeWidth.toPx() } * userSettings.strength
        val halfStroke = strokeWidthPx / 2f

        val isActive = state == Tile.STATE_ACTIVE

        val borderStart = if (isActive) theme.activeBorderStart else theme.inactiveBorderStart
        val borderEnd = if (isActive) theme.activeBorderEnd else theme.inactiveBorderEnd
        val tintColor = if (isActive) accentColor else neutralColor
        val tintAlpha = if (isActive) theme.activeTintAlpha else theme.inactiveTintAlpha

        var startColor = tuneColor(GradientHelper.paletteColor(context.resources, borderStart))
        var endColor = tuneColor(GradientHelper.paletteColor(context.resources, borderEnd))

        if (tintAlpha > 0f) {
            startColor = blendColors(startColor, tintColor, tintAlpha)
            endColor = blendColors(endColor, tintColor, tintAlpha)
        }

        val (gradStart, gradEnd) = calculateGradientOffsets(iconBackgroundBounds, theme.borderGradientAngle + userSettings.angle)
        val gradientBrush = Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = gradStart,
            end = gradEnd
        )

        drawRoundRect(
            brush = gradientBrush,
            topLeft = Offset(iconBackgroundBounds.left + halfStroke, iconBackgroundBounds.top + halfStroke),
            size = Size(iconBackgroundBounds.width - strokeWidthPx, iconBackgroundBounds.height - strokeWidthPx),
            cornerRadius = CornerRadius((cornerRadius - halfStroke).coerceAtLeast(0f)),
            style = Stroke(width = strokeWidthPx)
        )
    }

    private fun tuneColor(color: Color): Color {
        val argb = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)
        hsl[1] = (hsl[1] * userSettings.saturation).coerceIn(0f, 1f)
        hsl[2] = (hsl[2] * userSettings.lightness).coerceIn(0f, 1f)
        val newColor = Color(ColorUtils.HSLToColor(hsl))
        return newColor.copy(alpha = (color.alpha * userSettings.opacity).coerceIn(0f, 1f))
    }

    private fun blendColors(base: Color, tint: Color, alpha: Float): Color {
        return Color(
            red = base.red * (1f - alpha) + tint.red * alpha,
            green = base.green * (1f - alpha) + tint.green * alpha,
            blue = base.blue * (1f - alpha) + tint.blue * alpha,
            alpha = base.alpha
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
