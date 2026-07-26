/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.qs.renderers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Strokes along the real [shape] outline (rect, rounded rect, or path).
 * UI styles must use this instead of drawRoundRect so effects follow path-based tile
 * silhouettes (squircle, teardrop, …), not a corner-radius approximation.
 */
internal fun DrawScope.drawShapeStroke(
    shape: Shape,
    bounds: Rect,
    density: Density,
    strokeWidthPx: Float,
    color: Color,
) {
    drawShapeStrokeInternal(
        shape = shape,
        bounds = bounds,
        density = density,
        strokeWidthPx = strokeWidthPx,
        color = color,
        brush = null,
    )
}

internal fun DrawScope.drawShapeStroke(
    shape: Shape,
    bounds: Rect,
    density: Density,
    strokeWidthPx: Float,
    brush: Brush,
) {
    drawShapeStrokeInternal(
        shape = shape,
        bounds = bounds,
        density = density,
        strokeWidthPx = strokeWidthPx,
        color = null,
        brush = brush,
    )
}

private fun DrawScope.drawShapeStrokeInternal(
    shape: Shape,
    bounds: Rect,
    density: Density,
    strokeWidthPx: Float,
    color: Color?,
    brush: Brush?,
) {
    if (strokeWidthPx <= 0f) return
    val halfStroke = strokeWidthPx / 2f
    val insetWidth = (bounds.width - strokeWidthPx).coerceAtLeast(0f)
    val insetHeight = (bounds.height - strokeWidthPx).coerceAtLeast(0f)
    if (insetWidth <= 0f || insetHeight <= 0f) return

    val outline =
        shape.createOutline(
            size = Size(insetWidth, insetHeight),
            layoutDirection = LayoutDirection.Ltr,
            density = density,
        )
    translate(left = bounds.left + halfStroke, top = bounds.top + halfStroke) {
        when (outline) {
            is Outline.Rectangle -> {
                if (brush != null) {
                    drawRect(
                        brush = brush,
                        topLeft = Offset.Zero,
                        size = outline.rect.size,
                        style = Stroke(width = strokeWidthPx),
                    )
                } else if (color != null) {
                    drawRect(
                        color = color,
                        topLeft = Offset.Zero,
                        size = outline.rect.size,
                        style = Stroke(width = strokeWidthPx),
                    )
                }
            }
            is Outline.Rounded -> {
                val size =
                    Size(
                        width = outline.roundRect.right - outline.roundRect.left,
                        height = outline.roundRect.bottom - outline.roundRect.top,
                    )
                val corner = outline.roundRect.topLeftCornerRadius
                if (brush != null) {
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = corner,
                        style = Stroke(width = strokeWidthPx),
                    )
                } else if (color != null) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = corner,
                        style = Stroke(width = strokeWidthPx),
                    )
                }
            }
            is Outline.Generic -> {
                if (brush != null) {
                    drawPath(
                        path = outline.path,
                        brush = brush,
                        style = Stroke(width = strokeWidthPx),
                    )
                } else if (color != null) {
                    drawPath(
                        path = outline.path,
                        color = color,
                        style = Stroke(width = strokeWidthPx),
                    )
                }
            }
        }
    }
}
