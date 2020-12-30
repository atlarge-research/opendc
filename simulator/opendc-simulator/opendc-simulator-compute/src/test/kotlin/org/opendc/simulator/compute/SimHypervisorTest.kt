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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.simulator.compute.model.SimMemoryUnit
import org.opendc.simulator.compute.model.SimProcessingNode
import org.opendc.simulator.compute.model.SimProcessingUnit
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

        val cpuNode = SimProcessingNode("Intel", "Xeon", "amd64", 1)
        machineModel = SimMachineModel(
            cpus = List(cpuNode.coreCount) { SimProcessingUnit(cpuNode, it, 3200.0) },
            memory = List(4) { SimMemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }

    /**
     * Test overcommitting of resources via the hypervisor with a single VM.
     */
    @Test
    fun testOvercommittedSingle() {
        val listener = object : SimHypervisor.Listener {
            var totalRequestedWork = 0L
            var totalGrantedWork = 0L
            var totalOvercommittedWork = 0L

            override fun onSliceFinish(
                hypervisor: SimHypervisor,
                requestedWork: Long,
                grantedWork: Long,
                overcommittedWork: Long,
                interferedWork: Long,
                cpuUsage: Double,
                cpuDemand: Double
            ) {
                totalRequestedWork += requestedWork
                totalGrantedWork += grantedWork
                totalOvercommittedWork += overcommittedWork
            }
        }

        scope.launch {
            val duration = 5 * 60L
            val workloadA =
                SimTraceWorkload(
                    sequenceOf(
                        SimTraceWorkload.Fragment(duration * 1000, 28.0, 1),
                        SimTraceWorkload.Fragment(duration * 1000, 3500.0, 1),
                        SimTraceWorkload.Fragment(duration * 1000, 0.0, 1),
                        SimTraceWorkload.Fragment(duration * 1000, 183.0, 1)
                    ),
                )

            val machine = SimBareMetalMachine(scope, clock, machineModel)
            val hypervisor = SimFairShareHypervisor(listener)

            launch {
                machine.run(hypervisor)
            }

            yield()
            launch { hypervisor.createMachine(machineModel).run(workloadA) }
        }

        scope.advanceUntilIdle()
        scope.uncaughtExceptions.forEach { it.printStackTrace() }

        assertAll(
            { assertEquals(emptyList<Throwable>(), scope.uncaughtExceptions, "No errors") },
            { assertEquals(1113300, listener.totalRequestedWork, "Requested Burst does not match") },
            { assertEquals(1023300, listener.totalGrantedWork, "Granted Burst does not match") },
            { assertEquals(90000, listener.totalOvercommittedWork, "Overcommissioned Burst does not match") },
            { assertEquals(1200000, scope.currentTime) }
        )
    }

    /**
     * Test overcommitting of resources via the hypervisor with two VMs.
     */
    @Test
    fun testOvercommittedDual() {
        val listener = object : SimHypervisor.Listener {
            var totalRequestedWork = 0L
            var totalGrantedWork = 0L
            var totalOvercommittedWork = 0L

            override fun onSliceFinish(
                hypervisor: SimHypervisor,
                requestedWork: Long,
                grantedWork: Long,
                overcommittedWork: Long,
                interferedWork: Long,
                cpuUsage: Double,
                cpuDemand: Double
            ) {
                totalRequestedWork += requestedWork
                totalGrantedWork += grantedWork
                totalOvercommittedWork += overcommittedWork
            }
        }

        scope.launch {
            val duration = 5 * 60L
            val workloadA =
                SimTraceWorkload(
                    sequenceOf(
                        SimTraceWorkload.Fragment(duration * 1000, 28.0, 1),
                        SimTraceWorkload.Fragment(duration * 1000, 3500.0, 1),
                        SimTraceWorkload.Fragment(duration * 1000, 0.0, 1),
                        SimTraceWorkload.Fragment(duration * 1000, 183.0, 1)
                    ),
                )
            val workloadB =
                SimTraceWorkload(
                    sequenceOf(
                        SimTraceWorkload.Fragment(duration * 1000, 28.0, 1),
                        SimTraceWorkload.Fragment(duration * 1000, 3100.0, 1),
                        SimTraceWorkload.Fragment(duration * 1000, 0.0, 1),
                        SimTraceWorkload.Fragment(duration * 1000, 73.0, 1)
                    )
                )

            val machine = SimBareMetalMachine(scope, clock, machineModel)
            val hypervisor = SimFairShareHypervisor(listener)

            launch {
                machine.run(hypervisor)
            }

            yield()
            launch { hypervisor.createMachine(machineModel).run(workloadA) }
            launch { hypervisor.createMachine(machineModel).run(workloadB) }
        }

        scope.advanceUntilIdle()
        scope.uncaughtExceptions.forEach { it.printStackTrace() }

        assertAll(
            { assertEquals(emptyList<Throwable>(), scope.uncaughtExceptions, "No errors") },
            { assertEquals(2082000, listener.totalRequestedWork, "Requested Burst does not match") },
            { assertEquals(1062000, listener.totalGrantedWork, "Granted Burst does not match") },
            { assertEquals(1020000, listener.totalOvercommittedWork, "Overcommissioned Burst does not match") },
            { assertEquals(1200000, scope.currentTime) }
        )
    }
}
