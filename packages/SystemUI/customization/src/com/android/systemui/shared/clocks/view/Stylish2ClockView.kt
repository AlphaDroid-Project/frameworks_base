/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.shared.clocks.view

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.systemui.shared.clocks.ClockSettingsRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Stylish2ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : AxClockView(context, attrs, defStyleAttr, defStyleRes) {

    override fun getTag(): String =
        if (isLargeClock) "Stylish2LargeClockView" else "Stylish2ClockView"

    @Composable
    override fun Content() {
        if (isLargeClock) LargeContent() else SmallContent()
    }

    @Composable
    private fun SmallContent() {
        val (time, date, isDoze, screenOff, regionDark) = rememberClockState()
        val dynSizeScale by ClockSettingsRepository.sizeScale.collectAsState()
        val tintColor = tintColor(isDoze, screenOff, regionDark)

        val bgColor = if (isDoze) Color.Transparent else tintColor.copy(alpha = 0.12f)
        val textOnBg = tintColor
        val greetingAlpha = if (isDoze) 0.6f else 0.85f
        val dayOfWeek = remember(date) { formatDayOfWeek() }
        val monthDay = remember(date) { formatMonthDay() }

        val (hours, minutes) = splitTimeLines(time)

        val contentAlign = when {
            isLeftAligned -> Alignment.CenterStart
            isRightAligned -> Alignment.CenterEnd
            else -> Alignment.Center
        }
        val sidePadding = if (isSideAligned) {
            (clockPaddingStart / context.resources.displayMetrics.density).dp
        } else {
            0.dp
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (isRightAligned) 0.dp else sidePadding,
                    end = if (isRightAligned) sidePadding else 0.dp,
                ),
            contentAlignment = contentAlign,
        ) {
            Column(
                horizontalAlignment = when {
                    isLeftAligned -> Alignment.Start
                    isRightAligned -> Alignment.End
                    else -> Alignment.CenterHorizontally
                },
            ) {
                Text(
                    text = "Have a great",
                    style = TextStyle(
                        fontSize = 28.sp * dynSizeScale,
                        fontWeight = FontWeight.Bold,
                        color = tintColor.copy(alpha = greetingAlpha),
                    ),
                )
                Text(
                    text = "$dayOfWeek !",
                    style = TextStyle(
                        fontSize = 28.sp * dynSizeScale,
                        fontWeight = FontWeight.Bold,
                        color = tintColor.copy(alpha = greetingAlpha),
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TimeBox(hours, textOnBg, bgColor, dynSizeScale, isDoze)

                    Spacer(modifier = Modifier.width(8.dp))
                    ColonDots(tintColor, dynSizeScale)
                    Spacer(modifier = Modifier.width(8.dp))

                    TimeBox(minutes, textOnBg, bgColor, dynSizeScale, isDoze)

                    Spacer(modifier = Modifier.width(8.dp))

                    DateBox(monthDay, textOnBg, bgColor, dynSizeScale, isDoze)
                }
            }
        }
    }

    @Composable
    private fun LargeContent() {
        val (time, date, isDoze, screenOff, regionDark) = rememberClockState()
        val tintColor = tintColor(isDoze, screenOff, regionDark)

        val bgColor = if (isDoze) Color.Transparent else tintColor.copy(alpha = 0.12f)
        val textOnBg = tintColor
        val greetingAlpha = if (isDoze) 0.6f else 0.85f
        val dayOfWeek = remember(date) { formatDayOfWeek() }
        val monthDay = remember(date) { formatMonthDay() }

        val boxScale = LARGE_BASE_SCALE * sizeScale
        val greetingSize = 36.sp * sizeScale

        val (hours, minutes) = splitTimeLines(time)

        val horizontalAlign = when {
            isLeftAligned -> Alignment.Start
            isRightAligned -> Alignment.End
            else -> Alignment.CenterHorizontally
        }
        val sidePadding = if (isSideAligned) {
            (clockPaddingStart / context.resources.displayMetrics.density).dp
        } else {
            0.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (isRightAligned) 0.dp else sidePadding,
                    end = if (isRightAligned) sidePadding else 0.dp,
                ),
            horizontalAlignment = horizontalAlign,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Have a great",
                style = TextStyle(
                    fontSize = greetingSize,
                    fontWeight = FontWeight.Bold,
                    color = tintColor.copy(alpha = greetingAlpha),
                ),
            )
            Text(
                text = "$dayOfWeek !",
                style = TextStyle(
                    fontSize = greetingSize,
                    fontWeight = FontWeight.Bold,
                    color = tintColor.copy(alpha = greetingAlpha),
                ),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                TimeBox(hours, textOnBg, bgColor, boxScale, isDoze)

                Spacer(modifier = Modifier.width(10.dp))
                ColonDots(tintColor, boxScale)
                Spacer(modifier = Modifier.width(10.dp))

                TimeBox(minutes, textOnBg, bgColor, boxScale, isDoze)

                Spacer(modifier = Modifier.width(10.dp))

                DateBox(monthDay, textOnBg, bgColor, boxScale, isDoze)
            }

            Spacer(modifier = Modifier.height(12.dp))

            EnhancedDateArea(
                textColor = tintColor,
                textSize = 16.sp,
                iconSize = 18.dp,
            )
        }
    }

    @Composable
    private fun TimeBox(
        text: String,
        textColor: Color,
        bgColor: Color,
        scale: Float,
        isDoze: Boolean,
    ) {
        val shape = RoundedCornerShape(16.dp)
        val bg = if (isDoze) Color.White.copy(alpha = 0.08f) else bgColor
        Box(
            modifier = Modifier
                .clip(shape)
                .background(bg)
                .padding(horizontal = (16 * scale).dp, vertical = (10 * scale).dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = TextStyle(
                    fontSize = 48.sp * scale,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    letterSpacing = (-1).sp,
                ),
            )
        }
    }

    @Composable
    private fun ColonDots(color: Color, scale: Float) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size((6 * scale).dp)
                    .background(color, CircleShape),
            )
            Box(
                modifier = Modifier
                    .size((6 * scale).dp)
                    .background(color, CircleShape),
            )
        }
    }

    @Composable
    private fun DateBox(
        text: String,
        textColor: Color,
        bgColor: Color,
        scale: Float,
        isDoze: Boolean,
    ) {
        val shape = RoundedCornerShape(16.dp)
        val bg = if (isDoze) Color.White.copy(alpha = 0.08f) else bgColor
        Box(
            modifier = Modifier
                .clip(shape)
                .background(bg)
                .padding(horizontal = (12 * scale).dp, vertical = (10 * scale).dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = TextStyle(
                    fontSize = 16.sp * scale,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.8f),
                ),
            )
        }
    }

    private fun formatDayOfWeek(): String {
        val sdf = SimpleDateFormat("EEEE", interactor.locale)
        sdf.timeZone = interactor.calendar.timeZone
        return sdf.format(interactor.calendar.time)
    }

    private fun formatMonthDay(): String {
        val sdf = SimpleDateFormat("MMM dd", interactor.locale)
        sdf.timeZone = interactor.calendar.timeZone
        return sdf.format(interactor.calendar.time)
    }

    companion object {
        // Base multiplier for the boxed digits on the large clock, before the user's Large
        // size toggle (sizeScale) is applied. Matches the previous fixed 1.3f at scale 1.0.
        private const val LARGE_BASE_SCALE = 1.3f
    }
}
