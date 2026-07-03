/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.axdynamicbar.ui.compose

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.android.systemui.axdynamicbar.ui.AxDynamicBarChipViewModel
import kotlin.math.abs
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Attaches the standard island tap-and-swipe gesture recogniser.
 *
 * Uses [PointerEventPass.Main] so we do not preempt system / shade gestures on
 * [PointerEventPass.Initial] (important for cutout-mode overlays above the status bar).
 *
 * - Tap (no movement beyond [touchSlop]) → [AxDynamicBarChipViewModel.togglePanel]
 * - Long press (held ≥ system long-press timeout without movement) →
 *   [AxDynamicBarChipViewModel.openSettings]
 * - Horizontal swipe right → [AxDynamicBarChipViewModel.cyclePrev]
 * - Horizontal swipe left  → [AxDynamicBarChipViewModel.cycleNext]
 * - Vertical swipe         → not consumed; pointer is drained until release. Note this only
 *   lets *sibling views in the same window* (status bar / shade) handle the gesture — Android
 *   never re-dispatches unconsumed touches to other windows. For the cutout-mode overlay the
 *   equivalent is handled at the window level: [AxDynamicBarExpandedPanel] carves the window's
 *   touchable region down to the pill/ring bounds, so touches outside them never enter the
 *   overlay in the first place.
 */
internal fun Modifier.islandGestures(
    viewModel: AxDynamicBarChipViewModel,
    touchSlop: Float,
): Modifier = this.pointerInput(viewModel) {
    val pass = PointerEventPass.Main
    val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        val down = awaitFirstDown(pass = pass)
        val startX = down.position.x
        val startY = down.position.y
        var dragging = false
        var totalDx = 0f
        var decided = false
        var longPressed = false
        var isReleased = false

        val timeoutResult = withTimeoutOrNull(longPressTimeoutMs) {
            while (true) {
                val event = awaitPointerEvent(pass)
                val change = event.changes.firstOrNull() ?: break
                
                if (!change.pressed) {
                    isReleased = true
                    change.consume()
                    viewModel.togglePanel()
                    decided = true
                    break
                }
                
                val dx = change.position.x - startX
                val dy = change.position.y - startY
                if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                    decided = true
                    if (abs(dx) >= abs(dy)) {
                        dragging = true
                        totalDx = dx
                        change.consume()
                    }
                    break
                }
            }
            true
        }

        if (timeoutResult == null && !decided) {
            longPressed = true
            decided = true
            viewModel.openSettings()
        }

        if (!isReleased) {
            while (true) {
                val event = awaitPointerEvent(pass)
                val change = event.changes.firstOrNull() ?: break

                if (!change.pressed) {
                    if (dragging) {
                        change.consume()
                        if (totalDx > 0) viewModel.cyclePrev() else viewModel.cycleNext()
                    } else if (longPressed) {
                        change.consume()
                    }
                    break
                }

                if (dragging) {
                    val dx = change.position.x - startX
                    totalDx = dx
                    change.consume()
                }
            }
        }
    }
}

/**
 * Swipe-only gesture capture for the status-bar chip area in the expanded overlay.
 *
 * When the card is open in non-cutout mode the chip lives in the status bar view (behind the
 * overlay), so its [islandGestures] modifier never receives events. This modifier is applied to a
 * transparent [Box] that sits over the chip band and forwards horizontal swipes to
 * [AxDynamicBarChipViewModel.cycleNext] / [AxDynamicBarChipViewModel.cyclePrev].
 *
 * Key constraint: the down event must NOT be consumed so the scrim handler at
 * [PointerEventPass.Final] still sees it and can collapse the card on a tap.
 * Only move events that are confirmed as a horizontal drag are consumed.
 *
 * Vertical swipes are not consumed so QS / shade pull-down still works.
 */
internal fun Modifier.islandSwipeCapture(
    viewModel: AxDynamicBarChipViewModel,
): Modifier = this.pointerInput(viewModel) {
    val slop = viewConfiguration.touchSlop
    val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        // Observe the down at Initial pass WITHOUT consuming it.
        val down = awaitPointerEvent(PointerEventPass.Initial)
        val downChange = down.changes.firstOrNull()?.also {
            // Do NOT consume — just record position.
        } ?: return@awaitEachGesture
        if (!downChange.pressed) return@awaitEachGesture
        val startX = downChange.position.x
        val startY = downChange.position.y

        var decided = false
        var dragging = false
        var totalDx = 0f
        var longPressed = false
        var isReleased = false

        val timeoutResult = withTimeoutOrNull(longPressTimeoutMs) {
            while (true) {
                // Use Main pass for move/up events
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull() ?: break
                
                if (!change.pressed) {
                    isReleased = true
                    change.consume()
                    viewModel.togglePanel()
                    decided = true
                    break
                }
                
                val dx = change.position.x - startX
                val dy = change.position.y - startY
                if (abs(dx) > slop || abs(dy) > slop) {
                    decided = true
                    if (abs(dx) >= abs(dy)) {
                        dragging = true
                        totalDx = dx
                        change.consume()
                    }
                    break
                }
            }
            true
        }

        if (timeoutResult == null && !decided) {
            longPressed = true
            decided = true
            viewModel.openSettings()
        }

        if (!isReleased) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull() ?: break

                if (!change.pressed) {
                    if (dragging) {
                        change.consume()
                        if (totalDx > 0) viewModel.cyclePrev() else viewModel.cycleNext()
                    } else if (longPressed) {
                        change.consume()
                    }
                    break
                }

                if (dragging) {
                    val dx = change.position.x - startX
                    totalDx = dx
                    change.consume()
                }
            }
        }
    }
}
