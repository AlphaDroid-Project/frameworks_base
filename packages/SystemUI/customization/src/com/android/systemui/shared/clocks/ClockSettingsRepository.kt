/*
 * Copyright (C) 2026 AxionOS Project
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

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

object ClockSettingsRepository {

    const val SETTING_CLOCK_FACE = "lock_screen_custom_clock_face"
    const val SETTING_ALIGNMENT = "ax_clock_alignment"
    const val SETTING_SIZE = "ax_clock_size"
    const val SETTING_DATE_POSITION = "ax_clock_compose_date_position"
    const val SETTING_CLOCK_COLOR = "ax_clock_color"

    const val SETTING_ALIGNMENT_SMALL = "ax_clock_alignment_small"
    const val SETTING_ALIGNMENT_LARGE = "ax_clock_alignment_large"
    const val SETTING_SCALE_SMALL = "ax_clock_scale_small"
    const val SETTING_SCALE_LARGE = "ax_clock_scale_large"

    const val COLOR_AUTO = "auto"

    const val ALIGNMENT_LEFT = "left"
    const val ALIGNMENT_CENTER = "center"
    const val ALIGNMENT_RIGHT = "right"
    const val SIZE_DEFAULT = "default"
    const val SIZE_LARGE = "large"
    const val DATE_POSITION_ABOVE = "above"
    const val DATE_POSITION_BELOW = "below"

    private const val LARGE_SIZE_SCALE = 1.4f

    @JvmField val clockFaceUri: Uri = Settings.Secure.getUriFor(SETTING_CLOCK_FACE)
    @JvmField val alignmentUri: Uri = Settings.Secure.getUriFor(SETTING_ALIGNMENT)
    @JvmField val alignmentSmallUri: Uri = Settings.Secure.getUriFor(SETTING_ALIGNMENT_SMALL)
    @JvmField val alignmentLargeUri: Uri = Settings.Secure.getUriFor(SETTING_ALIGNMENT_LARGE)
    @JvmField val sizeUri: Uri = Settings.Secure.getUriFor(SETTING_SIZE)
    @JvmField val scaleSmallUri: Uri = Settings.Secure.getUriFor(SETTING_SCALE_SMALL)
    @JvmField val scaleLargeUri: Uri = Settings.Secure.getUriFor(SETTING_SCALE_LARGE)
    @JvmField val datePositionUri: Uri = Settings.Secure.getUriFor(SETTING_DATE_POSITION)
    @JvmField val clockColorUri: Uri = Settings.Secure.getUriFor(SETTING_CLOCK_COLOR)

    private val _clockId = MutableStateFlow("DEFAULT")
    val clockId: StateFlow<String> = _clockId.asStateFlow()

    private val _smallAlignment = MutableStateFlow(ALIGNMENT_CENTER)
    val smallAlignment: StateFlow<String> = _smallAlignment.asStateFlow()
    
    private val _largeAlignment = MutableStateFlow(ALIGNMENT_CENTER)
    val largeAlignment: StateFlow<String> = _largeAlignment.asStateFlow()

    val alignment: StateFlow<String> = smallAlignment // Legacy back-compat

    private val _smallScale = MutableStateFlow(1f)
    val smallScale: StateFlow<Float> = _smallScale.asStateFlow()
    
    private val _largeScale = MutableStateFlow(1f)
    val largeScale: StateFlow<Float> = _largeScale.asStateFlow()

    val sizeScale: StateFlow<Float> = smallScale // Legacy back-compat

    private val _isDateBelow = MutableStateFlow(false)
    val isDateBelow: StateFlow<Boolean> = _isDateBelow.asStateFlow()

    private val _clockColorOverride = MutableStateFlow<Int?>(null)
    val clockColorOverride: StateFlow<Int?> = _clockColorOverride.asStateFlow()

    private val _shouldCenterIcons = MutableStateFlow(true)
    val shouldCenterIcons: StateFlow<Boolean> = _shouldCenterIcons.asStateFlow()

    private var contentResolver: ContentResolver? = null
    private var registered = false
    private val handler = Handler(Looper.getMainLooper())

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            val cr = contentResolver ?: return
            when (uri) {
                clockFaceUri -> {
                    _clockId.value = readClockId(cr)
                    _shouldCenterIcons.value = computeShouldCenter(cr)
                }
                alignmentSmallUri -> {
                    val align = readSmallAlignment(cr)
                    _smallAlignment.value = align
                    _shouldCenterIcons.value = computeShouldCenter(cr)
                    // Write-through to legacy key for compatibility
                    try {
                        Settings.Secure.putString(cr, SETTING_ALIGNMENT, align)
                    } catch (_: Exception) {}
                }
                alignmentLargeUri -> {
                    _largeAlignment.value = readLargeAlignment(cr)
                }
                scaleSmallUri -> {
                    _smallScale.value = readSmallScale(cr)
                }
                scaleLargeUri -> {
                    _largeScale.value = readLargeScale(cr)
                }
                datePositionUri -> {
                    _isDateBelow.value = readDateBelow(cr)
                }
                clockColorUri -> {
                    _clockColorOverride.value = readClockColor(cr)
                }
            }
        }
    }

    @JvmStatic
    fun init(context: Context) {
        contentResolver = context.contentResolver
        val cr = context.contentResolver

        if (registered) {
            _clockColorOverride.value = readClockColor(cr)
            return
        }
        registered = true

        cr.registerContentObserver(clockFaceUri, false, observer)
        cr.registerContentObserver(alignmentSmallUri, false, observer)
        cr.registerContentObserver(alignmentLargeUri, false, observer)
        cr.registerContentObserver(scaleSmallUri, false, observer)
        cr.registerContentObserver(scaleLargeUri, false, observer)
        cr.registerContentObserver(datePositionUri, false, observer)
        cr.registerContentObserver(clockColorUri, false, observer)

        performMigrationIfNeeded(cr)

        _clockId.value = readClockId(cr)
        _smallAlignment.value = readSmallAlignment(cr)
        _largeAlignment.value = readLargeAlignment(cr)
        _smallScale.value = readSmallScale(cr)
        _largeScale.value = readLargeScale(cr)
        _isDateBelow.value = readDateBelow(cr)
        _clockColorOverride.value = readClockColor(cr)
        _shouldCenterIcons.value = computeShouldCenter(cr)
    }

    @JvmStatic
    fun shouldCenterIcons(context: Context): Boolean {
        init(context)
        return _shouldCenterIcons.value
    }

    private fun readClockId(cr: ContentResolver): String {
        return try {
            val json = Settings.Secure.getString(cr, SETTING_CLOCK_FACE)
            if (!json.isNullOrEmpty()) JSONObject(json).optString("clockId", "DEFAULT")
            else "DEFAULT"
        } catch (_: Exception) {
            "DEFAULT"
        }
    }

    private fun performMigrationIfNeeded(cr: ContentResolver) {
        val smallScaleStr = Settings.Secure.getString(cr, SETTING_SCALE_SMALL)
        if (smallScaleStr == null) {
            // First time initialization / migration
            val legacySize = Settings.Secure.getString(cr, SETTING_SIZE)
            val legacyAlignment = Settings.Secure.getString(cr, SETTING_ALIGNMENT) ?: ALIGNMENT_CENTER
            
            val initialSmallScale = if (legacySize == SIZE_LARGE) 140 else 100
            val initialLargeScale = 100
            
            try {
                Settings.Secure.putInt(cr, SETTING_SCALE_SMALL, initialSmallScale)
                Settings.Secure.putInt(cr, SETTING_SCALE_LARGE, initialLargeScale)
                Settings.Secure.putString(cr, SETTING_ALIGNMENT_SMALL, legacyAlignment)
                Settings.Secure.putString(cr, SETTING_ALIGNMENT_LARGE, legacyAlignment)
            } catch (_: Exception) {}
        }
    }

    private fun readSmallAlignment(cr: ContentResolver): String {
        return Settings.Secure.getString(cr, SETTING_ALIGNMENT_SMALL) ?: ALIGNMENT_CENTER
    }
    
    private fun readLargeAlignment(cr: ContentResolver): String {
        return Settings.Secure.getString(cr, SETTING_ALIGNMENT_LARGE) ?: ALIGNMENT_CENTER
    }

    private fun readSmallScale(cr: ContentResolver): Float {
        val pct = Settings.Secure.getInt(cr, SETTING_SCALE_SMALL, 100)
        return pct / 100f
    }
    
    private fun readLargeScale(cr: ContentResolver): Float {
        val pct = Settings.Secure.getInt(cr, SETTING_SCALE_LARGE, 100)
        return pct / 100f
    }

    private fun readDateBelow(cr: ContentResolver): Boolean {
        return Settings.Secure.getString(cr, SETTING_DATE_POSITION) == DATE_POSITION_BELOW
    }

    private fun readClockColor(cr: ContentResolver): Int? {
        val raw = Settings.Secure.getString(cr, SETTING_CLOCK_COLOR)
        if (raw.isNullOrEmpty() || raw == COLOR_AUTO) return null
        return try {
            android.graphics.Color.parseColor(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun computeShouldCenter(cr: ContentResolver): Boolean {
        val align = readSmallAlignment(cr)
        if (align == ALIGNMENT_LEFT || align == ALIGNMENT_RIGHT) return false
        if (align == ALIGNMENT_CENTER) return true
        val id = readClockId(cr)
        return !isDefaultLeftAlignedClock(id)
    }

    @JvmStatic
    fun isDefaultLeftAlignedClock(clockId: String): Boolean {
        return clockId.equals("GENERAL", ignoreCase = true) ||
               clockId.equals("OLD_QUICKLOOK", ignoreCase = true) ||
               clockId.equals("AXION_AGE", ignoreCase = true)
    }
}
