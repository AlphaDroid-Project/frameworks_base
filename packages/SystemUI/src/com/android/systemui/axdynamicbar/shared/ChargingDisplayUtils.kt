/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.axdynamicbar.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.res.R

/**
 * Returns the battery level as a percentage string (e.g. "73%"), or an empty
 * string if the level is unknown. Pure — no Compose context required.
 */
fun IslandEvent.Charging.displayLevel(): String = level?.let { "$it%" } ?: ""

/**
 * Returns the most meaningful time-remaining label for this charging state:
 * - 100 %  → fully-charged string
 * - otherwise → raw [timeRemaining] from the system, or null if unavailable
 */
@Composable
fun IslandEvent.Charging.displayTimeRemaining(): String? =
    if (level == 100) stringResource(R.string.ax_dynamic_bar_fully_charged)
    else timeRemaining?.takeIf { it.isNotEmpty() }

/**
 * Returns the charging-mode label that best describes the current state:
 * - 100 %    → fully-charged string
 * - wireless → wireless-charging string
 * - else     → generic charging string
 */
@Composable
fun IslandEvent.Charging.displayMode(): String = when {
    level == 100 -> stringResource(R.string.ax_dynamic_bar_fully_charged)
    isWireless   -> stringResource(R.string.ax_dynamic_bar_wireless_charging)
    else         -> stringResource(R.string.ax_dynamic_bar_charging)
}

/**
 * Returns a single composed string suitable for single-line display
 * (status-bar chip, stack card text). Combines level + time/mode.
 *
 * Examples:
 *   "73% • 1h 20m left"   (level + time estimate)
 *   "Charged"              (fully-charged — level omitted)
 *   "73%"                  (level only, no estimate)
 *   "Charging"             (no level — edge case)
 */
@Composable
fun IslandEvent.Charging.formatBatteryInfo(): String {
    val lvl  = displayLevel()
    val time = displayTimeRemaining()
    return when {
        level == 100                              -> displayMode()
        lvl.isNotEmpty() && !time.isNullOrEmpty() -> "$lvl • $time"
        lvl.isNotEmpty()                          -> lvl
        !time.isNullOrEmpty()                     -> time
        else                                      -> displayMode()
    }
}
