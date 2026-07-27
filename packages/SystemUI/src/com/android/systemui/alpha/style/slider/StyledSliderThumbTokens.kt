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
 * Styled multiplies the along factor by 2 → 0.2 (8dp on a 40dp track).
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
     * Stock: 4/40 = 0.1; styled: 2× stock → **0.2** (8dp on a 40dp track).
     */
    const val AlongTrackFactor: Float = 0.2f

    /**
     * Cross-track visual size as a fraction of track thickness.
     * Stock: 52/40 = **1.3** (overhangs the track).
     */
    const val CrossTrackFactor: Float = 1.3f

    /**
     * Fixed along-track size on the reference track (documentation / fallbacks).
     * Prefer [alongTrack] with the real track thickness.
     */
    val AlongTrack: Dp = ReferenceTrackThickness * AlongTrackFactor // 8.dp

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

    /**
     * Along-track offset for the **visual** pill center relative to the logical thumb center.
     *
     * Ideal (mid-range): park the pill fully on the active side of the value split so the
     * inactive-facing edge of the visual pill sits on the logical center
     * (`offset = ±visualHalf`).
     *
     * Edges: the old centered-thumb compensation (`±maxEdge` added on top of the bias) was
     * wrong for active-side parking — at min it pushed the pill off-track, at max it pulled
     * the pill back from the end. Instead we **lerp the ideal bias toward a track-safe
     * offset** only on the side that would overflow:
     *
     * - Active toward start (LTR horizontal): at f→0, lerp toward `+(visualHalf−logicalHalf)`
     *   so the pill's outer edge sits on the track start; at mid/max use ideal `−visualHalf`.
     * - Active toward end (vertical reverse): mirror — clamp only as f→1.
     *
     * @param fraction value in 0..1
     * @param visualAlongTrack visual pill extent along the track
     * @param logicalAlongTrack AOSP logical thumb extent along the track (usually 4dp)
     * @param activeTowardStart true when active fill grows toward the track start
     *   (Material horizontal LTR: true → negative X). False when active grows toward the
     *   end (e.g. volume dialog vertical with active below the thumb → positive Y).
     */
    fun visualCenterOffsetAlongTrack(
        fraction: Float,
        visualAlongTrack: Dp,
        logicalAlongTrack: Dp,
        activeTowardStart: Boolean = true,
    ): Dp {
        val f = fraction.coerceIn(0f, 1f)
        val visualHalf = visualAlongTrack / 2
        val logicalHalf = logicalAlongTrack / 2
        // Offset that keeps the visual pill flush with the track edge when the logical
        // thumb is already at its AOSP min/max rest position (~logicalHalf from the edge).
        val trackSafeEdge = (visualHalf - logicalHalf).coerceAtLeast(0.dp)
        val ideal = if (activeTowardStart) -visualHalf else visualHalf
        // Slightly wider blend than before so the min clamp eases in earlier.
        val edgeThreshold = 0.12f

        return if (activeTowardStart) {
            // Only the start overflows with a negative ideal bias.
            if (f < edgeThreshold && trackSafeEdge > 0.dp) {
                val t = f / edgeThreshold // 0 at min → 1 once clear of the edge zone
                lerpDp(trackSafeEdge, ideal, t)
            } else {
                ideal
            }
        } else {
            // Only the end overflows with a positive ideal bias.
            if (f > (1f - edgeThreshold) && trackSafeEdge > 0.dp) {
                val t = (1f - f) / edgeThreshold // 0 at max → 1 once clear of the edge zone
                lerpDp(-trackSafeEdge, ideal, t)
            } else {
                ideal
            }
        }
    }

    private fun lerpDp(from: Dp, to: Dp, t: Float): Dp {
        val x = t.coerceIn(0f, 1f)
        return from + (to - from) * x
    }
}
