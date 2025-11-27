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

package com.android.systemui.brightness.ui.compose

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.UserHandle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.app.tracing.coroutines.launchTraced as launch
import com.android.compose.modifiers.padding
import com.android.compose.modifiers.sliderPercentage
import com.android.compose.modifiers.thenIf
import com.android.compose.theme.LocalAndroidColorScheme
import com.android.compose.ui.graphics.drawInOverlay
import com.android.systemui.alpha.style.brightness.BrightnessMaterialColors
import com.android.systemui.alpha.style.brightness.BrightnessSliderStyleWrapper
import com.android.systemui.alpha.style.brightness.renderers.BrightnessSliderStyleRenderer
import com.android.systemui.alpha.style.common.LocalAlphaColorScheme
import com.android.systemui.alpha.style.common.defaultAlphaColorScheme
import com.android.systemui.biometrics.Utils.toBitmap
import com.android.systemui.brightness.shared.model.GammaBrightness
import com.android.systemui.brightness.ui.compose.AnimationSpecs.IconAppearSpec
import com.android.systemui.brightness.ui.compose.AnimationSpecs.IconDisappearSpec
import com.android.systemui.brightness.ui.compose.Dimensions.IconPadding
import com.android.systemui.brightness.ui.compose.Dimensions.IconSize
import com.android.systemui.brightness.ui.compose.Dimensions.SliderBackgroundFrameSize
import com.android.systemui.brightness.ui.compose.Dimensions.SliderBackgroundRoundedCorner
import com.android.systemui.brightness.ui.compose.Dimensions.SliderTrackRoundedCorner
import com.android.systemui.brightness.ui.compose.Dimensions.ThumbTrackGapSize
import com.android.systemui.brightness.ui.compose.Dimensions.TrackHeight
import com.android.systemui.brightness.ui.viewmodel.BrightnessSliderViewModel
import com.android.systemui.brightness.ui.viewmodel.Drag
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.compose.modifiers.sysuiResTag
import com.android.systemui.haptics.slider.SeekableSliderTrackerConfig
import com.android.systemui.haptics.slider.SliderHapticFeedbackConfig
import com.android.systemui.haptics.slider.compose.ui.SliderHapticsViewModel
import com.android.systemui.lifecycle.rememberViewModel
import com.android.systemui.qs.ui.compose.borderOnFocus
import com.android.systemui.res.R
import com.android.systemui.utils.PolicyRestriction
import platform.test.motion.compose.values.MotionTestValueKey
import platform.test.motion.compose.values.motionTestValues

internal object ThumbDimensions {
    val AospWidth: Dp
        @Composable
        @ReadOnlyComposable
        get() = dimensionResource(id = R.dimen.overlay_qs_layout_brightness_thumb_width)

    val AospHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = dimensionResource(id = R.dimen.overlay_qs_layout_brightness_thumb_height)

    val StyledSize = 48.dp
}

internal object AutoButtonDimensions {
    val Size = 45.dp
}

internal fun getCornerRadiusForShape(shapeMode: Int, size: Float): Float {
    return when (shapeMode) {
        3 -> 0f
        2 -> size * 0.25f
        else -> size / 2f
    }
}

@Composable
internal fun getShapeForMode(shapeMode: Int, sizeDp: Dp): Shape {
    return remember(shapeMode, sizeDp) {
        when (shapeMode) {
            3 -> RoundedCornerShape(0.dp)
            2 -> RoundedCornerShape(sizeDp * 0.25f)
            else -> CircleShape
        }
    }
}

private fun DrawScope.drawActiveTrackSegment(
    left: Float,
    right: Float,
    height: Float,
    color: Color,
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
            )
        )
    }
    drawPath(path, color)
}

private fun DrawScope.drawInactiveTrackSegment(
    left: Float,
    right: Float,
    height: Float,
    color: Color,
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
            )
        )
    }
    drawPath(path, color)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
