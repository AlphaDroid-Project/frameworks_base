/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.axdynamicbar.shared

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PathIterator
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Punch-hole vs edge-contour cutout classification for Dynamic Bar "collapse to ring".
 *
 * HOLE keeps the existing circle/arc renderer. CONTOUR strokes the open cutout outline.
 */
enum class CutoutKind {
    HOLE,
    CONTOUR,
}

private const val DEFAULT_EDGE_SLOP_PX = 2
private const val DEFAULT_MAX_HOLE_ASPECT = 1.4f
private const val STRIP_STEP_PX = 1f
private const val MIN_CONTOUR_LENGTH_PX = 4f
private const val CORNER_STEP_RAD = 0.15f

fun classifyCutout(
    bounds: Rect,
    screenW: Int,
    screenH: Int,
    edgeSlopPx: Int = DEFAULT_EDGE_SLOP_PX,
    maxHoleAspect: Float = DEFAULT_MAX_HOLE_ASPECT,
): CutoutKind {
    val w = bounds.width().toFloat()
    val h = bounds.height().toFloat()
    if (w <= 0f || h <= 0f || screenW <= 0 || screenH <= 0) return CutoutKind.CONTOUR
    val touchesEdge =
        bounds.top <= edgeSlopPx ||
            bounds.left <= edgeSlopPx ||
            bounds.right >= screenW - edgeSlopPx ||
            bounds.bottom >= screenH - edgeSlopPx
    if (touchesEdge) return CutoutKind.CONTOUR
    val aspect = maxOf(w, h) / minOf(w, h)
    return if (aspect <= maxHoleAspect) CutoutKind.HOLE else CutoutKind.CONTOUR
}

private enum class DisplayEdge {
    TOP,
    RIGHT,
    BOTTOM,
    LEFT,
}

private data class ContourSegment(
    val verb: Int,
    val points: FloatArray,
    val conicWeight: Float = 0f,
) {
    val startX: Float get() = points[0]
    val startY: Float get() = points[1]

    val endX: Float
        get() = when (verb) {
            PathIterator.VERB_LINE -> points[2]
            PathIterator.VERB_QUAD, PathIterator.VERB_CONIC -> points[4]
            PathIterator.VERB_CUBIC -> points[6]
            else -> points[2]
        }

    val endY: Float
        get() = when (verb) {
            PathIterator.VERB_LINE -> points[3]
            PathIterator.VERB_QUAD, PathIterator.VERB_CONIC -> points[5]
            PathIterator.VERB_CUBIC -> points[7]
            else -> points[3]
        }

    val isStraight: Boolean get() = verb == PathIterator.VERB_LINE

    fun appendTo(path: Path) {
        when (verb) {
            PathIterator.VERB_LINE -> path.lineTo(endX, endY)
            PathIterator.VERB_QUAD -> path.quadTo(points[2], points[3], endX, endY)
            PathIterator.VERB_CONIC ->
                path.conicTo(points[2], points[3], endX, endY, conicWeight)
            PathIterator.VERB_CUBIC ->
                path.cubicTo(
                    points[2], points[3], points[4], points[5], endX, endY,
                )
        }
    }
}

/**
 * Returns the physical outline of an edge cutout without its artificial display-edge closure.
 *
 * A display cutout is a closed fill path: the run along the display edge exists only to close the
 * region and is invisible when filled, but a stroke has to drop it. Remove the connected chain of
 * straight segments that lie *along* that edge, so the result is the same hardware boundary as
 * SystemUI fills, open at the display edge. Segments merely touching it with one endpoint are the
 * notch's own walls and must survive, or the contour ends mid-flare.
 */
fun stripDisplayEdge(
    path: Path,
    screenW: Int,
    screenH: Int,
    edgeSlopPx: Int = DEFAULT_EDGE_SLOP_PX,
): Path? {
    if (path.isEmpty || screenW <= 0 || screenH <= 0) return null
    val exact = pathContours(path)
        .mapNotNull { removeEdgeClosure(it, screenW, screenH, edgeSlopPx) }
        .maxByOrNull(::pathLength)
    if (exact != null && pathLength(exact) >= MIN_CONTOUR_LENGTH_PX) return exact

    // Non-standard paths may not expose a straight edge cap. Preserve the previous sampled
    // fallback rather than making collapse-to-ring disappear on those devices.
    return stripDisplayEdgeBySampling(path, screenW, screenH, edgeSlopPx)
}

