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

package com.android.systemui.volume.dialog.ringer.ui.binder

import android.animation.ArgbEvaluator
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.LayoutRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionScene
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.app.tracing.coroutines.launchInTraced
import com.android.app.tracing.coroutines.launchTraced
import com.android.compose.theme.PlatformTheme
import com.android.systemui.alpha.style.common.LocalAlphaColorScheme
import com.android.systemui.alpha.style.common.defaultAlphaColorScheme
import com.android.systemui.alpha.style.volume.VolumeMaterialColors
import com.android.systemui.alpha.style.volume.VolumeSliderStyleManager
import com.android.systemui.alpha.style.volume.VolumeSliderStyleWrapper
import com.android.systemui.res.R
import com.android.systemui.volume.dialog.dagger.scope.VolumeDialogScope
import com.android.systemui.volume.dialog.ringer.ui.util.VolumeDialogRingerDrawerTransitionListener
import com.android.systemui.volume.dialog.ringer.ui.util.updateCloseState
import com.android.systemui.volume.dialog.ringer.ui.util.updateOpenState
import com.android.systemui.volume.dialog.ringer.ui.viewmodel.RingerButtonUiModel
import com.android.systemui.volume.dialog.ringer.ui.viewmodel.RingerButtonViewModel
import com.android.systemui.volume.dialog.ringer.ui.viewmodel.RingerDrawerState
import com.android.systemui.volume.dialog.ringer.ui.viewmodel.RingerViewModel
import com.android.systemui.volume.dialog.ringer.ui.viewmodel.RingerViewModelState
import com.android.systemui.volume.dialog.ringer.ui.viewmodel.VolumeDialogRingerDrawerViewModel
import com.android.systemui.volume.dialog.sliders.ui.VolumeDialogSliderDimensions
import com.android.systemui.volume.dialog.ui.binder.ViewBinder
import com.android.systemui.volume.dialog.ui.utils.getVolumeThumbOrButtonShapeForMode
import com.android.systemui.volume.dialog.ui.utils.rememberVolumeSliderShapeMode
import com.android.systemui.volume.dialog.ui.utils.VOLUME_SLIDER_SHAPE_DEFAULT
import com.android.systemui.volume.dialog.ui.utils.suspendAnimate
import com.android.systemui.volume.dialog.ui.viewmodel.VolumeDialogViewModel
import java.util.WeakHashMap
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.mapLatest

private const val CLOSE_DRAWER_DELAY = 300L
private const val BUTTON_MIN_VISIBLE_CHANGE = 0.05F

