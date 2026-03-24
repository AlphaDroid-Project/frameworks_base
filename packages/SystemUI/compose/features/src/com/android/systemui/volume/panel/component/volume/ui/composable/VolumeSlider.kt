/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.systemui.volume.panel.component.volume.ui.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.android.compose.PlatformSlider
import com.android.compose.PlatformSliderColors
import com.android.systemui.Flags
import com.android.systemui.alpha.style.brightness.renderers.BrightnessSliderStyleRenderer
import com.android.systemui.alpha.style.common.LocalAlphaColorScheme
import com.android.systemui.common.shared.model.Icon as IconModel
import com.android.systemui.common.ui.compose.Icon
import com.android.systemui.compose.modifiers.sysuiResTag
import com.android.systemui.haptics.slider.SliderHapticFeedbackFilter
import com.android.systemui.haptics.slider.compose.ui.SliderHapticsViewModel
import com.android.systemui.lifecycle.rememberViewModel
import com.android.systemui.res.R
import com.android.systemui.volume.dialog.sliders.ui.compose.SliderTrack
import com.android.systemui.volume.dialog.ui.utils.getVolumeThumbOrButtonCornerRadiusForMode
import com.android.systemui.volume.dialog.ui.utils.getVolumeThumbOrButtonShapeForMode
import com.android.systemui.volume.dialog.ui.utils.rememberVolumeSliderShapeMode
import com.android.systemui.volume.dialog.ui.utils.useCustomVolumeThumb
import com.android.systemui.volume.haptics.ui.VolumeHapticsConfigsProvider
import com.android.systemui.volume.panel.component.volume.slider.ui.viewmodel.SliderState
import com.android.systemui.volume.ui.compose.slider.AccessibilityParams
import com.android.systemui.volume.ui.compose.slider.Haptics
import com.android.systemui.volume.ui.compose.slider.Slider
import com.android.systemui.volume.ui.compose.slider.SliderIcon
import com.google.common.annotations.VisibleForTesting
import kotlin.math.round
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VolumeSlider(
    state: SliderState,
    onValueChange: (newValue: Float) -> Unit,
    onIconTapped: () -> Unit,
    sliderColors: PlatformSliderColors,
    styleRenderer: BrightnessSliderStyleRenderer? = null,
    modifier: Modifier = Modifier,
    hapticsViewModelFactory: SliderHapticsViewModel.Factory?,
    onValueChangeFinished: (() -> Unit)? = null,
    button: (@Composable RowScope.() -> Unit)? = null,
    showLabel: Boolean = true,
    dimensions: VolumeSliderDimensions = VolumeSliderDimensions.Defaults,
) {
    if (!Flags.volumeRedesign()) {
        LegacyVolumeSlider(
            state = state,
            onValueChange = onValueChange,
            onIconTapped = onIconTapped,
            sliderColors = sliderColors,
            onValueChangeFinished = onValueChangeFinished,
            modifier = modifier,
            hapticsViewModelFactory = hapticsViewModelFactory,
        )
        return
    }

    val scheme = LocalAlphaColorScheme.current
    val density = LocalDensity.current
    val shapeMode = rememberVolumeSliderShapeMode()

    Column(modifier = modifier.animateContentSize()) {
        if (showLabel) {
            Text(
                text = state.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier.fillMaxWidth().padding(vertical = dimensions.verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val themedSliderColors =
                SliderDefaults.colors(
                    thumbColor = scheme.thumb,
                    activeTrackColor = scheme.accent,
                    inactiveTrackColor = scheme.neutral,
                    activeTickColor = scheme.onAccent,
                    inactiveTickColor = scheme.onNeutral,
                    disabledThumbColor = scheme.thumb.copy(alpha = 0.50f),
                    disabledActiveTrackColor = scheme.accent.copy(alpha = 0.45f),
                    disabledInactiveTrackColor = scheme.neutral.copy(alpha = 0.45f),
                    disabledActiveTickColor = scheme.onAccent.copy(alpha = 0.45f),
                    disabledInactiveTickColor = scheme.onNeutral.copy(alpha = 0.45f),
                )

            if (state is SliderState.Empty) {
                Spacer(
                    modifier = Modifier.weight(1f).height(dimensions.thumbHeight)
                )
            } else {
                Slider(
                    value = state.value,
                    valueRange = state.valueRange,
                    onValueChanged = onValueChange,
                    onValueChangeFinished = { onValueChangeFinished?.invoke() },
                    colors = themedSliderColors,
                    isEnabled = state.isEnabled,
                    stepDistance = state.step,
                    accessibilityParams =
                        AccessibilityParams(
                            contentDescription = state.a11yContentDescription,
                            stateDescription = state.a11yStateDescription,
                        ),
                    track = { sliderState ->
                        SliderTrack(
                            sliderState = sliderState,
                            colors = themedSliderColors,
                            isEnabled = state.isEnabled,
                            trackSize = dimensions.trackHeight,
                            isVertical = false,
                            styleRenderer = styleRenderer,
                            shapeMode = shapeMode,
                            activeTrackEndIcon =
                                state.icon?.let { icon ->
                                    { iconsState ->
                                        SliderIcon(
                                            icon = {
                                                Icon(
                                                    icon = icon,
                                                    tint = null,
                                                    modifier =
                                                        Modifier.size(24.dp)
                                                            .testTag(
                                                                VolumeSlidersMotionTestKeys
                                                                    .ACTIVE_ICON_TAG
                                                            ),
                                                )
                                            },
                                            isVisible = !iconsState.isInactiveTrackEndIconVisible,
                                        )
                                    }
                                },
                            inactiveTrackEndIcon =
                                state.icon?.let { icon ->
                                    { iconsState ->
                                        SliderIcon(
                                            icon = {
                                                Icon(
                                                    icon = icon,
                                                    tint = null,
                                                    modifier =
                                                        Modifier.size(24.dp)
                                                            .testTag(
                                                                VolumeSlidersMotionTestKeys
                                                                    .INACTIVE_ICON_TAG
                                                            ),
                                                )
                                            },
                                            isVisible = iconsState.isInactiveTrackEndIconVisible,
                                        )
                                    }
                                },
                        )
                    },
                    thumb = { sliderState, interactionSource ->
                        val logicalThumbSize = DpSize(dimensions.thumbWidth, dimensions.thumbHeight)
                        val shouldUseCustomThumb =
                            useCustomVolumeThumb(
                                shapeMode = shapeMode,
                                hasStyleRenderer = styleRenderer != null,
                            )

                        if (!shouldUseCustomThumb) {
                            SliderDefaults.Thumb(
                                sliderState = sliderState,
                                interactionSource = interactionSource,
                                enabled = state.isEnabled,
                                colors = themedSliderColors,
                                thumbSize = logicalThumbSize,
                            )
                        } else {
                            val visualThumbSize = dimensions.trackHeight + 4.dp
                            val thumbColor =
                                styleRenderer?.getThumbColor(
                                    scheme.thumb,
                                    scheme.accent,
                                ) ?: scheme.thumb

                            val defaultThumbShape =
                                remember(visualThumbSize) {
                                    RoundedCornerShape(visualThumbSize * 0.25f)
                                }
                            val defaultCornerRadius =
                                with(density) { (visualThumbSize * 0.25f).toPx() }
                            val thumbShape =
                                getVolumeThumbOrButtonShapeForMode(
                                    shapeMode = shapeMode,
                                    sizeDp = visualThumbSize,
                                    defaultShape = defaultThumbShape,
                                )
                            val thumbCornerRadius =
                                with(density) {
                                    getVolumeThumbOrButtonCornerRadiusForMode(
                                        shapeMode = shapeMode,
                                        sizePx = visualThumbSize.toPx(),
                                        defaultCornerRadius = defaultCornerRadius,
                                    )
                                }

                            val offsetX =
                                calculateHorizontalThumbOffset(
                                    fraction = sliderState.coercedValueAsFraction,
                                    visualThumbSize = visualThumbSize,
                                    logicalThumbWidth = logicalThumbSize.width,
                                )

                            Box(
                                modifier = Modifier.size(logicalThumbSize),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier =
                                        Modifier.offset(x = offsetX)
                                            .requiredSize(visualThumbSize)
                                            .clip(thumbShape),
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val bounds = Rect(0f, 0f, size.width, size.height)
                                        drawRect(color = thumbColor)

                                        if (styleRenderer != null &&
                                            !styleRenderer.skipThumbOverlay()
                                        ) {
                                            with(styleRenderer) {
                                                renderThumbOverlay(
                                                    thumbBounds = bounds,
                                                    shape = thumbShape,
                                                    cornerRadius = thumbCornerRadius,
                                                    thumbColor = thumbColor,
                                                    density = density,
                                                )
                                            }
                                        }

                                        drawThumbInset(
                                            bounds = bounds,
                                            cornerRadius = thumbCornerRadius,
                                            density = density,
                                        )
                                    }
                                }
                            }
                        }
                    },
                    haptics =
                        hapticsViewModelFactory?.let {
                            Haptics.Enabled(
                                hapticsViewModelFactory = it,
                                hapticConfigs =
                                    VolumeHapticsConfigsProvider.continuousConfigs(
                                        state.hapticFilter
                                    ),
                                orientation = Orientation.Horizontal,
                            )
                        } ?: Haptics.Disabled,
                    modifier =
                        Modifier.weight(1f)
                            .height(dimensions.thumbHeight)
                            .sysuiResTag(state.label),
                )
            }
            button?.invoke(this)
        }
        state.disabledMessage?.let { disabledMessage ->
            AnimatedVisibility(visible = !state.isEnabled) {
                Row(
                    modifier =
                        Modifier.padding(bottom = 12.dp)
                            .testTag(VolumeSlidersMotionTestKeys.DISABLED_MESSAGE_TAG),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MaterialIcon(
                        painter = painterResource(R.drawable.ic_error_outline),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = disabledMessage,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.basicMarquee().clearAndSetSemantics {},
                    )
                }
            }
        }
    }
}