private fun pathContours(path: Path): List<List<ContourSegment>> {
    val contours = mutableListOf<List<ContourSegment>>()
    var segments = mutableListOf<ContourSegment>()
    var contourStartX = 0f
    var contourStartY = 0f
    var currentX = 0f
    var currentY = 0f
    val points = FloatArray(8)
    val iterator = path.pathIterator

    fun finishContour() {
        if (segments.isNotEmpty()) contours += segments
        segments = mutableListOf()
    }

    while (iterator.hasNext()) {
        when (val verb = iterator.next(points, 0)) {
            PathIterator.VERB_MOVE -> {
                finishContour()
                contourStartX = points[0]
                contourStartY = points[1]
                currentX = contourStartX
                currentY = contourStartY
            }
            PathIterator.VERB_LINE,
            PathIterator.VERB_QUAD,
            PathIterator.VERB_CONIC,
            PathIterator.VERB_CUBIC -> {
                val weight = if (verb == PathIterator.VERB_CONIC) points[6] else 0f
                val segment = ContourSegment(verb, points.copyOf(), weight)
                segments += segment
                currentX = segment.endX
                currentY = segment.endY
            }
            PathIterator.VERB_CLOSE -> {
                if (currentX != contourStartX || currentY != contourStartY) {
                    segments += ContourSegment(
                        PathIterator.VERB_LINE,
                        floatArrayOf(currentX, currentY, contourStartX, contourStartY),
                    )
                }
                currentX = contourStartX
                currentY = contourStartY
            }
        }
    }
    finishContour()
    return contours
}

private fun removeEdgeClosure(
    segments: List<ContourSegment>,
    screenW: Int,
    screenH: Int,
    edgeSlopPx: Int,
): Path? {
    if (segments.size < 2) return null

    val edgeSeed = segments.indices
        .asSequence()
        .filter { segments[it].isStraight }
        .mapNotNull { index ->
            alignedDisplayEdge(segments[index], screenW, screenH, edgeSlopPx)
                ?.let { edge -> Triple(index, edge, segmentLength(segments[index])) }
        }
        .maxByOrNull { it.third }
        ?: return null

    val removed = BooleanArray(segments.size)
    val seedIndex = edgeSeed.first
    val edge = edgeSeed.second
    removed[seedIndex] = true
    var removedCount = 1

    var index = (seedIndex - 1 + segments.size) % segments.size
    while (!removed[index] &&
        isClosureConnector(segments[index], edge, screenW, screenH, edgeSlopPx)
    ) {
        removed[index] = true
        removedCount++
        index = (index - 1 + segments.size) % segments.size
    }

    index = (seedIndex + 1) % segments.size
    while (!removed[index] &&
        isClosureConnector(segments[index], edge, screenW, screenH, edgeSlopPx)
    ) {
        removed[index] = true
        removedCount++
        index = (index + 1) % segments.size
    }

    if (removedCount == segments.size) return null
    val firstKept = segments.indices.firstOrNull { current ->
        !removed[current] && removed[(current - 1 + segments.size) % segments.size]
    } ?: return null

    val out = Path()
    out.moveTo(segments[firstKept].startX, segments[firstKept].startY)
    repeat(segments.size) { offset ->
        val current = (firstKept + offset) % segments.size
        if (!removed[current]) segments[current].appendTo(out)
    }
    return out.takeUnless { it.isEmpty }
}

private fun alignedDisplayEdge(
    segment: ContourSegment,
    screenW: Int,
    screenH: Int,
    slop: Int,
): DisplayEdge? = when {
    segment.startY <= slop && segment.endY <= slop -> DisplayEdge.TOP
    segment.startX >= screenW - slop && segment.endX >= screenW - slop -> DisplayEdge.RIGHT
    segment.startY >= screenH - slop && segment.endY >= screenH - slop -> DisplayEdge.BOTTOM
    segment.startX <= slop && segment.endX <= slop -> DisplayEdge.LEFT
    else -> null
}

