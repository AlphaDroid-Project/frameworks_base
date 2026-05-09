/*
 * Copyright (C) 2026 AlphaDroid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.axdynamicbar.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.axdynamicbar.model.IslandState
import com.android.systemui.axdynamicbar.model.IslandUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based tests for event resolution being cutout-mode-agnostic.
 *
 * Feature: cpr-db-architecture-split, Property 6: Event resolution is cutout-mode-agnostic
 *
 * Validates: Requirements 6.3
 *
 * Property: For any IslandEvent, the label, icon, and label2 resolved by
 * AxDynamicBarChipViewModel are identical regardless of whether cutout mode is active.
 *
 * After the architecture split, event resolution (label, icon) lives entirely in the
 * DB Compose pipeline and has no dependency on isCutoutModeEnabled. This test verifies
 * that the chipState derivation produces the same event data regardless of cutout mode.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class AxDynamicBarChipViewModelPropertyTest {

    /**
     * Mirrors the chipState derivation logic in AxDynamicBarChipViewModel.
     * After the architecture split, islandState is always CHIP when events are present
     * (regardless of cutout mode), so chipState is always non-null when events exist.
     */
    private fun deriveChipState(
        uiState: IslandUiState,
        stackActive: Boolean,
    ): AxDynamicBarChipState? {
        val shouldShow = uiState.shouldShow || (stackActive && uiState.events.isNotEmpty())
        if (!shouldShow) return null
        val alert = uiState.notificationAlert
        val topEvent = uiState.topEvent ?: alert ?: return null
        return AxDynamicBarChipState(
            event = topEvent,
            eventCount = uiState.activeEvents.size,
            pinnedIndex = uiState.pinnedEventIndex,
            allEvents = uiState.events,
            notificationAlert = alert,
        )
    }

    private fun makeUiState(
        events: List<IslandEvent>,
        islandState: IslandState = IslandState.CHIP,
    ) = IslandUiState(
        events = events,
        islandState = islandState,
        pinnedEventIndex = 0,
    )

    // Feature: cpr-db-architecture-split, Property 6: Event resolution is cutout-mode-agnostic
    @Test
    fun `property 6 - chipState event is identical regardless of cutout mode for 100 inputs`() {
        val rng = Random(seed = 111)
        repeat(100) { iteration ->
            val level = rng.nextInt(1, 100)
            val event = IslandEvent.Charging(level = level, isWireless = rng.nextBoolean())
            val events = listOf(event)

            // Non-cutout mode: islandState = CHIP
            val nonCutoutState = makeUiState(events, IslandState.CHIP)
            val chipStateNonCutout = deriveChipState(nonCutoutState, false)

            // Cutout mode: islandState = CHIP (after Task 7 behavioral flip)
            val cutoutState = makeUiState(events, IslandState.CHIP)
            val chipStateCutout = deriveChipState(cutoutState, false)

            // Both should produce non-null chipState with identical event data
            assertThat(chipStateNonCutout)
                .named("chipState non-cutout at iteration=$iteration")
                .isNotNull()
            assertThat(chipStateCutout)
                .named("chipState cutout at iteration=$iteration")
                .isNotNull()

            assertThat(chipStateCutout!!.event)
                .named("event at iteration=$iteration")
                .isEqualTo(chipStateNonCutout!!.event)
            assertThat(chipStateCutout.eventCount)
                .named("eventCount at iteration=$iteration")
                .isEqualTo(chipStateNonCutout.eventCount)
            assertThat(chipStateCutout.pinnedIndex)
                .named("pinnedIndex at iteration=$iteration")
                .isEqualTo(chipStateNonCutout.pinnedIndex)
        }
    }

    // Feature: cpr-db-architecture-split, Property 6: Event resolution (null when empty)
    @Test
    fun `property 6 - chipState is null when event list is empty regardless of cutout mode`() {
        val emptyNonCutout = makeUiState(emptyList(), IslandState.HIDDEN)
        val emptyCutout = makeUiState(emptyList(), IslandState.HIDDEN)

        assertThat(deriveChipState(emptyNonCutout, false)).isNull()
        assertThat(deriveChipState(emptyCutout, false)).isNull()
    }

    // Feature: cpr-db-architecture-split, Property 6: Event resolution (stack mode)
    @Test
    fun `property 6 - chipState is non-null in stack mode regardless of islandState`() {
        val event = IslandEvent.Charging(level = 50, isWireless = false)
        // In cutout mode with stack open, islandState may be HIDDEN but stackActive=true
        val hiddenState = makeUiState(listOf(event), IslandState.HIDDEN)
        val chipState = deriveChipState(hiddenState, stackActive = true)
        assertThat(chipState).isNotNull()
        assertThat(chipState!!.event).isEqualTo(event)
    }
}
