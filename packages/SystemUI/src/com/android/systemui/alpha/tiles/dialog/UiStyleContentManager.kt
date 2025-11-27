/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.tiles.dialog

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import com.android.internal.R as InternalR
import com.android.systemui.alpha.style.UiStyleRepository
import com.android.internal.alpha.style.UserStyleSettings
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.res.R
import com.android.systemui.statusbar.phone.SystemUIDialog
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "UiStyleContentManager"
private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)

@SysUISingleton
class UiStyleContentManager
@Inject
constructor(
    private val uiStyleRepository: UiStyleRepository,
    private val activityStarter: ActivityStarter,
) {
    private lateinit var contentView: View
    private lateinit var context: Context
    private lateinit var coroutineScope: CoroutineScope

    private lateinit var doneButton: Button
    private lateinit var resetAllButton: Button

    private val styleViews = mutableMapOf<String, Pair<RadioButton, View?>>()
    private var styleFlowJob: Job? = null

    private val styles = listOf(
        Triple("system_default", R.id.style_radio_system_default, 0),
        Triple("outline", R.id.row_outline, InternalR.string.ui_style_outline),
        Triple("neon", R.id.row_neon, InternalR.string.ui_style_neon),
        Triple("bevel", R.id.row_bevel, InternalR.string.ui_style_bevel),
        Triple("gradient", R.id.row_gradient, InternalR.string.ui_style_gradient),
        Triple("reflective", R.id.row_reflective, InternalR.string.ui_style_reflective),
        Triple("slash", R.id.row_slash, InternalR.string.ui_style_slash),
        Triple("aerogel", R.id.row_aerogel, InternalR.string.ui_style_aerogel),
        Triple("metallic", R.id.row_metallic, InternalR.string.ui_style_metallic)
    )

    fun bind(view: View, scope: CoroutineScope) {
        if (DEBUG) Log.d(TAG, "bind")
        contentView = view
        context = view.context
        coroutineScope = scope
        doneButton = view.findViewById(R.id.done_button)
        resetAllButton = view.findViewById(R.id.reset_all_button)
        setupListItems()
        setupMainButtons()
    }

    private fun setupListItems() {
        styleViews.clear()
        styles.forEach { (styleId, viewId, nameResId) ->
            val itemRoot = contentView.findViewById<View>(viewId) ?: return@forEach
            if (styleId == "system_default") {
                val radio = itemRoot as RadioButton
                styleViews[styleId] = radio to null
                radio.setOnClickListener { selectStyle(styleId) }
            } else {
                val radio = itemRoot.findViewById<RadioButton>(R.id.item_radio)
                val gear = itemRoot.findViewById<View>(R.id.item_settings)
                if (radio != null) {
                    radio.text = context.getString(nameResId)
                    styleViews[styleId] = radio to gear
                }
                itemRoot.setOnClickListener { selectStyle(styleId) }
                gear?.setOnClickListener {
                    if (uiStyleRepository.styleState.value.styleId == styleId) {
                        openTuningDialog(styleId, context.getString(nameResId))
                    }
                }
            }
        }
    }

    private fun selectStyle(selectedId: String) {
        if (selectedId != uiStyleRepository.styleState.value.styleId) {
            coroutineScope.launch {
                uiStyleRepository.setStyle(selectedId)
            }
        }
    }

    private fun setupMainButtons() {
        doneButton.setOnClickListener {
            UiStyleDialogManager.dialog?.dismiss()
        }
        resetAllButton.setOnClickListener {
            coroutineScope.launch {
                UiStyleRepository.SUPPORTED_STYLE_IDS.forEach { id ->
                    uiStyleRepository.resetSettings(id)
                }
                uiStyleRepository.setStyle("system_default")
            }
        }
    }

    private fun updateRadioState(activeId: String) {
        styleViews.forEach { (id, views) ->
            val (radio, gear) = views
            val isActive = (id == activeId)
            radio.isChecked = isActive
            gear?.let {
                it.isEnabled = isActive
                it.alpha = if (isActive) 1.0f else 0.3f
                it.isClickable = isActive
            }
        }
    }

    private fun openTuningDialog(styleId: String, styleName: String) {
        val dialog = SystemUIDialog(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.ui_style_tuning_dialog, null)
        val titleView = dialogView.findViewById<TextView>(R.id.tuning_title)
        titleView?.text = "${context.getString(InternalR.string.ui_style_tile_label)}: $styleName"

        val seekSat = dialogView.findViewById<SeekBar>(R.id.seek_saturation)
        val seekLight = dialogView.findViewById<SeekBar>(R.id.seek_lightness)
        val seekOpac = dialogView.findViewById<SeekBar>(R.id.seek_opacity)
        val seekStr = dialogView.findViewById<SeekBar>(R.id.seek_strength)
        val seekAngle = dialogView.findViewById<SeekBar>(R.id.seek_angle)
        val btnReset = dialogView.findViewById<Button>(R.id.btn_reset)
        val btnDone = dialogView.findViewById<Button>(R.id.btn_done)

        // Always start with current settings. If null/default, sliders show default positions.
        val currentSettings = if (styleId == uiStyleRepository.styleState.value.styleId) {
            uiStyleRepository.styleState.value.settings
        } else {
            UserStyleSettings.DEFAULT
        }

        fun setSeekBars(settings: UserStyleSettings) {
            seekSat?.progress = (settings.saturation * 100).toInt()
            seekLight?.progress = (settings.lightness * 100).toInt()
            val opacP = ((settings.opacity - 0.2f) / 0.8f * 100).toInt()
            seekOpac?.progress = opacP.coerceIn(0, 100)
            val strP = ((settings.strength - 0.2f) / 1.8f * 200).toInt()
            seekStr?.progress = strP.coerceIn(0, 200)
            seekAngle?.progress = (settings.angle + 180).toInt()
        }
        setSeekBars(currentSettings)

        dialog.setView(dialogView)
        dialog.window?.setWindowAnimations(R.style.Animation_InternetDialog)

        fun applySettings() {
            if (seekSat == null) return
            val saturation = seekSat.progress / 100f
            val lightness = seekLight.progress / 100f
            val opacity = 0.2f + (seekOpac.progress / 100f * 0.8f)
            val strength = 0.2f + (seekStr.progress / 200f * 1.8f)
            val angle = seekAngle.progress.toFloat() - 180f

            val newSettings = UserStyleSettings(saturation, lightness, opacity, strength, angle)
            coroutineScope.launch {
                uiStyleRepository.updateSettings(styleId, newSettings)
            }
        }

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                applySettings()
            }
        }
        seekSat?.setOnSeekBarChangeListener(listener)
        seekLight?.setOnSeekBarChangeListener(listener)
        seekOpac?.setOnSeekBarChangeListener(listener)
        seekStr?.setOnSeekBarChangeListener(listener)
        seekAngle?.setOnSeekBarChangeListener(listener)

        btnReset?.setOnClickListener {
            // Reset to generic defaults (1.0/1.0)
            // The Renderers should handle the "vivid" look if the user sets (or resets to) default settings.
            setSeekBars(UserStyleSettings.DEFAULT)
            applySettings()
        }
        btnDone?.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    fun start() {
        styleFlowJob = uiStyleRepository.styleState
            .onEach { state -> updateRadioState(state.styleId) }
            .launchIn(coroutineScope)
    }

    fun stop() {
        styleFlowJob?.cancel()
        styleViews.values.forEach { (radio, gear) ->
            radio.setOnClickListener(null)
            gear?.setOnClickListener(null)
        }
        doneButton.setOnClickListener(null)
        resetAllButton.setOnClickListener(null)
    }
}
