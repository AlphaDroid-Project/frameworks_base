/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.qs

import android.content.Context
import com.android.systemui.alpha.style.UiStyleRepository
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.dagger.SysUISingleton
import dagger.Module
import dagger.Provides

@Module
object QSTileStyleModule {
    @Provides
    @SysUISingleton
    fun provideQSTileStyleManager(
        @Application context: Context,
        uiStyleRepository: UiStyleRepository
    ): QSTileStyleManager {
        return QSTileStyleManager(context, uiStyleRepository)
    }
}
