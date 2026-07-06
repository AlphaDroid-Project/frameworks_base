/*
 * Copyright (C) 2025-2026 AxionOS Project
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

package com.android.systemui.shared.clocks.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.icu.util.TimeZone
import android.text.format.DateFormat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.systemui.customization.R
import com.android.systemui.log.core.MessageBuffer
import com.android.systemui.plugins.keyguard.data.model.AlarmData
import com.android.systemui.plugins.keyguard.ui.clocks.*
import com.android.systemui.shared.clocks.ClockConfigs
import com.android.systemui.shared.clocks.ClockSettingsRepository
import com.android.systemui.shared.clocks.extensions.*
import java.util.Locale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Base view for all AlphaDroid custom lockscreen clocks.
 * 
 * This class hosts Compose-based clock faces (`SmallContent` and `LargeContent`) within
 * a traditional Android View hierarchy (`ConstraintLayout` via `ClockSection`). It manages
 * state propagation (doze, region dark, scale, user configuration) so subclasses only need
 * to focus on rendering.
 *
 * ### Architectural Guidelines for Subclasses
 * 
 * 1. **Horizontal Alignment (`isLeftAligned`, `isRightAligned`)**:
 *    Subclasses MUST respect the user's horizontal alignment preference in both small and 
 *    large modes. This is typically done by passing appropriate `horizontalAlignment` and
 *    `padding` parameters to the root `Column` or `Box`.
 *
 * 2. **Large Clock Vertical Centering**:
 *    The `ConstraintLayout` container provides a fixed vertical ceiling via `MATCH_CONSTRAINT`.
 *    To ensure proper vertical centering within this space, large clock composables MUST use 
 *    `Modifier.fillMaxSize()` on their root element combined with `verticalArrangement = Arrangement.Center`
 *    (or `contentAlignment = Alignment.Center` for `Box`). Do not use `wrapContentHeight()`.
 *
 * 3. **Date and Weather Area (`EnhancedDateArea`)**:
 *    Use the provided `EnhancedDateArea` composable to render smartspace/date/weather. It 
 *    automatically respects horizontal alignment constraints and handles proportional scaling 
 *    (`sizeScale`) so the date grows appropriately when the "Large" size toggle is enabled.
 *    Do not hard-code `rowArrangement = Arrangement.Center` unless explicitly required by the design.
 */
abstract class AxClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes) {

    val state = AxClockState()
    val quickLook = QuickLookController(this)
    val interactor = AxClockInteractor(context, state, quickLook)
    val viewModel = AxClockViewModel(state, quickLook)
    internal val host = AxClockHost(this)

    private var uiScope: CoroutineScope? = null

    var isLargeClock = false

    // Max height (px) the host allows for this clock, or 0 when unconstrained. Fed from the
    // keyguard/preview target region via AxClockFaceController.onTargetRegionChanged. Used by
    // onMeasure to clamp the large clock so its content never paints past the bottom anchor
    // (the UDFPS / lock icon).
    var maxRenderHeightPx: Int = 0
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    // Set during onMeasure for the large clock: ratio (<=1f) applied to the compose host so
    // content that would exceed the height ceiling shrinks to fit instead of overflowing.
    private var largeContentScale = 1f

    var isPreviewMode = false
        set(value) {
            field = value
            if (value) {
                touchEnabled = false
                depthEffectEnabled = false
                animAlpha = 1f
            }
        }

    var animAlpha: Float = 1f
        set(value) {
            if (isPreviewMode && value != 1f) return
            field = value
            alpha = value
        }

    var touchEnabled: Boolean = true

    private val depthController = ClockDepthController(this)
    var depthEffectEnabled: Boolean
        get() = depthController.enabled
        set(value) { depthController.enabled = value }

    protected open val clockHeightBase: Int get() = context.scaledDimenInt(R.dimen.clock_height)
    val clockPaddingTop get() = context.scaledDimen(R.dimen.clock_padding_top)
    val clockPaddingStart get() = context.scaledDimen(R.dimen.clock_padding_start)
    val clockDateTextSize get() = context.scaledDimen(R.dimen.clock_date_text_size)
    val clockDateMarginTop get() = context.scaledDimen(R.dimen.clock_date_margin_top)
    val scaleRatio get() = context.scaleRatio

    val scaleFlow: StateFlow<Float>
        get() = if (isLargeClock) ClockSettingsRepository.largeScale else ClockSettingsRepository.smallScale

    val sizeScale: Float
        get() = if (isPreviewMode) 1f else scaleFlow.value

    val iconSize get() = context.scaledDimenInt(R.dimen.clock_icon_secondary_size)

