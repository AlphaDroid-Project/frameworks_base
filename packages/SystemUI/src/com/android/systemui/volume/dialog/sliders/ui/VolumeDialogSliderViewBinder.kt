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

package com.android.systemui.volume.dialog.sliders.ui

import android.os.UserHandle
import android.provider.Settings
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.theme.PlatformTheme
import com.android.systemui.alpha.style.common.LocalAlphaColorScheme
import com.android.systemui.alpha.style.common.defaultAlphaColorScheme
import com.android.systemui.alpha.style.volume.VolumeSliderStyleManager
import com.android.systemui.common.ui.compose.Icon
import com.android.systemui.haptics.slider.SliderHapticFeedbackFilter
import com.android.systemui.haptics.slider.compose.ui.SliderHapticsViewModel
import com.android.systemui.res.R
import com.android.systemui.volume.dialog.domain.interactor.DesktopAudioTileDetailsFeatureInteractor
import com.android.systemui.volume.dialog.sliders.dagger.VolumeDialogSliderScope
import com.android.systemui.volume.dialog.sliders.ui.compose.SliderTrack
import com.android.systemui.volume.dialog.sliders.ui.viewmodel.VolumeDialogOverscrollViewModel
import com.android.systemui.volume.dialog.sliders.ui.viewmodel.VolumeDialogSliderViewModel
import com.android.systemui.volume.dialog.ui.utils.getVolumeThumbOrButtonCornerRadiusForMode
import com.android.systemui.volume.dialog.ui.utils.getVolumeThumbOrButtonShapeForMode
import com.android.systemui.volume.dialog.ui.utils.rememberVolumeSliderShapeMode
import com.android.systemui.volume.dialog.ui.utils.useCustomVolumeThumb
import com.android.systemui.volume.haptics.ui.VolumeHapticsConfigsProvider
import com.android.systemui.volume.ui.compose.slider.AccessibilityParams
import com.android.systemui.volume.ui.compose.slider.Haptics
import com.android.systemui.volume.ui.compose.slider.Slider
import com.android.systemui.volume.ui.compose.slider.SliderIcon
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

@VolumeDialogSliderScope
class VolumeDialogSliderViewBinder
@Inject
constructor(
    private val viewModel: VolumeDialogSliderViewModel,
    private val overscrollViewModel: VolumeDialogOverscrollViewModel,
    private val hapticsViewModelFactory: SliderHapticsViewModel.Factory,
    private val desktopAudioTileDetailsFeatureInteractor: DesktopAudioTileDetailsFeatureInteractor,
    private val volumeSliderStyleManager: VolumeSliderStyleManager,
) {
    fun bind(view: View) {
        val isVolumeDialogVertical = !desktopAudioTileDetailsFeatureInteractor.isEnabled()
        val sliderComposeView: ComposeView = view.requireViewById(R.id.volume_dialog_slider)
        sliderComposeView.setContent {
            PlatformTheme {
                VolumeDialogSlider(
                    viewModel = viewModel,
                    overscrollViewModel = overscrollViewModel,
                    hapticsViewModelFactory = hapticsViewModelFactory,
                    isVolumeDialogVertical = isVolumeDialogVertical,
                    volumeSliderStyleManager = volumeSliderStyleManager,
                )
            }
        }
    }
}

