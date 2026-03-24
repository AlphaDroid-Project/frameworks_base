/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.volume

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.systemui.alpha.style.brightness.renderers.BrightnessSliderStyleRenderer
import com.android.systemui.alpha.style.brightness.renderers.SegmentCornerRadii
import kotlin.math.min

@Composable
fun VolumeSliderStyleWrapper(
    renderer: BrightnessSliderStyleRenderer?,
    shape: Shape,
    segmentMode: Boolean,
    isVertical: Boolean,
    isActive: Boolean,
    activeFraction: Float = 0f,
    trackCornerDp: Dp = 0.dp,
    trackInsideCornerDp: Dp = 0.dp,
    thumbGapDp: Dp = 0.dp,
    logicalThumbWidthDp: Dp = 0.dp,
    logicalThumbHeightDp: Dp = 0.dp,
    visualThumbAlongTrackDp: Dp = 0.dp,
    materialColors: VolumeMaterialColors,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (renderer == null) {
        Box(modifier = modifier.clip(shape), content = content)
        return
    }

    val density = LocalDensity.current

    val styledModifier = modifier
        .clip(shape)
        .drawWithContent {
            drawContent()

            if (segmentMode) {
                if (isVertical) {
                    renderVerticalTrackSegments(
                        renderer = renderer,
                        shape = shape,
                        activeFraction = activeFraction,
                        trackCornerDp = trackCornerDp,
                        trackInsideCornerDp = trackInsideCornerDp,
                        thumbGapDp = thumbGapDp,
                        logicalThumbWidthDp = logicalThumbWidthDp,
                        logicalThumbHeightDp = logicalThumbHeightDp,
                        visualThumbAlongTrackDp = visualThumbAlongTrackDp,
                        materialColors = materialColors,
                        density = density,
                    )
                } else {
                    renderHorizontalTrackSegments(
                        renderer = renderer,
                        shape = shape,
                        activeFraction = activeFraction,
                        trackCornerDp = trackCornerDp,
                        trackInsideCornerDp = trackInsideCornerDp,
                        thumbGapDp = thumbGapDp,
                        logicalThumbWidthDp = logicalThumbWidthDp,
                        logicalThumbHeightDp = logicalThumbHeightDp,
                        visualThumbAlongTrackDp = visualThumbAlongTrackDp,
                        materialColors = materialColors,
                        density = density,
                    )
                }
            } else {
                renderButton(
                    renderer = renderer,
                    shape = shape,
                    isActive = isActive,
                    materialColors = materialColors,
                    density = density,
                )
            }
        }

    Box(modifier = styledModifier, content = content)
}

private fun DrawScope.renderHorizontalTrackSegments(
    renderer: BrightnessSliderStyleRenderer,
    shape: Shape,
    activeFraction: Float,
    trackCornerDp: Dp,
    trackInsideCornerDp: Dp,
    thumbGapDp: Dp,
    logicalThumbWidthDp: Dp,
    logicalThumbHeightDp: Dp,
    visualThumbAlongTrackDp: Dp,
    materialColors: VolumeMaterialColors,
    density: Density,
) {
    val fraction = activeFraction.coerceIn(0f, 1f)
    val thumbGapPx = with(density) { thumbGapDp.toPx() }
    val trackCornerPx = with(density) { trackCornerDp.toPx() }
    val trackInsideCornerPx = with(density) { trackInsideCornerDp.toPx() }

    val effectiveThumbPx = with(density) {
        (if (visualThumbAlongTrackDp > 0.dp) visualThumbAlongTrackDp else logicalThumbWidthDp)
            .toPx()
    }

    val thumbHalf = effectiveThumbPx / 2f
    val minThumbCenter = thumbHalf
    val maxThumbCenter = (size.width - thumbHalf).coerceAtLeast(minThumbCenter)
    val unclampedThumbCenter = size.width * fraction
    val thumbCenterX = unclampedThumbCenter.coerceIn(minThumbCenter, maxThumbCenter)

    val activeTrackStart = 0f
    val activeTrackEnd = (thumbCenterX - thumbHalf - thumbGapPx).coerceAtLeast(0f)

    val inactiveTrackStart = (thumbCenterX + thumbHalf + thumbGapPx).coerceAtMost(size.width)
    val inactiveTrackEnd = size.width

    if (activeTrackEnd > activeTrackStart) {
        val activeSegmentBounds = Rect(
            left = activeTrackStart,
            top = 0f,
            right = activeTrackEnd,
            bottom = size.height,
        )

        val activeCornerRadii = SegmentCornerRadii(
            topLeft = trackCornerPx,
            topRight = trackInsideCornerPx,
            bottomLeft = trackCornerPx,
            bottomRight = trackInsideCornerPx,
        )

        with(renderer) {
            renderActiveSegmentOverlay(
                segmentBounds = activeSegmentBounds,
                shape = shape,
                cornerRadii = activeCornerRadii,
                materialColor = materialColors.activeSegment,
                density = density,
            )
        }
    }

    if (inactiveTrackStart < inactiveTrackEnd) {
        val inactiveSegmentBounds = Rect(
            left = inactiveTrackStart,
            top = 0f,
            right = inactiveTrackEnd,
            bottom = size.height,
        )

        val inactiveCornerRadii = SegmentCornerRadii(
            topLeft = trackInsideCornerPx,
            topRight = trackCornerPx,
            bottomLeft = trackInsideCornerPx,
            bottomRight = trackCornerPx,
        )

        with(renderer) {
            renderInactiveSegmentOverlay(
                segmentBounds = inactiveSegmentBounds,
                shape = shape,
                cornerRadii = inactiveCornerRadii,
                materialColor = materialColors.inactiveSegment,
                density = density,
            )
        }
    }
}

