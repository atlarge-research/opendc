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
import kotlinx.coroutines.flow.toList
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
 * Test suite for the [SimBareMetalMachine] class.
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
    fun testFlopsWorkload() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val machine = SimBareMetalMachine(coroutineContext, clock, machineModel)

        try {
            machine.run(SimFlopsWorkload(2_000, utilization = 1.0))

            // Two cores execute 1000 MFlOps per second (1000 ms)
            assertEquals(1000, currentTime)
        } finally {
            machine.close()
        }
    }

    @Test
    fun testUsage() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val machine = SimBareMetalMachine(coroutineContext, clock, machineModel)

        val res = mutableListOf<Double>()
        val job = launch { machine.usage.toList(res) }

        try {
            machine.run(SimFlopsWorkload(2_000, utilization = 1.0))
            yield()
            job.cancel()
            assertEquals(listOf(0.0, 0.5, 1.0, 0.5, 0.0), res) { "Machine is fully utilized" }
        } finally {
            machine.close()
        }
    }
}
