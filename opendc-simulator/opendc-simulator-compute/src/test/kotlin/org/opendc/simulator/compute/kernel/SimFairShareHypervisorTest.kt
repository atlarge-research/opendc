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

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.kernel.cpufreq.ScalingGovernors
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.runWorkload
import org.opendc.simulator.compute.workload.SimTrace
import org.opendc.simulator.compute.workload.SimTraceFragment
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.flow2.mux.FlowMultiplexerFactory
import org.opendc.simulator.kotlin.runSimulation
import java.util.SplittableRandom

/**
 * Test suite for the [SimHypervisor] class.
 */
internal class SimFairShareHypervisorTest {
    private lateinit var model: MachineModel

    @BeforeEach
    fun setUp() {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 1)
        model = MachineModel(
            /*cpus*/ List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 3200.0) },
            /*memory*/ List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }

    /**
     * Test overcommitting of resources via the hypervisor with a single VM.
     */
    @Test
    fun testOvercommittedSingle() = runSimulation {
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

        val machine = SimBareMetalMachine.create(graph, model)
        val hypervisor = SimHypervisor.create(FlowMultiplexerFactory.maxMinMultiplexer(), SplittableRandom(0L), ScalingGovernors.performance())

        launch { machine.runWorkload(hypervisor) }
        yield()

        val vm = hypervisor.newMachine(model)
        vm.runWorkload(workloadA)

        yield()
        machine.cancel()

        assertAll(
            { assertEquals(319781, hypervisor.counters.cpuActiveTime, "Active time does not match") },
            { assertEquals(880219, hypervisor.counters.cpuIdleTime, "Idle time does not match") },
            { assertEquals(28125, hypervisor.counters.cpuStealTime, "Steal time does not match") },
            { assertEquals(1200000, timeSource.millis()) { "Current time is correct" } }
        )
    }

    /**
     * Test overcommitting of resources via the hypervisor with two VMs.
     */
    @Test
    fun testOvercommittedDual() = runSimulation {
        val duration = 5 * 60L
        val workloadA =
            SimTrace.ofFragments(
                SimTraceFragment(0, duration * 1000, 28.0, 1),
                SimTraceFragment(duration * 1000, duration * 1000, 3500.0, 1),
                SimTraceFragment(duration * 2000, duration * 1000, 0.0, 1),
                SimTraceFragment(duration * 3000, duration * 1000, 183.0, 1)
            ).createWorkload(0)
        val workloadB =
            SimTrace.ofFragments(
                SimTraceFragment(0, duration * 1000, 28.0, 1),
                SimTraceFragment(duration * 1000, duration * 1000, 3100.0, 1),
                SimTraceFragment(duration * 2000, duration * 1000, 0.0, 1),
                SimTraceFragment(duration * 3000, duration * 1000, 73.0, 1)
            ).createWorkload(0)

        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(graph, model)
        val hypervisor = SimHypervisor.create(FlowMultiplexerFactory.maxMinMultiplexer(), SplittableRandom(0L), ScalingGovernors.performance())

        launch { machine.runWorkload(hypervisor) }

        yield()
        coroutineScope {
            launch {
                val vm = hypervisor.newMachine(model)
                vm.runWorkload(workloadA)
                hypervisor.removeMachine(vm)
            }
            val vm = hypervisor.newMachine(model)
            vm.runWorkload(workloadB)
            hypervisor.removeMachine(vm)
        }
        yield()
        machine.cancel()
        yield()

        assertAll(
            { assertEquals(329250, hypervisor.counters.cpuActiveTime, "Active time does not match") },
            { assertEquals(870750, hypervisor.counters.cpuIdleTime, "Idle time does not match") },
            { assertEquals(318750, hypervisor.counters.cpuStealTime, "Steal time does not match") },
            { assertEquals(1200000, timeSource.millis()) }
        )
    }

    @Test
    fun testMultipleCPUs() = runSimulation {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)
        val model = MachineModel(
            /*cpus*/ List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 3200.0) },
            /*memory*/ List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )

        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(graph, model)
        val hypervisor = SimHypervisor.create(FlowMultiplexerFactory.maxMinMultiplexer(), SplittableRandom(0L), ScalingGovernors.performance())

        assertDoesNotThrow {
            launch { machine.runWorkload(hypervisor) }
        }

        machine.cancel()
    }

    @Test
    fun testInterference() = runSimulation {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)
        val model = MachineModel(
            /*cpus*/ List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 3200.0) },
            /*memory*/ List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )

        val interferenceModel = VmInterferenceModel.builder()
            .addGroup(setOf("a", "b"), 0.0, 0.9)
            .addGroup(setOf("a", "c"), 0.0, 0.6)
            .addGroup(setOf("a", "n"), 0.1, 0.8)
            .build()

        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(graph, model)
        val hypervisor = SimHypervisor.create(FlowMultiplexerFactory.maxMinMultiplexer(), SplittableRandom(0L))

        val duration = 5 * 60L
        val workloadA =
            SimTrace.ofFragments(
                SimTraceFragment(0, duration * 1000, 0.0, 1),
                SimTraceFragment(duration * 1000, duration * 1000, 28.0, 1),
                SimTraceFragment(duration * 2000, duration * 1000, 3500.0, 1),
                SimTraceFragment(duration * 3000, duration * 1000, 183.0, 1)
            ).createWorkload(0)
        val workloadB =
            SimTrace.ofFragments(
                SimTraceFragment(0, duration * 1000, 0.0, 1),
                SimTraceFragment(duration * 1000, duration * 1000, 28.0, 1),
                SimTraceFragment(duration * 2000, duration * 1000, 3100.0, 1),
                SimTraceFragment(duration * 3000, duration * 1000, 73.0, 1)
            ).createWorkload(0)

        launch {
            machine.runWorkload(hypervisor)
        }

        coroutineScope {
            launch {
                val vm = hypervisor.newMachine(model)
                vm.runWorkload(workloadA, meta = mapOf("interference-model" to interferenceModel.getProfile("a")!!))
                hypervisor.removeMachine(vm)
            }
            val vm = hypervisor.newMachine(model)
            vm.runWorkload(workloadB, meta = mapOf("interference-model" to interferenceModel.getProfile("b")!!))
            hypervisor.removeMachine(vm)
        }

        machine.cancel()
    }
}
