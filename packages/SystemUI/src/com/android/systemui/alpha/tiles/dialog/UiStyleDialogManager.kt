/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
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

private const val TAG = "UiStyleDialogManager"
private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)

@SysUISingleton
class UiStyleDialogManager
@Inject
constructor(
    private val dialogTransitionAnimator: DialogTransitionAnimator,
    private val dialogFactory: UiStyleDialogDelegate.Factory,
    @Main private val mainDispatcher: CoroutineDispatcher,
) {
    private lateinit var coroutineScope: CoroutineScope

    companion object {
        private const val INTERACTION_JANK_TAG = "ui_style"
        var dialog: SystemUIDialog? = null
    }

    fun create(expandable: Expandable?) {
        if (dialog != null) {
            if (DEBUG) {
                Log.d(TAG, "UiStyleDialog is showing, do not create it twice.")
            }
            return
        }

        if (DEBUG) {
            Log.d(TAG, "Creating UI Style dialog")
        }

        coroutineScope = CoroutineScope(mainDispatcher + newTracingContext("UiStyleDialogScope"))
        dialog = dialogFactory.create(coroutineScope).createDialog()

        val standardController = expandable?.dialogTransitionController(
            DialogCuj(InteractionJankMonitor.CUJ_SHADE_DIALOG_OPEN, INTERACTION_JANK_TAG)
        )

        val controller = if (standardController != null) {
            if (DEBUG) {
                Log.d(TAG, "Wrapping controller to skip exit checks")
            }
            UiStyleDialogController(standardController)
        } else {
            null
        }

        if (controller != null) {
            if (DEBUG) {
                Log.d(TAG, "Showing dialog with animation")
            }

            dialogTransitionAnimator.show(
                dialog!!,
                controller,
                animateBackgroundBoundsChange = true,
            )
        } else {
            if (DEBUG) {
                Log.d(TAG, "Showing dialog without animation")
            }
            dialog?.show()
        }
    }

    fun destroyDialog() {
        if (DEBUG) {
            Log.d(TAG, "destroyDialog")
        }
        if (::coroutineScope.isInitialized) {
            coroutineScope.cancel()
        }
        dialog = null
    }
}
