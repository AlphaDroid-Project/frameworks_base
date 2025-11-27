/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.common

import android.content.res.Resources
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Utilities for deriving colors and gradients for custom tile/slider styles.
 *
 * Provides:
 * - Material You palette color resolution (accent1/2/3, neutral1/2)
 * - HSL lightness manipulation
 * - Color blending
 * - Gradient derivation from accent/surface tones
 * - Shape-aware glow radius calculation
 * - Android Canvas Shader creation
 */
object GradientHelper {

    // ========== Material You Color Space Constants ==========

    /**
     * Material You color spaces (palettes).
     */
    object ColorSpace {
        const val ACCENT1 = 1      // Primary accent
        const val ACCENT2 = 2      // Secondary accent
        const val ACCENT3 = 3      // Tertiary accent
        const val NEUTRAL1 = 4     // Cool neutrals
        const val NEUTRAL2 = 5     // Warm neutrals
    }

    /**
     * Material You tonal palette values (0-1000).
     * Only includes values that exist in Android system resources.
     */
    object Tone {
        const val T0 = 0           // Pure black (or white in inverted)
        const val T10 = 10
        const val T50 = 50
        const val T100 = 100
        const val T200 = 200
        const val T300 = 300
        const val T400 = 400
        const val T500 = 500
        const val T600 = 600
        const val T700 = 700
        const val T800 = 800
        const val T900 = 900
        const val T1000 = 1000     // Pure white (or black in inverted)

        /**
         * All valid tone values.
         */
        val ALL = intArrayOf(T0, T10, T50, T100, T200, T300, T400, T500, T600, T700, T800, T900, T1000)

        /**
         * Find nearest valid tone to a given value.
         */
        fun nearest(value: Int): Int {
            return ALL.minByOrNull { kotlin.math.abs(it - value) } ?: T600
        }
    }

    /**
     * Palette color definition for Material You.
     */
    data class PaletteColor(
        val colorSpace: Int,    // ColorSpace.ACCENT1/2/3 or NEUTRAL1/2
        val tone: Int           // Tone.T0 to T1000
    ) {
        init {
            require(colorSpace in 1..5) { "Invalid color space: $colorSpace (must be 1-5)" }
            require(tone in Tone.ALL) { "Invalid tone: $tone (must be one of ${Tone.ALL.contentToString()})" }
        }
    }

    // ========== Palette Color Resolution ==========

    /**
     * Resolves a Material You palette color to actual Color.
     *
     * @param resources Android resources
     * @param colorSpace ColorSpace.ACCENT1/2/3 or NEUTRAL1/2
     * @param tone Tone value (0, 10, 50, 100, 200...1000)
     * @return Resolved color from system palette
     */
    fun paletteColor(resources: Resources, colorSpace: Int, tone: Int): Color {
        val resourceId = getPaletteResourceId(colorSpace, tone)
        return Color(resources.getColor(resourceId, null))
    }

    /**
     * Resolves a PaletteColor to actual Color.
     */
    fun paletteColor(resources: Resources, palette: PaletteColor): Color {
        return paletteColor(resources, palette.colorSpace, palette.tone)
    }

    /**
     * Maps color space and tone to Android resource ID.
     */
    private fun getPaletteResourceId(colorSpace: Int, tone: Int): Int {
        return when (colorSpace) {
            ColorSpace.ACCENT1 -> getAccent1ResourceId(tone)
            ColorSpace.ACCENT2 -> getAccent2ResourceId(tone)
            ColorSpace.ACCENT3 -> getAccent3ResourceId(tone)
            ColorSpace.NEUTRAL1 -> getNeutral1ResourceId(tone)
            ColorSpace.NEUTRAL2 -> getNeutral2ResourceId(tone)
            else -> android.R.color.system_accent1_600 // Fallback
        }
    }

    private fun getAccent1ResourceId(tone: Int): Int = when (tone) {
        Tone.T0 -> android.R.color.system_accent1_0
        Tone.T10 -> android.R.color.system_accent1_10
        Tone.T50 -> android.R.color.system_accent1_50
        Tone.T100 -> android.R.color.system_accent1_100
        Tone.T200 -> android.R.color.system_accent1_200
        Tone.T300 -> android.R.color.system_accent1_300
        Tone.T400 -> android.R.color.system_accent1_400
        Tone.T500 -> android.R.color.system_accent1_500
        Tone.T600 -> android.R.color.system_accent1_600
        Tone.T700 -> android.R.color.system_accent1_700
        Tone.T800 -> android.R.color.system_accent1_800
        Tone.T900 -> android.R.color.system_accent1_900
        Tone.T1000 -> android.R.color.system_accent1_1000
        else -> android.R.color.system_accent1_600
    }

