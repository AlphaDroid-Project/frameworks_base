/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.systemui.keyguard.data.repository

import android.view.View
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.settings.UserTracker
import com.android.systemui.util.settings.SecureSettings
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface KeyguardSmartspaceRepository {
    val bcSmartspaceVisibility: StateFlow<Int>
    val isWeatherEnabled: StateFlow<Boolean>

    fun setBcSmartspaceVisibility(visibility: Int)
}

@SysUISingleton
class KeyguardSmartspaceRepositoryImpl
@Inject
constructor(
    private val secureSettings: SecureSettings,
    private val userTracker: UserTracker,
    @Application private val applicationScope: CoroutineScope,
) : KeyguardSmartspaceRepository {
    private val _bcSmartspaceVisibility: MutableStateFlow<Int> = MutableStateFlow(View.GONE)
    override val bcSmartspaceVisibility: StateFlow<Int> = _bcSmartspaceVisibility.asStateFlow()
    override val isWeatherEnabled: StateFlow<Boolean> =
        MutableStateFlow(true)

    override fun setBcSmartspaceVisibility(visibility: Int) {
        _bcSmartspaceVisibility.value = visibility
    }

    /**
     * Weather data is always considered enabled because AxQuickLook handles weather
     * rendering inside the active Axion clock face. The BC smartspace pipeline is
     * permanently disabled — see LockscreenSmartspaceController.isEnabled.
     */
    private fun getLockscreenWeatherEnabled(): Boolean = true
}
