/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.qs.renderers

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

class QSAerogelStyleRenderer(
    private val accentColor: Color,
    private val neutralColor: Color,
    private val isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings
) : QSTileStyleRenderer {

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

    override fun DrawScope.renderTileBackgroundOverlay(
        tileBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        isSmallTile: Boolean,
        density: Density
    ) {
        drawAerogelEffect(tileBounds, cornerRadius, density, state == TileState.STATE_ACTIVE)
    }

    override fun DrawScope.renderIconBackgroundOverlay(
        iconBackgroundBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        density: Density
    ) {
        drawAerogelEffect(iconBackgroundBounds, cornerRadius, density, state == TileState.STATE_ACTIVE)
    }

    private fun DrawScope.drawAerogelEffect(
        bounds: Rect,
        cornerRadius: Float,
        density: Density,
        isActive: Boolean
    ) {
        val lightColor = if (isDarkTheme) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.5f)

        val overheadGlow = Brush.radialGradient(
            colors = listOf(lightColor, Color.Transparent),
            center = Offset(bounds.center.x, bounds.top),
            radius = bounds.width * 0.9f
        )

        drawRect(
            brush = overheadGlow,
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height)
        )

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
                        cornerRadius = CornerRadius((cornerRadius - halfStroke).coerceAtLeast(0f))
                    )
                )
            }

            drawPath(
                path = rimPath,
                color = Color.White.copy(alpha = 0.2f),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}
