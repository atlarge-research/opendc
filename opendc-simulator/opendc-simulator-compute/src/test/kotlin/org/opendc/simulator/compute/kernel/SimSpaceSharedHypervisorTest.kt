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

package org.opendc.simulator.compute.kernel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.ConstantPowerModel
import org.opendc.simulator.compute.power.SimplePowerDriver
import org.opendc.simulator.compute.workload.SimFlopsWorkload
import org.opendc.simulator.compute.workload.SimRuntimeWorkload
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.flow.FlowEngine

/**
 * A test suite for the [SimSpaceSharedHypervisor].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SimSpaceSharedHypervisorTest {
    private lateinit var machineModel: MachineModel

    @BeforeEach
    fun setUp() {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 1)
        machineModel = MachineModel(
            cpus = List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 3200.0) },
            memory = List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }

    /**
     * Test a trace workload.
     */
    @Test
    fun testTrace() = runBlockingSimulation {
        val duration = 5 * 60L
        val workloadA =
            SimTraceWorkload(
                sequenceOf(
                    SimTraceWorkload.Fragment(0, duration * 1000, 28.0, 1),
                    SimTraceWorkload.Fragment(duration * 1000, duration * 1000, 3500.0, 1),
                    SimTraceWorkload.Fragment(duration * 2000, duration * 1000, 0.0, 1),
                    SimTraceWorkload.Fragment(duration * 3000, duration * 1000, 183.0, 1)
                ),
            )

        val engine = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(engine, machineModel, SimplePowerDriver(ConstantPowerModel(0.0)))
        val hypervisor = SimSpaceSharedHypervisor(engine, null, null)

        launch { machine.run(hypervisor) }
        val vm = hypervisor.createMachine(machineModel)
        vm.run(workloadA)
        yield()

        vm.close()
        machine.close()

        assertEquals(5 * 60L * 4000, clock.millis()) { "Took enough time" }
    }

    /**
     * Test runtime workload on hypervisor.
     */
    @Test
    fun testRuntimeWorkload() = runBlockingSimulation {
        val duration = 5 * 60L * 1000
        val workload = SimRuntimeWorkload(duration)
        val engine = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(engine, machineModel, SimplePowerDriver(ConstantPowerModel(0.0)))
        val hypervisor = SimSpaceSharedHypervisor(engine, null, null)

        launch { machine.run(hypervisor) }
        yield()
        val vm = hypervisor.createMachine(machineModel)
        vm.run(workload)
        vm.close()
        machine.close()

        assertEquals(duration, clock.millis()) { "Took enough time" }
    }

    /**
     * Test FLOPs workload on hypervisor.
     */
    @Test
    fun testFlopsWorkload() = runBlockingSimulation {
        val duration = 5 * 60L * 1000
        val workload = SimFlopsWorkload((duration * 3.2).toLong(), 1.0)
        val engine = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(
            engine, machineModel, SimplePowerDriver(ConstantPowerModel(0.0))
        )
        val hypervisor = SimSpaceSharedHypervisor(engine, null, null)

        launch { machine.run(hypervisor) }
        yield()
        val vm = hypervisor.createMachine(machineModel)
        vm.run(workload)
        machine.close()

        assertEquals(duration, clock.millis()) { "Took enough time" }
    }

    /**
     * Test two workloads running sequentially.
     */
    @Test
    fun testTwoWorkloads() = runBlockingSimulation {
        val duration = 5 * 60L * 1000
        val engine = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(
            engine, machineModel, SimplePowerDriver(ConstantPowerModel(0.0))
        )
        val hypervisor = SimSpaceSharedHypervisor(engine, null, null)

        launch { machine.run(hypervisor) }
        yield()

        val vm = hypervisor.createMachine(machineModel)
        vm.run(SimRuntimeWorkload(duration))
        vm.close()

        yield()

        val vm2 = hypervisor.createMachine(machineModel)
        vm2.run(SimRuntimeWorkload(duration))
        vm2.close()
        machine.close()

        assertEquals(duration * 2, clock.millis()) { "Took enough time" }
    }

    /**
     * Test concurrent workloads on the machine.
     */
    @Test
    fun testConcurrentWorkloadFails() = runBlockingSimulation {
        val engine = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(engine, machineModel, SimplePowerDriver(ConstantPowerModel(0.0)))
        val hypervisor = SimSpaceSharedHypervisor(engine, null, null)

        launch { machine.run(hypervisor) }
        yield()

        hypervisor.createMachine(machineModel)

        assertAll(
            { assertFalse(hypervisor.canFit(machineModel)) },
            { assertThrows<IllegalArgumentException> { hypervisor.createMachine(machineModel) } }
        )

        machine.close()
    }

    /**
     * Test concurrent workloads on the machine.
     */
    @Test
    fun testConcurrentWorkloadSucceeds() = runBlockingSimulation {
        val interpreter = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(
            interpreter, machineModel, SimplePowerDriver(ConstantPowerModel(0.0))
        )
        val hypervisor = SimSpaceSharedHypervisor(interpreter, null, null)

        launch { machine.run(hypervisor) }
        yield()

        hypervisor.createMachine(machineModel).close()

        assertAll(
            { assertTrue(hypervisor.canFit(machineModel)) },
            { assertDoesNotThrow { hypervisor.createMachine(machineModel) } }
        )

        machine.close()
    }
}