private fun DrawScope.renderVerticalTrackSegments(
    renderer: BrightnessSliderStyleRenderer,
    shape: Shape,
    activeFraction: Float,
    trackCornerDp: Dp,
    trackInsideCornerDp: Dp,
    thumbGapDp: Dp,
    logicalThumbWidthDp: Dp,
    logicalThumbHeightDp: Dp,
    visualThumbAlongTrackDp: Dp,
    materialColors: VolumeMaterialColors,
    density: Density,
) {
    val fraction = activeFraction.coerceIn(0f, 1f)
    val thumbGapPx = with(density) { thumbGapDp.toPx() }
    val trackCornerPx = with(density) { trackCornerDp.toPx() }
    val trackInsideCornerPx = with(density) { trackInsideCornerDp.toPx() }

    val effectiveThumbPx = with(density) {
        (if (visualThumbAlongTrackDp > 0.dp) visualThumbAlongTrackDp else logicalThumbHeightDp)
            .toPx()
    }

    val thumbHalf = effectiveThumbPx / 2f
    val minThumbCenter = thumbHalf
    val maxThumbCenter = (size.height - thumbHalf).coerceAtLeast(minThumbCenter)
    val unclampedThumbCenter = size.height * (1f - fraction)
    val thumbCenterY = unclampedThumbCenter.coerceIn(minThumbCenter, maxThumbCenter)

    val inactiveTrackTop = 0f
    val inactiveTrackBottom = (thumbCenterY - thumbHalf - thumbGapPx).coerceAtLeast(0f)

    val activeTrackTop = (thumbCenterY + thumbHalf + thumbGapPx).coerceAtMost(size.height)
    val activeTrackBottom = size.height

    if (inactiveTrackBottom > inactiveTrackTop) {
        val inactiveSegmentBounds = Rect(
            left = 0f,
            top = inactiveTrackTop,
            right = size.width,
            bottom = inactiveTrackBottom,
        )

        val inactiveCornerRadii = SegmentCornerRadii(
            topLeft = trackCornerPx,
            topRight = trackCornerPx,
            bottomLeft = trackInsideCornerPx,
            bottomRight = trackInsideCornerPx,
        )

        with(renderer) {
            renderInactiveSegmentOverlay(
                segmentBounds = inactiveSegmentBounds,
                shape = shape,
                cornerRadii = inactiveCornerRadii,
                materialColor = materialColors.inactiveSegment,
                density = density,
            )
        }
    }

    if (activeTrackBottom > activeTrackTop) {
        val activeSegmentBounds = Rect(
            left = 0f,
            top = activeTrackTop,
            right = size.width,
            bottom = activeTrackBottom,
        )

        val activeCornerRadii = SegmentCornerRadii(
            topLeft = trackInsideCornerPx,
            topRight = trackInsideCornerPx,
            bottomLeft = trackCornerPx,
            bottomRight = trackCornerPx,
        )

        with(renderer) {
            renderActiveSegmentOverlay(
                segmentBounds = activeSegmentBounds,
                shape = shape,
                cornerRadii = activeCornerRadii,
                materialColor = materialColors.activeSegment,
                density = density,
            )
        }
    }
}

private fun DrawScope.renderButton(
    renderer: BrightnessSliderStyleRenderer,
    shape: Shape,
    isActive: Boolean,
    materialColors: VolumeMaterialColors,
    density: Density,
) {
    val buttonBounds = Rect(
        left = 0f,
        top = 0f,
        right = size.width,
        bottom = size.height,
    )

    val cornerRadius = when (shape) {
        CircleShape -> min(size.width, size.height) / 2f
        is RoundedCornerShape -> with(density) { shape.topStart.toPx(Size.Unspecified, this) }
        else -> 0f
    }

    val materialColor = if (isActive) {
        materialColors.activeButton
    } else {
        materialColors.inactiveButton
    }

    with(renderer) {
        renderButtonOverlay(
            buttonBounds = buttonBounds,
            shape = shape,
            cornerRadius = cornerRadius,
            materialColor = materialColor,
            isActive = isActive,
            density = density,
        )
    }
}

data class VolumeMaterialColors(
    val activeSegment: Color,
    val inactiveSegment: Color,
    val activeButton: Color,
    val inactiveButton: Color,
)