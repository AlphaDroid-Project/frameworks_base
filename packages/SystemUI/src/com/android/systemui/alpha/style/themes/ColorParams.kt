/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.themes

/**
 * Common configuration parameters for base color transformation.
 *
 */
data class ColorParams(
    val baseLightness: Float,      // Force background to this lightness (0.0-1.0)
    val baseSaturation: Float,     // Multiply background saturation by this factor
    val baseAlpha: Float,          // Opacity of the background
    val forceLightContent: Boolean // If true, forces text/icons to White (useful for dark tiles in light mode)
)
