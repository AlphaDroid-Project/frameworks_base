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
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.ColorUtils
import com.android.systemui.alpha.style.common.AlphaColorScheme
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.alpha.style.themes.BevelTheme
import com.android.systemui.alpha.style.themes.BevelLightTheme
import com.android.systemui.alpha.style.themes.BevelDarkTheme

class BSBevelStyleRenderer(
    private val accentColor: Color,
    private val neutralColor: Color,
    private val isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings
) : BrightnessSliderStyleRenderer {

    override val id = "bevel"
    override val name = "Bevel"
    override val blendMode: BlendMode = BlendMode.SrcOver

    private val theme: BevelTheme = if (isDarkTheme) BevelDarkTheme else BevelLightTheme

    override fun produceColorScheme(default: AlphaColorScheme): AlphaColorScheme {
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

    override fun DrawScope.renderActiveSegmentOverlay(
        segmentBounds: Rect,
        shape: Shape,
        cornerRadii: SegmentCornerRadii,
        materialColor: Color,
        density: Density
    ) {
        drawSegmentBevel(segmentBounds, cornerRadii, true, density)
    }

    override fun DrawScope.renderInactiveSegmentOverlay(
        segmentBounds: Rect,
        shape: Shape,
        cornerRadii: SegmentCornerRadii,
        materialColor: Color,
        density: Density
    ) {
        drawSegmentBevel(segmentBounds, cornerRadii, false, density)
    }

    override fun DrawScope.renderButtonOverlay(
        buttonBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        isActive: Boolean,
        density: Density
    ) {
        drawButtonBevel(buttonBounds, cornerRadius, isActive, density)
    }

    override fun DrawScope.renderThumbOverlay(
        thumbBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        thumbColor: Color,
        density: Density
    ) {
        val bevelWidthPx = with(density) { theme.bevelWidth.toPx() } * userSettings.strength

        val (highlightColor, shadowColor) = theme.activeHighlight to theme.activeShadow

        if (bevelWidthPx > 0f) {
            val (bevelStart, bevelEnd) = calculateGradientOffsets(thumbBounds, theme.bevelAngle + userSettings.angle)

            val bevelBrush = Brush.linearGradient(
                colors = listOf(highlightColor, shadowColor),
                start = bevelStart,
                end = bevelEnd
            )

            val halfStroke = bevelWidthPx / 2f
            val innerRadius = (cornerRadius - halfStroke).coerceAtLeast(0f)

            drawRoundRect(
                brush = bevelBrush,
                topLeft = Offset(thumbBounds.left + halfStroke, thumbBounds.top + halfStroke),
                size = Size(thumbBounds.width - bevelWidthPx, thumbBounds.height - bevelWidthPx),
                cornerRadius = CornerRadius(innerRadius),
                style = Stroke(width = bevelWidthPx)
            )
        }

        val (gradStart, gradEnd) = calculateGradientOffsets(thumbBounds, theme.surfaceGradientAngle + userSettings.angle)
        val innerCornerRadius = (cornerRadius - bevelWidthPx).coerceAtLeast(0f)

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = theme.surfaceGradientAlpha),
                    Color.Black.copy(alpha = theme.surfaceGradientAlpha)
                ),
                start = gradStart,
                end = gradEnd
            ),
            topLeft = Offset(thumbBounds.left + bevelWidthPx, thumbBounds.top + bevelWidthPx),
            size = Size(thumbBounds.width - bevelWidthPx * 2, thumbBounds.height - bevelWidthPx * 2),
            cornerRadius = CornerRadius(innerCornerRadius)
        )
    }

    private fun DrawScope.drawSegmentBevel(
        bounds: Rect,
        cornerRadii: SegmentCornerRadii,
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
            val borderPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRoundRect(RoundRect(
                    left = bounds.left, top = bounds.top, right = bounds.right, bottom = bounds.bottom,
                    topLeftCornerRadius = CornerRadius(cornerRadii.topLeft),
                    topRightCornerRadius = CornerRadius(cornerRadii.topRight),
                    bottomLeftCornerRadius = CornerRadius(cornerRadii.bottomLeft),
                    bottomRightCornerRadius = CornerRadius(cornerRadii.bottomRight)
                ))
                val innerTL = (cornerRadii.topLeft - bevelWidthPx).coerceAtLeast(0f)
                val innerTR = (cornerRadii.topRight - bevelWidthPx).coerceAtLeast(0f)
                val innerBL = (cornerRadii.bottomLeft - bevelWidthPx).coerceAtLeast(0f)
                val innerBR = (cornerRadii.bottomRight - bevelWidthPx).coerceAtLeast(0f)
                addRoundRect(RoundRect(
                    left = bounds.left + bevelWidthPx, top = bounds.top + bevelWidthPx,
                    right = bounds.right - bevelWidthPx, bottom = bounds.bottom - bevelWidthPx,
                    topLeftCornerRadius = CornerRadius(innerTL),
                    topRightCornerRadius = CornerRadius(innerTR),
                    bottomLeftCornerRadius = CornerRadius(innerBL),
                    bottomRightCornerRadius = CornerRadius(innerBR)
                ))
            }
            val (bevelStart, bevelEnd) = calculateGradientOffsets(bounds, theme.bevelAngle + userSettings.angle)
            drawPath(
                path = borderPath,
                brush = Brush.linearGradient(
                    colors = listOf(highlightColor, shadowColor),
                    start = bevelStart,
                    end = bevelEnd
                )
            )
        }

        val (gradStart, gradEnd) = calculateGradientOffsets(bounds, theme.surfaceGradientAngle + userSettings.angle)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = theme.surfaceGradientAlpha), Color.Black.copy(alpha = theme.surfaceGradientAlpha)),
                start = gradStart, end = gradEnd
            ),
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height)
        )

        if (outlineWidthPx > 0f) {
            val halfOutline = outlineWidthPx / 2f
            val outlineAlpha = if (isActive) theme.outlineActiveAlpha else theme.outlineInactiveAlpha
            val outlinePath = Path().apply {
                addRoundRect(RoundRect(
                    left = bounds.left + halfOutline, top = bounds.top + halfOutline,
                    right = bounds.right - halfOutline, bottom = bounds.bottom - halfOutline,
                    topLeftCornerRadius = CornerRadius((cornerRadii.topLeft - halfOutline).coerceAtLeast(0f)),
                    topRightCornerRadius = CornerRadius((cornerRadii.topRight - halfOutline).coerceAtLeast(0f)),
                    bottomLeftCornerRadius = CornerRadius((cornerRadii.bottomLeft - halfOutline).coerceAtLeast(0f)),
                    bottomRightCornerRadius = CornerRadius((cornerRadii.bottomRight - halfOutline).coerceAtLeast(0f))
                ))
            }
            drawPath(path = outlinePath, color = Color.Black.copy(alpha = outlineAlpha), style = Stroke(width = outlineWidthPx))
        }

        if (isActive) {
            drawRect(
                color = accentColor.copy(alpha = theme.tintAlpha),
                topLeft = Offset(bounds.left, bounds.top),
                size = Size(bounds.width, bounds.height)
            )
        }
    }

    private fun DrawScope.drawButtonBevel(
        bounds: Rect,
        cornerRadius: Float,
        isActive: Boolean,
        density: Density
    ) {
        val bevelWidthPx = with(density) { theme.bevelWidth.toPx() } * userSettings.strength
        val outlineWidthPx = with(density) { theme.outlineWidth.toPx() }
        val (highlightColor, shadowColor) = if (isActive) theme.activeHighlight to theme.activeShadow else theme.inactiveHighlight to theme.inactiveShadow

        if (bevelWidthPx > 0f && (highlightColor.alpha > 0f || shadowColor.alpha > 0f)) {
            val outerRadius = cornerRadius
            val innerRadius = (outerRadius - bevelWidthPx).coerceAtLeast(0f)
            val borderPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRoundRect(RoundRect(
                    left = bounds.left, top = bounds.top, right = bounds.right, bottom = bounds.bottom,
                    cornerRadius = CornerRadius(outerRadius)
                ))
                addRoundRect(RoundRect(
                    left = bounds.left + bevelWidthPx, top = bounds.top + bevelWidthPx,
                    right = bounds.right - bevelWidthPx, bottom = bounds.bottom - bevelWidthPx,
                    cornerRadius = CornerRadius(innerRadius)
                ))
            }
            val (bevelStart, bevelEnd) = calculateGradientOffsets(bounds, theme.bevelAngle + userSettings.angle)
            drawPath(path = borderPath, brush = Brush.linearGradient(colors = listOf(highlightColor, shadowColor), start = bevelStart, end = bevelEnd))
        }

        val (gradStart, gradEnd) = calculateGradientOffsets(bounds, theme.surfaceGradientAngle + userSettings.angle)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = theme.surfaceGradientAlpha), Color.Black.copy(alpha = theme.surfaceGradientAlpha)),
                start = gradStart, end = gradEnd
            ),
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height)
        )

        if (outlineWidthPx > 0f) {
            val halfOutline = outlineWidthPx / 2f
            val outlineAlpha = if (isActive) theme.outlineActiveAlpha else theme.outlineInactiveAlpha
            drawRoundRect(
                color = Color.Black.copy(alpha = outlineAlpha),
                topLeft = Offset(bounds.left + halfOutline, bounds.top + halfOutline),
                size = Size(bounds.width - outlineWidthPx, bounds.height - outlineWidthPx),
                cornerRadius = CornerRadius((cornerRadius - halfOutline).coerceAtLeast(0f)),
                style = Stroke(width = outlineWidthPx)
            )
        }

        if (isActive) {
            drawRect(color = accentColor.copy(alpha = theme.tintAlpha), topLeft = Offset(bounds.left, bounds.top), size = Size(bounds.width, bounds.height))
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
        return Offset(centerX - dx, centerY - dy) to Offset(centerX + dx, centerY + dy)
    }
}