private fun isClosureConnector(
    segment: ContourSegment,
    edge: DisplayEdge,
    screenW: Int,
    screenH: Int,
    slop: Int,
): Boolean {
    if (!segment.isStraight) return false
    return alignedDisplayEdge(segment, screenW, screenH, slop) == edge
}

private fun segmentLength(segment: ContourSegment): Float =
    hypot(segment.endX - segment.startX, segment.endY - segment.startY)

private fun stripDisplayEdgeBySampling(
    path: Path,
    screenW: Int,
    screenH: Int,
    edgeSlopPx: Int,
): Path? {
    val measure = PathMeasure(path, false)
    val length = measure.length
    if (length < MIN_CONTOUR_LENGTH_PX) return null

    var bestStart = 0f
    var bestEnd = 0f
    var bestSpan = 0f
    var runStart = -1f
    val pos = FloatArray(2)
    var d = 0f
    while (d <= length) {
        measure.getPosTan(d.coerceAtMost(length), pos, null)
        val onEdge =
            pos[0] <= edgeSlopPx ||
                pos[0] >= screenW - edgeSlopPx ||
                pos[1] <= edgeSlopPx ||
                pos[1] >= screenH - edgeSlopPx
        if (!onEdge) {
            if (runStart < 0f) runStart = d
        } else if (runStart >= 0f) {
            val span = d - runStart
            if (span > bestSpan) {
                bestSpan = span
                bestStart = runStart
                bestEnd = d
            }
            runStart = -1f
        }
        d += STRIP_STEP_PX
    }
    if (runStart >= 0f) {
        val span = length - runStart
        if (span > bestSpan) {
            bestSpan = span
            bestStart = runStart
            bestEnd = length
        }
    }
    if (bestSpan < MIN_CONTOUR_LENGTH_PX) return null
    val out = Path()
    if (!measure.getSegment(bestStart, bestEnd, out, true)) return null
    return out.takeUnless { it.isEmpty }
}

private fun cutoutDisplayEdge(
    bounds: Rect,
    screenW: Int,
    screenH: Int,
    slop: Int,
): DisplayEdge? = when {
    bounds.top <= slop -> DisplayEdge.TOP
    bounds.bottom >= screenH - slop -> DisplayEdge.BOTTOM
    bounds.left <= slop -> DisplayEdge.LEFT
    bounds.right >= screenW - slop -> DisplayEdge.RIGHT
    else -> null
}

/** Scale pivot: the midpoint of the display edge the cutout sits on, else the bounds center. */
fun contourPivot(
    bounds: Rect,
    screenW: Int,
    screenH: Int,
    edgeSlopPx: Int = DEFAULT_EDGE_SLOP_PX,
): PointF {
    val cx = bounds.exactCenterX()
    val cy = bounds.exactCenterY()
    return when (cutoutDisplayEdge(bounds, screenW, screenH, edgeSlopPx)) {
        DisplayEdge.TOP -> PointF(cx, 0f)
        DisplayEdge.BOTTOM -> PointF(cx, screenH.toFloat())
        DisplayEdge.LEFT -> PointF(0f, cy)
        DisplayEdge.RIGHT -> PointF(screenW.toFloat(), cy)
        null -> PointF(cx, cy)
    }
}

/**
 * Widens an open contour by [gapPx] along its own outward normal — a parallel curve, not a scale.
 *
 * Scaling about the edge midpoint spreads the gap radially, so it thins wherever the outline runs
 * along a radius and steps at every tangent break: on grus's notch the border goes 8.25px down the
 * wall and 6.58px the moment the shoulder turns 45 deg. Offsetting each sample along its own normal
 * holds one width the whole way round. Corners are rounded rather than bevelled, matching the
 * round join the contour is stroked with.
 */
