/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.brightness

import android.content.Context
import com.android.systemui.alpha.style.UiStyleRepository
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.dagger.SysUISingleton
import dagger.Module
import dagger.Provides

@Module
object BrightnessSliderStyleModule {
    @Provides
    @SysUISingleton
    fun provideBrightnessSliderStyleManager(
        @Application context: Context,
        uiStyleRepository: UiStyleRepository
    ): BrightnessSliderStyleManager {
        return BrightnessSliderStyleManager(context, uiStyleRepository)
    }
}