    protected val config: ClockConfigs.ClockStyleConfig?
        get() {
            val className = this::class.simpleName ?: return null
            return ClockConfigs.resolveConfig(className, isLargeClock, state.alignmentState.value)
        }

    val isLeftAligned: Boolean get() = config?.align == ClockConfigs.Align.LEFT
    val isRightAligned: Boolean get() = config?.align == ClockConfigs.Align.RIGHT
    val isSideAligned: Boolean get() = isLeftAligned || isRightAligned

    val clockHeight: Int
        get() {
            val resHeight = config?.customHeightRes?.let { context.scaledDimenInt(it) } ?: clockHeightBase
            val bottomPad = if (!isLargeClock) {
                (SMALL_CLOCK_BOTTOM_PAD_DP * context.resources.displayMetrics.density).toInt()
            } else 0
            return ((resHeight + dateHeight) * sizeScale).toInt() + bottomPad
        }

    val dateMarginTop: Int
        get() {
            val cfg = config ?: return 0
            if (!cfg.visible) return 0
            return (cfg.customDateMarginTop?.let { context.scaledDimen(it) } ?: clockDateMarginTop).toInt()
        }

    val dateHeight: Int
        get() {
            val cfg = config ?: return 0
            if (!cfg.visible) return 0
            return when (cfg.position) {
                ClockConfigs.Position.ABOVE -> (clockDateTextSize + dateMarginTop + clockPaddingTop).toInt()
                ClockConfigs.Position.BELOW -> clockDateTextSize.toInt()
                else -> 0
            }
        }

    var isDoze: Boolean
        get() = state.isDoze
        set(value) { state.isDoze = value }
    var isScreenOff: Boolean
        get() = state.isScreenOff
        set(value) { state.isScreenOff = value }
    var isRegionDark: Boolean
        get() = state.isRegionDark
        set(value) { state.isRegionDark = value }
    val dateStr: String get() = state.dateStr

    init {
        host.attach { Content() }
    }

    abstract override fun getTag(): String

    @Composable
    protected abstract fun Content()

    internal open val useGlitchInteraction: Boolean = false

    open fun onAlarmDataChanged(data: AlarmData) { interactor.onAlarmDataChanged(data) }
    open fun onClockDataChanged(data: ClockData) { interactor.onClockDataChanged(data) }
    open fun onDateChanged() {}
    open fun onThemeChanged(isDarkTheme: Boolean) {}
    open fun onPlaybackStateChanged(playing: Boolean) { interactor.onPlaybackStateChanged(playing) }
    open fun onMetadataChanged(track: String, artist: String, packageName: String) { interactor.onMetadataChanged(track, artist, packageName) }
    open fun onNowPlayingUpdate(npText: String) { interactor.onNowPlayingUpdate(npText) }
    open fun onClockLayoutChanged(isCentered: Boolean, isLargeClockVisible: Boolean) {}
    fun onDepthEffectVisibilityChanged(visible: Boolean) { depthController.setDepthVisible(visible) }
    fun setMessageBuffer(buffer: MessageBuffer) {}
    open fun onDozeChanged(doze: Boolean) { interactor.onDozeChanged(doze) }
    open fun onChargeAnimation() {}
    open fun onPulsingChanged(doze: Boolean) {}
    open fun onScreenOff(screenOff: Boolean) { interactor.onScreenOff(screenOff) }
    open fun onRegionDarknessChanged(regionDark: Boolean) { interactor.onRegionDarknessChanged(regionDark) }
    open fun onFontSettingChanged() { interactor.onFontSettingChanged() }
    open fun onTimeZoneChanged(timeZone: TimeZone) { interactor.onTimeZoneChanged(timeZone) }

    open fun onDozeAmountChanged(linear: Float, eased: Float) {
        if (isPreviewMode) return
        state.dozeAmountFlow.value = eased
    }

    open fun onStartedWakingUp() {
        interactor.onStartedWakingUp()
        uiScope?.launch {
            delay(1250)
            interactor.refreshTime()
        }
    }

    open fun onStartedGoingToSleep(isKeyguardVisible: Boolean) {}
    open fun onWakefulnessStateChanged(isWakingUp: Boolean, tapPosition: Point?) {}

    open fun refreshFormat(use24: Boolean, newLocale: Locale = interactor.locale) {
        interactor.needsSeconds = (this as? BitmapDigitComposeClockView)?.faceStyle?.needsPerSecondTick == true
        // OldQuickLook and Simple render the time as a single line and need the colon
        // separator; bitmap/stacked faces split hh/mm themselves and use the compact pattern.
        interactor.useStandardFormat = this is OldQuickLookClockView || this is SimpleClockView
        interactor.refreshFormat(use24, newLocale)
    }

