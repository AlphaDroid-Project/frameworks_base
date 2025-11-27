/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.android.compose.theme.LocalAndroidColorScheme

/**
 * AlphaDroid Color Schema.
 *
 * This acts as the unified abstraction layer for colors used in SystemUI components
 * (Tiles, Sliders, etc).
 *
 * 1. It defaults to mapping standard AOSP tokens (MaterialTheme + AndroidColorScheme).
 * 2. It allows Custom Styles to override these values globally before components render.
 */
@Immutable
data class AlphaColorScheme(
    // Active State (e.g., Active Tiles, Slider Track)
    val accent: Color,
    val onAccent: Color,

    // Inactive State (e.g., Inactive Tiles, Slider Track Background)
    val neutral: Color,
    val onNeutral: Color,

    // Secondary Neutral (e.g., Icon background in Dual-Target tiles)
    val neutralVariant: Color,
    val onNeutralVariant: Color,

    // Special UI Elements
    val thumb: Color, // Specific color for Slider Thumb (needs contrast against accent track)

    // Semantic colors
    val error: Color,
    val onError: Color
)

/**
 * CompositionLocal to provide the schema down the tree.
 */
val LocalAlphaColorScheme = staticCompositionLocalOf<AlphaColorScheme> {
    error("No AlphaColorScheme provided. Make sure to wrap content in a provider.")
}

/**
 * Factory function to create the default AlphaColorScheme based on the current
 * system state (Light/Dark mode, Monet engine, etc).
 */
@Composable
@ReadOnlyComposable
fun defaultAlphaColorScheme(): AlphaColorScheme {
    // 1. Get Standard Material 3 Colors (Dynamic Monet)
    val material = MaterialTheme.colorScheme

    // 2. Get Custom SystemUI Colors (Surface Effects, etc)
    val android = LocalAndroidColorScheme.current

    return AlphaColorScheme(
        // Map Material Primary -> Accent
        accent = material.primary,
        onAccent = material.onPrimary,

        // Map SurfaceEffect2 -> Neutral (Standard AOSP inactive tile color)
        neutral = android.surfaceEffect2,
        onNeutral = material.onSurface,

        // Map SurfaceEffect3 -> Neutral Variant (Standard AOSP dual-tile icon background)
        neutralVariant = android.surfaceEffect3,
        onNeutralVariant = material.onSurface,

        // Thumb usually matches primary/accent
        thumb = material.primary,

        // Error states
        error = material.error,
        onError = material.onError
    )
}
