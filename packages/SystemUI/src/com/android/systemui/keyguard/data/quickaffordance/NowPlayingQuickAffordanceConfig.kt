/*
 * Copyright (C) 2026 AlphaDroid Project
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

package com.android.systemui.keyguard.data.quickaffordance

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.android.systemui.animation.Expandable
import com.android.systemui.common.shared.model.ContentDescription
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.keyguard.shared.quickaffordance.ActivationState
import com.android.systemui.res.R
import com.android.systemui.settings.UserTracker
import com.android.systemui.shade.ShadeDisplayAware
import com.android.systemui.util.settings.SecureSettings
import com.android.systemui.util.settings.SettingsProxyExt.observerFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart

/**
 * Arms or disarms background music recognition from the lockscreen.
 *
 * The thing worth reaching quickly is consent, not the result: walking somewhere you would rather
 * the phone did not listen is exactly when the lockscreen is all you have. Recognised songs
 * already surface on their own, so this carries no metadata.
 */
@SysUISingleton
class NowPlayingQuickAffordanceConfig
@Inject
constructor(
    @ShadeDisplayAware private val context: Context,
    private val userTracker: UserTracker,
    private val secureSettings: SecureSettings,
    @Background private val backgroundDispatcher: CoroutineDispatcher,
) : KeyguardQuickAffordanceConfig {

    override val key: String = BuiltInKeyguardQuickAffordanceKeys.NOW_PLAYING

    override fun pickerName(): String = context.getString(R.string.now_playing_affordance)

    override val pickerIconResourceId: Int = R.drawable.ic_now_playing_note

    /** Nothing answers the setting without ASI, so the shortcut would be inert. */
    private val recognizerInstalled: Boolean by lazy {
        try {
            context.packageManager.getApplicationInfo(RECOGNIZER_PACKAGE, 0).enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private val supported: Boolean by lazy {
        recognizerInstalled &&
            context.resources.getBoolean(
                com.android.internal.R.bool.config_supportsBackgroundMusicRecognition
            )
    }

    override val lockScreenState: Flow<KeyguardQuickAffordanceConfig.LockScreenState> =
        merge(
                secureSettings.observerFlow(userTracker.userId, SETTING),
                secureSettings.observerFlow(userTracker.userId, ASI_MASTER_SETTING),
            )
            .onStart { emit(Unit) }
            // Both rows matter, so both belong in the key -- keying on the switch alone would
            // swallow a master change that left it untouched.
            .map { isEnabled() to masterEnabled() }
            .distinctUntilChanged()
            .map { (enabled, master) ->
                // The master takes the shortcut with it: without it there is no recognizer, so
                // offering a toggle here would only let the user store a state that cannot hold.
                if (!supported || !master) {
                    KeyguardQuickAffordanceConfig.LockScreenState.Hidden
                } else {
                    KeyguardQuickAffordanceConfig.LockScreenState.Visible(
                        Icon.Resource(
                            R.drawable.ic_now_playing_note,
                            ContentDescription.Resource(R.string.now_playing_affordance),
                        ),
                        if (enabled) ActivationState.Active else ActivationState.Inactive,
                    )
                }
            }
            .flowOn(backgroundDispatcher)

    override suspend fun getPickerScreenState():
        KeyguardQuickAffordanceConfig.PickerScreenState =
        when {
            !supported -> KeyguardQuickAffordanceConfig.PickerScreenState.UnavailableOnDevice
            !masterEnabled() ->
                KeyguardQuickAffordanceConfig.PickerScreenState.Disabled(
                    explanation =
                        context.getString(R.string.now_playing_affordance_unavailable_explanation),
                    actionText =
                        context.getString(R.string.now_playing_affordance_unavailable_action),
                    actionIntent = Intent(NOW_PLAYING_SETTINGS_ACTION),
                )
            else -> KeyguardQuickAffordanceConfig.PickerScreenState.Default()
        }

    override fun onTriggered(
        expandable: Expandable?
    ): KeyguardQuickAffordanceConfig.OnTriggeredResult {
        if (!masterEnabled()) {
            return KeyguardQuickAffordanceConfig.OnTriggeredResult.Handled(false)
        }
        secureSettings.putIntForUser(SETTING, if (isEnabled()) 0 else 1, userTracker.userId)
        return KeyguardQuickAffordanceConfig.OnTriggeredResult.Handled(false)
    }

    private fun isEnabled(): Boolean =
        secureSettings.getIntForUser(SETTING, 0, userTracker.userId) != 0

    private fun masterEnabled(): Boolean =
        secureSettings.getIntForUser(ASI_MASTER_SETTING, 0, userTracker.userId) != 0

    companion object {
        private const val SETTING = "background_music_recognition"

        /** ASI's on-demand master. Read as a prerequisite; never written here. */
        private const val ASI_MASTER_SETTING = "now_playing_enabled"
        private const val RECOGNIZER_PACKAGE = "com.google.android.as"
        private const val NOW_PLAYING_SETTINGS_ACTION =
            "com.google.intelligence.sense.NOW_PLAYING_SETTINGS"
    }
}
