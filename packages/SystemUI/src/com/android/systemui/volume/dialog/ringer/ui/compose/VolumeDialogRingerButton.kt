/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.volume.dialog.ringer.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.systemui.alpha.style.common.LocalAlphaColorScheme
import com.android.systemui.alpha.style.common.defaultAlphaColorScheme
import com.android.systemui.alpha.style.volume.VolumeMaterialColors
import com.android.systemui.alpha.style.volume.VolumeSliderStyleManager
import com.android.systemui.alpha.style.volume.VolumeSliderStyleWrapper
import com.android.systemui.volume.dialog.ringer.ui.viewmodel.RingerButtonUiModel
import com.android.systemui.volume.dialog.ringer.ui.viewmodel.RingerButtonViewModel

class RingerButtonComposeState {
    var buttonViewModel by mutableStateOf<RingerButtonViewModel?>(null)
    var uiModel by mutableStateOf<RingerButtonUiModel?>(null)
    var contentDescription by mutableStateOf("")
    var isSelected by mutableStateOf(false)
    var onClick by mutableStateOf<() -> Unit>({})
}

@Composable
fun VolumeDialogRingerButton(
    state: RingerButtonComposeState,
    volumeSliderStyleManager: VolumeSliderStyleManager,
    modifier: Modifier = Modifier,
) {
    val buttonViewModel = state.buttonViewModel ?: return
    val uiModel = state.uiModel ?: return

    val defaultScheme = defaultAlphaColorScheme()
    val styleState by volumeSliderStyleManager.styleState.collectAsStateWithLifecycle()

    val styleRenderer = androidx.compose.runtime.remember(
        styleState.styleId,
        styleState.settings,
        styleState.themeVersion,
        styleState.isNightMode,
        defaultScheme.accent,
        defaultScheme.neutral,
    ) {
        volumeSliderStyleManager.getRenderer(
            accentColor = defaultScheme.accent,
            neutralColor = defaultScheme.neutral,
        )
    }

    val themedScheme = androidx.compose.runtime.remember(styleRenderer, defaultScheme) {
        styleRenderer?.produceColorScheme(defaultScheme) ?: defaultScheme
    }

    val backgroundColor = Color(uiModel.backgroundColor)
    val iconTint = Color(uiModel.tintColor)
    val cornerRadiusDp = with(LocalDensity.current) { uiModel.cornerRadius.toDp() }
    val shape = RoundedCornerShape(cornerRadiusDp)

    androidx.compose.runtime.CompositionLocalProvider(LocalAlphaColorScheme provides themedScheme) {
        VolumeSliderStyleWrapper(
            renderer = styleRenderer,
            shape = shape,
            segmentMode = false,
            isVertical = false,
            isActive = state.isSelected,
            materialColors = VolumeMaterialColors(
                activeSegment = backgroundColor,
                inactiveSegment = backgroundColor,
                activeButton = backgroundColor,
                inactiveButton = backgroundColor,
            ),
            modifier = modifier
                .fillMaxSize()
                .clip(shape)
                .clickable(role = Role.Button, onClick = state.onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = state.contentDescription
                    selected = state.isSelected
                    role = Role.Button
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = buttonViewModel.imageResId),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconTint),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
