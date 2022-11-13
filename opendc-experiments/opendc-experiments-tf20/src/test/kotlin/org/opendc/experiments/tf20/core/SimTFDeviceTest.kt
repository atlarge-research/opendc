/*
 * Copyright (c) 2021 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.experiments.tf20.core

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.CpuPowerModels
import org.opendc.simulator.kotlin.runSimulation
import java.util.UUID

/**
 * Test suite for the [SimTFDevice] class.
 */
internal class SimTFDeviceTest {
    @Test
    fun testSmoke() = runSimulation {
        val puNode = ProcessingNode("NVIDIA", "Tesla V100", "unknown", 1)
        val pu = ProcessingUnit(puNode, 0, 960 * 1230.0)
        val memory = MemoryUnit("NVIDIA", "Tesla V100", 877.0, 32_000)

        val device = SimTFDevice(
            UUID.randomUUID(),
            isGpu = true,
            dispatcher,
            pu,
            memory,
            CpuPowerModels.linear(250.0, 100.0)
        )

        // Load 1 GiB into GPU memory
        device.load(1000)
        assertEquals(1140, timeSource.millis())

        coroutineScope {
            launch { device.compute(1e6) }
            launch { device.compute(2e6) }
        }

        device.close()

        val stats = device.getDeviceStats()

        assertAll(
            { assertEquals(3681, timeSource.millis()) },
            { assertEquals(749.25, stats.energyUsage) }
        )
    }
}
