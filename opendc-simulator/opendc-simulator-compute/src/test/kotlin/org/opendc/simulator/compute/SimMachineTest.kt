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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.simulator.compute.cpu.CpuPowerModels
import org.opendc.simulator.compute.machine.SimMachine
import org.opendc.simulator.compute.models.CpuModel
import org.opendc.simulator.compute.models.MachineModel
import org.opendc.simulator.compute.models.MemoryUnit
import org.opendc.simulator.compute.workload.TraceWorkload
import org.opendc.simulator.engine.FlowEngine
import org.opendc.simulator.kotlin.runSimulation
import java.util.concurrent.ThreadLocalRandom

/**
 * Test suite for the [SimBareMetalMachine] class.
 */
class SimMachineTest {
    private lateinit var machineModel: MachineModel

    @BeforeEach
    fun setUp() {
        machineModel =
            MachineModel(
                CpuModel(
                    0,
                    2,
                    1000.0f,
                    "Intel",
                    "Xeon",
                    "amd64",
                ),
                MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000 * 4),
            )
    }

//    @Test
//    fun testFlopsWorkload() =
//        runSimulation {
//            val engine = FlowEngine.create(dispatcher)
//            val graph = engine.newGraph()
//
//            val machine =
//                SimBareMetalMachine.create(
//                    graph,
//                    machineModel,
//                )
//
//            machine.runWorkload(SimWorkloads.flops(2_000, 1.0))
//
//            // Two cores execute 1000 MFlOps per second (1000 ms)
//            assertEquals(1000, timeSource.millis())
//        }

    @Test
    fun testTraceWorkload() =
        runSimulation {
            val random = ThreadLocalRandom.current()
            val builder = TraceWorkload.builder()
            repeat(100) {
                builder.add(1000, random.nextDouble(0.0, 4500.0), 1)
            }
            val traceWorkload = builder.build()

            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val simMachine =
                SimMachine(
                    graph,
                    machineModel,
                    CpuPowerModels.constant(0.0),
                ) { cause ->
                }

            val virtualMachine =
                simMachine.startWorkload(traceWorkload) { cause ->
                    assertEquals(100000, timeSource.millis())
                }

            // Two cores execute 1000 MFlOps per second (1000 ms)
        }

