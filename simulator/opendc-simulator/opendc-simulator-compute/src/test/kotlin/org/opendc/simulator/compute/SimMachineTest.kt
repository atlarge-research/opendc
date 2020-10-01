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

package org.opendc.simulator.compute

import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.workload.SimFlopsWorkload
import org.opendc.simulator.utils.DelayControllerClockAdapter

/**
 * Test suite for the [SimMachine] class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SimMachineTest {
    private lateinit var machineModel: SimMachineModel

    @BeforeEach
    fun setUp() {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)

        machineModel = SimMachineModel(
            cpus = List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 1000.0) },
            memory = List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }

    @Test
    fun testFlopsWorkload() {
        val testScope = TestCoroutineScope()
        val clock = DelayControllerClockAdapter(testScope)
        val machine = SimMachine(testScope, clock, machineModel)

        testScope.runBlockingTest {
            machine.run(SimFlopsWorkload(2_000, 2, utilization = 1.0))

            // Two cores execute 1000 MFlOps per second (1000 ms)
            assertEquals(1000, testScope.currentTime)
        }
    }

    @Test
    fun testUsage() {
        val testScope = TestCoroutineScope()
        val clock = DelayControllerClockAdapter(testScope)
        val machine = SimMachine(testScope, clock, machineModel)

        testScope.runBlockingTest {
            machine.run(SimFlopsWorkload(2_000, 2, utilization = 1.0))
            assertEquals(1.0, machine.usage.value)

            // Wait for the usage to reset
            delay(1)
            assertEquals(0.0, machine.usage.value)
        }
    }
}