@Composable
private fun LegacyVolumeSlider(
    state: SliderState,
    onValueChange: (newValue: Float) -> Unit,
    onIconTapped: () -> Unit,
    sliderColors: PlatformSliderColors,
    hapticsViewModelFactory: SliderHapticsViewModel.Factory?,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val value by valueState(state)
    val interactionSource = remember { MutableInteractionSource() }
    val hapticsViewModel: SliderHapticsViewModel? =
        setUpHapticsViewModel(
            value,
            state.valueRange,
            state.hapticFilter,
            interactionSource,
            hapticsViewModelFactory,
        )

    PlatformSlider(
        modifier =
            modifier.sysuiResTag(state.label).clearAndSetSemantics {
                if (state.isEnabled) {
                    contentDescription = state.label
                    state.a11yClickDescription?.let {
                        customActions =
                            listOf(
                                CustomAccessibilityAction(it) {
                                    onIconTapped()
                                    true
                                }
                            )
                    }

                    state.a11yStateDescription?.let { stateDescription = it }
                    progressBarRangeInfo = ProgressBarRangeInfo(state.value, state.valueRange)
                } else {
                    disabled()
                    contentDescription =
                        state.disabledMessage?.let { "${state.label}, $it" } ?: state.label
                }
                setProgress { targetValue ->
                    val targetDirection =
                        when {
                            targetValue > value -> 1
                            targetValue < value -> -1
                            else -> 0
                        }

                    val newValue =
                        (value + targetDirection * state.step).coerceIn(
                            state.valueRange.start,
                            state.valueRange.endInclusive,
                        )
                    onValueChange(newValue)
                    true
                }
            },
        value = value,
        valueRange = state.valueRange,
        onValueChange = { newValue ->
            hapticsViewModel?.addVelocityDataPoint(newValue)
            onValueChange(newValue)
        },
        onValueChangeFinished = {
            hapticsViewModel?.onValueChangeEnded()
            onValueChangeFinished?.invoke()
        },
        enabled = state.isEnabled,
        icon = {
            state.icon?.let {
                LegacySliderIcon(
                    icon = it,
                    onIconTapped = onIconTapped,
                    isTappable = state.isMutable,
                )
            }
        },
        colors = sliderColors,
        label = { isDragging ->
            AnimatedVisibility(
                visible = !isDragging,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150)),
            ) {
                VolumeSliderContent(
                    modifier = Modifier,
                    label = state.label,
                    isEnabled = state.isEnabled,
                    disabledMessage = state.disabledMessage,
                )
            }
        },
        interactionSource = interactionSource,
    )
}