fun offsetContourOutward(
    path: Path,
    gapPx: Float,
    bounds: Rect,
    screenW: Int,
    screenH: Int,
    edgeSlopPx: Int = DEFAULT_EDGE_SLOP_PX,
): Path {
    val edge = cutoutDisplayEdge(bounds, screenW, screenH, edgeSlopPx) ?: return path
    if (gapPx <= 0.01f) return path
    val measure = PathMeasure(path, false)
    val length = measure.length
    if (length < MIN_CONTOUR_LENGTH_PX) return path

    val pos = FloatArray(2)
    val tan = FloatArray(2)
    val ptX = ArrayList<Float>()
    val ptY = ArrayList<Float>()
    val nX = ArrayList<Float>()
    val nY = ArrayList<Float>()
    var d = 0f
    while (d <= length) {
        measure.getPosTan(d.coerceAtMost(length), pos, tan)
        val tl = hypot(tan[0], tan[1])
        if (tl > 1e-6f) {
            ptX += pos[0]
            ptY += pos[1]
            nX += -tan[1] / tl
            nY += tan[0] / tl
        }
        d += STRIP_STEP_PX
    }
    if (ptX.size < 2) return path

    // The sample furthest from the display edge settles which side is "out": a cutout can only
    // grow away from the edge it hangs off.
    var far = 0
    for (i in ptX.indices) {
        val better = when (edge) {
            DisplayEdge.TOP -> ptY[i] > ptY[far]
            DisplayEdge.BOTTOM -> ptY[i] < ptY[far]
            DisplayEdge.LEFT -> ptX[i] > ptX[far]
            DisplayEdge.RIGHT -> ptX[i] < ptX[far]
        }
        if (better) far = i
    }
    val outward = when (edge) {
        DisplayEdge.TOP -> nY[far]
        DisplayEdge.BOTTOM -> -nY[far]
        DisplayEdge.LEFT -> nX[far]
        DisplayEdge.RIGHT -> -nX[far]
    }
    val sign = if (outward >= 0f) 1f else -1f

    val out = Path()
    var prevX = 0f
    var prevY = 0f
    for (i in ptX.indices) {
        val ox = sign * nX[i]
        val oy = sign * nY[i]
        if (i == 0) {
            out.moveTo(ptX[i] + ox * gapPx, ptY[i] + oy * gapPx)
        } else {
            val turn = acos((prevX * ox + prevY * oy).coerceIn(-1f, 1f))
            if (turn > CORNER_STEP_RAD) {
                val steps = ceil(turn / CORNER_STEP_RAD).toInt()
                val way = if (prevX * oy - prevY * ox >= 0f) 1f else -1f
                for (k in 1 until steps) {
                    val a = way * turn * k / steps
                    out.lineTo(
                        ptX[i] + (prevX * cos(a) - prevY * sin(a)) * gapPx,
                        ptY[i] + (prevX * sin(a) + prevY * cos(a)) * gapPx,
                    )
                }
            }
            out.lineTo(ptX[i] + ox * gapPx, ptY[i] + oy * gapPx)
        }
        prevX = ox
        prevY = oy
    }
    return out.takeUnless { it.isEmpty } ?: path
}

fun transformContourPath(
    stripped: Path,
    pivot: PointF,
    scaleX: Float,
    scaleY: Float,
    offsetX: Float,
    offsetY: Float,
): Path {
    val out = Path(stripped)
    if (scaleX != 1f || scaleY != 1f) {
        val correction = Matrix()
        correction.postScale(scaleX, scaleY, pivot.x, pivot.y)
        out.transform(correction)
    }

    out.offset(offsetX, offsetY)
    return out
}

/** Distance along [path] of the sample with the largest Y (teardrop tip on a top notch). */
fun contourTipDistance(path: Path, stepPx: Float = STRIP_STEP_PX): Float {
    val measure = PathMeasure(path, false)
    val length = measure.length
    if (length <= 0f) return 0f
    val pos = FloatArray(2)
    var bestD = 0f
    var bestY = Float.NEGATIVE_INFINITY
    var d = 0f
    while (d <= length) {
        measure.getPosTan(d.coerceAtMost(length), pos, null)
        if (pos[1] > bestY) {
            bestY = pos[1]
            bestD = d
        }
        d += stepPx
    }
    return bestD
}

/**
 * Charging fill along an open contour: grows from the tip toward both ends.
 * At [level] = 1 the range is the full length.
 */
fun chargingContourRange(length: Float, tipDistance: Float, level: Float): Pair<Float, Float> {
    if (length <= 0f) return 0f to 0f
    val t = level.coerceIn(0f, 1f)
    val tip = tipDistance.coerceIn(0f, length)
    return (tip * (1f - t)) to (tip + (length - tip) * t)
}

fun pathLength(path: Path): Float = PathMeasure(path, false).length
