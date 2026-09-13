/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.axdynamicbar.ui.compose

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.systemui.alpha.theme.AlphaColors
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.axdynamicbar.shared.AlphaTertiary
import com.android.systemui.axdynamicbar.shared.CutoutKind
import com.android.systemui.axdynamicbar.shared.chargingContourRange
import com.android.systemui.axdynamicbar.shared.contourPivot
import com.android.systemui.axdynamicbar.shared.contourTipDistance
import com.android.systemui.axdynamicbar.shared.transformContourPath
import com.android.systemui.axdynamicbar.ui.AxDynamicBarChipViewModel
import kotlin.math.abs

private const val MEDIA_WINDOW_FRACTION = 0.08f

// The gap slider is a radius ratio, which suits a punch-hole and not an edge cutout: the same
// 0.155 buys ~3px around a 40px hole and 13.5px around grus's 174px-wide notch. Read it as a
// distance for contours, with the slider's default landing on CONTOUR_GAP_DP.
private const val CONTOUR_GAP_DP = 3f
private const val DEFAULT_RING_GAP = 1.155f

/**
 * Picks the punch-hole circle renderer or the edge-cutout contour renderer.
 *
 * HOLE always calls [AnimatedCutoutRing] with scale 1 and offset 0 — the same args the
 * previous call sites passed after baking user scale into [cutoutRectPx].
 */
@Composable
internal fun CutoutCollapsedRingOrContour(
    event: IslandEvent,
    cutoutRectPx: Rect,
    accent: Color,
    progress: Float?,
    ringGap: Float,
    ringOpacity: Int,
    ringStrokeDp: Float,
    containerTopDp: Dp,
    viewModel: AxDynamicBarChipViewModel,
    modifier: Modifier = Modifier,
) {
    val kind by viewModel.cutoutKind.collectAsStateWithLifecycle()
    val contourPath by viewModel.cutoutContourPath.collectAsStateWithLifecycle()
    val physicalRect by viewModel.cutoutRectPx.collectAsStateWithLifecycle()
    val contourScaleX by viewModel.ringScaleX.collectAsStateWithLifecycle()
    val contourScaleY by viewModel.ringScaleY.collectAsStateWithLifecycle()
    val contourOffsetXDp by viewModel.ringOffsetXDp.collectAsStateWithLifecycle()
    val contourOffsetYDp by viewModel.ringOffsetYDp.collectAsStateWithLifecycle()
    val dm = LocalContext.current.resources.displayMetrics

    val path = contourPath
    val physical = physicalRect
    if (kind == CutoutKind.CONTOUR && path != null && !path.isEmpty && physical != null) {
        AnimatedCutoutContour(
            event = event,
            contourPath = path,
            physicalCutoutRectPx = physical,
            accent = accent,
            progress = progress,
            ringGap = ringGap,
            ringScaleX = contourScaleX,
            ringScaleY = contourScaleY,
            ringOffsetXDp = contourOffsetXDp,
            ringOffsetYDp = contourOffsetYDp,
            ringOpacity = ringOpacity,
            ringStrokeDp = ringStrokeDp,
            screenW = dm.widthPixels,
            screenH = dm.heightPixels,
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier.padding(top = containerTopDp)) {
            AnimatedCutoutRing(
                event = event,
                cutoutRectPx = cutoutRectPx,
                accent = accent,
                progress = progress,
                ringGap = ringGap,
                ringScaleX = 1f,
                ringScaleY = 1f,
                ringOffsetXDp = 0f,
                ringOffsetYDp = 0f,
                ringOpacity = ringOpacity,
                ringStrokeDp = ringStrokeDp,
            )
        }
    }
}

