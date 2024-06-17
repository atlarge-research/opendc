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

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.compute.device.SimNetworkAdapter
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.NetworkAdapter
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.model.StorageDevice
import org.opendc.simulator.compute.power.CpuPowerModels
import org.opendc.simulator.compute.workload.SimTrace
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.compute.workload.SimWorkloads
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.flow2.source.SimpleFlowSource
import org.opendc.simulator.kotlin.runSimulation
import org.opendc.simulator.network.SimNetworkSink
import org.opendc.simulator.power.SimPowerSource
import java.util.concurrent.ThreadLocalRandom

/**
 * Test suite for the [SimBareMetalMachine] class.
 */
class SimMachineTest {
    private lateinit var machineModel: MachineModel

    @BeforeEach
    fun setUp() {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)

        machineModel =
            MachineModel(
                List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 1000.0) },
                List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) },
                listOf(NetworkAdapter("Mellanox", "ConnectX-5", 25000.0)),
                listOf(StorageDevice("Samsung", "EVO", 1000.0, 250.0, 250.0)),
            )
    }

//    @Test
    fun testFlopsWorkload() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            machine.runWorkload(SimWorkloads.flops(2_000, 1.0))

            // Two cores execute 1000 MFlOps per second (1000 ms)
            assertEquals(1000, timeSource.millis())
        }

    @Test
    fun testTraceWorkload() =
        runSimulation {
            val random = ThreadLocalRandom.current()
            val builder = SimTrace.builder()
            repeat(1000000) {
                val timestamp = it.toLong() * 1000
                val deadline = timestamp + 1000
                builder.add(deadline, random.nextDouble(0.0, 4500.0), 1)
            }
            val trace = builder.build()

            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            machine.runWorkload(trace.createWorkload(0))

            // Two cores execute 1000 MFlOps per second (1000 ms)
            assertEquals(1000000000, timeSource.millis())
        }

