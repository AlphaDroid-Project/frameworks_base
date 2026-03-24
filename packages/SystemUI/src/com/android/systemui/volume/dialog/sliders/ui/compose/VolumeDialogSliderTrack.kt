/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.volume.dialog.sliders.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import com.android.systemui.alpha.style.brightness.renderers.BrightnessSliderStyleRenderer
import com.android.systemui.alpha.style.volume.VolumeMaterialColors
import com.android.systemui.alpha.style.volume.VolumeSliderStyleWrapper
import com.android.systemui.volume.dialog.sliders.ui.VolumeDialogSliderDimensions
import com.android.systemui.volume.dialog.ui.utils.VOLUME_SLIDER_SHAPE_DEFAULT
import com.android.systemui.volume.dialog.ui.utils.getVolumeTrackCornerDpForMode
import com.android.systemui.volume.dialog.ui.utils.getVolumeTrackInsideCornerDpForMode
import kotlin.math.min

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun SliderTrack(
    sliderState: SliderState,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    colors: SliderColors = SliderDefaults.colors(),
    thumbTrackGapSize: Dp = 6.dp,
    thumbIconGapSize: Dp = thumbTrackGapSize + 4.dp,
    trackCornerSize: Dp = 12.dp,
    trackInsideCornerSize: Dp = 2.dp,
    trackSize: Dp = VolumeDialogSliderDimensions.TrackThickness,
    isVertical: Boolean = false,
    styleRenderer: BrightnessSliderStyleRenderer? = null,
    shapeMode: Int = VOLUME_SLIDER_SHAPE_DEFAULT,
    activeTrackStartIcon: (@Composable BoxScope.(iconsState: SliderIconsState) -> Unit)? = null,
    activeTrackEndIcon: (@Composable BoxScope.(iconsState: SliderIconsState) -> Unit)? = null,
    inactiveTrackStartIcon: (@Composable BoxScope.(iconsState: SliderIconsState) -> Unit)? = null,
    inactiveTrackEndIcon: (@Composable BoxScope.(iconsState: SliderIconsState) -> Unit)? = null,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val effectiveTrackCornerSize = remember(shapeMode, trackSize, trackCornerSize) {
        getVolumeTrackCornerDpForMode(
            shapeMode = shapeMode,
            trackSize = trackSize,
            defaultCorner = trackCornerSize,
        )
    }
    val effectiveTrackInsideCornerSize = remember(shapeMode, trackInsideCornerSize) {
        getVolumeTrackInsideCornerDpForMode(
            shapeMode = shapeMode,
            defaultInsideCorner = trackInsideCornerSize,
        )
    }
    val useStyledVisualThumb = styleRenderer != null || shapeMode != VOLUME_SLIDER_SHAPE_DEFAULT

    val effectiveThumbAlongTrack = if (useStyledVisualThumb) {
        VolumeDialogSliderDimensions.StyledVisualThumbSize
    } else {
        if (isVertical) {
            VolumeDialogSliderDimensions.VerticalLogicalThumbSize.height
        } else {
            VolumeDialogSliderDimensions.HorizontalLogicalThumbSize.width
        }
    }

    val measurePolicy = remember(
        sliderState,
        isRtl,
        isVertical,
        thumbIconGapSize,
        effectiveThumbAlongTrack,
    ) {
        TrackMeasurePolicy(
            sliderState = sliderState,
            shouldMirrorIcons = (!isVertical && isRtl) || isVertical,
            isVertical = isVertical,
            gapSize = thumbIconGapSize,
            effectiveThumbAlongTrack = effectiveThumbAlongTrack,
        )
    }

    Layout(
        measurePolicy = measurePolicy,
        content = {
            val logicalThumbSize = if (isVertical) {
                VolumeDialogSliderDimensions.VerticalLogicalThumbSize
            } else {
                VolumeDialogSliderDimensions.HorizontalLogicalThumbSize
            }

            VolumeSliderStyleWrapper(
                renderer = styleRenderer,
                shape = RoundedCornerShape(effectiveTrackCornerSize),
                segmentMode = true,
                isVertical = isVertical,
                isActive = false,
                activeFraction = sliderState.coercedValueAsFraction,
                trackCornerDp = effectiveTrackCornerSize,
                trackInsideCornerDp = if (styleRenderer == null) {
                    effectiveTrackInsideCornerSize
                } else {
                    0.dp
                },
                thumbGapDp = if (styleRenderer == null) thumbTrackGapSize else 0.dp,
                logicalThumbWidthDp = logicalThumbSize.width,
                logicalThumbHeightDp = logicalThumbSize.height,
                materialColors = VolumeMaterialColors(
                    activeSegment = colors.activeTrackColor,
                    inactiveSegment = colors.inactiveTrackColor,
                    activeButton = colors.activeTrackColor,
                    inactiveButton = colors.inactiveTrackColor,
                ),
                modifier = Modifier
                    .then(
                        if (isVertical) {
                            Modifier.width(trackSize)
                        } else {
                            Modifier.height(trackSize)
                        },
                    )
                    .layoutId(Contents.Track),
            ) {
                if (styleRenderer == null) {
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        colors = colors,
                        enabled = isEnabled,
                        trackCornerSize = effectiveTrackCornerSize,
                        trackInsideCornerSize = effectiveTrackInsideCornerSize,
                        drawStopIndicator = null,
                        thumbTrackGapSize = thumbTrackGapSize,
                        drawTick = { _, _ -> },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cornerPx = effectiveTrackCornerSize.toPx()
                        val innerCornerPx = 0f

                        if (isVertical) {
                            val fraction = sliderState.coercedValueAsFraction.coerceIn(0f, 1f)
                            val thumbCenterY = size.height * (1f - fraction)
                            val thumbHalf =
                                VolumeDialogSliderDimensions.VerticalLogicalThumbSize.height.toPx() /
                                    2f

                            val inactiveTop = 0f
                            val inactiveBottom = (thumbCenterY - thumbHalf).coerceIn(0f, size.height)

                            val activeTop = (thumbCenterY + thumbHalf).coerceIn(0f, size.height)
                            val activeBottom = size.height

                            drawVerticalInactiveTrackSegment(
                                top = inactiveTop,
                                bottom = inactiveBottom,
                                width = size.width,
                                color = colors.inactiveTrackColor,
                                outerCornerPx = cornerPx,
                                innerCornerPx = innerCornerPx,
                            )

                            drawVerticalActiveTrackSegment(
                                top = activeTop,
                                bottom = activeBottom,
                                width = size.width,
                                color = colors.activeTrackColor,
                                outerCornerPx = cornerPx,
                                innerCornerPx = innerCornerPx,
                            )
                        } else {
                            val fraction = sliderState.coercedValueAsFraction.coerceIn(0f, 1f)
                            val thumbCenterX = size.width * fraction
                            val thumbHalf =
                                VolumeDialogSliderDimensions.HorizontalLogicalThumbSize.width.toPx() /
                                    2f

                            val activeStart = 0f
                            val activeEnd = (thumbCenterX - thumbHalf).coerceIn(0f, size.width)

                            val inactiveStart = (thumbCenterX + thumbHalf).coerceIn(0f, size.width)
                            val inactiveEnd = size.width

                            drawHorizontalActiveTrackSegment(
                                left = activeStart,
                                right = activeEnd,
                                height = size.height,
                                color = colors.activeTrackColor,
                                outerCornerPx = cornerPx,
                                innerCornerPx = innerCornerPx,
                            )

                            drawHorizontalInactiveTrackSegment(
                                left = inactiveStart,
                                right = inactiveEnd,
                                height = size.height,
                                color = colors.inactiveTrackColor,
                                outerCornerPx = cornerPx,
                                innerCornerPx = innerCornerPx,
                            )
                        }
                    }
                }
            }

            TrackIcon(
                icon = activeTrackStartIcon,
                contents = Contents.Active.TrackStartIcon,
                isEnabled = isEnabled,
                colors = colors,
                trackMeasurePolicy = measurePolicy,
            )
            TrackIcon(
                icon = activeTrackEndIcon,
                contents = Contents.Active.TrackEndIcon,
                isEnabled = isEnabled,
                colors = colors,
                trackMeasurePolicy = measurePolicy,
            )
            TrackIcon(
                icon = inactiveTrackStartIcon,
                contents = Contents.Inactive.TrackStartIcon,
                isEnabled = isEnabled,
                colors = colors,
                trackMeasurePolicy = measurePolicy,
            )
            TrackIcon(
                icon = inactiveTrackEndIcon,
                contents = Contents.Inactive.TrackEndIcon,
                isEnabled = isEnabled,
                colors = colors,
                trackMeasurePolicy = measurePolicy,
            )
        },
        modifier = modifier,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHorizontalActiveTrackSegment(
    left: Float,
    right: Float,
    height: Float,
    color: androidx.compose.ui.graphics.Color,
    outerCornerPx: Float,
    innerCornerPx: Float,
) {
    if (right <= left) return

    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = 0f,
                right = right,
                bottom = height,
                topLeftCornerRadius = CornerRadius(outerCornerPx),
                topRightCornerRadius = CornerRadius(innerCornerPx),
                bottomLeftCornerRadius = CornerRadius(outerCornerPx),
                bottomRightCornerRadius = CornerRadius(innerCornerPx),
            ),
        )
    }
    drawPath(path, color)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHorizontalInactiveTrackSegment(
    left: Float,
    right: Float,
    height: Float,
    color: androidx.compose.ui.graphics.Color,
    outerCornerPx: Float,
    innerCornerPx: Float,
) {
    if (right <= left) return

    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = 0f,
                right = right,
                bottom = height,
                topLeftCornerRadius = CornerRadius(innerCornerPx),
                topRightCornerRadius = CornerRadius(outerCornerPx),
                bottomLeftCornerRadius = CornerRadius(innerCornerPx),
                bottomRightCornerRadius = CornerRadius(outerCornerPx),
            ),
        )
    }
    drawPath(path, color)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVerticalActiveTrackSegment(
    top: Float,
    bottom: Float,
    width: Float,
    color: androidx.compose.ui.graphics.Color,
    outerCornerPx: Float,
    innerCornerPx: Float,
) {
    if (bottom <= top) return

    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = 0f,
                top = top,
                right = width,
                bottom = bottom,
                topLeftCornerRadius = CornerRadius(innerCornerPx),
                topRightCornerRadius = CornerRadius(innerCornerPx),
                bottomLeftCornerRadius = CornerRadius(outerCornerPx),
                bottomRightCornerRadius = CornerRadius(outerCornerPx),
            ),
        )
    }
    drawPath(path, color)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVerticalInactiveTrackSegment(
    top: Float,
    bottom: Float,
    width: Float,
    color: androidx.compose.ui.graphics.Color,
    outerCornerPx: Float,
    innerCornerPx: Float,
) {
    if (bottom <= top) return

    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = 0f,
                top = top,
                right = width,
                bottom = bottom,
                topLeftCornerRadius = CornerRadius(outerCornerPx),
                topRightCornerRadius = CornerRadius(outerCornerPx),
                bottomLeftCornerRadius = CornerRadius(innerCornerPx),
                bottomRightCornerRadius = CornerRadius(innerCornerPx),
            ),
        )
    }
    drawPath(path, color)
}

