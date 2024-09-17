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

package org.opendc.compute.simulator

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.api.Flavor
import org.opendc.compute.api.Image
import org.opendc.compute.api.Task
import org.opendc.compute.api.TaskState
import org.opendc.compute.api.TaskWatcher
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.driver.HostListener
import org.opendc.simulator.compute.SimBareMetalMachine
import org.opendc.simulator.compute.kernel.SimHypervisor
import org.opendc.simulator.compute.model.Cpu
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.workload.SimTrace
import org.opendc.simulator.compute.workload.SimTraceFragment
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.flow2.mux.FlowMultiplexerFactory
import org.opendc.simulator.kotlin.runSimulation
import java.time.Instant
import java.util.SplittableRandom
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Basic test-suite for the hypervisor.
 */
internal class SimHostTest {
    private lateinit var machineModel: MachineModel

    @BeforeEach
    fun setUp() {
        machineModel =
            MachineModel(
                Cpu(
                    0,
                    2,
                    3200.0,
                    "Intel",
                    "Xeon",
                    "amd64",
                ),
                // memory
                MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000 * 4),
            )
    }

    /**
     * Test a single virtual machine hosted by the hypervisor.
     */
    @Test
    fun testSingle() =
        runSimulation {
            val duration = 5 * 60L

            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine = SimBareMetalMachine.create(graph, machineModel)
            val hypervisor = SimHypervisor.create(FlowMultiplexerFactory.maxMinMultiplexer(), SplittableRandom(1))

            val host =
                SimHost(
                    uid = UUID.randomUUID(),
                    name = "test",
                    meta = emptyMap(),
                    timeSource,
                    machine,
                    hypervisor,
                )
            val vmImage =
                MockImage(
                    UUID.randomUUID(),
                    "<unnamed>",
                    emptyMap(),
                    mapOf(
                        "workload" to
                            SimTrace.ofFragments(
                                SimTraceFragment(0, duration * 1000, 0.0, 2),
                                SimTraceFragment(duration * 1000, duration * 1000, 3200.0, 2),
                                SimTraceFragment(duration * 2000, duration * 1000, 0.0, 2),
                                SimTraceFragment(duration * 3000, duration * 1000, 6500.0, 2),
                            ).createWorkload(0),
                    ),
                )

            val flavor = MockFlavor(2, 0)

            suspendCancellableCoroutine { cont ->
                host.addListener(
                    object : HostListener {
                        private var finished = 0

                        override fun onStateChanged(
                            host: Host,
                            task: Task,
                            newState: TaskState,
                        ) {
                            if (newState == TaskState.TERMINATED && ++finished == 1) {
                                cont.resume(Unit)
                            }
                        }
                    },
                )
                val server = MockTask(UUID.randomUUID(), "a", flavor, vmImage)
                host.spawn(server)
                host.start(server)
            }

            // Ensure last cycle is collected
//            delay(1000L * duration)
            host.close()

            val cpuStats = host.getCpuStats()

            assertAll(
                { assertEquals(450000, cpuStats.activeTime, "Active time does not match") },
                { assertEquals(750000, cpuStats.idleTime, "Idle time does not match") },
                { assertEquals(4688, cpuStats.stealTime, "Steal time does not match") },
                { assertEquals(1200000, timeSource.millis()) },
            )
        }

    /**
     * Test overcommitting of resources by the hypervisor.
     */
    @Test
    fun testOvercommitted() =
        runSimulation {
            val duration = 5 * 60L

            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine = SimBareMetalMachine.create(graph, machineModel)
            val hypervisor = SimHypervisor.create(FlowMultiplexerFactory.maxMinMultiplexer(), SplittableRandom(1))

            val host =
                SimHost(
                    uid = UUID.randomUUID(),
                    name = "test",
                    meta = emptyMap(),
                    timeSource,
                    machine,
                    hypervisor,
                )
            val vmImageA =
                MockImage(
                    UUID.randomUUID(),
                    "<unnamed>",
                    emptyMap(),
                    mapOf(
                        "workload" to
                            SimTrace.ofFragments(
                                SimTraceFragment(0, duration * 1000, 0.0, 2),
                                SimTraceFragment(duration * 1000, duration * 1000, 3200.0, 2),
                                SimTraceFragment(duration * 2000, duration * 1000, 0.0, 2),
                                SimTraceFragment(duration * 3000, duration * 1000, 6500.0, 2),
                            ).createWorkload(0),
                    ),
                )
            val vmImageB =
                MockImage(
                    UUID.randomUUID(),
                    "<unnamed>",
                    emptyMap(),
                    mapOf(
                        "workload" to
                            SimTrace.ofFragments(
                                SimTraceFragment(0, duration * 1000, 0.0, 2),
                                SimTraceFragment(duration * 1000, duration * 1000, 3200.0, 2),
                                SimTraceFragment(duration * 2000, duration * 1000, 0.0, 2),
                                SimTraceFragment(duration * 3000, duration * 1000, 6500.0, 2),
                            ).createWorkload(0),
                    ),
                )

            val flavor = MockFlavor(2, 0)

            coroutineScope {
                suspendCancellableCoroutine { cont ->
                    host.addListener(
                        object : HostListener {
                            private var finished = 0

                            override fun onStateChanged(
                                host: Host,
                                task: Task,
                                newState: TaskState,
                            ) {
                                if (newState == TaskState.TERMINATED && ++finished == 2) {
                                    cont.resume(Unit)
                                }
                            }
                        },
                    )
                    val serverA = MockTask(UUID.randomUUID(), "a", flavor, vmImageA)
                    host.spawn(serverA)
                    val serverB = MockTask(UUID.randomUUID(), "b", flavor, vmImageB)
                    host.spawn(serverB)

                    host.start(serverA)
                    host.start(serverB)
                }
            }

            // Ensure last cycle is collected
            delay(1000L * duration)
            host.close()

            val cpuStats = host.getCpuStats()

            assertAll(
                { assertEquals(600000, cpuStats.activeTime, "Active time does not match") },
                { assertEquals(900000, cpuStats.idleTime, "Idle time does not match") },
                { assertEquals(309375, cpuStats.stealTime, "Steal time does not match") },
                { assertEquals(1500000, timeSource.millis()) },
            )
        }

    /**
     * Test failure of the host.
     */
    @Test
    fun testFailure() =
        runSimulation {
            val duration = 5 * 60L

            val engine = FlowEngine.create(dispatcher)
            val graph = engine.newGraph()

            val machine = SimBareMetalMachine.create(graph, machineModel)
            val hypervisor = SimHypervisor.create(FlowMultiplexerFactory.maxMinMultiplexer(), SplittableRandom(1))
            val host =
                SimHost(
                    uid = UUID.randomUUID(),
                    name = "test",
                    meta = emptyMap(),
                    timeSource,
                    machine,
                    hypervisor,
                )
            val image =
                MockImage(
                    UUID.randomUUID(),
                    "<unnamed>",
                    emptyMap(),
                    mapOf(
                        "workload" to
                            SimTrace.ofFragments(
                                SimTraceFragment(0, duration * 1000, 0.0, 2),
                                SimTraceFragment(duration * 1000, duration * 1000, 3200.0, 2),
                                SimTraceFragment(duration * 2000, duration * 1000, 0.0, 2),
                                SimTraceFragment(duration * 3000, duration * 1000, 6500.0, 2),
                            ).createWorkload(0),
                    ),
                )
            val flavor = MockFlavor(2, 0)
            val server = MockTask(UUID.randomUUID(), "a", flavor, image)

            coroutineScope {
                host.spawn(server)
                host.start(server)
                delay(5000L)
                host.fail()
                delay(duration * 1000)
                host.recover()

                suspendCancellableCoroutine { cont ->
                    host.addListener(
                        object : HostListener {
                            override fun onStateChanged(
                                host: Host,
                                task: Task,
                                newState: TaskState,
                            ) {
                                if (newState == TaskState.TERMINATED) {
                                    cont.resume(Unit)
                                }
                            }
                        },
                    )
                }
            }

            host.close()
            // Ensure last cycle is collected
            delay(1000L * duration)

            val cpuStats = host.getCpuStats()
            val sysStats = host.getSystemStats()
            val guestSysStats = host.getSystemStats(server)

            assertAll(
                { assertEquals(755000, cpuStats.idleTime, "Idle time does not match") },
                { assertEquals(450000, cpuStats.activeTime, "Active time does not match") },
                { assertEquals(1205000, sysStats.uptime.toMillis(), "Uptime does not match") },
                { assertEquals(300000, sysStats.downtime.toMillis(), "Downtime does not match") },
                { assertEquals(1205000, guestSysStats.uptime.toMillis(), "Guest uptime does not match") },
                { assertEquals(300000, guestSysStats.downtime.toMillis(), "Guest downtime does not match") },
            )
        }

    private class MockFlavor(
        override val coreCount: Int,
        override val memorySize: Long,
    ) : Flavor {
        override val uid: UUID = UUID.randomUUID()
        override val name: String = "test"
        override val labels: Map<String, String> = emptyMap()
        override val meta: Map<String, Any> = emptyMap()

        override fun delete() {
            throw NotImplementedError()
        }

        override fun reload() {
            throw NotImplementedError()
        }
    }

    private class MockImage(
        override val uid: UUID,
        override val name: String,
        override val labels: Map<String, String>,
        override val meta: Map<String, Any>,
    ) : Image {
        override fun delete() {
            throw NotImplementedError()
        }

        override fun reload() {
            throw NotImplementedError()
        }
    }

    private class MockTask(
        override val uid: UUID,
        override val name: String,
        override val flavor: Flavor,
        override val image: Image,
        override val numFailures: Int = 10,
    ) : Task {
        override val labels: Map<String, String> = emptyMap()

        override val meta: Map<String, Any> = emptyMap()

        override val state: TaskState = TaskState.TERMINATED

        override val launchedAt: Instant? = null

        override fun start() {}

        override fun stop() {}

        override fun delete() {}

        override fun watch(watcher: TaskWatcher) {}

        override fun unwatch(watcher: TaskWatcher) {}

        override fun reload() {}
    }
}
