/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.tiles.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Window
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.android.systemui.res.R
import com.android.systemui.shade.domain.interactor.ShadeDialogContextInteractor
import com.android.systemui.statusbar.phone.SystemUIDialog
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope

private const val TAG = "WeatherDialogDelegate"
private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)

/** Delegate for the weather forecast QS tile dialog. */
class WeatherDialogDelegate
@AssistedInject
constructor(
    private val weatherContentManager: WeatherContentManager,
    private val weatherDialogManager: WeatherDialogManager,
    private val systemUIDialogFactory: SystemUIDialog.Factory,
    private val shadeDialogContextInteractor: ShadeDialogContextInteractor,
    @Assisted private val coroutineScope: CoroutineScope,
) : SystemUIDialog.Delegate {

    private lateinit var dialog: SystemUIDialog
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var lifecycleOwner: LifecycleOwner

    @AssistedFactory
    interface Factory {
        fun create(coroutineScope: CoroutineScope): WeatherDialogDelegate
    }

    override fun createDialog(): SystemUIDialog {
        if (DEBUG) Log.d(TAG, "createDialog")

        dialog = systemUIDialogFactory.create(this, shadeDialogContextInteractor.context)

        lifecycleOwner = object : LifecycleOwner {
            override val lifecycle: Lifecycle
                get() = lifecycleRegistry
        }
        lifecycleRegistry = LifecycleRegistry(lifecycleOwner)

        return dialog
    }

    override fun onCreate(dialog: SystemUIDialog, savedInstanceState: Bundle?) {
        if (DEBUG) Log.d(TAG, "onCreate")

        val context = dialog.context
        val dialogView = LayoutInflater.from(context).inflate(
            R.layout.weather_tile_dialog, null
        )
        dialogView.accessibilityPaneTitle = context.getText(R.string.omnijaws_label_default)

        val window: Window = dialog.window!!
        window.setContentView(dialogView)
        window.setWindowAnimations(R.style.Animation_InternetDialog)

        val params = window.attributes
        // ~65% of screen width — compact forecast card
        params.width = (context.resources.displayMetrics.widthPixels * 0.65).toInt()
        window.attributes = params

        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        weatherContentManager.bind(dialogView, coroutineScope)
    }

    override fun onStart(dialog: SystemUIDialog) {
        if (DEBUG) Log.d(TAG, "onStart")
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        weatherContentManager.start()
    }

    override fun onStop(dialog: SystemUIDialog) {
        if (DEBUG) Log.d(TAG, "onStop")
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        weatherContentManager.stop()
        weatherDialogManager.destroyDialog()
    }
}