//    @Test
    fun testDualSocketMachine() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val cpuNode = machineModel.cpus[0].node
            val machineModel =
                MachineModel(
                    List(cpuNode.coreCount * 2) { ProcessingUnit(cpuNode, it % 2, 1000.0) },
                    List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) },
                )
            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            machine.runWorkload(SimWorkloads.flops(2_000, 1.0))

            // Two sockets with two cores execute 2000 MFlOps per second (500 ms)
            assertEquals(500, timeSource.millis())
        }

    @Test
    fun testPower() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()
            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                    SimPsuFactories.simple(CpuPowerModels.linear(100.0, 50.0)),
                )
            val source = SimPowerSource(graph, 1000.0f)
            source.connect(machine.psu)

            coroutineScope {
                launch { machine.runWorkload(SimWorkloads.flops(2_000, 1.0)) }

                yield()
                assertAll(
                    { assertEquals(100.0, machine.psu.powerDraw) },
                    { assertEquals(100.0f, source.powerDraw) },
                )
            }
        }

    @Test
    fun testCapacityClamp() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            machine.runWorkload(
                object : SimWorkload {
                    override fun onStart(ctx: SimMachineContext) {
                        val cpu = ctx.cpus[0]

                        cpu.frequency = (cpu.model.frequency + 1000.0)
                        assertEquals(cpu.model.frequency, cpu.frequency)
                        cpu.frequency = -1.0
                        assertEquals(0.0, cpu.frequency)

                        ctx.shutdown()
                    }

                    override fun setOffset(now: Long) {}

                    override fun onStop(ctx: SimMachineContext) {}

                    override fun snapshot(): SimWorkload = TODO()
                },
            )
        }

    @Test
    fun testMemory() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            machine.runWorkload(
                object : SimWorkload {
                    override fun onStart(ctx: SimMachineContext) {
                        assertEquals(32_000 * 4.0, ctx.memory.capacity)
                        ctx.shutdown()
                    }

                    override fun setOffset(now: Long) {}

                    override fun onStop(ctx: SimMachineContext) {}

                    override fun snapshot(): SimWorkload = TODO()
                },
            )
        }

    @Test
    fun testMemoryUsage() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            machine.runWorkload(
                object : SimWorkload {
                    override fun onStart(ctx: SimMachineContext) {
                        val source = SimpleFlowSource(ctx.graph, ctx.memory.capacity.toFloat(), 1.0f) { ctx.shutdown() }
                        ctx.graph.connect(source.output, ctx.memory.input)
                    }

                    override fun setOffset(now: Long) {}

                    override fun onStop(ctx: SimMachineContext) {}

                    override fun snapshot(): SimWorkload = TODO()
                },
            )

            assertEquals(1000, timeSource.millis())
        }

    @Test
    fun testNetUsage() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            val adapter = (machine.peripherals[0] as SimNetworkAdapter)
            adapter.connect(SimNetworkSink(graph, adapter.bandwidth.toFloat()))

            machine.runWorkload(
                object : SimWorkload {
                    override fun onStart(ctx: SimMachineContext) {
                        val iface = ctx.networkInterfaces[0]
                        val source =
                            SimpleFlowSource(ctx.graph, 800.0f, 0.8f) {
                                ctx.shutdown()
                                it.close()
                            }
                        ctx.graph.connect(source.output, iface.tx)
                    }

                    override fun setOffset(now: Long) {}

                    override fun onStop(ctx: SimMachineContext) {}

                    override fun snapshot(): SimWorkload = TODO()
                },
            )

            assertEquals(40, timeSource.millis())
        }

    @Test
    fun testDiskReadUsage() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            machine.runWorkload(
                object : SimWorkload {
                    override fun onStart(ctx: SimMachineContext) {
                        val disk = ctx.storageInterfaces[0]
                        val source = SimpleFlowSource(ctx.graph, 800.0f, 0.8f) { ctx.shutdown() }
                        ctx.graph.connect(source.output, disk.read)
                    }

                    override fun setOffset(now: Long) {}

                    override fun onStop(ctx: SimMachineContext) {}

                    override fun snapshot(): SimWorkload = TODO()
                },
            )

            assertEquals(4000, timeSource.millis())
        }

    @Test
    fun testDiskWriteUsage() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            machine.runWorkload(
                object : SimWorkload {
                    override fun onStart(ctx: SimMachineContext) {
                        val disk = ctx.storageInterfaces[0]
                        val source = SimpleFlowSource(ctx.graph, 800.0f, 0.8f) { ctx.shutdown() }
                        ctx.graph.connect(source.output, disk.write)
                    }

                    override fun setOffset(now: Long) {}

                    override fun onStop(ctx: SimMachineContext) {}

                    override fun snapshot(): SimWorkload = TODO()
                },
            )

            assertEquals(4000, timeSource.millis())
        }

    @Test
    fun testCancellation() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            try {
                coroutineScope {
                    launch { machine.runWorkload(SimWorkloads.flops(2_000, 1.0)) }
                    cancel()
                }
            } catch (_: CancellationException) {
                // Ignore
            }

            assertEquals(0, timeSource.millis())
        }

    @Test
    fun testConcurrentRuns() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            coroutineScope {
                launch {
                    machine.runWorkload(SimWorkloads.flops(2_000, 1.0))
                }

                assertThrows<IllegalStateException> {
                    machine.runWorkload(SimWorkloads.flops(2_000, 1.0))
                }
            }
        }

    @Test
    fun testCatchStartFailure() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            val workload = mockk<SimWorkload>()
            every { workload.onStart(any()) } throws IllegalStateException()

            assertThrows<IllegalStateException> { machine.runWorkload(workload) }
        }

    @Test
    fun testCatchStopFailure() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            val workload = mockk<SimWorkload>()
            every { workload.onStart(any()) } answers { (it.invocation.args[0] as SimMachineContext).shutdown() }
            every { workload.onStop(any()) } throws IllegalStateException()

            assertThrows<IllegalStateException> { machine.runWorkload(workload) }
        }

    @Test
    fun testCatchShutdownFailure() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            val workload = mockk<SimWorkload>()
            every { workload.onStart(any()) } answers { (it.invocation.args[0] as SimMachineContext).shutdown(IllegalStateException()) }

            assertThrows<IllegalStateException> { machine.runWorkload(workload) }
        }

    @Test
    fun testCatchNestedFailure() =
        runSimulation {
            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine =
                SimBareMetalMachine.create(
                    graph,
                    machineModel,
                )

            val workload = mockk<SimWorkload>()
            every { workload.onStart(any()) } answers { (it.invocation.args[0] as SimMachineContext).shutdown(IllegalStateException()) }
            every { workload.onStop(any()) } throws IllegalStateException()

            val exc = assertThrows<IllegalStateException> { machine.runWorkload(workload) }
            assertEquals(1, exc.cause!!.suppressedExceptions.size)
        }
}