@Composable
private fun valueState(state: SliderState): State<Float> {
    var prevState by remember { mutableStateOf(state) }
    // Don't animate slider value when receive the first value and when changing isEnabled state
    val shouldSkipAnimation =
        prevState is SliderState.Empty || prevState.isEnabled != state.isEnabled
    val value =
        if (shouldSkipAnimation) remember { mutableFloatStateOf(state.value) }
        else animateFloatAsState(targetValue = state.value, label = "VolumeSliderValueAnimation")
    prevState = state
    return value
}

@Composable
private fun LegacySliderIcon(
    icon: IconModel,
    onIconTapped: () -> Unit,
    isTappable: Boolean,
    modifier: Modifier = Modifier,
) {
    val boxModifier =
        if (isTappable) {
                modifier.clickable(
                    onClick = onIconTapped,
                    interactionSource = null,
                    indication = null,
                )
            } else {
                modifier
            }
            .fillMaxSize()
    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center,
        content = { Icon(modifier = Modifier.size(24.dp), icon = icon) },
    )
}

@Composable
private fun setUpHapticsViewModel(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    hapticFilter: SliderHapticFeedbackFilter,
    interactionSource: MutableInteractionSource,
    hapticsViewModelFactory: SliderHapticsViewModel.Factory?,
): SliderHapticsViewModel? {
    return hapticsViewModelFactory?.let {
        val configs =
            VolumeHapticsConfigsProvider.discreteConfigs(valueRange.stepSize(), hapticFilter)
        rememberViewModel(traceName = "SliderHapticsViewModel") {
                it.create(
                    interactionSource,
                    valueRange,
                    Orientation.Horizontal,
                    configs.hapticFeedbackConfig,
                    configs.sliderTrackerConfig,
                )
            }
            .also { hapticsViewModel ->
                var lastDiscreteStep by remember { mutableFloatStateOf(round(value)) }
                LaunchedEffect(value) {
                    snapshotFlow { value }
                        .map { round(it) }
                        .filter { it != lastDiscreteStep }
                        .distinctUntilChanged()
                        .collect { discreteStep ->
                            lastDiscreteStep = discreteStep
                            hapticsViewModel.onValueChange(discreteStep)
                        }
                }
            }
    }
}

