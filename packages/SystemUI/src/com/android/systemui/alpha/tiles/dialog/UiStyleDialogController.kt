/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.tiles.dialog

import com.android.systemui.animation.DialogCuj
import com.android.systemui.animation.DialogTransitionAnimator

/**
 * Wrapper controller that skips exit animation checks for UI Style dialog.
 */
class UiStyleDialogController(
    private val delegate: DialogTransitionAnimator.Controller
) : DialogTransitionAnimator.Controller by delegate {

    override val skipExitAnimationChecks: Boolean = true
}
