/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.qs.renderers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import kotlin.math.tan
import com.android.systemui.alpha.style.common.AlphaColorScheme
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.alpha.style.themes.ColorParams
import com.android.systemui.alpha.style.themes.SlashTheme
import com.android.systemui.alpha.style.themes.SlashLightTheme
import com.android.systemui.alpha.style.themes.SlashDarkTheme

class QSSlashStyleRenderer(
    private val accentColor: Color,
    private val neutralColor: Color,
    private val isDarkTheme: Boolean,
    private val userSettings: UserStyleSettings
) : QSTileStyleRenderer {

    override val id = "slash"
    override val name = "Slash"
    override val blendMode: BlendMode = BlendMode.SrcAtop

    private val theme: SlashTheme = if (isDarkTheme) SlashDarkTheme else SlashLightTheme

    override fun produceColorScheme(default: AlphaColorScheme): AlphaColorScheme {
        val newAccent = applyParams(default.accent, theme.activeParams)
        val newNeutral = applyParams(default.neutral, theme.inactiveParams)
        val newNeutralVariant = applyParams(default.neutralVariant, theme.iconBackgroundParams)
        val newThumb = applyParams(default.accent, theme.thumbParams)

        val newOnAccent = if (theme.activeParams.forceLightContent) Color.White else default.onAccent
        val newOnNeutral = if (theme.inactiveParams.forceLightContent) Color.White else default.onNeutral
        val newOnNeutralVariant = if (theme.iconBackgroundParams.forceLightContent) Color.White else default.onNeutralVariant

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
        drawSlashEffect(tileBounds, density)
    }

    override fun DrawScope.renderIconBackgroundOverlay(
        iconBackgroundBounds: Rect,
        shape: Shape,
        cornerRadius: Float,
        materialColor: Color,
        state: Int,
        density: Density
    ) {
        drawSlashEffect(iconBackgroundBounds, density)
    }

    private fun DrawScope.drawSlashEffect(
        bounds: Rect,
        density: Density
    ) {
        val radians = Math.toRadians(theme.slashAngle.toDouble() + userSettings.angle)
        val xOffset = (bounds.height / tan(radians)).toFloat()

        val topX = bounds.left + (bounds.width * 0.65f)
        val bottomX = topX - xOffset

        val slashPath = Path().apply {
            moveTo(topX, bounds.top)
            lineTo(bounds.right, bounds.top)
            lineTo(bounds.right, bounds.bottom)
            lineTo(bottomX, bounds.bottom)
            close()
        }

        drawPath(
            path = slashPath,
            color = Color.Black.copy(alpha = theme.slashAlpha * userSettings.strength)
        )

        val strokeWidth = with(density) { 1.5.dp.toPx() }
        drawLine(
            color = Color.White.copy(alpha = theme.cutLineAlpha),
            start = Offset(topX, bounds.top),
            end = Offset(bottomX, bounds.bottom),
            strokeWidth = strokeWidth
        )
    }
}
