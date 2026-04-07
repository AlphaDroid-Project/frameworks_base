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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.android.systemui.alpha.style.common.AlphaColorScheme
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.alpha.style.themes.ColorParams
import com.android.systemui.alpha.style.themes.ReflectiveTheme
import com.android.systemui.alpha.style.themes.ReflectiveLightTheme
import com.android.systemui.alpha.style.themes.ReflectiveDarkTheme

class QSReflectiveStyleRenderer(
    private val accentColor: Color,
    private val neutralColor: Color,
    private val isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings
) : QSTileStyleRenderer {

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

    override fun DrawScope.renderTileBackgroundOverlay(
        tileBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        isSmallTile: Boolean,
        density: Density
    ) {
        drawGlassyEffect(tileBounds, shape, density)
    }

    override fun DrawScope.renderIconBackgroundOverlay(
        iconBackgroundBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        density: Density
    ) {
        drawGlassyEffect(iconBackgroundBounds, shape, density)
    }

    private fun DrawScope.drawGlassyEffect(
        bounds: Rect,
        shape: Shape,
        density: Density
    ) {
        val reflectionHeight = bounds.height * theme.reflectionHeight
        val reflectionBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = theme.reflectionAlpha * userSettings.strength),
                Color.White.copy(alpha = 0.0f)
            ),
            startY = bounds.top,
            endY = bounds.top + reflectionHeight
        )

        drawRect(
            brush = reflectionBrush,
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, reflectionHeight)
        )

        val strokeWidth = with(density) { 1.5.dp.toPx() }
        val rimBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = theme.rimAlpha),
                Color.White.copy(alpha = 0.1f),
                Color.Black.copy(alpha = 0.1f)
            ),
            startY = bounds.top,
            endY = bounds.bottom
        )

        drawShapeStroke(
            shape = shape,
            bounds = bounds,
            density = density,
            strokeWidthPx = strokeWidth,
            brush = rimBrush,
        )
    }

    private fun DrawScope.drawShapeStroke(
        shape: Shape,
        bounds: Rect,
        density: Density,
        strokeWidthPx: Float,
        brush: Brush,
    ) {
        if (strokeWidthPx <= 0f) return
        val halfStroke = strokeWidthPx / 2f
        val insetWidth = (bounds.width - strokeWidthPx).coerceAtLeast(0f)
        val insetHeight = (bounds.height - strokeWidthPx).coerceAtLeast(0f)
        if (insetWidth <= 0f || insetHeight <= 0f) return

        val outline =
            shape.createOutline(
                size = Size(insetWidth, insetHeight),
                layoutDirection = LayoutDirection.Ltr,
                density = density,
            )
        translate(left = bounds.left + halfStroke, top = bounds.top + halfStroke) {
            when (outline) {
                is Outline.Rectangle -> {
                    drawRect(
                        brush = brush,
                        topLeft = Offset.Zero,
                        size = outline.rect.size,
                        style = Stroke(width = strokeWidthPx),
                    )
                }
                is Outline.Rounded -> {
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset.Zero,
                        size =
                            Size(
                                width = outline.roundRect.right - outline.roundRect.left,
                                height = outline.roundRect.bottom - outline.roundRect.top,
                            ),
                        cornerRadius = outline.roundRect.topLeftCornerRadius,
                        style = Stroke(width = strokeWidthPx),
                    )
                }
                is Outline.Generic -> {
                    drawPath(path = outline.path, brush = brush, style = Stroke(width = strokeWidthPx))
                }
            }
        }
    }
}
