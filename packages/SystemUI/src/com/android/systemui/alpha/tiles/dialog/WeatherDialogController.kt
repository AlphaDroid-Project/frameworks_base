/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.tiles.dialog

import com.android.systemui.animation.DialogTransitionAnimator

/** Wrapper controller that skips exit animation checks for the weather dialog. */
class WeatherDialogController(
    private val delegate: DialogTransitionAnimator.Controller
) : DialogTransitionAnimator.Controller by delegate {

    override val skipExitAnimationChecks: Boolean = true
}
