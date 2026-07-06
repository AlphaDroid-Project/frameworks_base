/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.shared.clocks.view

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.systemui.customization.clocks.R as clocksR
import com.android.systemui.shared.clocks.ClockSettingsRepository

/**
 * A minimal chrome-free lockscreen clock face that renders time digits using the
 * user-selected lockscreen font (config_clockFontFamily, overrideable via AlphaVisuals
 * lockscreen_clock_font RRO overlays) and date/weather/smartspace via EnhancedDateArea
 * from AxQuickLook. Serves as the default plain-digit option in the clock picker.
 */
class SimpleClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : AxClockView(context, attrs, defStyleAttr, defStyleRes) {

    override val clockHeightBase: Int
        get() {
            if (isLargeClock) return super.clockHeightBase
            val scaledDensity = context.resources.displayMetrics.scaledDensity
            val density = context.resources.displayMetrics.density
            // Match OldQuickLookClockView sizing: time text height + info area below
            val timePx = SMALL_PRIMARY_TEXT_SP * scaledDensity * scaleRatio
            val infoPx = SMALL_INFO_AREA_DP * density * scaleRatio
            return (timePx + infoPx).toInt()
        }

    @Composable
    override fun Content() {
        if (isLargeClock) LargeContent() else SmallContent()
    }

    @Composable
    private fun SmallContent() {
        val (time, _, isDoze, screenOff, regionDark) = rememberClockState()
        val tint = tintColor(isDoze, screenOff, regionDark)
        val timeFont = remember(state.fontVersion.intValue) {
            resolveClockTimeFontFamily(context.resources)
        }
        val dynSizeScale by scaleFlow.collectAsState()
        val dateBelow by state.dateBelowState
        val horizontalAlign = when {
            isLeftAligned -> Alignment.Start
            isRightAligned -> Alignment.End
            else -> Alignment.CenterHorizontally
        }
        val rowArrangement = when {
            isLeftAligned -> Arrangement.Start
            isRightAligned -> Arrangement.End
            else -> Arrangement.Center
        }
        val textAlign = when {
            isLeftAligned -> TextAlign.Start
            isRightAligned -> TextAlign.End
            else -> TextAlign.Center
        }
        val sidePadding = if (isSideAligned) {
            (clockPaddingStart / context.resources.displayMetrics.density).dp
        } else {
            0.dp
        }

        val timeStyle = TextStyle(
            fontSize = SMALL_PRIMARY_TEXT_SP.sp * dynSizeScale,
            fontWeight = FontWeight.Light,
            fontFamily = timeFont,
            color = tint,
            letterSpacing = (-1.5).sp,
        )

        val dateArea = @Composable {
            EnhancedDateArea(
                textColor = tint.copy(alpha = if (isDoze) 0.6f else 0.8f),
                textSize = 14.sp,
                iconSize = 16.dp,
                rowArrangement = rowArrangement,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (isRightAligned) 0.dp else sidePadding,
                    end = if (isRightAligned) sidePadding else 0.dp,
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = horizontalAlign,
        ) {
            // Date position honors the user setting (default "above"), matching other faces.
            if (!dateBelow) {
                dateArea()
                Spacer(modifier = Modifier.height(4.dp))
                FillWidthTime(time, timeStyle, textAlign)
            } else {
                FillWidthTime(time, timeStyle, textAlign)
                Spacer(modifier = Modifier.height(4.dp))
                dateArea()
            }
        }
    }

    /**
     * Renders the time digits scaled to the available width. In preview mode the digits grow
     * (capped) so the picker tile fills the same way the bitmap faces do; on the lockscreen
     * they keep the design size and only shrink if they would otherwise overflow the width.
     */
    @Composable
    private fun FillWidthTime(time: String, baseStyle: TextStyle, textAlign: TextAlign) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val availableWidthPx = with(density) { maxWidth.toPx() }
            val naturalWidthPx = remember(time, baseStyle, availableWidthPx) {
                measurer.measure(time, baseStyle, maxLines = 1).size.width.toFloat()
            }
            val rawScale = if (naturalWidthPx > 0f) availableWidthPx / naturalWidthPx else 1f
            val fitScale =
                if (isPreviewMode) rawScale.coerceIn(1f, PREVIEW_FILL_MAX_SCALE)
                else rawScale.coerceAtMost(1f)

            Text(
                text = time,
                maxLines = 1,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
                style = baseStyle.copy(fontSize = baseStyle.fontSize * fitScale),
            )
        }
    }

    @Composable
    private fun LargeContent() {
        val (time, _, isDoze, screenOff, regionDark) = rememberClockState()
        val tint = tintColor(isDoze, screenOff, regionDark)
        val timeFont = remember(state.fontVersion.intValue) {
            resolveClockTimeFontFamily(context.resources)
        }
        val (hours, minutes) = splitTimeLines(time)

        // Use large_clock_text_size dimen (150dp base / 170dp h700 / 200dp h800),
        // converted to Sp at runtime so it respects display density correctly.
        val largeFontSize = with(LocalDensity.current) {
            context.resources.getDimensionPixelSize(
                clocksR.dimen.large_clock_text_size
            ).toSp() * sizeScale
        }

        val horizontalAlign = when {
            isLeftAligned -> Alignment.Start
            isRightAligned -> Alignment.End
            else -> Alignment.CenterHorizontally
        }
        val textAlignment = when {
            isLeftAligned -> TextAlign.Start
            isRightAligned -> TextAlign.End
            else -> TextAlign.Center
        }
        val sidePadding = if (isSideAligned) {
            (clockPaddingStart / context.resources.displayMetrics.density).dp
        } else {
            0.dp
        }

        Box(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            contentAlignment = when {
                isLeftAligned -> Alignment.CenterStart
                isRightAligned -> Alignment.CenterEnd
                else -> Alignment.Center
            },
        ) {
            val timeStyle = TextStyle(
                fontSize = largeFontSize,
                fontWeight = FontWeight.Light,
                fontFamily = timeFont,
                color = tint,
                lineHeight = largeFontSize,
                textAlign = textAlignment,
                letterSpacing = (-2).sp,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (isRightAligned) 0.dp else sidePadding,
                        end = if (isRightAligned) sidePadding else 0.dp,
                    ),
                horizontalAlignment = horizontalAlign,
                verticalArrangement = Arrangement.Center,
            ) {
                FittedTimeColumn(
                    hours = hours,
                    minutes = minutes,
                    baseStyle = timeStyle,
                    horizontalAlignment = horizontalAlign,
                )

                Spacer(modifier = Modifier.height(16.dp))

                EnhancedDateArea(
                    textColor = tint.copy(alpha = if (isDoze) 0.6f else 0.85f),
                    textSize = 16.sp,
                    iconSize = 18.dp,
                    rowArrangement = when {
                        isLeftAligned -> Arrangement.Start
                        isRightAligned -> Arrangement.End
                        else -> Arrangement.Center
                    },
                )
            }
        }
    }

    override fun getTag(): String = if (isLargeClock) "SimpleLargeClockView" else "SimpleClockView"

    companion object {
        // Small clock: matches OldQuickLookClockView sizing constants
        private const val SMALL_PRIMARY_TEXT_SP = 80f
        private const val SMALL_INFO_AREA_DP = 60f
        // Upper bound for growing the small digits to fill the picker preview tile width,
        // so SIMPLE expands to the same visual weight as the bitmap-digit faces.
        private const val PREVIEW_FILL_MAX_SCALE = 2.2f
    }
}
