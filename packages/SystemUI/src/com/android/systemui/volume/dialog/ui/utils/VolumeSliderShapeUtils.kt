/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.systemui.volume.dialog.ui.utils

import android.database.ContentObserver
import android.os.UserHandle
import android.provider.Settings
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal const val VOLUME_SLIDER_SHAPE_DEFAULT = 0
internal const val VOLUME_SLIDER_SHAPE_CIRCLE = 1
internal const val VOLUME_SLIDER_SHAPE_ROUNDED_SQUARE = 2
internal const val VOLUME_SLIDER_SHAPE_SQUARE = 3

@Composable
internal fun rememberVolumeSliderShapeMode(): Int {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    fun readShapeMode(): Int {
        return try {
            Settings.System.getIntForUser(
                contentResolver,
                Settings.System.VOLUME_SLIDER_SHAPE,
                VOLUME_SLIDER_SHAPE_DEFAULT,
                UserHandle.USER_CURRENT,
            )
        } catch (_: Throwable) {
            VOLUME_SLIDER_SHAPE_DEFAULT
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
            Settings.System.getUriFor(Settings.System.VOLUME_SLIDER_SHAPE),
            false,
            observer,
            UserHandle.USER_ALL,
        )

        onDispose {
            contentResolver.unregisterContentObserver(observer)
        }
    }

    return shapeMode
}

internal fun useCustomVolumeThumb(
    shapeMode: Int,
    hasStyleRenderer: Boolean,
): Boolean {
    return hasStyleRenderer || shapeMode != VOLUME_SLIDER_SHAPE_DEFAULT
}

internal fun getVolumeTrackCornerDpForMode(
    shapeMode: Int,
    trackSize: Dp,
    defaultCorner: Dp,
): Dp {
    return when (shapeMode) {
        VOLUME_SLIDER_SHAPE_CIRCLE -> trackSize / 2f
        VOLUME_SLIDER_SHAPE_ROUNDED_SQUARE -> trackSize * 0.25f
        VOLUME_SLIDER_SHAPE_SQUARE -> 0.dp
        else -> defaultCorner
    }
}

internal fun getVolumeTrackInsideCornerDpForMode(
    shapeMode: Int,
    defaultInsideCorner: Dp,
): Dp {
    return if (shapeMode == VOLUME_SLIDER_SHAPE_DEFAULT) {
        defaultInsideCorner
    } else {
        0.dp
    }
}

@Composable
internal fun getVolumeThumbOrButtonShapeForMode(
    shapeMode: Int,
    sizeDp: Dp,
    defaultShape: Shape,
): Shape {
    return remember(shapeMode, sizeDp, defaultShape) {
        when (shapeMode) {
            VOLUME_SLIDER_SHAPE_SQUARE -> RoundedCornerShape(0.dp)
            VOLUME_SLIDER_SHAPE_ROUNDED_SQUARE -> RoundedCornerShape(sizeDp * 0.25f)
            VOLUME_SLIDER_SHAPE_CIRCLE -> CircleShape
            else -> defaultShape
        }
    }
}

internal fun getVolumeThumbOrButtonCornerRadiusForMode(
    shapeMode: Int,
    sizePx: Float,
    defaultCornerRadius: Float,
): Float {
    return when (shapeMode) {
        VOLUME_SLIDER_SHAPE_SQUARE -> 0f
        VOLUME_SLIDER_SHAPE_ROUNDED_SQUARE -> sizePx * 0.25f
        VOLUME_SLIDER_SHAPE_CIRCLE -> sizePx / 2f
        else -> defaultCornerRadius
    }
}
