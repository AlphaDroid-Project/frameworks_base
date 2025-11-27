/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.common

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.tan

/**
 * Helper to ensure consistent slash angles across different component sizes.
 */
object SlashGeometry {

    // Default angle and start ratio for consistent look
    private const val DEFAULT_ANGLE_DEGREES = 20f
    private const val DEFAULT_START_X_RATIO = 0.7f

    /**
     * Calculates the horizontal offset for the slant based on height and angle.
     *
     * @param height The height of the component
     * @param angleDegrees The angle of the slash from vertical (e.g., 20 degrees)
     */
    fun getSlashOffset(height: Float, angleDegrees: Float): Float {
        val radians = Math.toRadians(angleDegrees.toDouble())
        return (height * tan(radians)).toFloat()
    }

    /**
     * Simplified overload using default angle.
     */
    fun getSlashOffset(height: Float): Float {
        return getSlashOffset(height, DEFAULT_ANGLE_DEGREES)
    }

    /**
     * Creates a path for the "Slash" effect (the right-side overlay).
     *
     * @param bounds The full bounds of the container
     * @param offset The horizontal offset calculated from angle/height
     * @param startXRatio The horizontal position (0.0-1.0) where the slash starts at the top
     * @param isRightSide If true, draws the right chunk.If false, draws the left chunk.
     */
    fun createSlashPath(
        bounds: Rect,
        offset: Float,
        startXRatio: Float,
        isRightSide: Boolean = true
    ): Path {
        val topX = bounds.left + (bounds.width * startXRatio)
        val bottomX = topX - offset

        return Path().apply {
            if (isRightSide) {
                moveTo(topX, bounds.top)
                lineTo(bounds.right, bounds.top)
                lineTo(bounds.right, bounds.bottom)
                lineTo(bottomX, bounds.bottom)
                close()
            } else {
                moveTo(bounds.left, bounds.top)
                lineTo(topX, bounds.top)
                lineTo(bottomX, bounds.bottom)
                lineTo(bounds.left, bounds.bottom)
                close()
            }
        }
    }

    /**
     * Simplified overload using default angle and start ratio.
     */
    fun createSlashPath(bounds: Rect, isRightSide: Boolean = true): Path {
        val offset = getSlashOffset(bounds.height, DEFAULT_ANGLE_DEGREES)
        return createSlashPath(bounds, offset, DEFAULT_START_X_RATIO, isRightSide)
    }
}