@Composable
private fun rememberVolumeHapticsEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Secure.getIntForUser(
            context.contentResolver,
            Settings.Secure.VOLUME_DIALOG_HAPTIC_FEEDBACK,
            1,
            UserHandle.USER_CURRENT,
        ) != 0
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VolumeDialogSlider(
    viewModel: VolumeDialogSliderViewModel,
    overscrollViewModel: VolumeDialogOverscrollViewModel,
    hapticsViewModelFactory: SliderHapticsViewModel.Factory,
    isVolumeDialogVertical: Boolean,
    volumeSliderStyleManager: VolumeSliderStyleManager,
    modifier: Modifier = Modifier,
) {
    val defaultScheme = defaultAlphaColorScheme()
    val styleState by volumeSliderStyleManager.styleState.collectAsStateWithLifecycle()
    val styleRenderer = remember(
        styleState.styleId,
        styleState.settings,
        styleState.themeVersion,
        styleState.isNightMode,
        defaultScheme.accent,
        defaultScheme.neutral,
    ) {
        volumeSliderStyleManager.getRenderer(
            accentColor = defaultScheme.accent,
            neutralColor = defaultScheme.neutral,
        )
    }

    val themedScheme = remember(styleRenderer, defaultScheme) {
        styleRenderer?.produceColorScheme(defaultScheme) ?: defaultScheme
    }

    CompositionLocalProvider(LocalAlphaColorScheme provides themedScheme) {
        val colors = volumeSliderColors()
        val collectedSliderStateModel by viewModel.state.collectAsStateWithLifecycle(null)
        val sliderStateModel = collectedSliderStateModel ?: return@CompositionLocalProvider
        val interactionSource = remember { MutableInteractionSource() }
        val isHapticsEnabled = rememberVolumeHapticsEnabled()
        val density = LocalDensity.current
        val shapeMode = rememberVolumeSliderShapeMode()

        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect {
                when (it) {
                    is DragInteraction.Start -> viewModel.onSliderDragStarted()
                    is DragInteraction.Cancel -> viewModel.onSliderDragFinished()
                    is DragInteraction.Stop -> viewModel.onSliderDragFinished()
                }
            }
        }

        Slider(
            value = sliderStateModel.value,
            valueRange = sliderStateModel.valueRange,
            onValueChanged = { value ->
                overscrollViewModel.setSlider(
                    value = value,
                    min = sliderStateModel.valueRange.start,
                    max = sliderStateModel.valueRange.endInclusive,
                )
                viewModel.setStreamVolume(value, true)
            },
            onValueChangeFinished = { viewModel.onSliderChangeFinished(it) },
            isEnabled = !sliderStateModel.isDisabled,
            isReverseDirection = true,
            isVertical = isVolumeDialogVertical,
            colors = colors,
            interactionSource = interactionSource,
            haptics = if (isHapticsEnabled) {
                Haptics.Enabled(
                    hapticsViewModelFactory = hapticsViewModelFactory,
                    hapticConfigs = VolumeHapticsConfigsProvider.continuousConfigs(
                        SliderHapticFeedbackFilter(),
                    ),
                    orientation = if (isVolumeDialogVertical) {
                        Orientation.Vertical
                    } else {
                        Orientation.Horizontal
                    },
                )
            } else {
                Haptics.Disabled
            },
            stepDistance = 1f,
            track = { sliderState ->
                SliderTrack(
                    sliderState = sliderState,
                    colors = colors,
                    isEnabled = !sliderStateModel.isDisabled,
                    isVertical = isVolumeDialogVertical,
                    styleRenderer = styleRenderer,
                    shapeMode = shapeMode,
                    activeTrackEndIcon = { iconsState ->
                        SliderIcon(
                            icon = {
                                Icon(
                                    icon = sliderStateModel.icon,
                                    tint = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            isVisible = !iconsState.isInactiveTrackEndIconVisible,
                        )
                    },
                    inactiveTrackEndIcon = { iconsState ->
                        SliderIcon(
                            icon = {
                                Icon(
                                    icon = sliderStateModel.icon,
                                    tint = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            isVisible = iconsState.isInactiveTrackEndIconVisible,
                        )
                    },
                )
            },
            thumb = { sliderState, interactions ->
                val enabled = !sliderStateModel.isDisabled
                val logicalThumbSize = if (isVolumeDialogVertical) {
                    VolumeDialogSliderDimensions.VerticalLogicalThumbSize
                } else {
                    VolumeDialogSliderDimensions.HorizontalLogicalThumbSize
                }

                val shouldUseCustomThumb = useCustomVolumeThumb(
                    shapeMode = shapeMode,
                    hasStyleRenderer = styleRenderer != null,
                )

                if (!shouldUseCustomThumb) {
                    SliderDefaults.Thumb(
                        sliderState = sliderState,
                        interactionSource = interactions,
                        enabled = enabled,
                        colors = colors,
                        thumbSize = logicalThumbSize,
                    )
                } else {
                    val visualThumbSize = VolumeDialogSliderDimensions.StyledVisualThumbSize
                    val thumbColor = styleRenderer?.getThumbColor(
                        themedScheme.thumb,
                        themedScheme.accent,
                    ) ?: themedScheme.thumb

                    val defaultThumbShape = remember(visualThumbSize) {
                        RoundedCornerShape(visualThumbSize * 0.25f)
                    }
                    val defaultCornerRadius = with(density) {
                        (visualThumbSize * 0.25f).toPx()
                    }
                    val thumbShape = getVolumeThumbOrButtonShapeForMode(
                        shapeMode = shapeMode,
                        sizeDp = visualThumbSize,
                        defaultShape = defaultThumbShape,
                    )
                    val thumbCornerRadius = with(density) {
                        getVolumeThumbOrButtonCornerRadiusForMode(
                            shapeMode = shapeMode,
                            sizePx = visualThumbSize.toPx(),
                            defaultCornerRadius = defaultCornerRadius,
                        )
                    }

                    val fraction = sliderState.coercedValueAsFraction
                    val (offsetX, offsetY) = calculateVolumeThumbOffset(
                        fraction = fraction,
                        isVertical = isVolumeDialogVertical,
                        visualThumbSize = visualThumbSize,
                        logicalThumbSize = logicalThumbSize,
                    )

                    Box(
                        modifier = Modifier.size(logicalThumbSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = offsetX, y = offsetY)
                                .requiredSize(visualThumbSize)
                                .clip(thumbShape),
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val bounds = Rect(0f, 0f, size.width, size.height)

                                drawRect(color = thumbColor)

                                if (styleRenderer != null && !styleRenderer.skipThumbOverlay()) {
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
            accessibilityParams = AccessibilityParams(contentDescription = sliderStateModel.label),
            modifier = modifier.pointerInput(Unit) {
                coroutineScope {
                    val currentContext = currentCoroutineContext()
                    awaitPointerEventScope {
                        while (currentContext.isActive) {
                            viewModel.onTouchEvent(awaitPointerEvent())
                        }
                    }
                }
            },
        )
    }
}

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
        brush = Brush.linearGradient(
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
        brush = Brush.linearGradient(
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

@Composable
private fun calculateVolumeThumbOffset(
    fraction: Float,
    isVertical: Boolean,
    visualThumbSize: Dp,
    logicalThumbSize: DpSize,
): Pair<Dp, Dp> {
    val visualHalf = visualThumbSize / 2
    val logicalHalf = if (isVertical) {
        logicalThumbSize.height / 2
    } else {
        logicalThumbSize.width / 2
    }

    val maxOffset = visualHalf - logicalHalf
    val edgeThreshold = 0.08f

    val offset = when {
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

    return if (isVertical) {
        0.dp to -offset
    } else {
        offset to 0.dp
    }
}

@Composable
private fun volumeSliderColors(): SliderColors {
    val scheme = LocalAlphaColorScheme.current
    return SliderDefaults.colors(
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
}
