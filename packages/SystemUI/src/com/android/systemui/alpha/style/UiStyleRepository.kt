/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style

import android.content.Context
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.statusbar.policy.ConfigurationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiStyleState(
    val styleId: String,
    val isNightMode: Boolean,
    val settings: UserStyleSettings,
    val themeVersion: Int
)

@SysUISingleton
class UiStyleRepository @Inject constructor(
    private val context: Context,
    private val configurationController: ConfigurationController
) {

    companion object {
        const val TAG = "UiStyleRepository"

        const val SYSTEM_DEFAULT = "system_default"
        const val OUTLINE = "outline"
        const val NEON = "neon"
        const val BEVEL = "bevel"
        const val GRADIENT = "gradient"
        const val REFLECTIVE = "reflective"
        const val SLASH = "slash"
        const val AEROGEL = "aerogel"
        const val METALLIC = "metallic"

        val SUPPORTED_STYLE_IDS = listOf(
            SYSTEM_DEFAULT, OUTLINE, NEON, BEVEL, GRADIENT,
            REFLECTIVE, SLASH, AEROGEL, METALLIC
        )

        private const val KEY_SELECTED_STYLE = "ui_style"

        private val STYLE_ID_TO_KEY = mapOf(
            OUTLINE to "ui_style_outline_params",
            BEVEL to "ui_style_bevel_params",
            GRADIENT to "ui_style_gradient_params",
            NEON to "ui_style_neon_params",
            REFLECTIVE to "ui_style_reflective_params",
            METALLIC to "ui_style_metallic_params",
            SLASH to "ui_style_slash_params",
            AEROGEL to "ui_style_aerogel_params"
        )
    }

    private val bgDispatcher = Dispatchers.IO
    private val scope = CoroutineScope(SupervisorJob() + bgDispatcher)
    private val handler = Handler(Looper.getMainLooper())

    private val _styleState = MutableStateFlow(
        UiStyleState(
            styleId = SYSTEM_DEFAULT,
            isNightMode = isNightMode(),
            settings = UserStyleSettings.DEFAULT,
            themeVersion = 0
        )
    )
    val styleState: StateFlow<UiStyleState> = _styleState.asStateFlow()

    // Legacy flows
    val selectedStyleIdFlow: StateFlow<String>
        get() = _selectedStyleIdFlow
    private val _selectedStyleIdFlow = MutableStateFlow(SYSTEM_DEFAULT)

    val themeVersionFlow: StateFlow<Int>
        get() = _themeVersionFlow
    private val _themeVersionFlow = MutableStateFlow(0)

    private val contentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (uri == null) return
            refreshStateFromSettings()
        }
    }

    private val configListener = object : ConfigurationController.ConfigurationListener {
        override fun onUiModeChanged() { refreshStateFromSettings() }
        override fun onThemeChanged() { refreshStateFromSettings() }
    }

    init {
        configurationController.addCallback(configListener)
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(KEY_SELECTED_STYLE),
            false,
            contentObserver,
            UserHandle.USER_ALL
        )
        refreshStateFromSettings()
    }

    private fun refreshStateFromSettings() {
        scope.launch {
            try {
                val isNight = isNightMode()
                val rawStyleId = Settings.System.getStringForUser(
                    context.contentResolver, KEY_SELECTED_STYLE, UserHandle.USER_CURRENT
                ) ?: SYSTEM_DEFAULT
                val styleId = if (rawStyleId in SUPPORTED_STYLE_IDS) rawStyleId else SYSTEM_DEFAULT

                val paramsKey = STYLE_ID_TO_KEY[styleId]
                val combinedString = if (paramsKey != null) {
                    Settings.System.getStringForUser(
                        context.contentResolver, paramsKey, UserHandle.USER_CURRENT
                    )
                } else null

                val settings = parseSettingsForMode(combinedString, isNight)

                updateParamsObserver(paramsKey)

                updateState { current ->
                    current.copy(
                        styleId = styleId,
                        isNightMode = isNight,
                        settings = settings,
                        themeVersion = current.themeVersion + 1
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing state", e)
            }
        }
    }

    private fun updateState(transform: (UiStyleState) -> UiStyleState) {
        while (true) {
            val prev = _styleState.value
            val next = transform(prev)
            if (_styleState.compareAndSet(prev, next)) {
                _selectedStyleIdFlow.value = next.styleId
                _themeVersionFlow.value = next.themeVersion
                break
            }
        }
    }

    private fun updateParamsObserver(paramKey: String?) {
        try {
            context.contentResolver.unregisterContentObserver(contentObserver)
            context.contentResolver.registerContentObserver(
                Settings.System.getUriFor(KEY_SELECTED_STYLE),
                false,
                contentObserver,
                UserHandle.USER_ALL
            )
            if (paramKey != null) {
                context.contentResolver.registerContentObserver(
                    Settings.System.getUriFor(paramKey),
                    false,
                    contentObserver,
                    UserHandle.USER_ALL
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update observer", e)
        }
    }

    private fun parseSettingsForMode(combined: String?, isNight: Boolean): UserStyleSettings {
        if (combined.isNullOrEmpty()) return UserStyleSettings.DEFAULT
        val parts = combined.split("|")
        val modePrefix = if (isNight) "dark:" else "light:"
        val modeString = parts.find { it.startsWith(modePrefix) }?.removePrefix(modePrefix)
        return if (modeString != null) UserStyleSettings.fromString(modeString) else UserStyleSettings.DEFAULT
    }

    private fun createCombinedString(
        currentCombined: String?, newSettings: UserStyleSettings, isNight: Boolean
    ): String {
        val lightPrefix = "light:"
        val darkPrefix = "dark:"
        var lightSettingsStr = UserStyleSettings.DEFAULT.toString()
        var darkSettingsStr = UserStyleSettings.DEFAULT.toString()

        if (!currentCombined.isNullOrEmpty()) {
            val parts = currentCombined.split("|")
            parts.find { it.startsWith(lightPrefix) }?.let { lightSettingsStr = it.removePrefix(lightPrefix) }
            parts.find { it.startsWith(darkPrefix) }?.let { darkSettingsStr = it.removePrefix(darkPrefix) }
        }

        if (isNight) darkSettingsStr = newSettings.toString()
        else lightSettingsStr = newSettings.toString()

        return "$lightPrefix$lightSettingsStr|$darkPrefix$darkSettingsStr"
    }

    fun setStyle(styleId: String) {
        val validId = if (styleId in SUPPORTED_STYLE_IDS) styleId else SYSTEM_DEFAULT

        // Optimistic Update
        if (validId != _styleState.value.styleId) {
             updateState { current ->
                current.copy(styleId = validId, themeVersion = current.themeVersion + 1)
            }
        }

        scope.launch {
            Settings.System.putStringForUser(
                context.contentResolver, KEY_SELECTED_STYLE, validId, UserHandle.USER_CURRENT
            )
        }
    }

    fun updateSettings(styleId: String, settings: UserStyleSettings) {
        val key = STYLE_ID_TO_KEY[styleId] ?: return
        val isNight = isNightMode()

        // Optimistic Update for Compose Recomposition
        if (styleId == _styleState.value.styleId && isNight == _styleState.value.isNightMode) {
            updateState { current ->
                current.copy(
                    settings = settings,
                    themeVersion = current.themeVersion + 1
                )
            }
        }

        scope.launch {
            val currentString = Settings.System.getStringForUser(
                context.contentResolver, key, UserHandle.USER_CURRENT
            )
            val newString = createCombinedString(currentString, settings, isNight)
            Settings.System.putStringForUser(
                context.contentResolver, key, newString, UserHandle.USER_CURRENT
            )
        }
    }

    fun resetSettings(styleId: String) {
        updateSettings(styleId, UserStyleSettings.DEFAULT)
    }

    private fun isNightMode(): Boolean {
        val uiMode = context.resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}