@VisibleForTesting
fun BrightnessSlider(
    gammaValue: Int,
    valueRange: IntRange,
    autoMode: Boolean,
    iconResProvider: (Float) -> Int,
    imageLoader: suspend (Int, Context) -> Icon.Loaded?,
    restriction: PolicyRestriction,
    onRestrictedClick: (PolicyRestriction.Restricted) -> Unit,
    onDrag: (Int) -> Unit,
    onStop: (Int) -> Unit,
    onIconClick: suspend () -> Unit,
    overriddenByAppState: Boolean,
    modifier: Modifier = Modifier,
    showToast: () -> Unit = {},
    hapticsViewModelFactory: SliderHapticsViewModel.Factory,
    shape: Shape? = null,
    styleRenderer: BrightnessSliderStyleRenderer? = null,
    innerCornerDp: Dp = 2.dp,
) {
    val context = LocalContext.current
    val cr = context.contentResolver
    val density = LocalDensity.current

    var hapticsEnabled by remember { mutableStateOf(readEnableHaptics(cr)) }

    val shapeMode = rememberSliderShapeMode()
    val trackCornerDp: Dp = when (shapeMode) {
        1 -> 24.dp
        2 -> 12.dp
        3 -> 0.dp
        else -> SliderTrackRoundedCorner
    }
    val bgCornerDp: Dp = when (shapeMode) {
        1 -> 50.dp
        2 -> 24.dp
        3 -> 0.dp
        else -> SliderBackgroundRoundedCorner
    }
    val autoIconShape = getShapeForMode(shapeMode, AutoButtonDimensions.Size)
    val sliderShape = remember(shapeMode, bgCornerDp) { shape ?: RoundedCornerShape(bgCornerDp) }

    var value by remember(gammaValue) { mutableIntStateOf(gammaValue) }
    val animatedValue by
        animateFloatAsState(targetValue = value.toFloat(), label = "BrightnessSliderAnimatedValue")
    val floatValueRange = valueRange.first.toFloat()..valueRange.last.toFloat()
    val isRestricted = restriction is PolicyRestriction.Restricted
    val enabled = !isRestricted
    val contentDescription = stringResource(R.string.accessibility_brightness)
    val interactionSource = remember { MutableInteractionSource() }
    val hapticsViewModel: SliderHapticsViewModel? =
        if (hapticsEnabled) {
            rememberViewModel(traceName = "SliderHapticsViewModel") {
                hapticsViewModelFactory.create(
                    interactionSource,
                    floatValueRange,
                    Orientation.Horizontal,
                    SliderHapticFeedbackConfig(
                        maxVelocityToScale = 1f
                    ),
                    SeekableSliderTrackerConfig(),
                )
            }
        } else {
            null
        }

    val colorScheme = LocalAlphaColorScheme.current
    val colors = colors()

    val iconRes by
        remember(gammaValue, valueRange) {
            derivedStateOf {
                val percentage =
                    (value - valueRange.first) * 100f / (valueRange.last - valueRange.first)
                iconResProvider(percentage)
            }
        }
    val painter: Painter by
        produceState<Painter>(
            initialValue = ColorPainter(Color.Transparent),
            key1 = iconRes,
            key2 = context,
        ) {
            val icon: Icon.Loaded? = imageLoader(iconRes, context)
            if (icon != null) {
                val bitmap = icon.drawable.toBitmap()?.asImageBitmap()
                if (bitmap != null) {
                    this@produceState.value = BitmapPainter(bitmap)
                }
            }
        }
    val activeIconColor = colors.activeTickColor
    val inactiveIconColor = colors.inactiveTickColor
    val paddingPx = with(density) { IconPadding.toPx() }
    val iconSizePx = with(density) { IconSize.toSize() }

    val trackIcon: DrawScope.(Offset, Color, Float) -> Unit = remember {
        { offset, color, alpha ->
            val rtl = layoutDirection == LayoutDirection.Rtl
            scale(if (rtl) -1f else 1f, 1f) {
                translate(offset.x - IconPadding.toPx() - IconSize.toSize().width, offset.y) {
                    with(painter) {
                        draw(
                            IconSize.toSize(),
                            colorFilter = ColorFilter.tint(color),
                            alpha = alpha,
                        )
                    }
                }
            }
        }
    }

    val hasAutoBrightness = context.resources.getBoolean(
        com.android.internal.R.bool.config_automatic_brightness_available
    )
    var showAutoBrightness by remember { mutableStateOf(readShowAutoBrightness(cr)) }

    DisposableEffect(Unit) {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                context.mainExecutor.execute {
                    showAutoBrightness = readShowAutoBrightness(cr)
                    hapticsEnabled = readEnableHaptics(cr)
                }
            }
        }

        cr.registerContentObserver(
            Settings.System.getUriFor(Settings.System.QS_SHOW_AUTO_BRIGHTNESS),
            false, observer, UserHandle.USER_ALL
        )

        cr.registerContentObserver(
            Settings.System.getUriFor(Settings.System.QS_BRIGHTNESS_SLIDER_HAPTIC),
            false, observer, UserHandle.USER_ALL
        )

        onDispose {
            cr.unregisterContentObserver(observer)
        }
    }

    // Hoist @Composable dimension reads so they can be used in non-composable Canvas lambdas
    val isStyled = styleRenderer != null
    val logicalThumbWidth = ThumbDimensions.AospWidth
    val logicalThumbHeight = ThumbDimensions.AospHeight
    val logicalThumbWidthPx = with(density) { logicalThumbWidth.toPx() }
    val visualThumbSize =
        if (isStyled) ThumbDimensions.StyledSize else logicalThumbWidth
    val visualThumbSizePx = with(density) { visualThumbSize.toPx() }
    val thumbCornerRadius = getCornerRadiusForShape(shapeMode, visualThumbSizePx)
    val thumbShape = getShapeForMode(shapeMode, visualThumbSize)

    val thumbColor =
        remember(styleRenderer, colorScheme) {
            styleRenderer?.getThumbColor(colorScheme.thumb, colorScheme.accent)
                ?: colorScheme.thumb
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Slider(
            value = animatedValue,
            valueRange = floatValueRange,
            enabled = enabled,
            colors = colors,
            onValueChange = {
                if (enabled) {
                    if (!overriddenByAppState) {
                        hapticsViewModel?.onValueChange(it)
                        value = it.toInt()
                        onDrag(value)
                    }
                }
            },
            onValueChangeFinished = {
                if (enabled) {
                    if (!overriddenByAppState) {
                        hapticsViewModel?.onValueChangeEnded()
                        onStop(value)
                    }
                }
            },
            modifier =
                Modifier
                    .weight(1f)
                    .sysuiResTag("slider")
                    .semantics(mergeDescendants = true) {
                        this.text = AnnotatedString(contentDescription)
                    }
                    .sliderPercentage {
                        (value - valueRange.first).toFloat() / (valueRange.last - valueRange.first)
                    }
                    .thenIf(isRestricted) {
                        Modifier.clickable {
                            if (restriction is PolicyRestriction.Restricted) {
                                onRestrictedClick(restriction)
                            }
                        }
                    },
            interactionSource = interactionSource,
            thumb = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(logicalThumbWidth, logicalThumbHeight)
                ) {
                    if (isStyled) {
                        val fraction = (animatedValue - floatValueRange.start) /
                            (floatValueRange.endInclusive - floatValueRange.start)

                        val visualHalf = ThumbDimensions.StyledSize / 2
                        val logicalHalf = logicalThumbWidth / 2
                        val maxOffset = visualHalf - logicalHalf

                        val edgeThreshold = 0.08f

                        val centerOffset: Dp = when {
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

                        Box(
                            modifier = Modifier
                                .offset(x = centerOffset)
                                .requiredSize(visualThumbSize)
                                .clip(thumbShape)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val bounds = Rect(0f, 0f, size.width, size.height)

                                drawRect(color = thumbColor)

                                if (styleRenderer != null && !styleRenderer.skipThumbOverlay()) {
                                    with(styleRenderer) {
                                        renderThumbOverlay(
                                            bounds,
                                            thumbShape,
                                            thumbCornerRadius,
                                            thumbColor,
                                            density
                                        )
                                    }
                                }

                                drawThumbInset(bounds, thumbCornerRadius, density)
                            }
                        }
                    } else {
                        SliderDefaults.Thumb(
                            interactionSource = interactionSource,
                            enabled = enabled,
                            thumbSize = DpSize(logicalThumbWidth, logicalThumbHeight),
                            colors = colors,
                        )
                    }
                }
            },
            track = { sliderState ->
                var showIconActive by remember { mutableStateOf(true) }
                val iconActiveAlphaAnimatable = remember {
                    Animatable(
                        initialValue = 1f,
                        typeConverter = Float.VectorConverter,
                        label = "iconActiveAlpha",
                    )
                }
                val iconInactiveAlphaAnimatable = remember {
                    Animatable(
                        initialValue = 0f,
                        typeConverter = Float.VectorConverter,
                        label = "iconInactiveAlpha",
                    )
                }

                LaunchedEffect(iconActiveAlphaAnimatable, iconInactiveAlphaAnimatable, showIconActive) {
                    if (showIconActive) {
                        launch { iconActiveAlphaAnimatable.appear() }
                        launch { iconInactiveAlphaAnimatable.disappear() }
                    } else {
                        launch { iconActiveAlphaAnimatable.disappear() }
                        launch { iconInactiveAlphaAnimatable.appear() }
                    }
                }

                val sliderHeight = TrackHeight
                val effectiveThumbGap = if (isStyled) 0.dp else ThumbTrackGapSize
                val trackInsideCornerDp: Dp = innerCornerDp
                val trackCornerPx = with(density) { trackCornerDp.toPx() }
                val trackInsideCornerPx = with(density) { trackInsideCornerDp.toPx() }

                Box(modifier = Modifier.height(sliderHeight)) {
                    BrightnessSliderStyleWrapper(
                        renderer = styleRenderer,
                        shape = RoundedCornerShape(trackCornerDp),
                        segmentMode = true,
                        isActive = false,
                        activeFraction = sliderState.coercedValueAsFraction,
                        trackCornerDp = trackCornerDp,
                        trackInsideCornerDp = trackInsideCornerDp,
                        thumbGapDp = effectiveThumbGap,
                        materialColors =
                            BrightnessMaterialColors(
                                colors.activeTrackColor,
                                colors.inactiveTrackColor,
                                Color.Transparent,
                                Color.Transparent
                            ),
                        modifier = Modifier.height(sliderHeight)
                    ) {
                        if (styleRenderer == null) {
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier = Modifier
                                    .motionTestValues {
                                        (iconActiveAlphaAnimatable.isRunning ||
                                            iconInactiveAlphaAnimatable.isRunning) exportAs
                                            BrightnessSliderMotionTestKeys.AnimatingIcon

                                        iconActiveAlphaAnimatable.value exportAs
                                            BrightnessSliderMotionTestKeys.ActiveIconAlpha
                                        iconInactiveAlphaAnimatable.value exportAs
                                            BrightnessSliderMotionTestKeys.InactiveIconAlpha
                                    }
                                    .height(sliderHeight),
                                trackCornerSize = trackCornerDp,
                                trackInsideCornerSize = 2.dp,
                                drawStopIndicator = null,
                                thumbTrackGapSize = ThumbTrackGapSize,
                                colors = colors,
                            )
                        } else {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val fraction = sliderState.coercedValueAsFraction

                                val thumbHalf = logicalThumbWidthPx / 2f
                                val thumbCenterX = size.width * fraction

                                val thumbGapPx =
                                    with(density) { effectiveThumbGap.toPx() }
                                val thumbLeft = thumbCenterX - thumbHalf
                                val thumbRight = thumbCenterX + thumbHalf

                                val activeStart = 0f
                                val activeEnd =
                                    (thumbLeft - thumbGapPx).coerceIn(0f, size.width)

                                val inactiveStart =
                                    (thumbRight + thumbGapPx).coerceIn(0f, size.width)
                                val inactiveEnd = size.width

                                drawActiveTrackSegment(
                                    left = activeStart,
                                    right = activeEnd,
                                    height = size.height,
                                    color = colors.activeTrackColor,
                                    outerCornerPx = trackCornerPx,
                                    innerCornerPx = trackInsideCornerPx,
                                )

                                drawInactiveTrackSegment(
                                    left = inactiveStart,
                                    right = inactiveEnd,
                                    height = size.height,
                                    color = colors.inactiveTrackColor,
                                    outerCornerPx = trackCornerPx,
                                    innerCornerPx = trackInsideCornerPx,
                                )
                            }
                        }
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val yOffset = size.height / 2 - IconSize.toSize().height / 2
                        val fraction = sliderState.coercedValueAsFraction

                        val visualThumbWidthPx =
                            if (isStyled) visualThumbSizePx
                            else logicalThumbWidthPx
                        val visualThumbHalf = visualThumbWidthPx / 2f

                        val unclampedCenterX = size.width * fraction
                        val visualThumbCenterX =
                            unclampedCenterX.coerceIn(
                                visualThumbHalf,
                                size.width - visualThumbHalf
                            )

                        val thumbLeftEdge = visualThumbCenterX - visualThumbHalf
                        val thumbRightEdge = visualThumbCenterX + visualThumbHalf

                        val padding = IconPadding.toPx()
                        val iconWidth = IconSize.toSize().width

                        val activeIconVisibleWidth = thumbLeftEdge - padding * 2
                        val inactiveIconVisibleWidth = size.width - thumbRightEdge - padding * 2

                        if (iconWidth < inactiveIconVisibleWidth) {
                            showIconActive = false
                            trackIcon(
                                Offset(size.width, yOffset),
                                inactiveIconColor,
                                iconInactiveAlphaAnimatable.value,
                            )
                        } else if (iconWidth < activeIconVisibleWidth) {
                            showIconActive = true
                            trackIcon(
                                Offset(thumbLeftEdge, yOffset),
                                activeIconColor,
                                iconActiveAlphaAnimatable.value,
                            )
                        }
                    }
                }
            },
        )

        if (hasAutoBrightness && showAutoBrightness) {
            Spacer(modifier = Modifier.width(10.dp))

            val targetAutoBgColor =
                if (autoMode) colorScheme.accent else colorScheme.neutral
            val autoBrightnessBackgroundColor by animateColorAsState(
                targetAutoBgColor,
                label = "AutoBg"
            )
            val autoBrightnessIconTint by animateColorAsState(
                if (autoMode) colorScheme.onAccent else colorScheme.onNeutral,
                label = "AutoTint"
            )

            Box(
                modifier = Modifier.size(AutoButtonDimensions.Size),
                contentAlignment = Alignment.Center
            ) {
                BrightnessSliderStyleWrapper(
                    renderer = styleRenderer,
                    shape = autoIconShape,
                    segmentMode = false,
                    isActive = autoMode,
                    materialColors =
                        BrightnessMaterialColors(
                            Color.Transparent,
                            Color.Transparent,
                            autoBrightnessBackgroundColor,
                            autoBrightnessBackgroundColor
                        ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(autoBrightnessBackgroundColor, autoIconShape)
                    )
                }

                val view = LocalView.current
                val coroutineScope = rememberCoroutineScope()
                val painterRes = if (autoMode) {
                    R.drawable.ic_qs_brightness_auto_on
                } else {
                    R.drawable.ic_qs_brightness_auto_off
                }
                val hapticConstant = if (autoMode) {
                    HapticFeedbackConstants.TOGGLE_OFF
                } else {
                    HapticFeedbackConstants.TOGGLE_ON
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(autoIconShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (hapticsEnabled) {
                                    view.performHapticFeedback(hapticConstant)
                                }
                                coroutineScope.launch { onIconClick() }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(painterRes),
                        contentDescription = stringResource(R.string.accessibility_adaptive_brightness),
                        tint = autoBrightnessIconTint
                    )
                }
            }
        }
    }

    val currentShowToast by rememberUpdatedState(showToast)
    LaunchedEffect(interactionSource, overriddenByAppState) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start && overriddenByAppState) {
                currentShowToast()
            }
        }
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
        brush =
            Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                start = bounds.topLeft,
                end = bounds.bottomRight
            ),
        topLeft = strokeRect.topLeft,
        size = strokeRect.size,
        cornerRadius = CornerRadius(strokeRadius),
        style = Stroke(bevelWidth)
    )

    drawRoundRect(
        brush =
            Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                start = bounds.topLeft,
                end = bounds.bottomRight
            ),
        topLeft = strokeRect.topLeft,
        size = strokeRect.size,
        cornerRadius = CornerRadius(strokeRadius),
        style = Stroke(bevelWidth)
    )
}

