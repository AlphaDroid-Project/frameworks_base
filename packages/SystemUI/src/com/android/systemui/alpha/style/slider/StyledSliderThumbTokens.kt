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
 * Stock AOSP uses a 4dp-thick solid accent pill. Styled thumbs instead use a larger
 * neutral translucent capsule with style overlays (bevel, etc.) so the handle reads as
 * a raised glass element sitting on the track — not an accent block.
 *
 * See `uistyles.md` (workspace) for the design log.
 */
object StyledSliderThumbTokens {

    /** Stock AOSP along-track thickness (logical thumb width / height depending on orientation). */
    val StockAlongTrack: Dp = 4.dp

    /**
     * Visual along-track thickness for styled thumbs.
     * Stock is [StockAlongTrack]; styled is larger (4× stock).
     */
    val AlongTrack: Dp = 16.dp

    /**
     * Extra size **beyond** the track on the cross-track axis so the pill overhangs the
     * track segments (stock is 52dp thumb on a ~40dp track → +12dp total).
     * Split equally above and below (or left/right when vertical).
     */
    val CrossTrackOverhang: Dp = 12.dp

    /**
     * Fill alpha for the neutral glass body. Style-parameter opacity still tunes track
     * colours via [produceColorScheme]; the thumb base is intentionally fixed glass so
     * it stays readable and accent-free.
     */
    const val GlassAlphaDark: Float = 0.32f
    const val GlassAlphaLight: Float = 0.52f

    /** Pill shape (full stadium ends). */
    val PillShape: Shape = RoundedCornerShape(percent = 50)

    /** Cross-track visual extent = track thickness + overhang (pill sticks out past segments). */
    fun crossTrack(trackThickness: Dp, scale: Float = 1f): Dp =
        trackThickness + CrossTrackOverhang * scale

    /**
     * Horizontal layout: width = along track, height = track + overhang.
     * Ax QS vertical sliders are drawn as rotated horizontal tracks, so they use this too.
     *
     * @param scale multiplies along-track and overhang (ax vertical track scaling).
     */
    fun pillSizeHorizontal(trackThickness: Dp, scale: Float = 1f): DpSize =
        DpSize(
            width = AlongTrack * scale,
            height = crossTrack(trackThickness, scale),
        )

    /**
     * True vertical layout (volume dialog): width = track + overhang, height = along track.
     */
    fun pillSizeVertical(trackThickness: Dp, scale: Float = 1f): DpSize =
        DpSize(
            width = crossTrack(trackThickness, scale),
            height = AlongTrack * scale,
        )

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
