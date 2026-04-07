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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.ColorUtils
import com.android.systemui.alpha.style.common.AlphaColorScheme
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.alpha.style.themes.BevelTheme
import com.android.systemui.alpha.style.themes.BevelLightTheme
import com.android.systemui.alpha.style.themes.BevelDarkTheme
import kotlin.math.cos // Added
import kotlin.math.sin // Added

class QSBevelStyleRenderer(
    private val accentColor: Color,
    private val neutralColor: Color,
    private val isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings
) : QSTileStyleRenderer {

    override val id = "bevel"
    override val name = "Bevel"
    override val blendMode: BlendMode = BlendMode.SrcOver

    private val theme: BevelTheme = if (isDarkTheme) BevelDarkTheme else BevelLightTheme

    override fun produceColorScheme(default: AlphaColorScheme): AlphaColorScheme {
        // Apply user settings to default colors since Bevel doesn't use ColorParams
        return default.copy(
            accent = tuneColor(default.accent),
            neutral = tuneColor(default.neutral),
            neutralVariant = tuneColor(default.neutralVariant),
            thumb = tuneColor(default.thumb)
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

    override fun DrawScope.renderTileBackgroundOverlay(
        tileBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        isSmallTile: Boolean,
        density: Density
    ) {
        drawBevel(
            bounds = tileBounds,
            shape = shape,
            isActive = state == TileState.STATE_ACTIVE,
            density = density
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
        drawBevel(
            bounds = iconBackgroundBounds,
            shape = shape,
            isActive = state == TileState.STATE_ACTIVE,
            density = density
        )
    }

    private fun DrawScope.drawBevel(
        bounds: Rect,
        shape: Shape,
        isActive: Boolean,
        density: Density
    ) {
        val bevelWidthPx = with(density) { theme.bevelWidth.toPx() } * userSettings.strength
        val outlineWidthPx = with(density) { theme.outlineWidth.toPx() }

        val (highlightColor, shadowColor) = if (isActive) {
            theme.activeHighlight to theme.activeShadow
        } else {
            theme.inactiveHighlight to theme.inactiveShadow
        }

        if (bevelWidthPx > 0f && (highlightColor.alpha > 0f || shadowColor.alpha > 0f)) {
            val (bevelStart, bevelEnd) = calculateGradientOffsets(bounds, theme.bevelAngle + userSettings.angle)
            drawShapeStroke(
                shape = shape,
                bounds = bounds,
                density = density,
                strokeWidthPx = bevelWidthPx,
                brush = Brush.linearGradient(
                    colors = listOf(highlightColor, shadowColor),
                    start = bevelStart,
                    end = bevelEnd,
                ),
            )
        }

        val (gradStart, gradEnd) = calculateGradientOffsets(bounds, theme.surfaceGradientAngle + userSettings.angle)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = theme.surfaceGradientAlpha),
                    Color.Black.copy(alpha = theme.surfaceGradientAlpha)
                ),
                start = gradStart,
                end = gradEnd
            ),
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height)
        )

        if (outlineWidthPx > 0f) {
            val halfOutline = outlineWidthPx / 2f
            val outlineAlpha = if (isActive) theme.outlineActiveAlpha else theme.outlineInactiveAlpha

            drawShapeStroke(
                shape = shape,
                bounds = bounds,
                density = density,
                strokeWidthPx = outlineWidthPx,
                color = Color.Black.copy(alpha = outlineAlpha),
            )
        }

        if (isActive) {
            val tintColor = accentColor
            drawRect(
                color = tintColor.copy(alpha = theme.tintAlpha),
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

    private fun DrawScope.drawShapeStroke(
        shape: Shape,
        bounds: Rect,
        density: Density,
        strokeWidthPx: Float,
        color: Color? = null,
        brush: Brush? = null,
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
            when {
                brush != null -> {
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
                color != null -> {
                    when (outline) {
                        is Outline.Rectangle -> {
                            drawRect(
                                color = color,
                                topLeft = Offset.Zero,
                                size = outline.rect.size,
                                style = Stroke(width = strokeWidthPx),
                            )
                        }
                        is Outline.Rounded -> {
                            drawRoundRect(
                                color = color,
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
                            drawPath(path = outline.path, color = color, style = Stroke(width = strokeWidthPx))
                        }
                    }
                }
            }
        }
    }
}
