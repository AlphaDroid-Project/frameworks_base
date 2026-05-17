/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.tiles.dialog

import android.util.Log
import com.android.internal.jank.InteractionJankMonitor
import com.android.systemui.animation.DialogCuj
import com.android.systemui.animation.DialogTransitionAnimator
import com.android.systemui.animation.Expandable
import com.android.systemui.coroutines.newTracingContext
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.statusbar.phone.SystemUIDialog
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

private const val TAG = "WeatherDialogManager"
private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)

@SysUISingleton
class WeatherDialogManager
@Inject
constructor(
    private val dialogTransitionAnimator: DialogTransitionAnimator,
    private val dialogFactory: WeatherDialogDelegate.Factory,
    @Main private val mainDispatcher: CoroutineDispatcher,
) {
    private lateinit var coroutineScope: CoroutineScope

    companion object {
        private const val INTERACTION_JANK_TAG = "weather"
        var dialog: SystemUIDialog? = null
    }

    fun create(expandable: Expandable?) {
        if (dialog != null) {
            if (DEBUG) Log.d(TAG, "WeatherDialog is already showing.")
            return
        }

        if (DEBUG) Log.d(TAG, "Creating weather dialog")

        coroutineScope = CoroutineScope(mainDispatcher + newTracingContext("WeatherDialogScope"))
        dialog = dialogFactory.create(coroutineScope).createDialog()

        val standardController = expandable?.dialogTransitionController(
            DialogCuj(InteractionJankMonitor.CUJ_SHADE_DIALOG_OPEN, INTERACTION_JANK_TAG)
        )

        val controller = if (standardController != null) {
            WeatherDialogController(standardController)
        } else {
            null
        }

        if (controller != null) {
            dialogTransitionAnimator.show(
                dialog!!,
                controller,
                animateBackgroundBoundsChange = true,
            )
        } else {
            dialog?.show()
        }
    }

    fun destroyDialog() {
        if (DEBUG) Log.d(TAG, "destroyDialog")
        if (::coroutineScope.isInitialized) {
            coroutineScope.cancel()
        }
        dialog = null
    }
}
