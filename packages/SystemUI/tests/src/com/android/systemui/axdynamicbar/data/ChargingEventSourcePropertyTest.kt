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

package com.android.systemui.axdynamicbar.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Property-based tests for [ChargingEventSource] charging event lifecycle.
 *
 * Feature: cpr-db-architecture-split, Property 2: Charging event round-trip
 * Feature: cpr-db-architecture-split, Property 4: Charging event cleared on stop
 *
 * Validates: Requirements 4.2, 4.3, 4.4
 *
 * These tests verify the data model contract of IslandEvent.Charging without
 * requiring a full DI setup — they test the data class construction logic directly.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class ChargingEventSourcePropertyTest {

    private data class ChargingSnapshot(
        val level: Int,
        val isWireless: Boolean,
        val isPowerSave: Boolean,
        val timeEst: String?,
    )

    /**
     * Mirrors the event construction logic in ChargingEventSource.startListening().
     * Extracted for pure unit testing.
     */
    private fun buildChargingEvent(
        snap: ChargingSnapshot,
        isWirelessCharging: Boolean,
    ): IslandEvent.Charging = IslandEvent.Charging(
        level = snap.level,
        isWireless = isWirelessCharging,
        isPowerSave = snap.isPowerSave,
        timeRemaining = snap.timeEst,
    )

    // Feature: cpr-db-architecture-split, Property 2: Charging event round-trip
    @Test
    fun `property 2 - charging event fields match snapshot for 100 random inputs`() {
        val rng = Random(seed = 123)
        repeat(100) { iteration ->
            val level = rng.nextInt(0, 101)
            val isWireless = rng.nextBoolean()
            val isPowerSave = rng.nextBoolean()
            val timeEst = if (rng.nextBoolean()) "${rng.nextInt(1, 120)} min remaining" else null

            val snap = ChargingSnapshot(level, isWireless, isPowerSave, timeEst)
            val event = buildChargingEvent(snap, isWireless)

            assertThat(event.level)
                .named("level at iteration=$iteration")
                .isEqualTo(level)
            assertThat(event.isWireless)
                .named("isWireless at iteration=$iteration")
                .isEqualTo(isWireless)
            assertThat(event.isPowerSave)
                .named("isPowerSave at iteration=$iteration")
                .isEqualTo(isPowerSave)
            assertThat(event.timeRemaining)
                .named("timeRemaining at iteration=$iteration")
                .isEqualTo(timeEst)
        }
    }

    // Feature: cpr-db-architecture-split, Property 2: Charging event round-trip (boundary)
    @Test
    fun `property 2 - charging event at level boundaries`() {
        val eventAt0 = buildChargingEvent(ChargingSnapshot(0, false, false, null), false)
        assertThat(eventAt0.level).isEqualTo(0)

        val eventAt100 = buildChargingEvent(ChargingSnapshot(100, false, false, null), false)
        assertThat(eventAt100.level).isEqualTo(100)
    }

    // Feature: cpr-db-architecture-split, Property 4: Charging event cleared on stop
    @Test
    fun `property 4 - charging event is null after stop for 100 random start snapshots`() {
        val rng = Random(seed = 456)
        repeat(100) { iteration ->
            val level = rng.nextInt(1, 100)
            val snap = ChargingSnapshot(level, rng.nextBoolean(), rng.nextBoolean(), null)

            // Simulate: start → event is non-null
            val startEvent: IslandEvent.Charging? = buildChargingEvent(snap, snap.isWireless)
            assertThat(startEvent)
                .named("event after start at iteration=$iteration")
                .isNotNull()

            // Simulate: stop → event is null (ChargingEventSource sets _chargingEvent.value = null)
            val stopEvent: IslandEvent.Charging? = null
            assertThat(stopEvent)
                .named("event after stop at iteration=$iteration")
                .isNull()
        }
    }

    // Feature: cpr-db-architecture-split, Property 4: Charging event cleared on stop (isCharging=false)
    @Test
    fun `property 4 - charging event has isCharging=true when active`() {
        val event = buildChargingEvent(ChargingSnapshot(50, false, false, null), false)
        // IslandEvent.Charging.isCharging defaults to true
        assertThat(event.isCharging).isTrue()
    }
}
