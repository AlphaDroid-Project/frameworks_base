/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.alpha.tiles.dagger

import com.android.systemui.alpha.tiles.UiStyleTile
import com.android.systemui.qs.tileimpl.QSTileImpl
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/** Dagger module for Alpha custom QS tiles. */
@Module
interface AlphaQSTilesModule {

    @Binds
    @IntoMap
    @StringKey(UiStyleTile.TILE_SPEC)
    fun bindUiStyleTile(tile: UiStyleTile): QSTileImpl<*>
}
