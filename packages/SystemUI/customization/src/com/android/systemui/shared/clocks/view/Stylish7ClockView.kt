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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.systemui.shared.clocks.ClockSettingsRepository
import java.text.SimpleDateFormat
import java.util.Locale

class Stylish7ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : AxClockView(context, attrs, defStyleAttr, defStyleRes) {

    override fun getTag(): String =
        if (isLargeClock) "Stylish7LargeClockView" else "Stylish7ClockView"

    @Composable
    override fun Content() {
        if (isLargeClock) LargeContent() else SmallContent()
    }

    @Composable
    private fun SmallContent() {
        val (time, date, isDoze, screenOff, regionDark) = rememberClockState()
        val dynSizeScale by ClockSettingsRepository.sizeScale.collectAsState()
        val tintColor = tintColor(isDoze, screenOff, regionDark)

        val accentColor = if (isDoze) Color.White else Color(0xFF5AC8FA)
        val bgColor = if (isDoze) Color.White.copy(alpha = 0.06f) else tintColor.copy(alpha = 0.08f)
        val dividerColor = if (isDoze) Color.White.copy(alpha = 0.3f) else tintColor.copy(alpha = 0.2f)

        val dayOfWeek = remember(date) { formatDayOfWeek() }
        val monthDay = remember(date) { formatMonthDay() }
        val (hours, minutes) = splitTimeLines(time)
        val timeDisplay = "$hours \u2022 $minutes"

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
            ClockBody(
                timeDisplay = timeDisplay,
                monthDay = monthDay,
                dayOfWeek = dayOfWeek,
                tintColor = tintColor,
                accentColor = accentColor,
                bgColor = bgColor,
                dividerColor = dividerColor,
                isDoze = isDoze,
                scale = dynSizeScale,
                profileSize = 36.dp,
                timeFontSize = 42f,
                dateFontSize = 14f,
                dayFontSize = 13f,
                dividerWidth = 2.dp,
                sectionHeight = 100.dp,
            )
        }
    }

    @Composable
    private fun LargeContent() {
        val (time, date, isDoze, screenOff, regionDark) = rememberClockState()
        val tintColor = tintColor(isDoze, screenOff, regionDark)

        val accentColor = if (isDoze) Color.White else Color(0xFF5AC8FA)
        val bgColor = if (isDoze) Color.White.copy(alpha = 0.06f) else tintColor.copy(alpha = 0.08f)
        val dividerColor = if (isDoze) Color.White.copy(alpha = 0.3f) else tintColor.copy(alpha = 0.2f)

        val dayOfWeek = remember(date) { formatDayOfWeek() }
        val monthDay = remember(date) { formatMonthDay() }
        val (hours, minutes) = splitTimeLines(time)
        val timeDisplay = "$hours \u2022 $minutes"

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ClockBody(
                timeDisplay = timeDisplay,
                monthDay = monthDay,
                dayOfWeek = dayOfWeek,
                tintColor = tintColor,
                accentColor = accentColor,
                bgColor = bgColor,
                dividerColor = dividerColor,
                isDoze = isDoze,
                scale = 1f,
                profileSize = 48.dp,
                timeFontSize = 56f,
                dateFontSize = 18f,
                dayFontSize = 16f,
                dividerWidth = 2.5.dp,
                sectionHeight = 140.dp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            EnhancedDateArea(
                textColor = tintColor,
                textSize = 16.sp,
                iconSize = 18.dp,
                rowArrangement = Arrangement.Center,
            )
        }
    }

    @Composable
    private fun ClockBody(
        timeDisplay: String,
        monthDay: String,
        dayOfWeek: String,
        tintColor: Color,
        accentColor: Color,
        bgColor: Color,
        dividerColor: Color,
        isDoze: Boolean,
        scale: Float,
        profileSize: Dp,
        timeFontSize: Float,
        dateFontSize: Float,
        dayFontSize: Float,
        dividerWidth: Dp,
        sectionHeight: Dp,
    ) {
        val shape = RoundedCornerShape(20.dp)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bgColor)
                    .padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(sectionHeight * scale),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(profileSize * scale)
                                .background(
                                    if (isDoze) Color.White.copy(alpha = 0.15f)
                                    else accentColor.copy(alpha = 0.2f),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "\u0CA0\u1D25\u0CA0",
                                style = TextStyle(
                                    fontSize = (profileSize.value * 0.35f * scale).sp,
                                    color = if (isDoze) Color.White else accentColor,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp * scale))

                    Box(
                        modifier = Modifier
                            .width(dividerWidth)
                            .fillMaxHeight(0.7f)
                            .background(dividerColor),
                    )

                    Spacer(modifier = Modifier.width(16.dp * scale))

                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Text(
                            text = timeDisplay,
                            style = TextStyle(
                                fontSize = (timeFontSize * scale).sp,
                                fontWeight = FontWeight.Black,
                                color = tintColor,
                                letterSpacing = (-1).sp,
                            ),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = monthDay,
                            style = TextStyle(
                                fontSize = (dateFontSize * scale).sp,
                                fontWeight = FontWeight.Medium,
                                color = tintColor.copy(alpha = if (isDoze) 0.6f else 0.7f),
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp * scale))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDoze) Color.White.copy(alpha = 0.08f)
                        else accentColor.copy(alpha = 0.15f),
                    )
                    .padding(horizontal = (16 * scale).dp, vertical = (6 * scale).dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dayOfWeek.uppercase(),
                    style = TextStyle(
                        fontSize = (dayFontSize * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDoze) Color.White else accentColor,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }

    private fun formatDayOfWeek(): String {
        val sdf = SimpleDateFormat("EEEE", interactor.locale)
        sdf.timeZone = interactor.calendar.timeZone
        return sdf.format(interactor.calendar.time)
    }

    private fun formatMonthDay(): String {
        val sdf = SimpleDateFormat("MMMM dd", interactor.locale)
        sdf.timeZone = interactor.calendar.timeZone
        return sdf.format(interactor.calendar.time)
    }
}