@Composable
fun rememberSliderShapeMode(): Int {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    fun readShapeMode(): Int {
        return try {
            Settings.System.getIntForUser(
                contentResolver, Settings.System.QS_BRIGHTNESS_SLIDER_SHAPE, 0,
                UserHandle.USER_CURRENT
            )
        } catch (_: Throwable) {
            0
        }
    }

    var shapeMode by remember { mutableIntStateOf(readShapeMode()) }

    DisposableEffect(contentResolver) {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                context.mainExecutor.execute {
                    shapeMode = readShapeMode()
                }
            }
        }

        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.QS_BRIGHTNESS_SLIDER_SHAPE),
            false, observer, UserHandle.USER_ALL
        )

        onDispose {
            contentResolver.unregisterContentObserver(observer)
        }
    }

    return shapeMode
}

private fun Modifier.sliderBackground(color: Color, corner: Dp) = drawWithCache {
    val offsetAround = SliderBackgroundFrameSize.toSize()
    val newSize = Size(size.width + 2 * offsetAround.width, size.height + 2 * offsetAround.height)
    val offset = Offset(-offsetAround.width, -offsetAround.height)
    val cornerRadius = CornerRadius(corner.toPx())
    onDrawBehind {
        drawRoundRect(color = color, topLeft = offset, size = newSize, cornerRadius = cornerRadius)
    }
}

