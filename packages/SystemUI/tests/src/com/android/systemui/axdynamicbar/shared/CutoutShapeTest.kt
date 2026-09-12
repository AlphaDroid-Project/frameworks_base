/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.axdynamicbar.shared

import android.graphics.Path
import android.graphics.PathIterator
import android.graphics.PathMeasure
import android.graphics.Rect
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Punch-hole lock and grus waterdrop geometry for Dynamic Bar contour rings.
 *
 * Feature: db-cutout-contour
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class CutoutShapeTest {

    @Test
    fun punchHoleInteriorSquare_isHole() {
        val bounds = Rect(500, 40, 580, 120)
        assertThat(classifyCutout(bounds, SCREEN_W, SCREEN_H)).isEqualTo(CutoutKind.HOLE)
    }

    @Test
    fun slightlyOvalInteriorHole_isHole() {
        val bounds = Rect(500, 40, 580, 110) // 80 x 70, aspect ~1.14
        assertThat(classifyCutout(bounds, SCREEN_W, SCREEN_H)).isEqualTo(CutoutKind.HOLE)
    }

    @Test
    fun grusTeardropBounds_areContour() {
        val bounds = Rect(453, 0, 627, 81)
        assertThat(classifyCutout(bounds, SCREEN_W, SCREEN_H)).isEqualTo(CutoutKind.CONTOUR)
    }

    @Test
    fun wideInteriorPill_isContour() {
        val bounds = Rect(300, 20, 780, 80) // 480 x 60, aspect 8
        assertThat(classifyCutout(bounds, SCREEN_W, SCREEN_H)).isEqualTo(CutoutKind.CONTOUR)
    }

    @Test
    fun stripDisplayEdge_grusDropsTopBarKeepsTip() {
        val closed = grusCutoutPath()
        val originalLen = pathLength(closed)
        val stripped = stripDisplayEdge(closed, SCREEN_W, SCREEN_H)
        assertThat(stripped).isNotNull()

        val strippedPath = stripped!!
        val strippedLen = pathLength(strippedPath)
        assertThat(strippedLen).isGreaterThan(MIN_NONEMPTY)
        assertThat(strippedLen).isLessThan(originalLen)

        val pos = FloatArray(2)
        val measure = PathMeasure(strippedPath, false)
        var sawTip = false
        var d = 0f
        while (d <= measure.length) {
            measure.getPosTan(d.coerceAtMost(measure.length), pos, null)
            if (pos[1] >= TIP_Y - 2f && kotlin.math.abs(pos[0] - TIP_X) <= 8f) {
                sawTip = true
            }
            d += 1f
        }
        assertThat(sawTip).isTrue()
    }

    @Test
    fun stripDisplayEdge_grusKeepsSideWallsAndDropsOnlyTopBar() {
        val stripped = stripDisplayEdge(grusCutoutPath(), SCREEN_W, SCREEN_H)!!
        val measure = PathMeasure(stripped, false)
        val start = FloatArray(2)
        val end = FloatArray(2)
        measure.getPosTan(0f, start, null)
        measure.getPosTan(measure.length, end, null)

        assertThat(start[0]).isWithin(0.01f).of(ORIGIN_X - 87f)
        assertThat(start[1]).isWithin(0.01f).of(0f)
        assertThat(end[0]).isWithin(0.01f).of(ORIGIN_X + 87f)
        assertThat(end[1]).isWithin(0.01f).of(0f)

        val verbs = mutableListOf<Int>()
        val iterator = stripped.pathIterator
        val points = FloatArray(8)
        while (iterator.hasNext()) verbs += iterator.next(points, 0)
        assertThat(verbs).containsExactly(
            PathIterator.VERB_MOVE,
            PathIterator.VERB_LINE,
            PathIterator.VERB_CUBIC,
            PathIterator.VERB_CUBIC,
            PathIterator.VERB_CUBIC,
            PathIterator.VERB_CUBIC,
            PathIterator.VERB_LINE,
        ).inOrder()
    }

    @Test
    fun transformContourPath_grusDefaultsPreserveNativeOutline() {
        val stripped = stripDisplayEdge(grusCutoutPath(), SCREEN_W, SCREEN_H)!!
        val transformed = transformContourPath(
            stripped = stripped,
            pivot = contourPivot(Rect(453, 0, 627, 81), SCREEN_W, SCREEN_H),
            scaleX = 1f,
            scaleY = 1f,
            offsetX = 0f,
            offsetY = 0f,
        )
        val bounds = RectF()
        transformed.computeBounds(bounds, true)
        assertThat(bounds.left).isWithin(0.01f).of(ORIGIN_X - 87f)
        assertThat(bounds.right).isWithin(0.01f).of(ORIGIN_X + 87f)
        assertThat(bounds.top).isWithin(0.25f).of(0f)
        assertThat(bounds.bottom).isWithin(0.01f).of(TIP_Y)
    }

    @Test
    fun chargingRange_fullLevelCoversWholeContour() {
        val length = 220f
        val tip = 110f
        val (start, end) = chargingContourRange(length, tip, 1f)
        assertThat(start).isEqualTo(0f)
        assertThat(end).isEqualTo(length)
    }

    @Test
    fun chargingRange_zeroLevelIsAPointAtTheTip() {
        val (start, end) = chargingContourRange(220f, 110f, 0f)
        assertThat(start).isEqualTo(110f)
        assertThat(end).isEqualTo(110f)
    }

    @Test
    fun contourTipDistance_grusStrippedIsInterior() {
        val stripped = stripDisplayEdge(grusCutoutPath(), SCREEN_W, SCREEN_H)!!
        val length = pathLength(stripped)
        val tip = contourTipDistance(stripped)
        assertThat(tip).isGreaterThan(0f)
        assertThat(tip).isLessThan(length)
        val (start, end) = chargingContourRange(length, tip, 1f)
        assertThat(start).isEqualTo(0f)
        assertThat(end).isEqualTo(length)
    }

    @Test
    fun contourPivot_grusIsTopCenter() {
        val bounds = Rect(453, 0, 627, 81)
        val pivot = contourPivot(bounds, SCREEN_W, SCREEN_H)
        assertThat(pivot.x).isEqualTo(bounds.exactCenterX())
        assertThat(pivot.y).isEqualTo(0f)
    }

    companion object {
        private const val SCREEN_W = 1080
        private const val SCREEN_H = 2340
        private const val MIN_NONEMPTY = 4f
        private const val TIP_X = 540f
        private const val TIP_Y = 81f
        private const val ORIGIN_X = 540f

        /**
         * `config_mainBuiltInDisplayCutout` from grus, translated from top-center origin
         * to display pixels.
         */
        fun grusCutoutPath(): Path {
            val p = Path()
            fun x(v: Float) = v + ORIGIN_X
            p.moveTo(x(-86.9975334f), 10.7082389f)
            p.cubicTo(
                x(-74.1335099f), 23.7038575f,
                x(-64.104552f), 36.2300641f,
                x(-56.9106598f), 48.2868587f,
            )
            p.cubicTo(
                x(-45.0899716f), 68.1052477f,
                x(-23.8069241f), 81f,
                x(0.002904346f), 81f,
            )
            p.cubicTo(
                x(23.8127328f), 81f,
                x(45.0899716f), 68.1052477f,
                x(56.9164685f), 48.2868587f,
            )
            p.cubicTo(
                x(64.1090408f), 36.2290779f,
                x(74.1379482f), 23.7022572f,
                x(87.0031906f), 10.7063968f,
            )
            p.lineTo(x(87f), 0f)
            p.lineTo(x(-87f), 0f)
            p.close()
            return p
        }
    }
}
