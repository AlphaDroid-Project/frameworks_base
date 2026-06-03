/*
 * Copyright (C) 2025 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.shared.clocks

import com.android.systemui.customization.R
import com.android.systemui.shared.clocks.view.ClockFaceStyle

enum class AxClockType(
    val clockId: Int,
    val nameRes: Int,
    val viewId: Int,
    val largeViewId: Int = viewId,
    val bitmapFaceStyle: ClockFaceStyle? = null
) {
    SIMPLE(
        clockId = R.string.clock_id_simple,
        nameRes = R.string.clock_name_simple,
        viewId = R.layout.clock_simple,
        largeViewId = R.layout.clock_simple_large
    ),
    GENERAL(
        clockId = R.string.clock_id_general,
        nameRes = R.string.clock_name_general,
        viewId = R.layout.clock_general,
        largeViewId = R.layout.clock_general_large
    ),
    GRAPHIC(
        clockId = R.string.clock_id_graphic,
        nameRes = R.string.clock_name_graphic,
        viewId = R.layout.clock_bitmap_compose,
        largeViewId = R.layout.clock_bitmap_compose_large,
        bitmapFaceStyle = ClockFaceStyle.GRAPHIC
    ),
    LONDON_UG(
        clockId = R.string.clock_id_london_ug,
        nameRes = R.string.clock_name_london_ug,
        viewId = R.layout.clock_bitmap_compose,
        largeViewId = R.layout.clock_bitmap_compose_large,
        bitmapFaceStyle = ClockFaceStyle.LONDON_UG
    ),
    NDOT(
        clockId = R.string.clock_id_ndot,
        nameRes = R.string.clock_name_ndot,
        viewId = R.layout.clock_bitmap_compose,
        largeViewId = R.layout.clock_bitmap_compose_large,
        bitmapFaceStyle = ClockFaceStyle.NDOT
    ),
    NTYPE(
        clockId = R.string.clock_id_ntype,
        nameRes = R.string.clock_name_ntype,
        viewId = R.layout.clock_bitmap_compose,
        largeViewId = R.layout.clock_bitmap_compose_large,
        bitmapFaceStyle = ClockFaceStyle.NTYPE
    ),
    OLD_QUICKLOOK(
        clockId = R.string.clock_id_old_quick_look,
        nameRes = R.string.clock_name_old_quick_look,
        viewId = R.layout.clock_old_quick_look,
        largeViewId = R.layout.clock_old_quick_look_large
    ),
    SPACE_AGE(
        clockId = R.string.clock_id_space_age,
        nameRes = R.string.clock_name_space_age,
        viewId = R.layout.clock_bitmap_compose,
        largeViewId = R.layout.clock_bitmap_compose_large,
        bitmapFaceStyle = ClockFaceStyle.SPACE_AGE
    ),
    POLYLINE(
        clockId = R.string.clock_id_polyline,
        nameRes = R.string.clock_name_polyline,
        viewId = R.layout.clock_bitmap_compose,
        largeViewId = R.layout.clock_bitmap_compose_large,
        bitmapFaceStyle = ClockFaceStyle.POLYLINE
    ),
    CYBERPUNK(
        clockId = R.string.clock_id_cyberpunk,
        nameRes = R.string.clock_name_cyberpunk,
        viewId = R.layout.clock_cyberpunk,
        largeViewId = R.layout.clock_cyberpunk_large
    ),
    AXION_AGE(
        clockId = R.string.clock_id_axion_age,
        nameRes = R.string.clock_name_axion_age,
        viewId = R.layout.clock_axion_age,
        largeViewId = R.layout.clock_axion_age_large
    ),
    SEGMENTS(
        clockId = R.string.clock_id_segments,
        nameRes = R.string.clock_name_segments,
        viewId = R.layout.clock_bitmap_compose,
        largeViewId = R.layout.clock_bitmap_compose_large,
        bitmapFaceStyle = ClockFaceStyle.SEGMENTS
    ),
    STYLISH_2(
        clockId = R.string.clock_id_stylish2,
        nameRes = R.string.clock_name_stylish2,
        viewId = R.layout.clock_stylish2,
        largeViewId = R.layout.clock_stylish2_large
    ),
    STYLISH_7(
        clockId = R.string.clock_id_stylish7,
        nameRes = R.string.clock_name_stylish7,
        viewId = R.layout.clock_stylish7,
        largeViewId = R.layout.clock_stylish7_large
    );

    companion object {
        val entries: List<AxClockType> = values().toList()
    }
}
