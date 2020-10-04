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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.utils.DelayControllerClockAdapter
import java.time.Clock

/**
 * Test suite for the [SimHypervisor] class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SimHypervisorTest {
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
        val listener = object : SimHypervisor.Listener {
            var totalRequestedBurst = 0L
            var totalGrantedBurst = 0L
            var totalOvercommissionedBurst = 0L

            override fun onSliceFinish(
                hypervisor: SimHypervisor,
                requestedBurst: Long,
                grantedBurst: Long,
                overcommissionedBurst: Long,
                interferedBurst: Long,
                cpuUsage: Double,
                cpuDemand: Double
            ) {
                totalRequestedBurst += requestedBurst
                totalGrantedBurst += grantedBurst
                totalOvercommissionedBurst += overcommissionedBurst
            }
        }

        scope.launch {
            val duration = 5 * 60L
            val workloadA =
                SimTraceWorkload(
                    sequenceOf(
                        SimTraceWorkload.Fragment(0, 28L * duration, duration * 1000, 28.0, 2),
                        SimTraceWorkload.Fragment(0, 3500L * duration, duration * 1000, 3500.0, 2),
                        SimTraceWorkload.Fragment(0, 0, duration * 1000, 0.0, 2),
                        SimTraceWorkload.Fragment(0, 183L * duration, duration * 1000, 183.0, 2)
                    ),
                )
            val workloadB =
                SimTraceWorkload(
                    sequenceOf(
                        SimTraceWorkload.Fragment(0, 28L * duration, duration * 1000, 28.0, 2),
                        SimTraceWorkload.Fragment(0, 3100L * duration, duration * 1000, 3100.0, 2),
                        SimTraceWorkload.Fragment(0, 0, duration * 1000, 0.0, 2),
                        SimTraceWorkload.Fragment(0, 73L * duration, duration * 1000, 73.0, 2)
                    )
                )

            val machine = SimBareMetalMachine(scope, clock, machineModel)
            val hypervisor = SimHypervisor(scope, clock, listener)

            launch {
                machine.run(hypervisor)
            }

            yield()
            launch { hypervisor.createMachine(machineModel).run(workloadA) }
            launch { hypervisor.createMachine(machineModel).run(workloadB) }
        }

        scope.advanceUntilIdle()

        assertAll(
            { Assertions.assertEquals(emptyList<Throwable>(), scope.uncaughtExceptions, "No errors") },
            { Assertions.assertEquals(2073600, listener.totalRequestedBurst, "Requested Burst does not match") },
            { Assertions.assertEquals(2013600, listener.totalGrantedBurst, "Granted Burst does not match") },
            { Assertions.assertEquals(60000, listener.totalOvercommissionedBurst, "Overcommissioned Burst does not match") },
            { Assertions.assertEquals(1200001, scope.currentTime) }
        )
    }
}
