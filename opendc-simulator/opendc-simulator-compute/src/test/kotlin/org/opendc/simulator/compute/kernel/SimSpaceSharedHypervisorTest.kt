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

import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.runWorkload
import org.opendc.simulator.compute.workload.SimTrace
import org.opendc.simulator.compute.workload.SimTraceFragment
import org.opendc.simulator.compute.workload.SimWorkloads
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.flow2.mux.FlowMultiplexerFactory
import org.opendc.simulator.kotlin.runSimulation
import java.util.SplittableRandom

/**
 * A test suite for a space-shared [SimHypervisor].
 */
internal class SimSpaceSharedHypervisorTest {
    private lateinit var machineModel: MachineModel

    @BeforeEach
    fun setUp() {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 1)
        machineModel = MachineModel(
            /*cpus*/ List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 3200.0) },
            /*memory*/ List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }

    /**
     * Test a trace workload.
     */
    @Test
    fun testTrace() = runSimulation {
        val duration = 5 * 60L
        val workloadA =
            SimTrace.ofFragments(
                SimTraceFragment(0, duration * 1000, 28.0, 1),
                SimTraceFragment(duration * 1000, duration * 1000, 3500.0, 1),
                SimTraceFragment(duration * 2000, duration * 1000, 0.0, 1),
                SimTraceFragment(duration * 3000, duration * 1000, 183.0, 1)
            ).createWorkload(0)

        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(graph, machineModel)
        val hypervisor = SimHypervisor.create(FlowMultiplexerFactory.forwardingMultiplexer(), SplittableRandom(0L))

        launch { machine.runWorkload(hypervisor) }
        val vm = hypervisor.newMachine(machineModel)
        vm.runWorkload(workloadA)
        yield()

        hypervisor.removeMachine(vm)
        machine.cancel()

        assertEquals(5 * 60L * 4000, timeSource.millis()) { "Took enough time" }
    }

    /**
     * Test runtime workload on hypervisor.
     */
    @Test
    fun testRuntimeWorkload() = runSimulation {
        val duration = 5 * 60L * 1000
        val workload = SimWorkloads.runtime(duration, 1.0)
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(graph, machineModel)
        val hypervisor = SimHypervisor.create(FlowMultiplexerFactory.forwardingMultiplexer(), SplittableRandom(0L))

        launch { machine.runWorkload(hypervisor) }
        yield()
        val vm = hypervisor.newMachine(machineModel)
        vm.runWorkload(workload)
        hypervisor.removeMachine(vm)

        machine.cancel()

        assertEquals(duration, timeSource.millis()) { "Took enough time" }
    }

    /**
     * Test FLOPs workload on hypervisor.
     */
    @Test
    fun testFlopsWorkload() = runSimulation {
        val duration = 5 * 60L * 1000
        val workload = SimWorkloads.flops((duration * 3.2).toLong(), 1.0)
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(graph, machineModel)
        val hypervisor = SimHypervisor.create(FlowMultiplexerFactory.forwardingMultiplexer(), SplittableRandom(0L))

        launch { machine.runWorkload(hypervisor) }
        yield()
        val vm = hypervisor.newMachine(machineModel)
        vm.runWorkload(workload)
        machine.cancel()

        assertEquals(duration, timeSource.millis()) { "Took enough time" }
    }

    /**
     * Test two workloads running sequentially.
     */
    @Test
    fun testTwoWorkloads() = runSimulation {
        val duration = 5 * 60L * 1000
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(graph, machineModel)
        val hypervisor = SimHypervisor.create(FlowMultiplexerFactory.forwardingMultiplexer(), SplittableRandom(0L))

        launch { machine.runWorkload(hypervisor) }
        yield()

        val vm = hypervisor.newMachine(machineModel)
        vm.runWorkload(SimWorkloads.runtime(duration, 1.0))
        hypervisor.removeMachine(vm)

        yield()

        val vm2 = hypervisor.newMachine(machineModel)
        vm2.runWorkload(SimWorkloads.runtime(duration, 1.0))
        hypervisor.removeMachine(vm2)

        machine.cancel()

        assertEquals(duration * 2, timeSource.millis()) { "Took enough time" }
    }

    /**
     * Test concurrent workloads on the machine.
     */
    @Test
    fun testConcurrentWorkloadFails() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(graph, machineModel)
        val hypervisor = SimHypervisor.create(FlowMultiplexerFactory.forwardingMultiplexer(), SplittableRandom(0L))

        launch { machine.runWorkload(hypervisor) }
        yield()

        val vm = hypervisor.newMachine(machineModel)
        launch { vm.runWorkload(SimWorkloads.runtime(10_000, 1.0)) }
        yield()

        assertAll(
            { assertFalse(hypervisor.canFit(machineModel)) },
            { assertThrows<IllegalArgumentException> { hypervisor.newMachine(machineModel) } }
        )

        machine.cancel()
        vm.cancel()
    }

    /**
     * Test concurrent workloads on the machine.
     */
    @Test
    fun testConcurrentWorkloadSucceeds() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(graph, machineModel)
        val hypervisor = SimHypervisor.create(FlowMultiplexerFactory.forwardingMultiplexer(), SplittableRandom(0L))

        launch { machine.runWorkload(hypervisor) }
        yield()

        hypervisor.removeMachine(hypervisor.newMachine(machineModel))

        assertAll(
            { assertTrue(hypervisor.canFit(machineModel)) },
            { assertDoesNotThrow { hypervisor.newMachine(machineModel) } }
        )

        machine.cancel()
    }
}
