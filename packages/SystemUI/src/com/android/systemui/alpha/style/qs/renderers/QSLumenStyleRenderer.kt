/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.qs.renderers

import android.service.quicksettings.Tile
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.core.graphics.ColorUtils
import com.android.systemui.alpha.style.common.AlphaColorScheme
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.alpha.style.themes.LumenDarkTheme
import com.android.systemui.alpha.style.themes.LumenLightTheme
import com.android.systemui.alpha.style.themes.LumenTheme

/**
 * Rim-light + soft outer halo along the real [Shape] outline (path-safe via [drawShapeStroke]).
 */
class QSLumenStyleRenderer(
    private val accentColor: Color,
    private val neutralColor: Color,
    isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings,
) : QSTileStyleRenderer {

    override val id = "lumen"
    override val name = "Lumen"
    override val blendMode: BlendMode
        get() = theme.blendMode

    private val theme: LumenTheme = if (isDarkTheme) LumenDarkTheme else LumenLightTheme

    override fun produceColorScheme(default: AlphaColorScheme): AlphaColorScheme {
        // Mild lift so active tiles stay lively under a light rim; keep fills mostly stock.
        return default.copy(
            accent = tuneColor(default.accent, boostSat = 1.08f, boostLight = 1.02f),
            neutral = tuneColor(default.neutral, boostSat = 1f, boostLight = 1f),
            neutralVariant = tuneColor(default.neutralVariant, boostSat = 1f, boostLight = 1f),
            thumb = tuneColor(default.thumb, boostSat = 1.05f, boostLight = 1f),
        )
    }

    override fun DrawScope.renderTileBackgroundOverlay(
        tileBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        isSmallTile: Boolean,
        density: Density,
    ) {
        drawLumen(tileBounds, shape, state, density)
    }

    override fun DrawScope.renderIconBackgroundOverlay(
        iconBackgroundBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        density: Density,
    ) {
        drawLumen(iconBackgroundBounds, shape, state, density)
    }

    private fun DrawScope.drawLumen(
        bounds: Rect,
        shape: Shape,
        state: Int,
        density: Density,
    ) {
        val isActive = state == Tile.STATE_ACTIVE
        val strength = userSettings.strength.coerceIn(0.25f, 1.75f)
        val unavailable = state == Tile.STATE_UNAVAILABLE
        val alphaScale = if (unavailable) 0.38f else 1f

        val tint = if (isActive) accentColor else neutralColor
        val mix = if (isActive) theme.activeAccentMix else theme.inactiveAccentMix
        val haloAlpha =
            (if (isActive) theme.activeHaloAlpha else theme.inactiveHaloAlpha) *
                strength *
                alphaScale
        val rimAlpha =
            (if (isActive) theme.activeRimAlpha else theme.inactiveRimAlpha) *
                strength *
                alphaScale

        val baseLight = Color.White
        val haloColor =
            blendColors(baseLight, tint, mix).copy(alpha = haloAlpha.coerceIn(0f, 1f))
        val rimColor =
            blendColors(baseLight, tint, mix * 0.65f).copy(alpha = rimAlpha.coerceIn(0f, 1f))

        val haloWidthPx = with(density) { theme.haloWidth.toPx() } * strength
        val rimWidthPx = with(density) { theme.rimWidth.toPx() } * strength.coerceAtMost(1.35f)

        // Outer soft halo — wider stroke, lower alpha, sits outside the hard rim.
        if (haloWidthPx > 0f && haloColor.alpha > 0.01f) {
            drawShapeStroke(
                shape = shape,
                bounds = bounds,
                density = density,
                strokeWidthPx = haloWidthPx,
                color = haloColor,
            )
        }

        // Inner crisp rim light — thin, brighter, follows the same outline.
        if (rimWidthPx > 0f && rimColor.alpha > 0.01f) {
            drawShapeStroke(
                shape = shape,
                bounds = bounds,
                density = density,
                strokeWidthPx = rimWidthPx,
                color = rimColor,
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
}
