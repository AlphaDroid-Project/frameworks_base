/*
 * Copyright (C) 2026 AxionOS Project
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

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle

// Extra vertical room, as a fraction of the font size, reserved above the top digit line. Clock
// fonts vary wildly in ascent/overshoot (some digit glyphs rise well above the em box), and a
// line height equal to the font size clips those glyphs against the view bounds. This headroom
// is placed entirely above the first line so the stacked hours/minutes gap stays tight.
private const val TOP_LINE_HEADROOM_RATIO = 1.28f

/**
 * Renders a two-line (hours over minutes) large clock, shrinking the font uniformly so the widest
 * line always fits within the available width. This mirrors the width fit-to-scale that the
 * bitmap/canvas faces apply via [fitToWidth], keeping left/right alignment correct at any user
 * size scale or clock font instead of overflowing (and clipping) past the view edges.
 */
@Composable
internal fun FittedTimeColumn(
    hours: String,
    minutes: String,
    baseStyle: TextStyle,
    horizontalAlignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availablePx = with(LocalDensity.current) { maxWidth.toPx() }
        val naturalMaxPx = remember(hours, minutes, baseStyle, availablePx) {
            maxOf(
                measurer.measure(hours, baseStyle, maxLines = 1).size.width,
                measurer.measure(minutes, baseStyle, maxLines = 1).size.width,
            ).toFloat()
        }
        val fitScale =
            if (naturalMaxPx > availablePx && naturalMaxPx > 0f) availablePx / naturalMaxPx else 1f
        val fittedSize = if (fitScale < 1f) baseStyle.fontSize * fitScale else baseStyle.fontSize

        val noFontPadding = PlatformTextStyle(includeFontPadding = false)
        // First line: reserve headroom above so tall ascenders aren't clipped by the view top;
        // Bottom alignment pushes all of the extra leading above the glyph.
        val hoursStyle = baseStyle.copy(
            fontSize = fittedSize,
            lineHeight = fittedSize * TOP_LINE_HEADROOM_RATIO,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Bottom,
                trim = LineHeightStyle.Trim.None,
            ),
            platformStyle = noFontPadding,
        )
        // Second line: keep it tight so the two lines stack closely; Top alignment keeps its own
        // extra leading below, so the hours/minutes gap doesn't grow.
        val minutesStyle = baseStyle.copy(
            fontSize = fittedSize,
            lineHeight = fittedSize,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Top,
                trim = LineHeightStyle.Trim.None,
            ),
            platformStyle = noFontPadding,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = horizontalAlignment,
        ) {
            Text(text = hours, maxLines = 1, style = hoursStyle)
            Text(text = minutes, maxLines = 1, style = minutesStyle)
        }
    }
}
