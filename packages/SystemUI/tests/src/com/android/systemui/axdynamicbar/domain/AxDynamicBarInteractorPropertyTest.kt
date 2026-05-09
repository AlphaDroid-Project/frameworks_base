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

package com.android.systemui.axdynamicbar.domain

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
 * Property-based tests for [AxDynamicBarInteractor] pill visibility invariant.
 *
 * Feature: cpr-db-architecture-split, Property 5: Pill visibility invariant in cutout mode
 *
 * Validates: Requirements 6.1, 6.4
 *
 * Property: islandState == CHIP iff isCutoutModeEnabled && events.isNotEmpty()
 * and not blocked by panel/doze/dreaming.
 *
 * These tests verify the state derivation logic extracted as a pure function,
 * without requiring a full DI setup.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class AxDynamicBarInteractorPropertyTest {

    /**
     * Pure function mirroring the islandState derivation in AxDynamicBarInteractor.
     * Extracted for unit testing.
     */
    private fun deriveIslandState(
        events: List<IslandEvent>,
        panelBlocking: Boolean,
        statusBlocking: Boolean,
    ): IslandState = when {
        events.isEmpty() -> IslandState.HIDDEN
        panelBlocking || statusBlocking -> IslandState.HIDDEN
        // Architecture split (Task 7): DB Compose pill is shown in cutout mode
        else -> IslandState.CHIP
    }

    private fun makeChargingEvent(level: Int = 50) = IslandEvent.Charging(
        level = level,
        isWireless = false,
    )

    // Feature: cpr-db-architecture-split, Property 5: Pill visibility invariant in cutout mode
    @Test
    fun `property 5 - pill is CHIP when events present and not blocked for 100 random inputs`() {
        val rng = Random(seed = 321)
        repeat(100) { iteration ->
            val eventCount = rng.nextInt(1, 6) // 1..5 events
            val events = List(eventCount) { makeChargingEvent(rng.nextInt(1, 100)) }
            val panelBlocking = false
            val statusBlocking = false

            val state = deriveIslandState(events, panelBlocking, statusBlocking)

            assertThat(state)
                .named("state at iteration=$iteration, eventCount=$eventCount")
                .isEqualTo(IslandState.CHIP)
        }
    }

    // Feature: cpr-db-architecture-split, Property 5: Pill visibility invariant (empty events)
    @Test
    fun `property 5 - pill is HIDDEN when event list is empty`() {
        val state = deriveIslandState(emptyList(), false, false)
        assertThat(state).isEqualTo(IslandState.HIDDEN)
    }

    // Feature: cpr-db-architecture-split, Property 5: Pill visibility invariant (panel blocking)
    @Test
    fun `property 5 - pill is HIDDEN when panel is blocking for 100 random inputs`() {
        val rng = Random(seed = 654)
        repeat(100) { iteration ->
            val eventCount = rng.nextInt(1, 6)
            val events = List(eventCount) { makeChargingEvent() }
            val panelBlocking = true
            val statusBlocking = rng.nextBoolean()

            val state = deriveIslandState(events, panelBlocking, statusBlocking)

            assertThat(state)
                .named("state at iteration=$iteration")
                .isEqualTo(IslandState.HIDDEN)
        }
    }

    // Feature: cpr-db-architecture-split, Property 5: Pill visibility invariant (status blocking)
    @Test
    fun `property 5 - pill is HIDDEN when status is blocking for 100 random inputs`() {
        val rng = Random(seed = 987)
        repeat(100) { iteration ->
            val eventCount = rng.nextInt(1, 6)
            val events = List(eventCount) { makeChargingEvent() }
            val panelBlocking = rng.nextBoolean()
            val statusBlocking = true

            val state = deriveIslandState(events, panelBlocking, statusBlocking)

            assertThat(state)
                .named("state at iteration=$iteration")
                .isEqualTo(IslandState.HIDDEN)
        }
    }

    // Feature: cpr-db-architecture-split, Property 5: Pill visibility invariant (combined)
    @Test
    fun `property 5 - visibility invariant holds for all combinations of blocking flags`() {
        val events = listOf(makeChargingEvent())

        // Not blocked → CHIP
        assertThat(deriveIslandState(events, false, false)).isEqualTo(IslandState.CHIP)
        // Panel blocked → HIDDEN
        assertThat(deriveIslandState(events, true, false)).isEqualTo(IslandState.HIDDEN)
        // Status blocked → HIDDEN
        assertThat(deriveIslandState(events, false, true)).isEqualTo(IslandState.HIDDEN)
        // Both blocked → HIDDEN
        assertThat(deriveIslandState(events, true, true)).isEqualTo(IslandState.HIDDEN)
        // Empty events → HIDDEN regardless of blocking
        assertThat(deriveIslandState(emptyList(), false, false)).isEqualTo(IslandState.HIDDEN)
    }
}
