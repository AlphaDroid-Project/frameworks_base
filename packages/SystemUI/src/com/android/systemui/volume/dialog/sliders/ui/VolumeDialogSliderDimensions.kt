/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package com.android.systemui.volume.dialog.sliders.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

internal object VolumeDialogSliderDimensions {
    val TrackThickness: Dp = 40.dp

    /**
     * Styled visual thumb extent **along** the track (UI Styles glass pill).
     * Cross-track extent is [TrackThickness]. Legacy square size was TrackThickness + 4.
     */
    val StyledVisualThumbAlongTrack: Dp =
        com.android.systemui.alpha.style.slider.StyledSliderThumbTokens.AlongTrack

    /** @deprecated Prefer [StyledVisualThumbAlongTrack] + track thickness pill. */
    val StyledVisualThumbSize: Dp = StyledVisualThumbAlongTrack

    // Logical AOSP thumb sizes used by slider behavior/math.
    val VerticalLogicalThumbSize: DpSize = DpSize(width = 52.dp, height = 4.dp)
    val HorizontalLogicalThumbSize: DpSize = DpSize(width = 4.dp, height = 52.dp)
}