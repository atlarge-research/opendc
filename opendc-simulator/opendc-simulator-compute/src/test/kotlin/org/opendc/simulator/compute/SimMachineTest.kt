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

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.opendc.simulator.compute.device.SimNetworkAdapter
import org.opendc.simulator.compute.model.*
import org.opendc.simulator.compute.power.ConstantPowerModel
import org.opendc.simulator.compute.power.LinearPowerModel
import org.opendc.simulator.compute.power.SimplePowerDriver
import org.opendc.simulator.compute.workload.SimFlopsWorkload
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.compute.workload.SimWorkloadLifecycle
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.flow.FlowEngine
import org.opendc.simulator.flow.source.FixedFlowSource
import org.opendc.simulator.network.SimNetworkSink
import org.opendc.simulator.power.SimPowerSource

/**
 * Test suite for the [SimBareMetalMachine] class.
 */
class SimMachineTest {
    private lateinit var machineModel: MachineModel

    @BeforeEach
    fun setUp() {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)

        machineModel = MachineModel(
            cpus = List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 1000.0) },
            memory = List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) },
            net = listOf(NetworkAdapter("Mellanox", "ConnectX-5", 25000.0)),
            storage = listOf(StorageDevice("Samsung", "EVO", 1000.0, 250.0, 250.0))
        )
    }

    @Test
    fun testFlopsWorkload() = runBlockingSimulation {
        val machine = SimBareMetalMachine(
            FlowEngine(coroutineContext, clock),
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        try {
            machine.run(SimFlopsWorkload(2_000, utilization = 1.0))

            // Two cores execute 1000 MFlOps per second (1000 ms)
            assertEquals(1000, clock.millis())
        } finally {
            machine.close()
        }
    }

    @Test
    fun testDualSocketMachine() = runBlockingSimulation {
        val cpuNode = machineModel.cpus[0].node
        val machineModel = MachineModel(
            cpus = List(cpuNode.coreCount * 2) { ProcessingUnit(cpuNode, it % 2, 1000.0) },
            memory = List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
        val machine = SimBareMetalMachine(
            FlowEngine(coroutineContext, clock),
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        try {
            machine.run(SimFlopsWorkload(2_000, utilization = 1.0))

            // Two sockets with two cores execute 2000 MFlOps per second (500 ms)
            assertEquals(500, clock.millis())
        } finally {
            machine.close()
        }
    }

    @Test
    fun testPower() = runBlockingSimulation {
        val engine = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(
            engine,
            machineModel,
            SimplePowerDriver(LinearPowerModel(100.0, 50.0))
        )
        val source = SimPowerSource(engine, capacity = 1000.0)
        source.connect(machine.psu)

        try {
            coroutineScope {
                launch { machine.run(SimFlopsWorkload(2_000, utilization = 1.0)) }
                assertAll(
                    { assertEquals(100.0, machine.psu.powerDraw) },
                    { assertEquals(100.0, source.powerDraw) }
                )
            }
        } finally {
            machine.close()
        }
    }

    @Test
    fun testCapacityClamp() = runBlockingSimulation {
        val machine = SimBareMetalMachine(
            FlowEngine(coroutineContext, clock),
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        try {
            machine.run(object : SimWorkload {
                override fun onStart(ctx: SimMachineContext) {
                    val cpu = ctx.cpus[0]

                    cpu.capacity = cpu.model.frequency + 1000.0
                    assertEquals(cpu.model.frequency, cpu.capacity)
                    cpu.capacity = -1.0
                    assertEquals(0.0, cpu.capacity)

                    ctx.close()
                }
            })
        } finally {
            machine.close()
        }
    }

    @Test
    fun testMemory() = runBlockingSimulation {
        val machine = SimBareMetalMachine(
            FlowEngine(coroutineContext, clock),
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        try {
            machine.run(object : SimWorkload {
                override fun onStart(ctx: SimMachineContext) {
                    assertEquals(32_000 * 4.0, ctx.memory.capacity)
                    ctx.close()
                }
            })
        } finally {
            machine.close()
        }
    }

    @Test
    fun testMemoryUsage() = runBlockingSimulation {
        val machine = SimBareMetalMachine(
            FlowEngine(coroutineContext, clock),
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        try {
            machine.run(object : SimWorkload {
                override fun onStart(ctx: SimMachineContext) {
                    val lifecycle = SimWorkloadLifecycle(ctx)
                    ctx.memory.startConsumer(lifecycle.waitFor(FixedFlowSource(ctx.memory.capacity, utilization = 0.8)))
                }
            })

            assertEquals(1250, clock.millis())
        } finally {
            machine.close()
        }
    }

    @Test
    fun testNetUsage() = runBlockingSimulation {
        val engine = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(
            engine,
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        val adapter = (machine.peripherals[0] as SimNetworkAdapter)
        adapter.connect(SimNetworkSink(engine, adapter.bandwidth))

        try {
            machine.run(object : SimWorkload {
                override fun onStart(ctx: SimMachineContext) {
                    val lifecycle = SimWorkloadLifecycle(ctx)
                    val iface = ctx.net[0]
                    iface.tx.startConsumer(lifecycle.waitFor(FixedFlowSource(iface.bandwidth, utilization = 0.8)))
                }
            })

            assertEquals(1250, clock.millis())
        } finally {
            machine.close()
        }
    }

    @Test
    fun testDiskReadUsage() = runBlockingSimulation {
        val engine = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(
            engine,
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        try {
            machine.run(object : SimWorkload {
                override fun onStart(ctx: SimMachineContext) {
                    val lifecycle = SimWorkloadLifecycle(ctx)
                    val disk = ctx.storage[0]
                    disk.read.startConsumer(lifecycle.waitFor(FixedFlowSource(disk.read.capacity, utilization = 0.8)))
                }
            })

            assertEquals(1250, clock.millis())
        } finally {
            machine.close()
        }
    }

    @Test
    fun testDiskWriteUsage() = runBlockingSimulation {
        val engine = FlowEngine(coroutineContext, clock)
        val machine = SimBareMetalMachine(
            engine,
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        try {
            machine.run(object : SimWorkload {
                override fun onStart(ctx: SimMachineContext) {
                    val lifecycle = SimWorkloadLifecycle(ctx)
                    val disk = ctx.storage[0]
                    disk.write.startConsumer(lifecycle.waitFor(FixedFlowSource(disk.write.capacity, utilization = 0.8)))
                }
            })

            assertEquals(1250, clock.millis())
        } finally {
            machine.close()
        }
    }

    @Test
    fun testCancellation() = runBlockingSimulation {
        val machine = SimBareMetalMachine(
            FlowEngine(coroutineContext, clock),
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        try {
            coroutineScope {
                launch { machine.run(SimFlopsWorkload(2_000, utilization = 1.0)) }
                cancel()
            }
        } catch (_: CancellationException) {
            // Ignore
        } finally {
            machine.close()
        }

        assertEquals(0, clock.millis())
    }

    @Test
    fun testConcurrentRuns() = runBlockingSimulation {
        val machine = SimBareMetalMachine(
            FlowEngine(coroutineContext, clock),
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        try {
            coroutineScope {
                launch {
                    machine.run(SimFlopsWorkload(2_000, utilization = 1.0))
                }

                assertThrows<IllegalStateException> {
                    machine.run(SimFlopsWorkload(2_000, utilization = 1.0))
                }
            }
        } finally {
            machine.close()
        }
    }

    @Test
    fun testClose() = runBlockingSimulation {
        val machine = SimBareMetalMachine(
            FlowEngine(coroutineContext, clock),
            machineModel,
            SimplePowerDriver(ConstantPowerModel(0.0))
        )

        machine.close()
        assertDoesNotThrow { machine.close() }
        assertThrows<IllegalStateException> { machine.run(SimFlopsWorkload(2_000, utilization = 1.0)) }
    }
}