    private fun getAccent2ResourceId(tone: Int): Int = when (tone) {
        Tone.T0 -> android.R.color.system_accent2_0
        Tone.T10 -> android.R.color.system_accent2_10
        Tone.T50 -> android.R.color.system_accent2_50
        Tone.T100 -> android.R.color.system_accent2_100
        Tone.T200 -> android.R.color.system_accent2_200
        Tone.T300 -> android.R.color.system_accent2_300
        Tone.T400 -> android.R.color.system_accent2_400
        Tone.T500 -> android.R.color.system_accent2_500
        Tone.T600 -> android.R.color.system_accent2_600
        Tone.T700 -> android.R.color.system_accent2_700
        Tone.T800 -> android.R.color.system_accent2_800
        Tone.T900 -> android.R.color.system_accent2_900
        Tone.T1000 -> android.R.color.system_accent2_1000
        else -> android.R.color.system_accent2_600
    }

    private fun getAccent3ResourceId(tone: Int): Int = when (tone) {
        Tone.T0 -> android.R.color.system_accent3_0
        Tone.T10 -> android.R.color.system_accent3_10
        Tone.T50 -> android.R.color.system_accent3_50
        Tone.T100 -> android.R.color.system_accent3_100
        Tone.T200 -> android.R.color.system_accent3_200
        Tone.T300 -> android.R.color.system_accent3_300
        Tone.T400 -> android.R.color.system_accent3_400
        Tone.T500 -> android.R.color.system_accent3_500
        Tone.T600 -> android.R.color.system_accent3_600
        Tone.T700 -> android.R.color.system_accent3_700
        Tone.T800 -> android.R.color.system_accent3_800
        Tone.T900 -> android.R.color.system_accent3_900
        Tone.T1000 -> android.R.color.system_accent3_1000
        else -> android.R.color.system_accent3_600
    }

    private fun getNeutral1ResourceId(tone: Int): Int = when (tone) {
        Tone.T0 -> android.R.color.system_neutral1_0
        Tone.T10 -> android.R.color.system_neutral1_10
        Tone.T50 -> android.R.color.system_neutral1_50
        Tone.T100 -> android.R.color.system_neutral1_100
        Tone.T200 -> android.R.color.system_neutral1_200
        Tone.T300 -> android.R.color.system_neutral1_300
        Tone.T400 -> android.R.color.system_neutral1_400
        Tone.T500 -> android.R.color.system_neutral1_500
        Tone.T600 -> android.R.color.system_neutral1_600
        Tone.T700 -> android.R.color.system_neutral1_700
        Tone.T800 -> android.R.color.system_neutral1_800
        Tone.T900 -> android.R.color.system_neutral1_900
        Tone.T1000 -> android.R.color.system_neutral1_1000
        else -> android.R.color.system_neutral1_900
    }

    private fun getNeutral2ResourceId(tone: Int): Int = when (tone) {
        Tone.T0 -> android.R.color.system_neutral2_0
        Tone.T10 -> android.R.color.system_neutral2_10
        Tone.T50 -> android.R.color.system_neutral2_50
        Tone.T100 -> android.R.color.system_neutral2_100
        Tone.T200 -> android.R.color.system_neutral2_200
        Tone.T300 -> android.R.color.system_neutral2_300
        Tone.T400 -> android.R.color.system_neutral2_400
        Tone.T500 -> android.R.color.system_neutral2_500
        Tone.T600 -> android.R.color.system_neutral2_600
        Tone.T700 -> android.R.color.system_neutral2_700
        Tone.T800 -> android.R.color.system_neutral2_800
        Tone.T900 -> android.R.color.system_neutral2_900
        Tone.T1000 -> android.R.color.system_neutral2_1000
        else -> android.R.color.system_neutral2_900
    }

    // ========== Legacy Helper Functions (kept for compatibility) ==========

    /**
     * Resolves a system accent color swatch from Material You palette.
     * @deprecated Use paletteColor(resources, ColorSpace.ACCENT1, tone) instead
     */
    @Deprecated("Use paletteColor() with ColorSpace.ACCENT1",
        ReplaceWith("paletteColor(resources, ColorSpace.ACCENT1, level)"))
    fun systemAccent(resources: Resources, night: Boolean, level: Int = 600): Color {
        return paletteColor(resources, ColorSpace.ACCENT1, Tone.nearest(level))
    }

