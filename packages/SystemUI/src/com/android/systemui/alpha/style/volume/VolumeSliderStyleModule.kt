/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.volume

import android.content.Context
import com.android.systemui.alpha.style.UiStyleRepository
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import dagger.Module
import dagger.Provides

@Module
object VolumeSliderStyleModule {
    @Provides
    @SysUISingleton
    fun provideVolumeSliderStyleManager(
        @Application context: Context,
        uiStyleRepository: UiStyleRepository,
    ): VolumeSliderStyleManager {
        return VolumeSliderStyleManager(context, uiStyleRepository)
    }
}