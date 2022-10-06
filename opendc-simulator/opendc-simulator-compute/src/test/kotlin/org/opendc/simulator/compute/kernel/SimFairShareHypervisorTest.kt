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
import org.opendc.simulator.compute.kernel.cpufreq.PerformanceScalingGovernor
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.ConstantPowerModel
import org.opendc.simulator.compute.power.SimplePowerDriver
import org.opendc.simulator.compute.runWorkload
import org.opendc.simulator.compute.workload.SimTrace
import org.opendc.simulator.compute.workload.SimTraceFragment
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.flow.FlowEngine
import org.opendc.simulator.flow.mux.FlowMultiplexerFactory
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
            cpus = List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 3200.0) },
            memory = List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }

    /**
     * Test overcommitting of resources via the hypervisor with a single VM.
     */
    @Test
    fun testOvercommittedSingle() = runSimulation {
        val duration = 5 * 60L
        val workloadA =
            SimTraceWorkload(
                SimTrace.ofFragments(
                    SimTraceFragment(0, duration * 1000, 28.0, 1),
                    SimTraceFragment(duration * 1000, duration * 1000, 3500.0, 1),
                    SimTraceFragment(duration * 2000, duration * 1000, 0.0, 1),
                    SimTraceFragment(duration * 3000, duration * 1000, 183.0, 1)
                )
            )

        val engine = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(engine, model, SimplePowerDriver(ConstantPowerModel(0.0)))
        val hypervisor = SimHypervisor(engine, FlowMultiplexerFactory.maxMinMultiplexer(), SplittableRandom(1), PerformanceScalingGovernor())

        launch {
            machine.runWorkload(hypervisor)
            println("Hypervisor finished")
        }
        yield()

        val vm = hypervisor.newMachine(model)
        vm.runWorkload(workloadA)

        yield()
        machine.cancel()

        assertAll(
            { assertEquals(319781, hypervisor.counters.cpuActiveTime, "Active time does not match") },
            { assertEquals(880219, hypervisor.counters.cpuIdleTime, "Idle time does not match") },
            { assertEquals(28125, hypervisor.counters.cpuStealTime, "Steal time does not match") },
            { assertEquals(1200000, clock.millis()) { "Current time is correct" } }
        )
    }

    /**
     * Test overcommitting of resources via the hypervisor with two VMs.
     */
    @Test
    fun testOvercommittedDual() = runSimulation {
        val duration = 5 * 60L
        val workloadA =
            SimTraceWorkload(
                SimTrace.ofFragments(
                    SimTraceFragment(0, duration * 1000, 28.0, 1),
                    SimTraceFragment(duration * 1000, duration * 1000, 3500.0, 1),
                    SimTraceFragment(duration * 2000, duration * 1000, 0.0, 1),
                    SimTraceFragment(duration * 3000, duration * 1000, 183.0, 1)
                )
            )
        val workloadB =
            SimTraceWorkload(
                SimTrace.ofFragments(
                    SimTraceFragment(0, duration * 1000, 28.0, 1),
                    SimTraceFragment(duration * 1000, duration * 1000, 3100.0, 1),
                    SimTraceFragment(duration * 2000, duration * 1000, 0.0, 1),
                    SimTraceFragment(duration * 3000, duration * 1000, 73.0, 1)
                )
            )

        val engine = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(engine, model, SimplePowerDriver(ConstantPowerModel(0.0)))
        val hypervisor = SimHypervisor(engine, FlowMultiplexerFactory.maxMinMultiplexer(), SplittableRandom(1), null)

        launch {
            machine.runWorkload(hypervisor)
        }

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
            { assertEquals(1200000, clock.millis()) }
        )
    }

    @Test
    fun testMultipleCPUs() = runSimulation {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)
        val model = MachineModel(
            cpus = List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 3200.0) },
            memory = List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )

        val engine = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(engine, model, SimplePowerDriver(ConstantPowerModel(0.0)))
        val hypervisor = SimHypervisor(engine, FlowMultiplexerFactory.maxMinMultiplexer(), SplittableRandom(1), null)

        assertDoesNotThrow {
            launch {
                machine.runWorkload(hypervisor)
            }
        }

        machine.cancel()
    }

    @Test
    fun testInterference() = runSimulation {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)
        val model = MachineModel(
            cpus = List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 3200.0) },
            memory = List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )

        val interferenceModel = VmInterferenceModel.builder()
            .addGroup(targetLoad = 0.0, score = 0.9, members = setOf("a", "b"))
            .addGroup(targetLoad = 0.0, score = 0.6, members = setOf("a", "c"))
            .addGroup(targetLoad = 0.1, score = 0.8, members = setOf("a", "n"))
            .build()

        val engine = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(engine, model, SimplePowerDriver(ConstantPowerModel(0.0)))
        val hypervisor = SimHypervisor(engine, FlowMultiplexerFactory.maxMinMultiplexer(), SplittableRandom(1), null)

        val duration = 5 * 60L
        val workloadA =
            SimTraceWorkload(
                SimTrace.ofFragments(
                    SimTraceFragment(0, duration * 1000, 0.0, 1),
                    SimTraceFragment(duration * 1000, duration * 1000, 28.0, 1),
                    SimTraceFragment(duration * 2000, duration * 1000, 3500.0, 1),
                    SimTraceFragment(duration * 3000, duration * 1000, 183.0, 1)
                )
            )
        val workloadB =
            SimTraceWorkload(
                SimTrace.ofFragments(
                    SimTraceFragment(0, duration * 1000, 0.0, 1),
                    SimTraceFragment(duration * 1000, duration * 1000, 28.0, 1),
                    SimTraceFragment(duration * 2000, duration * 1000, 3100.0, 1),
                    SimTraceFragment(duration * 3000, duration * 1000, 73.0, 1)
                )
            )

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
