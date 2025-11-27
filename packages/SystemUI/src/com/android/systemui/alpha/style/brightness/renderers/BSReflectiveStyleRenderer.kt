/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.brightness.renderers

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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.android.systemui.alpha.style.common.AlphaColorScheme
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.alpha.style.themes.ColorParams
import com.android.systemui.alpha.style.themes.ReflectiveTheme
import com.android.systemui.alpha.style.themes.ReflectiveLightTheme
import com.android.systemui.alpha.style.themes.ReflectiveDarkTheme

class BSReflectiveStyleRenderer(
    private val accentColor: Color,
    private val neutralColor: Color,
    private val isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings
) : BrightnessSliderStyleRenderer {

    override val id = "reflective"
    override val name = "Reflective"
    override val blendMode: BlendMode = BlendMode.SrcAtop

    private val theme: ReflectiveTheme = if (isDarkTheme) ReflectiveDarkTheme else ReflectiveLightTheme

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

    override fun DrawScope.renderActiveSegmentOverlay(
        segmentBounds: Rect,
        shape: Shape,
        cornerRadii: SegmentCornerRadii,
        materialColor: Color,
        density: Density
    ) {
        drawGlassyTrack(segmentBounds, cornerRadii, density)
    }

    override fun DrawScope.renderInactiveSegmentOverlay(
        segmentBounds: Rect,
        shape: Shape,
        cornerRadii: SegmentCornerRadii,
        materialColor: Color,
        density: Density
    ) {
        drawGlassyTrack(segmentBounds, cornerRadii, density)
    }

    override fun DrawScope.renderButtonOverlay(
        buttonBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        isActive: Boolean,
        density: Density
    ) {
        val radii = SegmentCornerRadii(cornerRadius, cornerRadius, cornerRadius, cornerRadius)
        drawGlassyTrack(buttonBounds, radii, density)
    }

    override fun DrawScope.renderThumbOverlay(
        thumbBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        thumbColor: Color,
        density: Density
    ) {
        val radii = SegmentCornerRadii(cornerRadius, cornerRadius, cornerRadius, cornerRadius)
        drawGlassyTrack(thumbBounds, radii, density)
    }

    private fun DrawScope.drawGlassyTrack(
        bounds: Rect,
        cornerRadii: SegmentCornerRadii,
        density: Density
    ) {
        // Create shaped path for clipping
        val shapedPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = bounds.left,
                    top = bounds.top,
                    right = bounds.right,
                    bottom = bounds.bottom,
                    topLeftCornerRadius = CornerRadius(cornerRadii.topLeft),
                    topRightCornerRadius = CornerRadius(cornerRadii.topRight),
                    bottomLeftCornerRadius = CornerRadius(cornerRadii.bottomLeft),
                    bottomRightCornerRadius = CornerRadius(cornerRadii.bottomRight)
                )
            )
        }

        // 1.Reflection (Top Half) - clipped to shape
        val reflectionHeight = bounds.height * theme.reflectionHeight
        val reflectionBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = (theme.reflectionAlpha * userSettings.strength).coerceAtMost(1f)),
                Color.White.copy(alpha = 0.0f)
            ),
            startY = bounds.top,
            endY = bounds.top + reflectionHeight
        )

        clipPath(shapedPath) {
            drawRect(
                brush = reflectionBrush,
                topLeft = Offset(bounds.left, bounds.top),
                size = Size(bounds.width, reflectionHeight)
            )
        }

        // 2. Rim Light (Border) - uses cornerRadii
        val strokeWidth = with(density) { 1.5.dp.toPx() }
        val halfStroke = strokeWidth / 2f
        val rimBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = theme.rimAlpha),
                Color.White.copy(alpha = 0.1f),
                Color.Black.copy(alpha = 0.1f)
            ),
            startY = bounds.top,
            endY = bounds.bottom
        )

        val rimPath = Path().apply {
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
            path = rimPath,
            brush = rimBrush,
            style = Stroke(width = strokeWidth)
        )
    }
}