private fun readShowAutoBrightness(cr: ContentResolver): Boolean =
    try {
        Settings.System.getIntForUser(
            cr, Settings.System.QS_SHOW_AUTO_BRIGHTNESS,
            1, UserHandle.USER_CURRENT
        ) != 0
    } catch (_: Throwable) {
        false
    }

private fun readEnableHaptics(cr: ContentResolver): Boolean =
    try {
        Settings.System.getIntForUser(
            cr, Settings.System.QS_BRIGHTNESS_SLIDER_HAPTIC,
            1, UserHandle.USER_CURRENT
        ) != 0
    } catch (_: Throwable) {
        false
    }

@Composable
fun BrightnessSliderContainer(
    viewModel: BrightnessSliderViewModel,
    modifier: Modifier = Modifier,
    containerColors: ContainerColors,
    shape: Shape? = null,
) {
    val gamma = viewModel.currentBrightness.value
    if (gamma == BrightnessSliderViewModel.initialValue.value) {
        return
    }
    val autoMode = viewModel.autoMode
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val restriction by
        viewModel.policyRestriction.collectAsStateWithLifecycle(
            initialValue = PolicyRestriction.NoRestriction
        )
    val overriddenByAppState by viewModel.brightnessOverriddenByWindow.collectAsStateWithLifecycle()

    DisposableEffect(Unit) { onDispose { viewModel.setIsDragging(false) } }

    var dragging by remember { mutableStateOf(false) }

    val shapeMode = rememberSliderShapeMode()
    val trackCornerDp: Dp = when (shapeMode) {
        1 -> 24.dp
        2 -> 12.dp
        3 -> 0.dp
        else -> SliderTrackRoundedCorner
    }
    val bgCornerDp: Dp = when (shapeMode) {
        1 -> 50.dp
        2 -> 24.dp
        3 -> 0.dp
        else -> SliderBackgroundRoundedCorner
    }

    val sliderShape = remember(shapeMode, bgCornerDp) { shape ?: RoundedCornerShape(bgCornerDp) }

    val styleState by viewModel.styleManager.styleState.collectAsStateWithLifecycle()
    val originalAccent = MaterialTheme.colorScheme.primary
    val originalNeutral = LocalAndroidColorScheme.current.surfaceEffect1

    val styleRenderer =
        remember(
            styleState.styleId,
            styleState.settings,
            styleState.themeVersion,
            styleState.isNightMode,
            originalAccent,
            originalNeutral
        ) {
            viewModel.styleManager.getRenderer(originalAccent, originalNeutral)
        }

    val isSystemDefaultStyle = styleState.styleId == "system_default"

    val innerCornerDp: Dp =
        if (isSystemDefaultStyle) 2.dp else 0.dp

    val containerColor by
        animateColorAsState(
            if (dragging) containerColors.mirrorColor else containerColors.idleColor
        )
    val defaultScheme = defaultAlphaColorScheme()
    val themedScheme =
        remember(styleRenderer, defaultScheme) {
            styleRenderer?.produceColorScheme(defaultScheme) ?: defaultScheme
        }

    Box(
        modifier =
            modifier
                .padding(vertical = { SliderBackgroundFrameSize.height.roundToPx() })
                .fillMaxWidth()
                .sysuiResTag("brightness_slider")
    ) {
        CompositionLocalProvider(LocalAlphaColorScheme provides themedScheme) {
            BrightnessSlider(
                gammaValue = gamma,
                valueRange = viewModel.minBrightness.value..viewModel.maxBrightness.value,
                autoMode = autoMode,
                iconResProvider = BrightnessSliderViewModel::getIconForPercentage,
                imageLoader = viewModel::loadImage,
                restriction = restriction,
                onRestrictedClick = viewModel::showPolicyRestrictionDialog,
                onDrag = {
                    viewModel.setIsDragging(true)
                    dragging = true
                    coroutineScope.launch { viewModel.onDrag(Drag.Dragging(GammaBrightness(it))) }
                },
                onStop = {
                    viewModel.setIsDragging(false)
                    dragging = false
                    coroutineScope.launch { viewModel.onDrag(Drag.Stopped(GammaBrightness(it))) }
                },
                onIconClick = { viewModel.onIconClick() },
                modifier =
                    Modifier.borderOnFocus(
                            color = MaterialTheme.colorScheme.secondary,
                            cornerSize = CornerSize(trackCornerDp),
                        )
                        .then(if (viewModel.showMirror) Modifier.drawInOverlay() else Modifier)
                        .sliderBackground(containerColor, bgCornerDp)
                        .fillMaxWidth()
                        .pointerInteropFilter {
                            if (
                                it.actionMasked == MotionEvent.ACTION_UP ||
                                    it.actionMasked == MotionEvent.ACTION_CANCEL
                            ) {
                                viewModel.emitBrightnessTouchForFalsing()
                            }
                            false
                        },
                hapticsViewModelFactory = viewModel.hapticsViewModelFactory,
                overriddenByAppState = overriddenByAppState,
                showToast = {
                    viewModel.showToast(context, R.string.quick_settings_brightness_unable_adjust_msg)
                },
                shape = sliderShape,
                styleRenderer = styleRenderer,
                innerCornerDp = innerCornerDp,
            )
        }
    }
}

