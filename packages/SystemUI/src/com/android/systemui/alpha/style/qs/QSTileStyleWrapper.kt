/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.style.qs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.android.systemui.alpha.style.qs.renderers.QSTileStyleRenderer
import kotlin.math.min

/**
 * Wrapper that applies custom style effects to QS tiles.
 *
 * @param renderer Style renderer, null for AOSP default
 * @param shape Tile shape (respects external shape configuration)
 * @param state Tile state (STATE_ACTIVE, STATE_INACTIVE, STATE_UNAVAILABLE)
 * @param materialColor Material You background color
 * @param isSmallTile True for small tile, false for large tile component
 * @param isIconBackground True if wrapping icon background in large tile
 * @param modifier Modifier chain
 * @param content Tile content (backgrounds only, icons/text outside)
 */
@Composable
fun QSTileStyleWrapper(
    renderer: QSTileStyleRenderer?,
    shape: Shape,
    state: Int,
    materialColor: Color,
    isSmallTile: Boolean = true,
    isIconBackground: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (renderer == null) {
        Box(modifier = modifier.clip(shape), content = content)
        return
    }

    val density = LocalDensity.current

    val cornerRadiusPx = remember(shape, density) {
        when {
            shape === CircleShape -> -1f
            shape is RoundedCornerShape -> {
                with(density) { shape.topStart.toPx(Size.Unspecified, this) }
            }
            else -> 0f
        }
    }

    val styledModifier = modifier
        .clip(shape)
        .drawWithContent {
            drawContent()

            val actualCornerRadius = if (cornerRadiusPx == -1f) {
                min(size.width, size.height) / 2f
            } else {
                cornerRadiusPx
            }

            val bounds = Rect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height
            )

            if (isIconBackground) {
                with(renderer) {
                    renderIconBackgroundOverlay(
                        iconBackgroundBounds = bounds,
                        shape = shape,
                        cornerRadius = actualCornerRadius,
                        materialColor = materialColor,
                        state = state,
                        density = density
                    )
                }
            } else {
                with(renderer) {
                    renderTileBackgroundOverlay(
                        tileBounds = bounds,
                        shape = shape,
                        cornerRadius = actualCornerRadius,
                        materialColor = materialColor,
                        state = state,
                        isSmallTile = isSmallTile,
                        density = density
                    )
                }
            }
        }

    Box(modifier = styledModifier, content = content)
}
