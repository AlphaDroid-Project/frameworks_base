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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.android.systemui.alpha.style.common.AlphaColorScheme
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.alpha.style.themes.ColorParams
import com.android.systemui.alpha.style.themes.AerogelTheme
import com.android.systemui.alpha.style.themes.AerogelLightTheme
import com.android.systemui.alpha.style.themes.AerogelDarkTheme

class BSAerogelStyleRenderer(
    private val accentColor: Color,
    private val neutralColor: Color,
    private val isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings
) : BrightnessSliderStyleRenderer {

    override val id = "aerogel"
    override val name = "Aerogel"
    override val blendMode: BlendMode = BlendMode.SrcAtop

    private val theme: AerogelTheme = if (isDarkTheme) AerogelDarkTheme else AerogelLightTheme

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
        drawAerogelEffect(segmentBounds, cornerRadii, density, true)
    }

    override fun DrawScope.renderInactiveSegmentOverlay(
        segmentBounds: Rect,
        shape: Shape,
        cornerRadii: SegmentCornerRadii,
        materialColor: Color,
        density: Density
    ) {
        drawAerogelEffect(segmentBounds, cornerRadii, density, false)
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
        drawAerogelEffect(buttonBounds, radii, density, isActive)
    }

    override fun DrawScope.renderThumbOverlay(
        thumbBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        thumbColor: Color,
        density: Density
    ) {
        val radii = SegmentCornerRadii(cornerRadius, cornerRadius, cornerRadius, cornerRadius)
        drawAerogelEffect(thumbBounds, radii, density, isActive = true)
    }

    private fun DrawScope.drawAerogelEffect(
        bounds: Rect,
        cornerRadii: SegmentCornerRadii,
        density: Density,
        isActive: Boolean
    ) {
        val baseAlpha = if (isDarkTheme) 0.25f else 0.5f
        val lightColor = Color.White.copy(alpha = baseAlpha * userSettings.strength)

        // Overhead glow (radial gradient from top center)
        val overheadGlow = Brush.radialGradient(
            colors = listOf(lightColor, Color.Transparent),
            center = Offset(bounds.center.x, bounds.top),
            radius = bounds.width * 0.9f
        )

        // Draw glow into shaped path instead of rect
        val glowPath = Path().apply {
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

        drawPath(
            path = glowPath,
            brush = overheadGlow
        )

        // Rim highlight on active elements
        if (isActive) {
            val strokeWidth = with(density) { 1.dp.toPx() }
            val halfStroke = strokeWidth / 2f

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
                color = Color.White.copy(alpha = 0.2f * userSettings.strength),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}