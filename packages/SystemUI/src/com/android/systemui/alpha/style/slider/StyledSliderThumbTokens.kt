/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.slider

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Shared tokens for the UI Styles "glass pill" slider thumb (round 1).
 *
 * Stock AOSP uses a 4dp-thick solid accent pill on a ~40dp track. Styled thumbs instead
 * use a larger neutral translucent capsule whose sizes are **ratios of track thickness**
 * so brightness, volume, and ax QS (any scale) stay balanced.
 *
 * Reference stock (40dp track): along 4dp, cross 52dp → factors 0.1 and 1.3.
 * Styled multiplies the along factor by 4 → 0.4 (16dp on a 40dp track).
 *
 * See `uistyles.md` (workspace) for the design log.
 */
object StyledSliderThumbTokens {

    /** Stock AOSP along-track thickness (logical thumb). */
    val StockAlongTrack: Dp = 4.dp

    /** Reference track thickness used to derive stock ratios (brightness/volume ~40dp). */
    val ReferenceTrackThickness: Dp = 40.dp

    /**
     * Along-track visual size as a fraction of track thickness.
     * Stock: 4/40 = 0.1; styled: 4× stock → **0.4** (16dp on a 40dp track).
     */
    const val AlongTrackFactor: Float = 0.4f

    /**
     * Cross-track visual size as a fraction of track thickness.
     * Stock: 52/40 = **1.3** (overhangs the track).
     */
    const val CrossTrackFactor: Float = 1.3f

    /**
     * Fixed along-track size on the reference track (documentation / fallbacks).
     * Prefer [alongTrack] with the real track thickness.
     */
    val AlongTrack: Dp = ReferenceTrackThickness * AlongTrackFactor // 16.dp

    /**
     * Fixed overhang on the reference track (documentation).
     * Prefer [crossTrack] with the real track thickness.
     */
    val CrossTrackOverhang: Dp =
        ReferenceTrackThickness * (CrossTrackFactor - 1f) // 12.dp

    /**
     * Fill alpha for the neutral glass body. Style-parameter opacity still tunes track
     * colours via [produceColorScheme]; the thumb base is intentionally fixed glass so
     * it stays readable and accent-free.
     */
    const val GlassAlphaDark: Float = 0.32f
    const val GlassAlphaLight: Float = 0.52f

    /** Pill shape (full stadium ends). */
    val PillShape: Shape = RoundedCornerShape(percent = 50)

    /** Visual along-track extent for a given track thickness. */
    fun alongTrack(trackThickness: Dp): Dp = trackThickness * AlongTrackFactor

    /** Cross-track visual extent (track × 1.3 — pill sticks out past segments). */
    fun crossTrack(trackThickness: Dp): Dp = trackThickness * CrossTrackFactor

    /**
     * Horizontal layout: width = along track, height = cross track.
     * Ax QS vertical sliders are drawn as rotated horizontal tracks, so they use this too.
     */
    fun pillSizeHorizontal(trackThickness: Dp): DpSize =
        DpSize(width = alongTrack(trackThickness), height = crossTrack(trackThickness))

    /**
     * True vertical layout (volume dialog): width = cross track, height = along track.
     */
    fun pillSizeVertical(trackThickness: Dp): DpSize =
        DpSize(width = crossTrack(trackThickness), height = alongTrack(trackThickness))

    /**
     * Neutral frosted fill — white glass, no accent. Darker themes use a lighter frost so
     * the bevel still reads; light themes use a stronger frost for contrast on accent track.
     */
    fun glassFill(isDarkTheme: Boolean): Color {
        val alpha = if (isDarkTheme) GlassAlphaDark else GlassAlphaLight
        return Color.White.copy(alpha = alpha)
    }

    fun cornerRadiusPx(pillSize: DpSize, density: androidx.compose.ui.unit.Density): Float {
        val minSide = minOf(pillSize.width, pillSize.height)
        return with(density) { (minSide / 2).toPx() }
    }
}
