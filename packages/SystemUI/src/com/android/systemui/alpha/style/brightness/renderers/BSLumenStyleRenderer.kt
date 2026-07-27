/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.brightness.renderers

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.core.graphics.ColorUtils
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.alpha.style.common.AlphaColorScheme
import com.android.systemui.alpha.style.themes.LumenDarkTheme
import com.android.systemui.alpha.style.themes.LumenLightTheme
import com.android.systemui.alpha.style.themes.LumenTheme

/**
 * Soft outer halo + thin rim light on slider segments, buttons, and thumbs
 * (same language as [com.android.systemui.alpha.style.qs.renderers.QSLumenStyleRenderer]).
 */
class BSLumenStyleRenderer(
    private val accentColor: Color,
    private val neutralColor: Color,
    isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings,
) : BrightnessSliderStyleRenderer {

    override val id = "lumen"
    override val name = "Lumen"
    override val blendMode: BlendMode
        get() = theme.blendMode

    private val theme: LumenTheme = if (isDarkTheme) LumenDarkTheme else LumenLightTheme

    override fun produceColorScheme(default: AlphaColorScheme): AlphaColorScheme {
        return default.copy(
            accent = tuneColor(default.accent, boostSat = 1.08f, boostLight = 1.02f),
            neutral = tuneColor(default.neutral, boostSat = 1f, boostLight = 1f),
            neutralVariant = tuneColor(default.neutralVariant, boostSat = 1f, boostLight = 1f),
            thumb = tuneColor(default.thumb, boostSat = 1.05f, boostLight = 1f),
        )
    }

    override fun DrawScope.renderActiveSegmentOverlay(
        segmentBounds: Rect,
        shape: Shape,
        cornerRadii: SegmentCornerRadii,
        materialColor: Color,
        density: Density,
    ) {
        drawSegmentLumen(segmentBounds, cornerRadii, isActive = true, density = density)
    }

    override fun DrawScope.renderInactiveSegmentOverlay(
        segmentBounds: Rect,
        shape: Shape,
        cornerRadii: SegmentCornerRadii,
        materialColor: Color,
        density: Density,
    ) {
        drawSegmentLumen(segmentBounds, cornerRadii, isActive = false, density = density)
    }

    override fun DrawScope.renderButtonOverlay(
        buttonBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        isActive: Boolean,
        density: Density,
    ) {
        drawRoundLumen(buttonBounds, cornerRadius, isActive, density)
    }

    override fun DrawScope.renderThumbOverlay(
        thumbBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        thumbColor: Color,
        density: Density,
    ) {
        // Thumb is always the "active" glass handle — use active rim/halo.
        drawRoundLumen(thumbBounds, cornerRadius, isActive = true, density = density)
    }

    private fun DrawScope.drawSegmentLumen(
        bounds: Rect,
        cornerRadii: SegmentCornerRadii,
        isActive: Boolean,
        density: Density,
    ) {
        val (haloColor, rimColor, haloWidthPx, rimWidthPx) = lumenStrokeParams(isActive, density)

        if (haloWidthPx > 0f && haloColor.alpha > 0.01f) {
            drawPath(
                path = segmentStrokePath(bounds, cornerRadii, haloWidthPx),
                color = haloColor,
                style = Stroke(width = haloWidthPx),
            )
        }
        if (rimWidthPx > 0f && rimColor.alpha > 0.01f) {
            drawPath(
                path = segmentStrokePath(bounds, cornerRadii, rimWidthPx),
                color = rimColor,
                style = Stroke(width = rimWidthPx),
            )
        }
    }

    private fun DrawScope.drawRoundLumen(
        bounds: Rect,
        cornerRadius: Float,
        isActive: Boolean,
        density: Density,
    ) {
        val (haloColor, rimColor, haloWidthPx, rimWidthPx) = lumenStrokeParams(isActive, density)

        if (haloWidthPx > 0f && haloColor.alpha > 0.01f) {
            val half = haloWidthPx / 2f
            drawRoundRect(
                color = haloColor,
                topLeft = Offset(bounds.left + half, bounds.top + half),
                size = Size(bounds.width - haloWidthPx, bounds.height - haloWidthPx),
                cornerRadius = CornerRadius((cornerRadius - half).coerceAtLeast(0f)),
                style = Stroke(width = haloWidthPx),
            )
        }
        if (rimWidthPx > 0f && rimColor.alpha > 0.01f) {
            val half = rimWidthPx / 2f
            drawRoundRect(
                color = rimColor,
                topLeft = Offset(bounds.left + half, bounds.top + half),
                size = Size(bounds.width - rimWidthPx, bounds.height - rimWidthPx),
                cornerRadius = CornerRadius((cornerRadius - half).coerceAtLeast(0f)),
                style = Stroke(width = rimWidthPx),
            )
        }
    }

    private fun lumenStrokeParams(
        isActive: Boolean,
        density: Density,
    ): LumenStrokeParams {
        val strength = userSettings.strength.coerceIn(0.25f, 1.75f)
        val tint = if (isActive) accentColor else neutralColor
        val mix = if (isActive) theme.activeAccentMix else theme.inactiveAccentMix
        val haloAlpha =
            (if (isActive) theme.activeHaloAlpha else theme.inactiveHaloAlpha) * strength
        val rimAlpha =
            (if (isActive) theme.activeRimAlpha else theme.inactiveRimAlpha) * strength

        val baseLight = Color.White
        val haloColor =
            blendColors(baseLight, tint, mix).copy(alpha = haloAlpha.coerceIn(0f, 1f))
        val rimColor =
            blendColors(baseLight, tint, mix * 0.65f).copy(alpha = rimAlpha.coerceIn(0f, 1f))

        val haloWidthPx = with(density) { theme.haloWidth.toPx() } * strength
        val rimWidthPx = with(density) { theme.rimWidth.toPx() } * strength.coerceAtMost(1.35f)
        return LumenStrokeParams(haloColor, rimColor, haloWidthPx, rimWidthPx)
    }

    private fun segmentStrokePath(
        bounds: Rect,
        cornerRadii: SegmentCornerRadii,
        strokeWidthPx: Float,
    ): Path {
        val half = strokeWidthPx / 2f
        return Path().apply {
            addRoundRect(
                RoundRect(
                    left = bounds.left + half,
                    top = bounds.top + half,
                    right = bounds.right - half,
                    bottom = bounds.bottom - half,
                    topLeftCornerRadius =
                        CornerRadius((cornerRadii.topLeft - half).coerceAtLeast(0f)),
                    topRightCornerRadius =
                        CornerRadius((cornerRadii.topRight - half).coerceAtLeast(0f)),
                    bottomLeftCornerRadius =
                        CornerRadius((cornerRadii.bottomLeft - half).coerceAtLeast(0f)),
                    bottomRightCornerRadius =
                        CornerRadius((cornerRadii.bottomRight - half).coerceAtLeast(0f)),
                )
            )
        }
    }

    private fun blendColors(base: Color, tint: Color, alpha: Float): Color {
        val a = alpha.coerceIn(0f, 1f)
        return Color(
            red = base.red * (1f - a) + tint.red * a,
            green = base.green * (1f - a) + tint.green * a,
            blue = base.blue * (1f - a) + tint.blue * a,
            alpha = 1f,
        )
    }

    private fun tuneColor(color: Color, boostSat: Float, boostLight: Float): Color {
        val argb =
            android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt(),
            )
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)
        hsl[1] = (hsl[1] * boostSat * userSettings.saturation).coerceIn(0f, 1f)
        hsl[2] = (hsl[2] * boostLight * userSettings.lightness).coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(hsl))
            .copy(alpha = (color.alpha * userSettings.opacity).coerceIn(0f, 1f))
    }

    private data class LumenStrokeParams(
        val haloColor: Color,
        val rimColor: Color,
        val haloWidthPx: Float,
        val rimWidthPx: Float,
    )
}