@OptIn(ExperimentalCoroutinesApi::class)
@VolumeDialogScope
class VolumeDialogRingerViewBinder
@Inject
constructor(
    private val viewModel: VolumeDialogRingerDrawerViewModel,
    private val dialogViewModel: VolumeDialogViewModel,
    private val volumeSliderStyleManager: VolumeSliderStyleManager,
) : ViewBinder {

    private val roundnessSpringForce =
        SpringForce(1F).apply {
            stiffness = 800F
            dampingRatio = 0.6F
        }

    private val colorSpringForce =
        SpringForce(1F).apply {
            stiffness = 3800F
            dampingRatio = 1F
        }

    private val rgbEvaluator = ArgbEvaluator()
    private val composeStates = WeakHashMap<ComposeView, RingerButtonComposeState>()

    override fun CoroutineScope.bind(view: View) {
        val volumeDialogBackgroundView = view.requireViewById<View>(R.id.volume_dialog_background)
        val ringerBackgroundView = view.requireViewById<View>(R.id.ringer_buttons_background)
        val drawerContainer = view.requireViewById<MotionLayout>(R.id.volume_ringer_drawer)

        val unselectedButtonUiModel = RingerButtonUiModel.getUnselectedButton(view.context)
        val selectedButtonUiModel = RingerButtonUiModel.getSelectedButton(view.context)

        val volumeDialogBgSmallRadius =
            view.context.resources.getDimensionPixelSize(
                R.dimen.volume_dialog_background_square_corner_radius,
            )
        val volumeDialogBgFullRadius =
            view.context.resources.getDimensionPixelSize(
                R.dimen.volume_dialog_background_corner_radius,
            )
        val bottomDefaultRadius = volumeDialogBgFullRadius.toFloat()
        val bottomCornerRadii =
            floatArrayOf(
                0F,
                0F,
                0F,
                0F,
                bottomDefaultRadius,
                bottomDefaultRadius,
                bottomDefaultRadius,
                bottomDefaultRadius,
            )

        var backgroundAnimationProgress: Float by
            Delegates.observable(0F) { _, _, progress ->
                ringerBackgroundView.applyCorners(
                    fullRadius = volumeDialogBgFullRadius,
                    diff = volumeDialogBgFullRadius - volumeDialogBgSmallRadius,
                    progress = progress,
                )
            }

        val ringerDrawerTransitionListener = VolumeDialogRingerDrawerTransitionListener {
            backgroundAnimationProgress = it
        }

        drawerContainer.setTransitionListener(ringerDrawerTransitionListener)
        volumeDialogBackgroundView.background = volumeDialogBackgroundView.background.mutate()
        ringerBackgroundView.background = ringerBackgroundView.background.mutate()

        launchTraced("VDRVB#addTouchableBounds") {
            dialogViewModel.addTouchableBounds(ringerBackgroundView)
        }

        viewModel.ringerViewModel
            .mapLatest { ringerState ->
                when (ringerState) {
                    is RingerViewModelState.Available -> {
                        val uiModel = ringerState.uiModel
                        val orientation =
                            if (
                                view.context.resources.getBoolean(
                                    R.bool.volume_dialog_ringer_drawer_should_open_to_the_side,
                                )
                            ) {
                                ringerState.orientation
                            } else {
                                Configuration.ORIENTATION_PORTRAIT
                            }

                        drawerContainer.visibility = View.VISIBLE
                        (volumeDialogBackgroundView.background as GradientDrawable).cornerRadii =
                            bottomCornerRadii

                        when (uiModel.drawerState) {
                            is RingerDrawerState.Initial -> {
                                drawerContainer.animateAndBindDrawerButtons(
                                    viewModel = viewModel,
                                    uiModel = uiModel,
                                    selectedButtonUiModel = selectedButtonUiModel,
                                    unselectedButtonUiModel = unselectedButtonUiModel,
                                )
                                ringerDrawerTransitionListener.setProgressChangeEnabled(true)
                                drawerContainer.closeDrawer(
                                    ringerBackground = ringerBackgroundView,
                                    selectedIndex = uiModel.currentButtonIndex,
                                    orientation = orientation,
                                )
                            }

                            is RingerDrawerState.Closed -> {
                                if (
                                    uiModel.selectedButton.ringerMode ==
                                        uiModel.drawerState.currentMode
                                ) {
                                    drawerContainer.animateAndBindDrawerButtons(
                                        viewModel = viewModel,
                                        uiModel = uiModel,
                                        selectedButtonUiModel = selectedButtonUiModel,
                                        unselectedButtonUiModel = unselectedButtonUiModel,
                                        onProgressChanged = { progress, isReverse ->
                                            backgroundAnimationProgress =
                                                if (isReverse) 1F - progress else progress
                                        },
                                    ) {
                                        if (
                                            uiModel.currentButtonIndex ==
                                                uiModel.availableButtons.size - 1
                                        ) {
                                            ringerDrawerTransitionListener
                                                .setProgressChangeEnabled(false)
                                        } else {
                                            ringerDrawerTransitionListener
                                                .setProgressChangeEnabled(true)
                                        }

                                        drawerContainer.closeDrawer(
                                            ringerBackground = ringerBackgroundView,
                                            selectedIndex = uiModel.currentButtonIndex,
                                            orientation = orientation,
                                        )
                                    }
                                }
                            }

                            is RingerDrawerState.Open -> {
                                drawerContainer.animateAndBindDrawerButtons(
                                    viewModel = viewModel,
                                    uiModel = uiModel,
                                    selectedButtonUiModel = selectedButtonUiModel,
                                    unselectedButtonUiModel = unselectedButtonUiModel,
                                )

                                if (
                                    uiModel.currentButtonIndex ==
                                        uiModel.availableButtons.size - 1
                                ) {
                                    ringerDrawerTransitionListener.setProgressChangeEnabled(false)
                                } else {
                                    ringerDrawerTransitionListener.setProgressChangeEnabled(true)
                                }

                                updateOpenState(drawerContainer, orientation, ringerBackgroundView)

                                drawerContainer
                                    .getTransition(R.id.close_to_open_transition)
                                    .setInterpolatorInfo(
                                        MotionScene.Transition.INTERPOLATE_REFERENCE_ID,
                                        null,
                                        R.anim.volume_dialog_ringer_open,
                                    )

                                drawerContainer.transitionToState(
                                    R.id.volume_dialog_ringer_drawer_open,
                                )
                                ringerBackgroundView.background =
                                    ringerBackgroundView.background.mutate()
                            }
                        }
                    }

                    is RingerViewModelState.Unavailable -> {
                        drawerContainer.visibility = View.GONE
                        volumeDialogBackgroundView.setBackgroundResource(
                            R.drawable.volume_dialog_background,
                        )
                    }
                }
            }
            .launchInTraced("VDRVB#ringerViewModel", this)
    }

    private suspend fun MotionLayout.animateAndBindDrawerButtons(
        viewModel: VolumeDialogRingerDrawerViewModel,
        uiModel: RingerViewModel,
        selectedButtonUiModel: RingerButtonUiModel,
        unselectedButtonUiModel: RingerButtonUiModel,
        onProgressChanged: (Float, Boolean) -> Unit = { _, _ -> },
        onAnimationEnd: Runnable? = null,
    ) {
        ensureChildCount(R.layout.volume_ringer_button, uiModel.availableButtons.size)

        if (
            uiModel.drawerState is RingerDrawerState.Closed &&
                uiModel.drawerState.currentMode != uiModel.drawerState.previousMode
        ) {
            val count = uiModel.availableButtons.size
            val selectedButton = getChildAt(count - uiModel.currentButtonIndex) as ComposeView
            val previousIndex =
                uiModel.availableButtons.indexOfFirst {
                    it.ringerMode == uiModel.drawerState.previousMode
                }
            val unselectedButton = getChildAt(count - previousIndex) as ComposeView

            coroutineScope {
                val selectedCornerRadius =
                    selectedButton.composeState().uiModel?.cornerRadius ?: 0
                if (selectedCornerRadius != selectedButtonUiModel.cornerRadius) {
                    launchTraced("VDRVB#selectedButtonAnimation") {
                        selectedButton.animateTo(
                            ringerButtonUiModel = selectedButtonUiModel,
                            onProgressChanged =
                                if (uiModel.currentButtonIndex == count - 1) {
                                    onProgressChanged
                                } else {
                                    { _, _ -> }
                                },
                        )
                    }
                }

                val unselectedCornerRadius =
                    unselectedButton.composeState().uiModel?.cornerRadius ?: 0
                if (unselectedCornerRadius != unselectedButtonUiModel.cornerRadius) {
                    launchTraced("VDRVB#unselectedButtonAnimation") {
                        unselectedButton.animateTo(
                            ringerButtonUiModel = unselectedButtonUiModel,
                            onProgressChanged =
                                if (previousIndex == count - 1) {
                                    onProgressChanged
                                } else {
                                    { _, _ -> }
                                },
                        )
                    }
                }

                launchTraced("VDRVB#bindButtons") {
                    delay(CLOSE_DRAWER_DELAY)
                    bindButtons(
                        viewModel = viewModel,
                        uiModel = uiModel,
                        onAnimationEnd = onAnimationEnd,
                        isAnimated = true,
                    )
                }
            }
        } else {
            bindButtons(
                viewModel = viewModel,
                uiModel = uiModel,
                onAnimationEnd = onAnimationEnd,
            )
        }
    }

    private fun MotionLayout.bindButtons(
        viewModel: VolumeDialogRingerDrawerViewModel,
        uiModel: RingerViewModel,
        onAnimationEnd: Runnable? = null,
        isAnimated: Boolean = false,
    ) {
        val count = uiModel.availableButtons.size
        val isOpen = uiModel.drawerState is RingerDrawerState.Open

        uiModel.availableButtons.fastForEachIndexed { index, ringerButton ->
            val buttonView = getChildAt(count - index) as ComposeView

            if (index == uiModel.currentButtonIndex) {
                buttonView.bindDrawerButton(
                    buttonViewModel = if (isOpen) ringerButton else uiModel.selectedButton,
                    viewModel = viewModel,
                    isOpen = isOpen,
                    isSelected = true,
                    isAnimated = isAnimated,
                )
            } else {
                buttonView.bindDrawerButton(
                    buttonViewModel = ringerButton,
                    viewModel = viewModel,
                    isOpen = isOpen,
                    isAnimated = isAnimated,
                )
            }
        }

        onAnimationEnd?.run()
    }

    private fun ComposeView.bindDrawerButton(
        buttonViewModel: RingerButtonViewModel,
        viewModel: VolumeDialogRingerDrawerViewModel,
        isOpen: Boolean,
        isSelected: Boolean = false,
        isAnimated: Boolean = false,
    ) {
        val state = composeState()
        val ringerContentDesc = context.getString(buttonViewModel.contentDescriptionResId)
        val resolvedContentDescription =
            if (isSelected && !isOpen) {
                context.getString(
                    R.string.volume_ringer_drawer_closed_content_description,
                    ringerContentDesc,
                )
            } else {
                ringerContentDesc
            }

        this.isSelected = isSelected
        contentDescription = resolvedContentDescription

        state.buttonViewModel = buttonViewModel
        state.contentDescription = resolvedContentDescription
        state.isSelected = isSelected
        state.onClick = {
            viewModel.onRingerButtonClicked(buttonViewModel.ringerMode, isSelected)
        }

        if (!isAnimated || state.uiModel == null) {
            state.uiModel =
                if (isSelected) {
                    RingerButtonUiModel.getSelectedButton(context)
                } else {
                    RingerButtonUiModel.getUnselectedButton(context)
                }
        }
    }

    private fun MotionLayout.ensureChildCount(@LayoutRes viewLayoutId: Int, count: Int) {
        val childCountDelta = childCount - count - 1
        when {
            childCountDelta > 0 -> {
                removeViews(0, childCountDelta)
            }

            childCountDelta < 0 -> {
                val inflater = LayoutInflater.from(context)
                repeat(-childCountDelta) {
                    inflater.inflate(viewLayoutId, this, true)
                    val child = getChildAt(childCount - 1) as ComposeView
                    child.id = View.generateViewId()
                    child.composeState()
                }
            }
        }
    }

    private fun MotionLayout.closeDrawer(
        ringerBackground: View,
        selectedIndex: Int,
        orientation: Int,
    ) {
        setTransition(R.id.close_to_open_transition)
        getTransition(R.id.close_to_open_transition)
            .setInterpolatorInfo(
                MotionScene.Transition.INTERPOLATE_REFERENCE_ID,
                null,
                R.anim.volume_dialog_ringer_close,
            )
        updateCloseState(this, selectedIndex, orientation, ringerBackground)
        transitionToState(R.id.volume_dialog_ringer_drawer_close)
    }

    private suspend fun ComposeView.animateTo(
        ringerButtonUiModel: RingerButtonUiModel,
        onProgressChanged: (Float, Boolean) -> Unit = { _, _ -> },
    ) {
        val state = composeState()
        val startUiModel = state.uiModel ?: ringerButtonUiModel

        val roundnessAnimation =
            SpringAnimation(FloatValueHolder(0F), 1F).setSpring(roundnessSpringForce)
        val colorAnimation = SpringAnimation(FloatValueHolder(0F), 1F).setSpring(colorSpringForce)

        val startRadius = startUiModel.cornerRadius.toFloat()
        val cornerRadiusDiff =
            (ringerButtonUiModel.cornerRadius - startUiModel.cornerRadius).toFloat()

        roundnessAnimation.minimumVisibleChange = BUTTON_MIN_VISIBLE_CHANGE
        colorAnimation.minimumVisibleChange = BUTTON_MIN_VISIBLE_CHANGE

        coroutineScope {
            launchTraced("VDRVB#colorAnimation") {
                colorAnimation.suspendAnimate { value ->
                    val fraction = value.coerceIn(0F, 1F)
                    val currentIconColor =
                        rgbEvaluator.evaluate(
                            fraction,
                            startUiModel.tintColor,
                            ringerButtonUiModel.tintColor,
                        ) as Int
                    val currentBgColor =
                        rgbEvaluator.evaluate(
                            fraction,
                            startUiModel.backgroundColor,
                            ringerButtonUiModel.backgroundColor,
                        ) as Int

                    val current = state.uiModel ?: startUiModel
                    state.uiModel =
                        current.copy(
                            tintColor = currentIconColor,
                            backgroundColor = currentBgColor,
                        )
                }
            }

            roundnessAnimation.suspendAnimate { value ->
                val fraction = value.coerceIn(0F, 1F)
                val current = state.uiModel ?: startUiModel

                onProgressChanged(fraction, cornerRadiusDiff > 0F)

                state.uiModel =
                    current.copy(
                        cornerRadius = (startRadius + fraction * cornerRadiusDiff).roundToInt(),
                    )
            }
        }

        state.uiModel = ringerButtonUiModel
    }

    private fun ComposeView.composeState(): RingerButtonComposeState {
        return composeStates.getOrPut(this) {
            RingerButtonComposeState().also { state ->
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    PlatformTheme {
                        VolumeDialogRingerButton(
                            state = state,
                            volumeSliderStyleManager = volumeSliderStyleManager,
                        )
                    }
                }
            }
        }
    }

    private fun View.applyCorners(fullRadius: Int, diff: Int, progress: Float) {
        val radius = fullRadius - progress * diff
        (background as GradientDrawable).cornerRadius = radius
        background.invalidateSelf()
    }
}