    /**
     * Resolves a system neutral surface color from Material You palette.
     * @deprecated Use paletteColor(resources, ColorSpace.NEUTRAL2, tone) instead
     */
    @Deprecated("Use paletteColor() with ColorSpace.NEUTRAL2",
        ReplaceWith("paletteColor(resources, ColorSpace.NEUTRAL2, neutral1Level)"))
    fun systemSurface(
        resources: Resources,
        night: Boolean,
        neutral1Level: Int = if (night) 900 else 50
    ): Color {
        return paletteColor(resources, ColorSpace.NEUTRAL2, Tone.nearest(neutral1Level))
    }

    // ========== Color Manipulation ==========

    /**
     * Adjusts a Compose color's lightness in HSL color space.
     */
    fun lightenDarken(color: Color, lightnessDelta: Float): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        hsl[2] = (hsl[2] + lightnessDelta).coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    /**
     * Adjusts an Android int color's lightness in HSL color space.
     */
    fun lightenDarkenInt(color: Int, lightnessDelta: Float): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] = (hsl[2] + lightnessDelta).coerceIn(0f, 1f)
        return ColorUtils.HSLToColor(hsl)
    }

    /**
     * Blends two Compose colors via linear interpolation in ARGB space.
     */
    fun blend(c1: Color, c2: Color, ratio: Float): Color {
        val blended = ColorUtils.blendARGB(c1.toArgb(), c2.toArgb(), ratio.coerceIn(0f, 1f))
        return Color(blended)
    }

    /**
     * Blends two Android int colors via linear interpolation in ARGB space.
     */
    fun blendInt(c1: Int, c2: Int, ratio: Float): Int {
        return ColorUtils.blendARGB(c1, c2, ratio.coerceIn(0f, 1f))
    }

    /**
     * Result of gradient color derivation.
     */
    data class DerivedGradient(val start: Color, val end: Color)

    /**
     * Derives a two-color gradient from an accent color, optionally blending with surface.
     */
    fun deriveGradient(
        accent: Color,
        surface: Color? = null,
        lightenDelta: Float = 0.08f,
        darkenDelta: Float = -0.08f,
        surfaceBlendStart: Float = 0f,
        surfaceBlendEnd: Float = 0.25f,
        alpha: Float = 1f
    ): DerivedGradient {
        var start = lightenDarken(accent, lightenDelta)
        var end = lightenDarken(accent, darkenDelta)

        if (surface != null) {
            start = blend(start, surface, surfaceBlendStart)
            end = blend(end, surface, surfaceBlendEnd)
        }

        start = start.copy(alpha = alpha.coerceIn(0f, 1f))
        end = end.copy(alpha = alpha.coerceIn(0f, 1f))

        return DerivedGradient(start, end)
    }

    /**
     * Computes start and end points for a linear gradient at a given angle.
     */
    fun gradientOffsets(width: Float, height: Float, angleDeg: Float): Pair<Offset, Offset> {
        val angleRad = (angleDeg % 360f) * (PI.toFloat() / 180f)
        val ux = cos(angleRad)
        val uy = sin(angleRad)
        val cx = width / 2f
        val cy = height / 2f
        val halfDiag = hypot(cx, cy)
        val start = Offset(cx - ux * halfDiag, cy - uy * halfDiag)
        val end = Offset(cx + ux * halfDiag, cy + uy * halfDiag)
        return start to end
    }

    /**
     * Calculates appropriate glow radius for a shape.
     */
    fun calculateGlowRadius(size: Size, baseGlowRadius: Float): Float {
        val minDim = min(size.width, size.height)
        val maxDim = max(size.width, size.height)
        val aspectRatio = maxDim / minDim.coerceAtLeast(1f)

        return if (aspectRatio <= 1.5f) {
            minDim / 2f + baseGlowRadius
        } else {
            val extensionFactor = 1f + (aspectRatio - 1.5f) * 0.4f
            (minDim / 2f + baseGlowRadius) * extensionFactor.coerceAtMost(2.5f)
        }
    }

    /**
     * Determines if a shape is approximately circular (aspect ratio ~1:1).
     */
    fun isCircularShape(size: Size, tolerance: Float = 1.3f): Boolean {
        val minDim = min(size.width, size.height)
        val maxDim = max(size.width, size.height)
        val aspectRatio = maxDim / minDim.coerceAtLeast(1f)
        return aspectRatio <= tolerance
    }

    /**
     * Creates a simple two-color linear gradient brush for Compose.
     */
    fun linearBrush(start: Color, end: Color): Brush {
        return Brush.linearGradient(colors = listOf(start, end))
    }

    // ========== Android Canvas Shader Helpers ==========

    /**
     * Derives gradient colors using color theory from a base accent color.
     *
     * @param accentColor Base color (Android int)
     * @param type Gradient type (0=Analogous, 1=Triadic, 2=Complementary, 3=Monochromatic, 4=Split-Complementary)
     * @return Array of derived colors
     */
    fun deriveTheoryBasedColors(accentColor: Int, type: Int): IntArray {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(accentColor, hsl)

        return when (type) {
            0 -> createAnalogousColors(hsl)
            1 -> createTriadicColors(hsl)
            2 -> createComplementaryColors(hsl)
            3 -> createMonochromaticColors(hsl)
            else -> createSplitComplementaryColors(hsl)
        }
    }

    /**
     * Analogous colors: neighboring colors on the color wheel (±30°)
     */
    private fun createAnalogousColors(hsl: FloatArray): IntArray {
        val color1 = ColorUtils.HSLToColor(floatArrayOf((hsl[0] - 30f + 360f) % 360f, hsl[1], hsl[2]))
        val color2 = ColorUtils.HSLToColor(hsl)
        val color3 = ColorUtils.HSLToColor(floatArrayOf((hsl[0] + 30f) % 360f, hsl[1], hsl[2]))
        return intArrayOf(color1, color2, color3)
    }

    /**
     * Triadic colors: evenly spaced around the color wheel (120° apart)
     */
    private fun createTriadicColors(hsl: FloatArray): IntArray {
        val color1 = ColorUtils.HSLToColor(hsl)
        val color2 = ColorUtils.HSLToColor(floatArrayOf((hsl[0] + 120f) % 360f, hsl[1], hsl[2]))
        val color3 = ColorUtils.HSLToColor(floatArrayOf((hsl[0] + 240f) % 360f, hsl[1], hsl[2]))
        return intArrayOf(color1, color2, color3)
    }

    /**
     * Complementary colors: opposite on the color wheel (180°)
     */
    private fun createComplementaryColors(hsl: FloatArray): IntArray {
        val color1 = ColorUtils.HSLToColor(hsl)
        val color2 = ColorUtils.HSLToColor(floatArrayOf((hsl[0] + 180f) % 360f, hsl[1], hsl[2]))
        return intArrayOf(color1, color2)
    }

    /**
     * Monochromatic: same hue, varying lightness
     */
    private fun createMonochromaticColors(hsl: FloatArray): IntArray {
        val color1 = ColorUtils.HSLToColor(floatArrayOf(hsl[0], hsl[1], (hsl[2] * 0.7f).coerceIn(0f, 1f)))
        val color2 = ColorUtils.HSLToColor(hsl)
        val color3 = ColorUtils.HSLToColor(floatArrayOf(hsl[0], hsl[1], (hsl[2] * 1.3f).coerceIn(0f, 1f)))
        return intArrayOf(color1, color2, color3)
    }

    /**
     * Split-complementary: complement + its neighbors (±30°)
     */
    private fun createSplitComplementaryColors(hsl: FloatArray): IntArray {
        val complementHue = (hsl[0] + 180f) % 360f
        val color1 = ColorUtils.HSLToColor(hsl)
        val color2 = ColorUtils.HSLToColor(floatArrayOf((complementHue - 30f + 360f) % 360f, hsl[1], hsl[2]))
        val color3 = ColorUtils.HSLToColor(floatArrayOf((complementHue + 30f) % 360f, hsl[1], hsl[2]))
        return intArrayOf(color1, color2, color3)
    }

    /**
     * Creates a LinearGradient shader for Android Canvas.
     */
    fun createLinearShader(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        colors: IntArray,
        positions: FloatArray? = null
    ): Shader {
        return LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP)
    }

    /**
     * Creates a RadialGradient shader for Android Canvas.
     */
    fun createRadialShader(
        centerX: Float,
        centerY: Float,
        radius: Float,
        colors: IntArray,
        positions: FloatArray? = null
    ): Shader {
        return RadialGradient(centerX, centerY, radius, colors, positions, Shader.TileMode.CLAMP)
    }

    /**
     * Creates a SweepGradient shader for Android Canvas.
     */
    fun createSweepShader(
        centerX: Float,
        centerY: Float,
        colors: IntArray,
        positions: FloatArray? = null
    ): Shader {
        return SweepGradient(centerX, centerY, colors, positions)
    }
}
