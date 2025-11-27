/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.brightness.renderers

import android.content.Context
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import com.android.systemui.alpha.style.common.AlphaColorScheme
import com.android.systemui.alpha.style.common.GradientHelper
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.alpha.style.themes.OutlineTheme
import com.android.systemui.alpha.style.themes.OutlineLightTheme
import com.android.systemui.alpha.style.themes.OutlineDarkTheme
import kotlin.math.cos
import kotlin.math.sin

class BSOutlineStyleRenderer(
    private val context: Context,
    private val accentColor: Color,
    private val neutralColor: Color,
    isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings
) : BrightnessSliderStyleRenderer {

    override val id = "outline"
    override val name = "Outline"
    override val blendMode: BlendMode get() = theme.blendMode

    private val theme: OutlineTheme = if (isDarkTheme) OutlineDarkTheme else OutlineLightTheme

    override fun produceColorScheme(default: AlphaColorScheme): AlphaColorScheme {
        return default
    }

    override fun DrawScope.renderActiveSegmentOverlay(
        segmentBounds: Rect,
        shape: Shape,
        cornerRadii: SegmentCornerRadii,
        materialColor: Color,
        density: Density
    ) {
        drawSegmentGradientBorder(
            bounds = segmentBounds,
            cornerRadii = cornerRadii,
            borderStart = theme.activeBorderStart,
            borderEnd = theme.activeBorderEnd,
            tintColor = accentColor,
            tintAlpha = theme.activeTintAlpha,
            density = density
        )
    }

    override fun DrawScope.renderInactiveSegmentOverlay(
        segmentBounds: Rect,
        shape: Shape,
        cornerRadii: SegmentCornerRadii,
        materialColor: Color,
        density: Density
    ) {
        drawSegmentGradientBorder(
            bounds = segmentBounds,
            cornerRadii = cornerRadii,
            borderStart = theme.inactiveBorderStart,
            borderEnd = theme.inactiveBorderEnd,
            tintColor = neutralColor,
            tintAlpha = theme.inactiveTintAlpha,
            density = density
        )
    }

    override fun DrawScope.renderButtonOverlay(
        buttonBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        isActive: Boolean,
        density: Density
    ) {
        drawButtonGradientBorder(
            bounds = buttonBounds,
            cornerRadius = cornerRadius,
            borderStart = if (isActive) theme.activeBorderStart else theme.inactiveBorderStart,
            borderEnd = if (isActive) theme.activeBorderEnd else theme.inactiveBorderEnd,
            tintColor = if (isActive) accentColor else neutralColor,
            tintAlpha = if (isActive) theme.activeTintAlpha else theme.inactiveTintAlpha,
            density = density
        )
    }

    override fun DrawScope.renderThumbOverlay(
        thumbBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        thumbColor: Color,
        density: Density
    ) {
        val strokeWidthPx = with(density) { theme.strokeWidth.toPx() } * userSettings.strength
        val halfStroke = strokeWidthPx / 2f

        var startColor = tuneColor(GradientHelper.paletteColor(context.resources, theme.activeBorderStart))
        var endColor = tuneColor(GradientHelper.paletteColor(context.resources, theme.activeBorderEnd))

        if (theme.activeTintAlpha > 0f) {
            startColor = blendColors(startColor, accentColor, theme.activeTintAlpha)
            endColor = blendColors(endColor, accentColor, theme.activeTintAlpha)
        }

        val (gradStart, gradEnd) = calculateGradientOffsets(thumbBounds, theme.borderGradientAngle + userSettings.angle)
        val gradientBrush = Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = gradStart,
            end = gradEnd
        )

        val innerCornerRadius = (cornerRadius - halfStroke).coerceAtLeast(0f)

        drawRoundRect(
            brush = gradientBrush,
            topLeft = Offset(thumbBounds.left + halfStroke, thumbBounds.top + halfStroke),
            size = Size(thumbBounds.width - strokeWidthPx, thumbBounds.height - strokeWidthPx),
            cornerRadius = CornerRadius(innerCornerRadius),
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
        androidx.core.graphics.ColorUtils.colorToHSL(argb, hsl)
        hsl[1] = (hsl[1] * userSettings.saturation).coerceIn(0f, 1f)
        hsl[2] = (hsl[2] * userSettings.lightness).coerceIn(0f, 1f)
        val newColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
        return newColor.copy(alpha = (color.alpha * userSettings.opacity).coerceIn(0f, 1f))
    }

    private fun DrawScope.drawSegmentGradientBorder(
        bounds: Rect,
        cornerRadii: SegmentCornerRadii,
        borderStart: GradientHelper.PaletteColor,
        borderEnd: GradientHelper.PaletteColor,
        tintColor: Color,
        tintAlpha: Float,
        density: Density
    ) {
        val strokeWidthPx = with(density) { theme.strokeWidth.toPx() } * userSettings.strength
        val halfStroke = strokeWidthPx / 2f

        var startColor = tuneColor(GradientHelper.paletteColor(context.resources, borderStart))
        var endColor = tuneColor(GradientHelper.paletteColor(context.resources, borderEnd))

        if (tintAlpha > 0f) {
            startColor = blendColors(startColor, tintColor, tintAlpha)
            endColor = blendColors(endColor, tintColor, tintAlpha)
        }

        val (gradStart, gradEnd) = calculateGradientOffsets(bounds, theme.borderGradientAngle + userSettings.angle)
        val gradientBrush = Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = gradStart,
            end = gradEnd
        )

        val outlinePath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = bounds.left + halfStroke,
                    top = bounds.top + halfStroke,
                    right = bounds.right - halfStroke,
                    bottom = bounds.bottom - halfStroke,
                    topLeftCornerRadius = CornerRadius((cornerRadii.topLeft - halfStroke).coerceAtLeast(0f)),
                    topRightCornerRadius = CornerRadius((cornerRadii.topRight - halfStroke).coerceAtLeast(0f)),
                    bottomLeftCornerRadius = CornerRadius((cornerRadii.bottomLeft - halfStroke).coerceAtLeast(0f)),
                    bottomRightCornerRadius = CornerRadius((cornerRadii.bottomRight - halfStroke).coerceAtLeast(0f))
                )
            )
        }

        drawPath(
            path = outlinePath,
            brush = gradientBrush,
            style = Stroke(width = strokeWidthPx)
        )
    }

    private fun DrawScope.drawButtonGradientBorder(
        bounds: Rect,
        cornerRadius: Float,
        borderStart: GradientHelper.PaletteColor,
        borderEnd: GradientHelper.PaletteColor,
        tintColor: Color,
        tintAlpha: Float,
        density: Density
    ) {
        val strokeWidthPx = with(density) { theme.strokeWidth.toPx() } * userSettings.strength
        val halfStroke = strokeWidthPx / 2f

        var startColor = tuneColor(GradientHelper.paletteColor(context.resources, borderStart))
        var endColor = tuneColor(GradientHelper.paletteColor(context.resources, borderEnd))

        if (tintAlpha > 0f) {
            startColor = blendColors(startColor, tintColor, tintAlpha)
            endColor = blendColors(endColor, tintColor, tintAlpha)
        }

        val (gradStart, gradEnd) = calculateGradientOffsets(bounds, theme.borderGradientAngle + userSettings.angle)
        val gradientBrush = Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = gradStart,
            end = gradEnd
        )

        drawRoundRect(
            brush = gradientBrush,
            topLeft = Offset(bounds.left + halfStroke, bounds.top + halfStroke),
            size = Size(bounds.width - strokeWidthPx, bounds.height - strokeWidthPx),
            cornerRadius = CornerRadius((cornerRadius - halfStroke).coerceAtLeast(0f)),
            style = Stroke(width = strokeWidthPx)
        )
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

        return Offset(centerX - dx, centerY - dy) to Offset(centerX + dx, centerY + dy)
    }
}