@Composable
private fun TrackIcon(
    icon: (@Composable BoxScope.(sliderIconsState: SliderIconsState) -> Unit)?,
    isEnabled: Boolean,
    contents: Contents,
    trackMeasurePolicy: TrackMeasurePolicy,
    colors: SliderColors,
    modifier: Modifier = Modifier,
) {
    icon ?: return

    val iconColor = when (contents) {
        is Contents.Inactive ->
            if (isEnabled) colors.inactiveTickColor else colors.disabledInactiveTickColor
        is Contents.Active ->
            if (isEnabled) colors.activeTickColor else colors.disabledActiveTickColor
        is Contents.Track -> error("$contents is unsupported by the TrackIcon")
    }

    Box(modifier = modifier.layoutId(contents).fillMaxSize()) {
        if (trackMeasurePolicy.isVisible(contents) != null) {
            CompositionLocalProvider(LocalContentColor provides iconColor) {
                icon(trackMeasurePolicy)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class TrackMeasurePolicy(
    private val sliderState: SliderState,
    private val shouldMirrorIcons: Boolean,
    private val gapSize: Dp,
    private val isVertical: Boolean,
    private val effectiveThumbAlongTrack: Dp,
) : MeasurePolicy, SliderIconsState {

    private val isVisible: Map<Contents, MutableState<Boolean?>> = mutableMapOf(
        Contents.Active.TrackStartIcon to mutableStateOf(null),
        Contents.Active.TrackEndIcon to mutableStateOf(null),
        Contents.Inactive.TrackStartIcon to mutableStateOf(null),
        Contents.Inactive.TrackEndIcon to mutableStateOf(null),
    )

    fun isVisible(contents: Contents): Boolean? = isVisible.getValue(contents.resolve()).value

    override val isActiveTrackStartIconVisible: Boolean
        get() = isVisible(Contents.Active.TrackStartIcon)!!

    override val isActiveTrackEndIconVisible: Boolean
        get() = isVisible(Contents.Active.TrackEndIcon)!!

    override val isInactiveTrackStartIconVisible: Boolean
        get() = isVisible(Contents.Inactive.TrackStartIcon)!!

    override val isInactiveTrackEndIconVisible: Boolean
        get() = isVisible(Contents.Inactive.TrackEndIcon)!!

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val track = measurables.fastFirst { it.layoutId == Contents.Track }.measure(constraints)

        val iconSize = min(track.width, track.height)
        val iconConstraints = constraints.copy(maxWidth = iconSize, maxHeight = iconSize)

        val components = buildMap {
            put(Contents.Track, track)
            for (measurable in measurables) {
                if (measurable.layoutId != Contents.Track) {
                    put(
                        (measurable.layoutId as Contents).resolve(),
                        measurable.measure(iconConstraints),
                    )
                }
            }
        }

        return layout(track.width, track.height) {
            val gapSizePx = gapSize.roundToPx()
            val coercedValueAsFraction = if (shouldMirrorIcons) {
                1f - sliderState.coercedValueAsFraction
            } else {
                sliderState.coercedValueAsFraction
            }

            val containerDimension = if (isVertical) track.height else track.width
            val thumbSizePx = effectiveThumbAlongTrack.roundToPx().toFloat()
            val thumbHalfPx = thumbSizePx / 2f

            val minThumbCenter = thumbHalfPx
            val maxThumbCenter = (containerDimension.toFloat() - thumbHalfPx).coerceAtLeast(minThumbCenter)
            val unclampedThumbCenter = containerDimension * coercedValueAsFraction
            val thumbCenter = unclampedThumbCenter.coerceIn(minThumbCenter, maxThumbCenter)

            val thumbStart = thumbCenter - thumbHalfPx
            val thumbEnd = thumbCenter + thumbHalfPx

            for (iconLayoutId in components.keys) {
                val placeable = components.getValue(iconLayoutId)

                val placeableDimension = if (isVertical) {
                    placeable.height
                } else {
                    placeable.width
                }

                val position = iconLayoutId.calculatePosition(
                    placeableDimension = placeableDimension,
                    containerDimension = containerDimension,
                    gapSize = gapSizePx,
                    thumbStart = thumbStart,
                    thumbEnd = thumbEnd,
                )

                if (isVertical) {
                    placeable.place(0, position)
                } else {
                    placeable.place(position, 0)
                }

                if (iconLayoutId != Contents.Track) {
                    val visible = iconLayoutId.isVisible(
                        placeableDimension = placeableDimension,
                        containerDimension = containerDimension,
                        gapSize = gapSizePx,
                        thumbStart = thumbStart,
                        thumbEnd = thumbEnd,
                    )
                    isVisible.getValue(iconLayoutId).value = visible
                }
            }
        }
    }

    private fun Contents.resolve(): Contents {
        return if (shouldMirrorIcons) mirrored else this
    }
}

private sealed interface Contents {

    data object Track : Contents {
        override val mirrored: Contents
            get() = error("unsupported for Track")

        override fun calculatePosition(
            placeableDimension: Int,
            containerDimension: Int,
            gapSize: Int,
            thumbStart: Float,
            thumbEnd: Float,
        ): Int = 0

        override fun isVisible(
            placeableDimension: Int,
            containerDimension: Int,
            gapSize: Int,
            thumbStart: Float,
            thumbEnd: Float,
        ): Boolean = true
    }

    interface Active : Contents {
        override fun isVisible(
            placeableDimension: Int,
            containerDimension: Int,
            gapSize: Int,
            thumbStart: Float,
            thumbEnd: Float,
        ): Boolean = thumbStart - gapSize > placeableDimension

        data object TrackStartIcon : Active {
            override val mirrored: Contents
                get() = Inactive.TrackEndIcon

            override fun calculatePosition(
                placeableDimension: Int,
                containerDimension: Int,
                gapSize: Int,
                thumbStart: Float,
                thumbEnd: Float,
            ): Int = 0
        }

        data object TrackEndIcon : Active {
            override val mirrored: Contents
                get() = Inactive.TrackStartIcon

            override fun calculatePosition(
                placeableDimension: Int,
                containerDimension: Int,
                gapSize: Int,
                thumbStart: Float,
                thumbEnd: Float,
            ): Int = (thumbStart - placeableDimension - gapSize).toInt()
        }
    }

    interface Inactive : Contents {
        override fun isVisible(
            placeableDimension: Int,
            containerDimension: Int,
            gapSize: Int,
            thumbStart: Float,
            thumbEnd: Float,
        ): Boolean = containerDimension - (thumbEnd + gapSize) > placeableDimension

        data object TrackStartIcon : Inactive {
            override val mirrored: Contents
                get() = Active.TrackEndIcon

            override fun calculatePosition(
                placeableDimension: Int,
                containerDimension: Int,
                gapSize: Int,
                thumbStart: Float,
                thumbEnd: Float,
            ): Int = (thumbEnd + gapSize).toInt()
        }

        data object TrackEndIcon : Inactive {
            override val mirrored: Contents
                get() = Active.TrackStartIcon

            override fun calculatePosition(
                placeableDimension: Int,
                containerDimension: Int,
                gapSize: Int,
                thumbStart: Float,
                thumbEnd: Float,
            ): Int = containerDimension - placeableDimension
        }
    }

    fun calculatePosition(
        placeableDimension: Int,
        containerDimension: Int,
        gapSize: Int,
        thumbStart: Float,
        thumbEnd: Float,
    ): Int

    fun isVisible(
        placeableDimension: Int,
        containerDimension: Int,
        gapSize: Int,
        thumbStart: Float,
        thumbEnd: Float,
    ): Boolean

    val mirrored: Contents
}

interface SliderIconsState {
    val isActiveTrackStartIconVisible: Boolean
    val isActiveTrackEndIconVisible: Boolean
    val isInactiveTrackStartIconVisible: Boolean
    val isInactiveTrackEndIconVisible: Boolean
}
