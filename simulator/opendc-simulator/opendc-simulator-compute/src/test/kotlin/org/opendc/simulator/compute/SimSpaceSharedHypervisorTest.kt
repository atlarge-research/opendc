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

package org.opendc.simulator.compute

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.compute.model.SimMemoryUnit
import org.opendc.simulator.compute.model.SimProcessingNode
import org.opendc.simulator.compute.model.SimProcessingUnit
import org.opendc.simulator.compute.workload.SimFlopsWorkload
import org.opendc.simulator.compute.workload.SimRuntimeWorkload
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.utils.DelayControllerClockAdapter
import java.time.Clock

/**
 * A test suite for the [SimSpaceSharedHypervisor].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SimSpaceSharedHypervisorTest {
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
     * Test a trace workload.
     */
    @Test
    fun testTrace() {
        val usagePm = mutableListOf<Double>()
        val usageVm = mutableListOf<Double>()

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
            val hypervisor = SimSpaceSharedHypervisor()

            launch { machine.usage.toList(usagePm) }
            launch { machine.run(hypervisor) }

            yield()
            launch {
                val vm = hypervisor.createMachine(machineModel)
                launch { vm.usage.toList(usageVm) }
                vm.run(workloadA)
            }
        }

        scope.advanceUntilIdle()

        assertAll(
            { assertEquals(listOf(0.0, 0.00875, 1.0, 0.0, 0.0571875, 0.0), usagePm) { "Correct PM usage" } },
            { assertEquals(listOf(0.0, 0.00875, 1.0, 0.0, 0.0571875, 0.0), usageVm) { "Correct VM usage" } },
            { assertEquals(5 * 60L * 4000, scope.currentTime) { "Took enough time" } }
        )
    }

    /**
     * Test runtime workload on hypervisor.
     */
    @Test
    fun testRuntimeWorkload() {
        val duration = 5 * 60L * 1000
        val workload = SimRuntimeWorkload(duration)
        val machine = SimBareMetalMachine(scope, clock, machineModel)
        val hypervisor = SimSpaceSharedHypervisor()

        scope.launch {
            launch { machine.run(hypervisor) }

            yield()
            launch { hypervisor.createMachine(machineModel).run(workload) }
        }

        scope.advanceUntilIdle()

        assertEquals(duration, scope.currentTime) { "Took enough time" }
    }

    /**
     * Test FLOPs workload on hypervisor.
     */
    @Test
    fun testFlopsWorkload() {
        val duration = 5 * 60L * 1000
        val workload = SimFlopsWorkload((duration * 3.2).toLong(), 1.0)
        val machine = SimBareMetalMachine(scope, clock, machineModel)
        val hypervisor = SimSpaceSharedHypervisor()

        scope.launch {
            launch { machine.run(hypervisor) }

            yield()
            launch { hypervisor.createMachine(machineModel).run(workload) }
        }

        scope.advanceUntilIdle()

        assertEquals(duration, scope.currentTime) { "Took enough time" }
    }

    /**
     * Test two workloads running sequentially.
     */
    @Test
    fun testTwoWorkloads() {
        val duration = 5 * 60L * 1000
        val machine = SimBareMetalMachine(scope, clock, machineModel)
        val hypervisor = SimSpaceSharedHypervisor()

        scope.launch {
            launch { machine.run(hypervisor) }

            yield()
            launch {
                val vm = hypervisor.createMachine(machineModel)
                vm.run(SimRuntimeWorkload(duration))
                vm.close()

                val vm2 = hypervisor.createMachine(machineModel)
                vm2.run(SimRuntimeWorkload(duration))
            }
        }

        scope.advanceUntilIdle()

        assertEquals(duration * 2, scope.currentTime) { "Took enough time" }
    }

    /**
     * Test concurrent workloads on the machine.
     */
    @Test
    fun testConcurrentWorkloadFails() {
        val machine = SimBareMetalMachine(scope, clock, machineModel)
        val hypervisor = SimSpaceSharedHypervisor()

        scope.launch {
            launch { machine.run(hypervisor) }

            yield()

            hypervisor.createMachine(machineModel)

            assertAll(
                { assertFalse(hypervisor.canFit(machineModel)) },
                { assertThrows<IllegalStateException> { hypervisor.createMachine(machineModel) } }
            )
        }

        scope.advanceUntilIdle()
    }

    /**
     * Test concurrent workloads on the machine.
     */
    @Test
    fun testConcurrentWorkloadSucceeds() {
        val machine = SimBareMetalMachine(scope, clock, machineModel)
        val hypervisor = SimSpaceSharedHypervisor()

        scope.launch {
            launch { machine.run(hypervisor) }

            yield()

            hypervisor.createMachine(machineModel).close()

            assertAll(
                { assertTrue(hypervisor.canFit(machineModel)) },
                { assertDoesNotThrow { hypervisor.createMachine(machineModel) } }
            )
        }

        scope.advanceUntilIdle()
    }
}
