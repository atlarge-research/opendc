/*
 * Copyright (c) 2022 AtLarge Research
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

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.SimMachineContext
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.runWorkload
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.kotlin.runSimulation

/**
 * Test suite for the [SimChainWorkload] class.
 */
class SimChainWorkloadTest {
    private lateinit var machineModel: MachineModel

    @BeforeEach
    fun setUp() {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)

        machineModel = MachineModel(
            /*cpus*/ List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 1000.0) },
            /*memory*/ List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }

    @Test
    fun testMultipleWorkloads() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(
            graph,
            machineModel
        )

        val workload =
            SimWorkloads.chain(
                SimWorkloads.runtime(1000, 1.0),
                SimWorkloads.runtime(1000, 1.0)
            )

        machine.runWorkload(workload)

        assertEquals(2000, timeSource.millis())
    }

    @Test
    fun testStartFailure() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(
            graph,
            machineModel
        )

        val workloadA = mockk<SimWorkload>()
        every { workloadA.onStart(any()) } throws IllegalStateException("Staged")
        every { workloadA.onStop(any()) } returns Unit

        val workload =
            SimWorkloads.chain(
                workloadA,
                SimWorkloads.runtime(1000, 1.0)
            )

        assertThrows<IllegalStateException> { machine.runWorkload(workload) }

        assertEquals(0, timeSource.millis())
    }

    @Test
    fun testStartFailureSecond() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(
            graph,
            machineModel
        )

        val workloadA = mockk<SimWorkload>()
        every { workloadA.onStart(any()) } throws IllegalStateException("Staged")
        every { workloadA.onStop(any()) } returns Unit

        val workload =
            SimWorkloads.chain(
                SimWorkloads.runtime(1000, 1.0),
                workloadA,
                SimWorkloads.runtime(1000, 1.0)
            )

        assertThrows<IllegalStateException> { machine.runWorkload(workload) }

        assertEquals(1000, timeSource.millis())
    }

    @Test
    fun testStopFailure() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(
            graph,
            machineModel
        )

        val workloadA = spyk<SimWorkload>(SimRuntimeWorkload(1000, 1.0))
        every { workloadA.onStop(any()) } throws IllegalStateException("Staged")

        val workload =
            SimWorkloads.chain(
                workloadA,
                SimWorkloads.runtime(1000, 1.0)
            )

        assertThrows<IllegalStateException> { machine.runWorkload(workload) }

        assertEquals(1000, timeSource.millis())
    }

    @Test
    fun testStopFailureSecond() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(
            graph,
            machineModel
        )

        val workloadA = spyk<SimWorkload>(SimRuntimeWorkload(1000, 1.0))
        every { workloadA.onStop(any()) } throws IllegalStateException("Staged")

        val workload =
            SimWorkloads.chain(
                SimWorkloads.runtime(1000, 1.0),
                workloadA,
                SimWorkloads.runtime(1000, 1.0)
            )

        assertThrows<IllegalStateException> { machine.runWorkload(workload) }

        assertEquals(2000, timeSource.millis())
    }

    @Test
    fun testStartAndStopFailure() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(
            graph,
            machineModel
        )

        val workloadA = mockk<SimWorkload>()
        every { workloadA.onStart(any()) } throws IllegalStateException()
        every { workloadA.onStop(any()) } throws IllegalStateException()

        val workload =
            SimWorkloads.chain(
                SimRuntimeWorkload(1000, 1.0),
                workloadA
            )

        val exc = assertThrows<IllegalStateException> { machine.runWorkload(workload) }

        assertEquals(2, exc.cause!!.suppressedExceptions.size)
        assertEquals(1000, timeSource.millis())
    }

    @Test
    fun testShutdownAndStopFailure() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(
            graph,
            machineModel
        )

        val workloadA = mockk<SimWorkload>()
        every { workloadA.onStart(any()) } answers { (it.invocation.args[0] as SimMachineContext).shutdown(IllegalStateException()) }
        every { workloadA.onStop(any()) } throws IllegalStateException()

        val workload =
            SimWorkloads.chain(
                SimRuntimeWorkload(1000, 1.0),
                workloadA
            )

        val exc = assertThrows<IllegalStateException> { machine.runWorkload(workload) }

        assertEquals(1, exc.cause!!.suppressedExceptions.size)
        assertEquals(1000, timeSource.millis())
    }

    @Test
    fun testShutdownAndStartFailure() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(
            graph,
            machineModel
        )

        val workloadA = mockk<SimWorkload>(relaxUnitFun = true)
        every { workloadA.onStart(any()) } answers { (it.invocation.args[0] as SimMachineContext).shutdown(IllegalStateException()) }

        val workloadB = mockk<SimWorkload>(relaxUnitFun = true)
        every { workloadB.onStart(any()) } throws IllegalStateException()

        val workload =
            SimWorkloads.chain(
                SimRuntimeWorkload(1000, 1.0),
                workloadA,
                workloadB
            )

        val exc = assertThrows<IllegalStateException> { machine.runWorkload(workload) }
        assertEquals(1, exc.cause!!.suppressedExceptions.size)
        assertEquals(1000, timeSource.millis())
    }

    @Test
    fun testSnapshot() = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()

        val machine = SimBareMetalMachine.create(graph, machineModel)
        val workload =
            SimWorkloads.chain(
                SimWorkloads.runtime(1000, 1.0),
                SimWorkloads.runtime(1000, 1.0)
            )

        val job = launch { machine.runWorkload(workload) }
        delay(500L)
        val snapshot = workload.snapshot()

        job.join()

        assertEquals(2000, timeSource.millis())

        machine.runWorkload(snapshot)

        assertEquals(3500, timeSource.millis())
    }
}