@Composable
internal fun AnimatedCutoutContour(
    event: IslandEvent,
    contourPath: Path,
    physicalCutoutRectPx: Rect,
    accent: Color,
    progress: Float?,
    ringGap: Float,
    ringScaleX: Float,
    ringScaleY: Float,
    ringOffsetXDp: Float,
    ringOffsetYDp: Float,
    ringOpacity: Int,
    ringStrokeDp: Float,
    screenW: Int,
    screenH: Int,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val strokePx = with(density) { ringStrokeDp.dp.toPx() }
    val offsetXPx = with(density) { ringOffsetXDp.dp.toPx() }
    val offsetYPx = with(density) { ringOffsetYDp.dp.toPx() }
    val opacityFactor = ringOpacity / 100f

    val pivot = remember(physicalCutoutRectPx, screenW, screenH) {
        contourPivot(physicalCutoutRectPx, screenW, screenH)
    }
    val gapPx = with(density) { CONTOUR_GAP_DP.dp.toPx() } *
        ((ringGap - 1f) / (DEFAULT_RING_GAP - 1f))
    val gapScale = remember(physicalCutoutRectPx, gapPx) {
        val baseRadius =
            maxOf(physicalCutoutRectPx.width(), physicalCutoutRectPx.height()) / 2f
        if (baseRadius <= 0f) 1f else 1f + gapPx / baseRadius
    }
    // Offsets are applied at draw time, not here: bounds are measured off this path and the
    // canvas translate is bounds-relative, so an offset baked in would cancel itself out.
    val transformed = remember(contourPath, pivot, ringScaleX, ringScaleY, gapScale) {
        transformContourPath(
            contourPath, pivot, ringScaleX * gapScale, ringScaleY * gapScale, 0f, 0f,
        )
    }
    val bounds = remember(transformed) {
        RectF().also { transformed.computeBounds(it, true) }
    }
    val pathMeasure = remember(transformed) { PathMeasure(transformed, false) }
    val length = remember(pathMeasure) { pathMeasure.length }
    val tipDistance = remember(transformed) { contourTipDistance(transformed) }
    val segmentPath = remember(transformed) { Path() }
    val strokePaint = remember(strokePx) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    val pad = strokePx / 2f + 2f
    val canvasW = (bounds.width() + pad * 2f + abs(offsetXPx) * 2f).coerceAtLeast(1f)
    val canvasH = (bounds.height() + pad + abs(offsetYPx)).coerceAtLeast(1f)
    val canvasWDp = with(density) { canvasW.toDp() }
    val canvasHDp = with(density) { canvasH.toDp() }

    val mode = ringModeFor(event, progress)

    val chargingLevel =
        if (mode == RingMode.CHARGING_ARC && event is IslandEvent.Charging) {
            event.level / 100f
        } else 0f
    val chargingSweep = remember { Animatable(0f) }
    LaunchedEffect(chargingLevel) {
        chargingSweep.animateTo(
            targetValue = chargingLevel,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        )
    }

    val pulseAlpha = if (mode == RingMode.PULSING) {
        val transition = rememberInfiniteTransition(label = "contour_pulse")
        val alpha by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "contour_pulse_alpha",
        )
        alpha
    } else 1f

    val rotation = if (mode == RingMode.MEDIA_ROTATE) {
        val transition = rememberInfiniteTransition(label = "contour_rotate")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "contour_rotate_frac",
        )
        angle
    } else 0f

    Canvas(modifier = modifier.size(canvasWDp, canvasHDp)) {
        val native = drawContext.canvas.nativeCanvas
        native.save()
        native.translate(
            pad + abs(offsetXPx) - bounds.left + offsetXPx,
            -bounds.top + offsetYPx,
        )

        val effAccent = accent.copy(alpha = accent.alpha * opacityFactor)
        val ringColor = effAccent.copy(alpha = effAccent.alpha * pulseAlpha)

        fun paintFor(color: Color): Paint = strokePaint.apply {
            this.color = color.toArgb()
        }

        fun drawSegment(start: Float, end: Float, color: Color) {
            segmentPath.rewind()
            if (end > start && length > 0f) {
                pathMeasure.getSegment(start, end, segmentPath, true)
                native.drawPath(segmentPath, paintFor(color))
            }
        }

        when (mode) {
            RingMode.STATIC, RingMode.PULSING -> {
                native.drawPath(transformed, paintFor(ringColor))
            }

            RingMode.CHARGING_ARC -> {
                val track = effAccent.copy(alpha = effAccent.alpha * AlphaTertiary)
                native.drawPath(transformed, paintFor(track))
                val (start, end) = chargingContourRange(length, tipDistance, chargingSweep.value)
                drawSegment(start, end, effAccent)
            }

            RingMode.PROGRESS -> {
                val track = effAccent.copy(alpha = effAccent.alpha * AlphaTertiary)
                native.drawPath(transformed, paintFor(track))
                val frac = (progress ?: 0f).coerceIn(0f, 1f)
                drawSegment(0f, frac * length, effAccent)
            }

            RingMode.MEDIA_ROTATE -> {
                native.drawPath(transformed, paintFor(ringColor))
                val contrast = lerp(effAccent, AlphaColors.DbStatusBarChip.ringHighlight, 0.6f)
                val window = (length * MEDIA_WINDOW_FRACTION).coerceAtLeast(1f)
                fun windowAt(frac: Float) {
                    val mid = ((frac % 1f) + 1f) % 1f * length
                    val start = mid - window / 2f
                    val end = mid + window / 2f
                    if (start < 0f) {
                        drawSegment(0f, end.coerceAtLeast(0f), contrast)
                        drawSegment(length + start, length, contrast)
                    } else if (end > length) {
                        drawSegment(start.coerceAtMost(length), length, contrast)
                        drawSegment(0f, end - length, contrast)
                    } else {
                        drawSegment(start, end, contrast)
                    }
                }
                windowAt(rotation)
                windowAt(rotation + 0.5f)
            }
        }
        native.restore()
    }
}
