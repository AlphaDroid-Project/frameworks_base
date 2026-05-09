/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.axdynamicbar.model

/**
 * Hardware-derived placement hint for the display cutout.
 *
 * Computed from the physical cutout geometry by [AxDynamicBarInteractor.deriveCutoutPlacementHint].
 *
 * Derivation rule:
 *   LEFT   — cutout centerX < screenWidth / 3
 *   RIGHT  — cutout centerX > 2 * screenWidth / 3
 *   CENTER — otherwise, or when no cutout is present
 */
enum class CutoutPlacementHint {
    LEFT,
    CENTER,
    RIGHT,
}