private class RingerButtonComposeState {
    var buttonViewModel by mutableStateOf<RingerButtonViewModel?>(null)
    var uiModel by mutableStateOf<RingerButtonUiModel?>(null)
    var contentDescription by mutableStateOf("")
    var isSelected by mutableStateOf(false)
    var onClick by mutableStateOf<() -> Unit>({})
}

@Composable
private fun VolumeDialogRingerButton(
    state: RingerButtonComposeState,
    volumeSliderStyleManager: VolumeSliderStyleManager,
    modifier: Modifier = Modifier,
) {
    val buttonViewModel = state.buttonViewModel ?: return
    val uiModel = state.uiModel ?: return

    val defaultScheme = defaultAlphaColorScheme()
    val styleState by volumeSliderStyleManager.styleState.collectAsStateWithLifecycle()
    val styleRenderer =
        remember(
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

    val themedScheme =
        remember(styleRenderer, defaultScheme) {
            styleRenderer?.produceColorScheme(defaultScheme) ?: defaultScheme
        }

    val backgroundColor =
        if (state.isSelected) {
            themedScheme.accent
        } else {
            themedScheme.neutral
        }

    val iconTint =
        if (state.isSelected) {
            themedScheme.onAccent
        } else {
            themedScheme.onNeutral
        }

    val visualButtonSize = VolumeDialogSliderDimensions.TrackThickness
    val shapeMode = rememberVolumeSliderShapeMode()
    val cornerRadiusDp = with(LocalDensity.current) { uiModel.cornerRadius.toDp() }
    val defaultShape = remember(cornerRadiusDp) { RoundedCornerShape(cornerRadiusDp) }
    val shape = getVolumeThumbOrButtonShapeForMode(
        shapeMode = shapeMode,
        sizeDp = visualButtonSize,
        defaultShape = defaultShape,
    )

    CompositionLocalProvider(LocalAlphaColorScheme provides themedScheme) {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .semantics(mergeDescendants = true) {
                        this.contentDescription = state.contentDescription
                        this.selected = state.isSelected
                        this.role = Role.Button
                    },
            contentAlignment = Alignment.Center,
        ) {
            VolumeSliderStyleWrapper(
                renderer = styleRenderer,
                shape = shape,
                segmentMode = false,
                isVertical = false,
                isActive = state.isSelected,
                materialColors =
                    VolumeMaterialColors(
                        activeSegment = backgroundColor,
                        inactiveSegment = backgroundColor,
                        activeButton = backgroundColor,
                        inactiveButton = backgroundColor,
                    ),
                modifier =
                    Modifier
                        .size(visualButtonSize)
                        .clip(shape)
                        .clickable(role = Role.Button, onClick = state.onClick),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(backgroundColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(buttonViewModel.imageResId),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(iconTint),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