    open fun refreshTime() {
        if (interactor.refreshTime()) {
            contentDescription = interactor.talkBackContent
        }
    }

    fun refreshDate() { interactor.refreshDate() }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (!touchEnabled) return false
        return super.dispatchTouchEvent(ev)
    }

    override fun draw(canvas: Canvas) {
        if (!depthController.shouldApplyDepth()) {
            super.draw(canvas)
            return
        }
        depthController.drawWithDepth(canvas) { super.draw(it) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(tag, "onAttachedToWindow")
        ClockSettingsRepository.init(context)
        uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        (parent as? ViewGroup)?.let {
            it.clipChildren = false
            it.clipToPadding = false
        }
        // Don't clip the compose host: user position offsets translate it, potentially past our
        // own bounds.
        clipChildren = false
        clipToPadding = false
        depthController.onAttached()
        uiScope?.launch {
            val scaleFlow = if (isLargeClock) ClockSettingsRepository.largeScale else ClockSettingsRepository.smallScale
            scaleFlow.collect { requestLayout() }
        }
        uiScope?.launch {
            val alignFlow = if (isLargeClock) ClockSettingsRepository.largeAlignment else ClockSettingsRepository.smallAlignment
            alignFlow.collect { state.alignmentState.value = it }
        }
        uiScope?.launch {
            val offsetXFlow = if (isLargeClock) ClockSettingsRepository.largeOffsetX else ClockSettingsRepository.smallOffsetX
            offsetXFlow.collect { dp -> applyUserOffsetX(dp) }
        }
        uiScope?.launch {
            val offsetYFlow = if (isLargeClock) ClockSettingsRepository.largeOffsetY else ClockSettingsRepository.smallOffsetY
            offsetYFlow.collect { dp -> applyUserOffsetY(dp) }
        }
        refreshTime()
        state.timeState.value = interactor.timeStr
        state.dateBelowState.value = ClockSettingsRepository.isDateBelow.value
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(tag, "onDetachedFromWindow")
        uiScope?.cancel()
        depthController.onDetached()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newLocale = newConfig.locale
        if (newLocale != interactor.locale) {
            uiScope?.launch { refreshFormat(DateFormat.is24HourFormat(context), newLocale) }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val cv = host.view
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val maxH = MeasureSpec.getSize(heightMeasureSpec)

        cv.measure(
            MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
        )
        val naturalH = cv.measuredHeight

        if (isLargeClock) {
            // Upper bound from the host: the measure-spec ceiling (constrainMaxHeight in
            // ClockSection) and/or the target region height, whichever is set and smaller.
            val specCeiling = if (mode != MeasureSpec.UNSPECIFIED) maxH else Int.MAX_VALUE
            val regionCeiling = if (maxRenderHeightPx > 0) maxRenderHeightPx else Int.MAX_VALUE
            val ceiling = minOf(specCeiling, regionCeiling)

            // Shrink content to fit when it would exceed the ceiling.
            largeContentScale =
                if (ceiling != Int.MAX_VALUE && naturalH > ceiling && naturalH > 0)
                    ceiling.toFloat() / naturalH.toFloat()
                else 1f

            // Report the full available height so the view fills the ConstraintLayout slot;
            // applyLargeContentScale() will centre the (possibly scaled-down) content via
            // translationY.
            val viewH = if (ceiling != Int.MAX_VALUE) ceiling else naturalH
            setMeasuredDimension(w, viewH)
            if (w > 0 && naturalH > 0) {
                cv.measure(
                    MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(naturalH, MeasureSpec.EXACTLY),
                )
            }
            applyLargeContentScale()
            return
        }

        val floor = clockHeight
        val finalH = when (mode) {
            MeasureSpec.EXACTLY -> maxOf(naturalH, maxH, floor)
            else -> maxOf(naturalH, floor)
        }
        setMeasuredDimension(w, finalH)
        if (w > 0 && finalH > 0 && finalH != naturalH) {
            cv.measure(
                MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(finalH, MeasureSpec.EXACTLY),
            )
        }
    }

    private fun applyLargeContentScale() {
        val cv = host.view
        if (largeContentScale >= 1f) {
            cv.scaleX = 1f
            cv.scaleY = 1f
        } else {
            // Pivot the shrink at the aligned edge so a left/right-aligned clock stays flush
            // to that edge instead of drifting toward centre when the height clamp fires.
            val cvWidth = (cv.width.takeIf { it > 0 } ?: width).toFloat()
            cv.pivotX = when {
                isLeftAligned -> 0f
                isRightAligned -> cvWidth
                else -> cvWidth / 2f
            }
            cv.pivotY = 0f
            cv.scaleX = largeContentScale
            cv.scaleY = largeContentScale
        }

        // Center vertically if there is leftover space, then fold in the user vertical offset.
        val viewHeight = height.takeIf { it > 0 } ?: measuredHeight
        val contentHeight = (cv.measuredHeight * largeContentScale).toInt()
        cv.translationY =
            if (viewHeight > 0 && contentHeight > 0 && viewHeight > contentHeight) {
                (viewHeight - contentHeight) / 2f + userOffsetYPx
            } else {
                userOffsetYPx
            }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val cv = host.view
        if (!cv.isAttachedToWindow) return
        if (isLargeClock) {
            // Lay the compose host out at its full natural height so its content composes
            // without truncation; largeContentScale (applied in onMeasure) shrinks it into
            // the clamped view bounds, anchored at the top.
            val naturalH = cv.measuredHeight.takeIf { it > 0 } ?: height
            cv.layout(0, 0, width, naturalH)
            applyLargeContentScale()
            return
        }
        if (isPreviewMode) {
            cv.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
            )
        }
        cv.layout(0, 0, width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        pivotX = w / 2f
        pivotY = h / 2f
    }

    // User-configurable clock position offsets (px), applied to the compose host rather than this
    // view. The keyguard root binder owns this view's translationX/Y for burn-in and swipe
    // transitions and would clobber an offset placed here; the host child is never touched by it.
    // Skipped in preview mode so the picker tiles stay centred.
    private var userOffsetXPx = 0f
    private var userOffsetYPx = 0f

    private fun applyUserOffsetX(dp: Int) {
        userOffsetXPx = if (isPreviewMode) 0f else dp * resources.displayMetrics.density
        host.view.translationX = userOffsetXPx
    }

    private fun applyUserOffsetY(dp: Int) {
        userOffsetYPx = if (isPreviewMode) 0f else dp * resources.displayMetrics.density
        // Large clock folds the offset into its vertical centring; small applies it directly.
        if (isLargeClock) applyLargeContentScale() else host.view.translationY = userOffsetYPx
    }

    protected open fun getContentBounds(): RectF? = null

    open fun setupPreview() {
        interactor.setupPreview {
            isPreviewMode = true
            isDoze = false
            isScreenOff = false
            isRegionDark = false
        }
    }

    @Composable
    protected fun rememberClockState(): ClockUiState = viewModel.rememberClockState()

    @Composable
    protected fun tintColor(isDoze: Boolean, screenOff: Boolean, regionDark: Boolean): Color =
        viewModel.tintColor(isDoze, screenOff, regionDark)

    @Composable
    protected fun inverseSizeScaleModifier(): Modifier = Modifier

    @Composable
    protected fun digitScaleModifier(): Modifier {
        if (isPreviewMode) return Modifier
        val scaleFlow = if (isLargeClock) ClockSettingsRepository.largeScale else ClockSettingsRepository.smallScale
        val scaleValue by scaleFlow.collectAsState()
        if (scaleValue == 1f) return Modifier
        return Modifier.graphicsLayer {
            scaleX = scaleValue
            scaleY = scaleValue
        }
    }

    @Composable
    protected fun EnhancedDateArea(
        modifier: Modifier = Modifier,
        textColor: Color = tintColor(state.dozeFlow.value, state.screenOffFlow.value, state.regionDarkFlow.value)
            .copy(alpha = if (state.dozeFlow.value) 0.6f else 0.8f),
        textSize: TextUnit = 18.sp,
        fontFamily: FontFamily = remember(state.fontVersion.intValue) { resolveDateFontFamily() },
        fontWeight: FontWeight = FontWeight.Medium,
        letterSpacing: TextUnit = 0.sp,
        iconSize: Dp = 16.dp,
        uppercase: Boolean = false,
        rowArrangement: Arrangement.Horizontal = when {
            isLeftAligned -> Arrangement.Start
            isRightAligned -> Arrangement.End
            else -> Arrangement.Center
        },
    ) {
        val display = viewModel.rememberResolvedDisplay()
        val inverseModifier = inverseSizeScaleModifier()
        // Scale date text & icons proportionally with the user's size toggle so the
        // date area doesn't look disproportionately small next to scaled clock digits.
        val scaledTextSize = textSize * sizeScale
        val scaledIconSize = iconSize * sizeScale
        QuickLookDateArea(
            modifier = modifier.then(inverseModifier),
            display = display,
            dateStr = state.dateStr,
            sizeScale = 1f,
            textColor = textColor,
            textSize = scaledTextSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            iconSize = scaledIconSize,
            uppercase = uppercase,
            rowArrangement = rowArrangement,
        )
    }

    companion object {
        const val DEBUG = false
    }
}