//    @Test
//    fun testDualSocketMachine() =
//        runSimulation {
//            val engine = FlowEngine.create(dispatcher)
//            val graph = engine.newGraph()
//
//            val cpuNode = machineModel.cpu
//            val machineModel =
//                MachineModel(
//                    List(cpuNode.coreCount * 2) {
//                        CpuModel(
//                            it,
//                            cpuNode.coreCount,
//                            1000.0,
//                        )
//                    },
//                    MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000 * 4),
//                )
//            val machine =
//                SimBareMetalMachine.create(
//                    graph,
//                    machineModel,
//                    CpuPowerModels.constant(0.0)
//                )
//
//            machine.runWorkload(SimWorkloads.flops(2_000, 1.0))
//
//            // Two sockets with two cores execute 2000 MFlOps per second (500 ms)
//            assertEquals(500, timeSource.millis())
//        }
//
// //    @Test
// //    fun testPower() =
// //        runSimulation {
// //            val engine = FlowEngine.create(dispatcher)
// //            val graph = engine.newGraph()
// //            val machine =
// //                SimBareMetalMachine.create(
// //                    graph,
// //                    machineModel,
// //                    CpuPowerModels.linear(100.0, 50.0),
// //                )
// //            val source = SimPowerSource(graph, 1000.0f)
// //            source.connect(machine.psu)
// //
// //            coroutineScope {
// //                launch { machine.runWorkload(SimWorkloads.flops(2_000, 1.0)) }
// //
// //                yield()
// //                assertAll(
// //                    { assertEquals(100.0, machine.psu.powerDraw) },
// //                    { assertEquals(100.0f, source.powerDraw) },
// //                )
// //            }
// //        }
//
//    @Test
//    fun testCapacityClamp() =
//        runSimulation {
//            val engine = FlowEngine.create(dispatcher)
//            val graph = engine.newGraph()
//
//            val machine =
//                SimBareMetalMachine.create(
//                    graph,
//                    machineModel,
//                    CpuPowerModels.constant(0.0)
//                )
//
//            machine.runWorkload(
//                object : SimWorkload {
//                    override fun onStart(ctx: SimMachineContext) {
//                        val cpu = ctx.cpu
//
//                        cpu.frequency = (cpu.cpuModel.totalCapacity + 1000.0)
//                        assertEquals(cpu.cpuModel.totalCapacity, cpu.frequency)
//                        cpu.frequency = -1.0
//                        assertEquals(0.0, cpu.frequency)
//
//                        ctx.shutdown()
//                    }
//
//                    override fun setOffset(now: Long) {}
//
//                    override fun onStop(ctx: SimMachineContext) {}
//
//                    override fun makeSnapshot(now: Long) {
//                    }
//
//                    override fun getSnapshot(): SimWorkload = this
//
//                    override fun createCheckpointModel() {}
//
//                    override fun getCheckpointInterval(): Long {
//                        return -1
//                    }
//
//                    override fun getCheckpointDuration(): Long {
//                        return -1
//                    }
//
//                    override fun getCheckpointIntervalScaling(): Double {
//                        return -1.0
//                    }
//                },
//            )
//        }
//
//    @Test
//    fun testMemory() =
//        runSimulation {
//            val engine = FlowEngine.create(dispatcher)
//            val graph = engine.newGraph()
//
//            val machine =
//                SimBareMetalMachine.create(
//                    graph,
//                    machineModel,
//                    CpuPowerModels.constant(0.0)
//                )
//
//            machine.runWorkload(
//                object : SimWorkload {
//                    override fun onStart(ctx: SimMachineContext) {
//                        assertEquals(32_000 * 4.0, ctx.memory.capacity)
//                        ctx.shutdown()
//                    }
//
//                    override fun setOffset(now: Long) {}
//
//                    override fun onStop(ctx: SimMachineContext) {}
//
//                    override fun makeSnapshot(now: Long) {}
//
//                    override fun getSnapshot(): SimWorkload = this
//
//                    override fun createCheckpointModel() {}
//
//                    override fun getCheckpointInterval(): Long {
//                        return -1
//                    }
//
//                    override fun getCheckpointDuration(): Long {
//                        return -1
//                    }
//
//                    override fun getCheckpointIntervalScaling(): Double {
//                        return -1.0
//                    }
//                },
//            )
//        }
//
//    @Test
//    fun testMemoryUsage() =
//        runSimulation {
//            val engine = FlowEngine.create(dispatcher)
//            val graph = engine.newGraph()
//
//            val machine =
//                SimBareMetalMachine.create(
//                    graph,
//                    machineModel,
//                    CpuPowerModels.constant(0.0)
//                )
//
//            machine.runWorkload(
//                object : SimWorkload {
//                    override fun onStart(ctx: SimMachineContext) {
//                        val source = SimpleFlowSource(ctx.graph, ctx.memory.capacity.toFloat(), 1.0f) { ctx.shutdown() }
//                        ctx.graph.connect(source.output, ctx.memory.input)
//                    }
//
//                    override fun setOffset(now: Long) {}
//
//                    override fun onStop(ctx: SimMachineContext) {}
//
//                    override fun makeSnapshot(now: Long) {
//                    }
//
//                    override fun getSnapshot(): SimWorkload = this
//
//                    override fun createCheckpointModel() {}
//
//                    override fun getCheckpointInterval(): Long {
//                        return -1
//                    }
//
//                    override fun getCheckpointDuration(): Long {
//                        return -1
//                    }
//
//                    override fun getCheckpointIntervalScaling(): Double {
//                        return -1.0
//                    }
//                },
//            )
//
//            assertEquals(1000, timeSource.millis())
//        }
//
//    @Test
//    fun testCancellation() =
//        runSimulation {
//            val engine = FlowEngine.create(dispatcher)
//            val graph = engine.newGraph()
//
//            val machine =
//                SimBareMetalMachine.create(
//                    graph,
//                    machineModel,
//                    CpuPowerModels.constant(0.0)
//                )
//
//            try {
//                coroutineScope {
//                    launch { machine.runWorkload(SimWorkloads.flops(2_000, 1.0)) }
//                    cancel()
//                }
//            } catch (_: CancellationException) {
//                // Ignore
//            }
//
//            assertEquals(0, timeSource.millis())
//        }
//
//    @Test
//    fun testConcurrentRuns() =
//        runSimulation {
//            val engine = FlowEngine.create(dispatcher)
//            val graph = engine.newGraph()
//
//            val machine =
//                SimBareMetalMachine.create(
//                    graph,
//                    machineModel,
//                    CpuPowerModels.constant(0.0)
//                )
//
//            coroutineScope {
//                launch {
//                    machine.runWorkload(SimWorkloads.flops(2_000, 1.0))
//                }
//
//                assertThrows<IllegalStateException> {
//                    machine.runWorkload(SimWorkloads.flops(2_000, 1.0))
//                }
//            }
//        }
//
//    @Test
//    fun testCatchStartFailure() =
//        runSimulation {
//            val engine = FlowEngine.create(dispatcher)
//            val graph = engine.newGraph()
//
//            val machine =
//                SimBareMetalMachine.create(
//                    graph,
//                    machineModel,
//                    CpuPowerModels.constant(0.0)
//                )
//
//            val workload = mockk<SimWorkload>()
//            every { workload.onStart(any()) } throws IllegalStateException()
//
//            assertThrows<IllegalStateException> { machine.runWorkload(workload) }
//        }
//
//    @Test
//    fun testCatchStopFailure() =
//        runSimulation {
//            val engine = FlowEngine.create(dispatcher)
//            val graph = engine.newGraph()
//
//            val machine =
//                SimBareMetalMachine.create(
//                    graph,
//                    machineModel,
//                    CpuPowerModels.constant(0.0)
//                )
//
//            val workload = mockk<SimWorkload>()
//            every { workload.onStart(any()) } answers { (it.invocation.args[0] as SimMachineContext).shutdown() }
//            every { workload.onStop(any()) } throws IllegalStateException()
//
//            assertThrows<IllegalStateException> { machine.runWorkload(workload) }
//        }
//
//    @Test
//    fun testCatchShutdownFailure() =
//        runSimulation {
//            val engine = FlowEngine.create(dispatcher)
//            val graph = engine.newGraph()
//
//            val machine =
//                SimBareMetalMachine.create(
//                    graph,
//                    machineModel,
//                    CpuPowerModels.constant(0.0)
//                )
//
//            val workload = mockk<SimWorkload>()
//            every { workload.onStart(any()) } answers { (it.invocation.args[0] as SimMachineContext).shutdown(IllegalStateException()) }
//
//            assertThrows<IllegalStateException> { machine.runWorkload(workload) }
//        }
//
//    @Test
//    fun testCatchNestedFailure() =
//        runSimulation {
//            val engine = FlowEngine.create(dispatcher)
//            val graph = engine.newGraph()
//
//            val machine =
//                SimBareMetalMachine.create(
//                    graph,
//                    machineModel,
//                    CpuPowerModels.constant(0.0)
//                )
//
//            val workload = mockk<SimWorkload>()
//            every { workload.onStart(any()) } answers { (it.invocation.args[0] as SimMachineContext).shutdown(IllegalStateException()) }
//            every { workload.onStop(any()) } throws IllegalStateException()
//
//            val exc = assertThrows<IllegalStateException> { machine.runWorkload(workload) }
//            assertEquals(1, exc.cause!!.suppressedExceptions.size)
//        }
}
