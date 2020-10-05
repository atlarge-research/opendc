/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.compute.simulator

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.core.Flavor
import org.opendc.compute.core.virt.HypervisorEvent
import org.opendc.simulator.compute.SimMachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.utils.DelayControllerClockAdapter
import java.time.Clock
import java.util.UUID

/**
 * Basic test-suite for the hypervisor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SimVirtDriverTest {
    private lateinit var scope: TestCoroutineScope
    private lateinit var clock: Clock
    private lateinit var machineModel: SimMachineModel

    @BeforeEach
    fun setUp() {
        scope = TestCoroutineScope()
        clock = DelayControllerClockAdapter(scope)

        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)

        machineModel = SimMachineModel(
            cpus = List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 3200.0) },
            memory = List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }

    /**
     * Test overcommissioning of a hypervisor.
     */
    @Test
    fun overcommission() {
        var requestedBurst = 0L
        var grantedBurst = 0L
        var overcommissionedBurst = 0L

        scope.launch {
            val virtDriverWorkload = SimVirtDriverWorkload()
            val vmm = SimWorkloadImage(UUID.randomUUID(), "vmm", emptyMap(), virtDriverWorkload)
            val duration = 5 * 60L
            val vmImageA = SimWorkloadImage(
                UUID.randomUUID(),
                "<unnamed>",
                emptyMap(),
                SimTraceWorkload(
                    sequenceOf(
                        SimTraceWorkload.Fragment(0, 28L * duration, duration * 1000, 28.0, 2),
                        SimTraceWorkload.Fragment(0, 3500L * duration, duration * 1000, 3500.0, 2),
                        SimTraceWorkload.Fragment(0, 0, duration * 1000, 0.0, 2),
                        SimTraceWorkload.Fragment(0, 183L * duration, duration * 1000, 183.0, 2)
                    ),
                )
            )
            val vmImageB = SimWorkloadImage(
                UUID.randomUUID(),
                "<unnamed>",
                emptyMap(),
                SimTraceWorkload(
                    sequenceOf(
                        SimTraceWorkload.Fragment(0, 28L * duration, duration * 1000, 28.0, 2),
                        SimTraceWorkload.Fragment(0, 3100L * duration, duration * 1000, 3100.0, 2),
                        SimTraceWorkload.Fragment(0, 0, duration * 1000, 0.0, 2),
                        SimTraceWorkload.Fragment(0, 73L * duration, duration * 1000, 73.0, 2)
                    )
                ),
            )

            val metalDriver =
                SimBareMetalDriver(this, clock, UUID.randomUUID(), "test", emptyMap(), machineModel)

            metalDriver.init()
            metalDriver.setImage(vmm)
            metalDriver.start()

            delay(5)

            val flavor = Flavor(2, 0)
            val vmDriver = virtDriverWorkload.driver
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

        scope.advanceUntilIdle()

        assertAll(
            { assertEquals(emptyList<Throwable>(), scope.uncaughtExceptions, "No errors") },
            { assertEquals(2073600, requestedBurst, "Requested Burst does not match") },
            { assertEquals(2013600, grantedBurst, "Granted Burst does not match") },
            { assertEquals(60000, overcommissionedBurst, "Overcommissioned Burst does not match") },
            { assertEquals(1200007, scope.currentTime) }
        )
    }
}