private fun ClosedFloatingPointRange<Float>.stepSize(): Float = 1f / (endInclusive - start)

private fun DrawScope.drawThumbInset(
    bounds: Rect,
    cornerRadius: Float,
    density: Density,
) {
    val bevelWidth = with(density) { 1.5.dp.toPx() }
    val halfStroke = bevelWidth / 2f
    val strokeRect = bounds.deflate(halfStroke)
    val strokeRadius = (cornerRadius - halfStroke).coerceAtLeast(0f)

    drawRoundRect(
        brush =
            Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                start = bounds.topLeft,
                end = bounds.bottomRight,
            ),
        topLeft = strokeRect.topLeft,
        size = strokeRect.size,
        cornerRadius = CornerRadius(strokeRadius),
        style = Stroke(bevelWidth),
    )

    drawRoundRect(
        brush =
            Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                start = bounds.topLeft,
                end = bounds.bottomRight,
            ),
        topLeft = strokeRect.topLeft,
        size = strokeRect.size,
        cornerRadius = CornerRadius(strokeRadius),
        style = Stroke(bevelWidth),
    )
}

private fun calculateHorizontalThumbOffset(
    fraction: Float,
    visualThumbSize: Dp,
    logicalThumbWidth: Dp,
): Dp {
    val visualHalf = visualThumbSize / 2
    val logicalHalf = logicalThumbWidth / 2
    val maxOffset = visualHalf - logicalHalf
    val edgeThreshold = 0.08f

    return when {
        fraction < edgeThreshold -> {
            val t = 1f - (fraction / edgeThreshold)
            maxOffset * t
        }
        fraction > (1f - edgeThreshold) -> {
            val t = (fraction - (1f - edgeThreshold)) / edgeThreshold
            -maxOffset * t
        }
        else -> 0.dp
    }
}

@VisibleForTesting
object VolumeSlidersMotionTestKeys {
    const val ACTIVE_ICON_TAG = "Volume_Slider_activeStartIcon"
    const val INACTIVE_ICON_TAG = "Volume_Slider_inactiveStartIcon"
    const val DISABLED_MESSAGE_TAG = "disabledMessage"
}

data class VolumeSliderDimensions(
    val thumbHeight: Dp,
    val thumbWidth: Dp,
    val trackHeight: Dp,
    val verticalPadding: Dp,
) {
    companion object {
        val Defaults =
            VolumeSliderDimensions(
                thumbHeight = 52.dp,
                thumbWidth = 4.dp,
                trackHeight = 40.dp,
                verticalPadding = 4.dp,
            )
    }
}
