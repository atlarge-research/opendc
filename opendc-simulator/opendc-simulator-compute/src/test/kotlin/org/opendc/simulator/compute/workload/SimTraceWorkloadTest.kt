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

package org.opendc.simulator.compute.workload

import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.model.*
import org.opendc.simulator.compute.power.ConstantPowerModel
import org.opendc.simulator.compute.power.SimplePowerDriver
import org.opendc.simulator.compute.runWorkload
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.flow.FlowEngine

/**
 * Test suite for the [SimTraceWorkloadTest] class.
 */
class SimTraceWorkloadTest {
    private lateinit var machineModel: MachineModel

    @BeforeEach
    fun setUp() {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)

        machineModel = MachineModel(
            cpus = List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 1000.0) },
            memory = List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }

    @Test
    fun testSmoke() = runBlockingSimulation {
        val machine = SimBareMetalMachine(
            FlowEngine(coroutineContext, clock),
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        val workload = SimTraceWorkload(
            SimTrace.ofFragments(
                SimTraceFragment(0, 1000, 2 * 28.0, 2),
                SimTraceFragment(1000, 1000, 2 * 3100.0, 2),
                SimTraceFragment(2000, 1000, 0.0, 2),
                SimTraceFragment(3000, 1000, 2 * 73.0, 2)
            ),
            offset = 0
        )

        machine.runWorkload(workload)

        assertEquals(4000, clock.millis())
    }

    @Test
    fun testOffset() = runBlockingSimulation {
        val machine = SimBareMetalMachine(
            FlowEngine(coroutineContext, clock),
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        val workload = SimTraceWorkload(
            SimTrace.ofFragments(
                SimTraceFragment(0, 1000, 2 * 28.0, 2),
                SimTraceFragment(1000, 1000, 2 * 3100.0, 2),
                SimTraceFragment(2000, 1000, 0.0, 2),
                SimTraceFragment(3000, 1000, 2 * 73.0, 2)
            ),
            offset = 1000
        )

        machine.runWorkload(workload)

        assertEquals(5000, clock.millis())
    }

    @Test
    fun testSkipFragment() = runBlockingSimulation {
        val machine = SimBareMetalMachine(
            FlowEngine(coroutineContext, clock),
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        val workload = SimTraceWorkload(
            SimTrace.ofFragments(
                SimTraceFragment(0, 1000, 2 * 28.0, 2),
                SimTraceFragment(1000, 1000, 2 * 3100.0, 2),
                SimTraceFragment(2000, 1000, 0.0, 2),
                SimTraceFragment(3000, 1000, 2 * 73.0, 2)
            ),
            offset = 0
        )

        delay(1000L)
        machine.runWorkload(workload)

        assertEquals(4000, clock.millis())
    }

    @Test
    fun testZeroCores() = runBlockingSimulation {
        val machine = SimBareMetalMachine(
            FlowEngine(coroutineContext, clock),
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        val workload = SimTraceWorkload(
            SimTrace.ofFragments(
                SimTraceFragment(0, 1000, 2 * 28.0, 2),
                SimTraceFragment(1000, 1000, 2 * 3100.0, 2),
                SimTraceFragment(2000, 1000, 0.0, 0),
                SimTraceFragment(3000, 1000, 2 * 73.0, 2)
            ),
            offset = 0
        )

        machine.runWorkload(workload)

        assertEquals(4000, clock.millis())
    }
}
