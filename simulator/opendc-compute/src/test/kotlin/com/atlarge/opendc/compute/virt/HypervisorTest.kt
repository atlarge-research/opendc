/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.compute.virt

import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.ProcessingNode
import com.atlarge.opendc.compute.core.ProcessingUnit
import com.atlarge.opendc.compute.core.image.FlopsApplicationImage
import com.atlarge.opendc.compute.core.image.FlopsHistoryFragment
import com.atlarge.opendc.compute.core.image.VmImage
import com.atlarge.opendc.compute.metal.driver.SimpleBareMetalDriver
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.simulator.utils.DelayControllerClockAdapter
import java.util.UUID

/**
 * Basic test-suite for the hypervisor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class HypervisorTest {
    /**
     * A smoke test for the bare-metal driver.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Disabled
    fun smoke() {
        val testScope = TestCoroutineScope()
        val clock = DelayControllerClockAdapter(testScope)

        testScope.launch {
            val vmm = HypervisorImage
            val workloadA = FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 1_000, 1)
            val workloadB = FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), 2_000, 1)

            val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 1)
            val cpus = List(1) { ProcessingUnit(cpuNode, it, 2000.0) }
            val metalDriver = SimpleBareMetalDriver(this, clock, UUID.randomUUID(), "test", emptyMap(), cpus, emptyList())

            metalDriver.init()
            metalDriver.setImage(vmm)
            val node = metalDriver.start()
            node.server?.events?.onEach { println(it) }?.launchIn(this)

            delay(5)

            val flavor = Flavor(1, 0)
            val vmDriver = metalDriver.refresh().server!!.services[VirtDriver]
            vmDriver.events.onEach { println(it) }.launchIn(this)
            val vmA = vmDriver.spawn("a", workloadA, flavor)
            vmA.events.onEach { println(it) }.launchIn(this)
            val vmB = vmDriver.spawn("b", workloadB, flavor)
            vmB.events.onEach { println(it) }.launchIn(this)
        }

        testScope.advanceUntilIdle()
    }

    /**
     * Test overcommissioning of a hypervisor.
     */
    @Test
    fun overcommission() {
        val testScope = TestCoroutineScope()
        val clock = DelayControllerClockAdapter(testScope)

        var requestedBurst = 0L
        var grantedBurst = 0L
        var overcommissionedBurst = 0L

        testScope.launch {
            val vmm = HypervisorImage
            val duration = 5 * 60L
            val vmImageA = VmImage(
                UUID.randomUUID(),
                "<unnamed>",
                emptyMap(),
                sequenceOf(
                    FlopsHistoryFragment(0, 28L * duration, duration * 1000, 28.0, 2),
                    FlopsHistoryFragment(0, 3500L * duration, duration * 1000, 3500.0, 2),
                    FlopsHistoryFragment(0, 0, duration * 1000, 0.0, 2),
                    FlopsHistoryFragment(0, 183L * duration, duration * 1000, 183.0, 2)
                ),
                2,
                0
            )
            val vmImageB = VmImage(
                UUID.randomUUID(),
                "<unnamed>",
                emptyMap(),
                sequenceOf(
                    FlopsHistoryFragment(0, 28L * duration, duration * 1000, 28.0, 2),
                    FlopsHistoryFragment(0, 3100L * duration, duration * 1000, 3100.0, 2),
                    FlopsHistoryFragment(0, 0, duration * 1000, 0.0, 2),
                    FlopsHistoryFragment(0, 73L * duration, duration * 1000, 73.0, 2)
                ),
                2,
                0
            )

            val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)
            val cpus = List(2) { ProcessingUnit(cpuNode, it, 3200.0) }
            val metalDriver = SimpleBareMetalDriver(this, clock, UUID.randomUUID(), "test", emptyMap(), cpus, emptyList())

            metalDriver.init()
            metalDriver.setImage(vmm)
            metalDriver.start()

            delay(5)

            val flavor = Flavor(2, 0)
            val vmDriver = metalDriver.refresh().server!!.services[VirtDriver]
            vmDriver.events
                .onEach { event ->
                    when (event) {
                        is HypervisorEvent.SliceFinished -> {
                            requestedBurst += event.requestedBurst
                            grantedBurst += event.grantedBurst
                            overcommissionedBurst += event.overcommissionedBurst
                        }
                    }
                }
                .launchIn(this)

            vmDriver.spawn("a", vmImageA, flavor)
            vmDriver.spawn("b", vmImageB, flavor)
        }

        testScope.advanceUntilIdle()

        assertAll(
            { assertEquals(2073600, requestedBurst, "Requested Burst does not match") },
            { assertEquals(2013600, grantedBurst, "Granted Burst does not match") },
            { assertEquals(60000, overcommissionedBurst, "Overcommissioned Burst does not match") }
        )
    }
}
