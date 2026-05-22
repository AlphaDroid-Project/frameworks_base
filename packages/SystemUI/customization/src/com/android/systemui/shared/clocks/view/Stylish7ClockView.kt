/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.shared.clocks.view

import android.content.Context
import android.graphics.Bitmap
import android.os.UserManager
import android.util.AttributeSet
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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

    private val userManager: UserManager? =
        context.getSystemService(Context.USER_SERVICE) as? UserManager

    private fun loadUserAvatar(): Bitmap? {
        return try {
            val userId = android.app.ActivityManager.getCurrentUser()
            var icon = userManager?.getUserIcon(userId)
            if (icon == null) {
                val drawable = com.android.internal.util.UserIcons.getDefaultUserIcon(
                    context.resources, userId, false
                )
                icon = com.android.internal.util.UserIcons.convertToBitmap(drawable)
            }
            icon
        } catch (_: Exception) { null }
    }

    private fun loadUserName(): String {
        return try {
            val userId = android.app.ActivityManager.getCurrentUser()
            val userInfo = userManager?.getUserInfo(userId)
            userInfo?.name?.takeIf { it.isNotBlank() } ?: "\u0CA0\u1D25\u0CA0"
        } catch (_: Exception) { "\u0CA0\u1D25\u0CA0" }
    }

    @Composable
    override fun Content() {
        if (isLargeClock) LargeContent() else SmallContent()
    }

    @Composable
    private fun SmallContent() {
        val (time, date, isDoze, screenOff, regionDark) = rememberClockState()
        val dynSizeScale by ClockSettingsRepository.sizeScale.collectAsState()

        val accent1 = Color(context.getColor(android.R.color.system_accent1_600))
        val accent3 = Color(context.getColor(android.R.color.system_accent3_600))

        val dayOfWeek = remember(date) { formatDayOfWeek() }
        val monthDay = remember(date) { formatMonthDay() }
        val (hours, minutes) = splitTimeLines(time)
        val timeDisplay = "$hours \u2022 $minutes"
        val avatar = remember { loadUserAvatar() }
        val userName = remember { loadUserName() }

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
                accent1 = accent1,
                accent3 = accent3,
                isDoze = isDoze,
                scale = dynSizeScale,
                avatar = avatar,
                userName = userName,
                profileFrameSize = 75.dp,
                profileImageSize = 64.dp,
                timeFontSize = 34f,
                dateFontSize = 16f,
                dayFontSize = 20f,
                userNameSize = 14f,
                dividerHeight = 32.dp,
            )
        }
    }

    @Composable
    private fun LargeContent() {
        val (time, date, isDoze, screenOff, regionDark) = rememberClockState()
        val dynSizeScale by ClockSettingsRepository.sizeScale.collectAsState()

        val accent1 = Color(context.getColor(android.R.color.system_accent1_600))
        val accent3 = Color(context.getColor(android.R.color.system_accent3_600))

        val dayOfWeek = remember(date) { formatDayOfWeek() }
        val monthDay = remember(date) { formatMonthDay() }
        val (hours, minutes) = splitTimeLines(time)
        val timeDisplay = "$hours \u2022 $minutes"
        val avatar = remember { loadUserAvatar() }
        val userName = remember { loadUserName() }

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
                accent1 = accent1,
                accent3 = accent3,
                isDoze = isDoze,
                scale = dynSizeScale,
                avatar = avatar,
                userName = userName,
                profileFrameSize = 96.dp,
                profileImageSize = 82.dp,
                timeFontSize = 38f,
                dateFontSize = 20f,
                dayFontSize = 24f,
                userNameSize = 18f,
                dividerHeight = 40.dp,
            )
        }
    }

    @Composable
    private fun ClockBody(
        timeDisplay: String,
        monthDay: String,
        dayOfWeek: String,
        accent1: Color,
        accent3: Color,
        isDoze: Boolean,
        scale: Float,
        avatar: Bitmap?,
        userName: String,
        profileFrameSize: Dp,
        profileImageSize: Dp,
        timeFontSize: Float,
        dateFontSize: Float,
        dayFontSize: Float,
        userNameSize: Float,
        dividerHeight: Dp,
    ) {
        val bgBrush = if (isDoze) {
            Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f)))
        } else {
            Brush.horizontalGradient(listOf(accent1, accent3))
        }
        val dayPillBg = if (isDoze) Color.White.copy(alpha = 0.08f) else Color.White
        val dayTextColor = if (isDoze) Color.White else accent1

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                // Background Card
                Box(
                    modifier = Modifier
                        .padding(top = (profileFrameSize * scale) / 2)
                        .clip(RoundedCornerShape(20.dp * scale))
                        .background(bgBrush)
                ) {
                    // Foreground Content
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(
                            start = 20.dp * scale,
                            end = 20.dp * scale,
                            top = 0.dp,
                            bottom = 24.dp * scale
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.width(profileFrameSize * scale)
                        ) {
                            Spacer(modifier = Modifier.height((profileFrameSize * scale) / 2))
                            Spacer(modifier = Modifier.height(8.dp * scale))
                            Text(
                                text = userName,
                                maxLines = 1,
                                style = TextStyle(
                                    fontSize = (userNameSize * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp * scale))

                        Box(
                            modifier = Modifier
                                .width(2.dp * scale)
                                .height(dividerHeight * scale)
                                .background(Color.White),
                        )

                        Spacer(modifier = Modifier.width(12.dp * scale))

                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = timeDisplay,
                                modifier = Modifier.padding(top = 6.dp * scale),
                                style = TextStyle(
                                    fontSize = (timeFontSize * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                ),
                            )
                            Text(
                                text = monthDay,
                                style = TextStyle(
                                    fontSize = (dateFontSize * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                ),
                            )
                        }
                    }
                }

                // Avatar Box
                Box(
                    modifier = Modifier
                        .padding(start = 20.dp * scale)
                        .size(profileFrameSize * scale)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatar != null) {
                        Image(
                            bitmap = avatar.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(profileImageSize * scale)
                                .clip(CircleShape),
                        )
                    } else {
                        Text(
                            text = "\u0CA0\u1D25\u0CA0",
                            style = TextStyle(
                                fontSize = (profileFrameSize.value * 0.3f * scale).sp,
                                color = accent1,
                            ),
                        )
                    }
                }
            }

            // Day Pill (overlapping the main card by offset)
            Box(
                modifier = Modifier
                    .offset(y = (-16.dp * scale))
                    .clip(RoundedCornerShape(12.dp * scale))
                    .background(dayPillBg)
                    .padding(horizontal = 16.dp * scale, vertical = 8.dp * scale),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayOfWeek.uppercase(),
                        style = TextStyle(
                            fontSize = (dayFontSize * scale).sp,
                            fontWeight = FontWeight.Bold,
                            color = dayTextColor,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center,
                        ),
                    )
                    WeatherRow(
                        textColor = dayTextColor,
                        scale = scale,
                    )
                }
            }
        }
    }

    @Composable
    private fun WeatherRow(
        textColor: Color,
        scale: Float,
    ) {
        val display = viewModel.rememberResolvedDisplay()
        if (display is DateDisplay.Weather && display.temp.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.width(4.dp * scale))
                display.icon?.let { icon ->
                    Image(
                        bitmap = icon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size((18 * scale).dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp * scale))
                }
                Text(
                    text = display.temp,
                    style = TextStyle(
                        fontSize = (18f * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = 2.sp,
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