data class ContainerColors(val idleColor: Color, val mirrorColor: Color) {
    companion object {
        fun singleColor(color: Color) = ContainerColors(color, color)

        val defaultContainerColor: Color
            @Composable @ReadOnlyComposable get() = colorResource(R.color.shade_panel_fallback)
    }
}

private object Dimensions {
    val SliderBackgroundFrameSize = DpSize(10.dp, 6.dp)
    val SliderBackgroundRoundedCorner = 24.dp
    val SliderTrackRoundedCorner = 12.dp
    val IconSize = DpSize(28.dp, 28.dp)
    val IconPadding = 6.dp
    val ThumbTrackGapSize = 6.dp

    val TrackHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() =
            dimensionResource(id = R.dimen.overlay_qs_layout_brightness_track_height)
}

private object AnimationSpecs {
    val IconAppearSpec = tween<Float>(durationMillis = 100, delayMillis = 33)
    val IconDisappearSpec = tween<Float>(durationMillis = 50)
}

private suspend fun Animatable<Float, AnimationVector1D>.appear() =
    animateTo(targetValue = 1f, animationSpec = IconAppearSpec)

private suspend fun Animatable<Float, AnimationVector1D>.disappear() =
    animateTo(targetValue = 0f, animationSpec = IconDisappearSpec)

@VisibleForTesting
object BrightnessSliderMotionTestKeys {
    val AnimatingIcon = MotionTestValueKey<Boolean>("animatingIcon")
    val ActiveIconAlpha = MotionTestValueKey<Float>("activeIconAlpha")
    val InactiveIconAlpha = MotionTestValueKey<Float>("inactiveIconAlpha")
}

@Composable
internal fun colors(): SliderColors {
    val scheme = LocalAlphaColorScheme.current
    return SliderDefaults.colors().copy(
        inactiveTrackColor = scheme.neutral,
        activeTrackColor = scheme.accent,
        thumbColor = scheme.thumb,
        activeTickColor = scheme.onAccent,
        inactiveTickColor = scheme.onNeutral,
    )
}
