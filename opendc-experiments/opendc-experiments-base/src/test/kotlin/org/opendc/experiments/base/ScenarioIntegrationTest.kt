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

package org.opendc.experiments.base

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.CoreRamWeigher
import org.opendc.compute.simulator.provisioner.Provisioner
import org.opendc.compute.simulator.provisioner.registerComputeMonitor
import org.opendc.compute.simulator.provisioner.setupComputeService
import org.opendc.compute.simulator.provisioner.setupHosts
import org.opendc.compute.telemetry.ComputeMonitor
import org.opendc.compute.telemetry.table.HostTableReader
import org.opendc.compute.telemetry.table.ServiceTableReader
import org.opendc.compute.topology.clusterTopology
import org.opendc.compute.topology.specs.HostSpec
import org.opendc.compute.workload.ComputeWorkloadLoader
import org.opendc.compute.workload.VirtualMachine
import org.opendc.compute.workload.sampleByLoad
import org.opendc.compute.workload.trace
import org.opendc.experiments.base.runner.replay
import org.opendc.simulator.kotlin.runSimulation
import java.io.File
import java.util.Random

/**
 * An integration test suite for the Scenario experiments.
 */
class ScenarioIntegrationTest {
    /**
     * The monitor used to keep track of the metrics.
     */
    private lateinit var monitor: TestComputeMonitor

    /**
     * The [FilterScheduler] to use for all experiments.
     */
    private lateinit var computeScheduler: FilterScheduler

    /**
     * The [ComputeWorkloadLoader] responsible for loading the traces.
     */
    private lateinit var workloadLoader: ComputeWorkloadLoader

    /**
     * Set up the experimental environment.
     */
    @BeforeEach
    fun setUp() {
        monitor = TestComputeMonitor()
        computeScheduler =
            FilterScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(16.0), RamFilter(1.0)),
                weighers = listOf(CoreRamWeigher(multiplier = 1.0)),
            )
        workloadLoader = ComputeWorkloadLoader(File("src/test/resources/trace"))
    }

    /**
     * Test a large simulation setup.
     */
    @Test
    fun testLarge() =
        runSimulation {
            val seed = 0L
            val workload = createTestWorkload(1.0, seed)
            val topology = createTopology("multi.json")
            val monitor = monitor

            Provisioner(dispatcher, seed).use { provisioner ->
                provisioner.runSteps(
                    setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
                    registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor),
                    setupHosts(serviceDomain = "compute.opendc.org", topology),
                )

                val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
                service.replay(timeSource, workload, seed = seed)
            }

            println(
                "Scheduler " +
                    "Success=${monitor.attemptsSuccess} " +
                    "Failure=${monitor.attemptsFailure} " +
                    "Error=${monitor.attemptsError} " +
                    "Pending=${monitor.tasksPending} " +
                    "Active=${monitor.tasksActive}",
            )

            // Note that these values have been verified beforehand
            assertAll(
                { assertEquals(50, monitor.attemptsSuccess, "The scheduler should schedule 50 VMs") },
                { assertEquals(0, monitor.tasksActive, "All VMs should finish after a run") },
                { assertEquals(0, monitor.attemptsFailure, "No VM should be unscheduled") },
                { assertEquals(0, monitor.tasksPending, "No VM should not be in the queue") },
                { assertEquals(43101769345, monitor.idleTime) { "Incorrect idle time" } },
                { assertEquals(3489430672, monitor.activeTime) { "Incorrect active time" } },
                { assertEquals(0, monitor.stealTime) { "Incorrect steal time" } },
                { assertEquals(0, monitor.lostTime) { "Incorrect lost time" } },
                { assertEquals(3.3388920269258898E7, monitor.powerDraw, 1E4) { "Incorrect power draw" } },
                { assertEquals(1.0016127451211525E10, monitor.energyUsage, 1E4) { "Incorrect energy usage" } },
            )
        }

    /**
     * Test a small simulation setup.
     */
    @Test
    fun testSmall() =
        runSimulation {
            val seed = 1L
            val workload = createTestWorkload(0.25, seed)
            val topology = createTopology("single.json")
            val monitor = monitor

            Provisioner(dispatcher, seed).use { provisioner ->
                provisioner.runSteps(
                    setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
                    registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor),
                    setupHosts(serviceDomain = "compute.opendc.org", topology),
                )

                val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
                service.replay(timeSource, workload, seed = seed)
            }

            println(
                "Scheduler " +
                    "Success=${monitor.attemptsSuccess} " +
                    "Failure=${monitor.attemptsFailure} " +
                    "Error=${monitor.attemptsError} " +
                    "Pending=${monitor.tasksPending} " +
                    "Active=${monitor.tasksActive}",
            )

            // Note that these values have been verified beforehand
            assertAll(
                { assertEquals(1373419781, monitor.idleTime) { "Idle time incorrect" } },
                { assertEquals(1217668222, monitor.activeTime) { "Active time incorrect" } },
                { assertEquals(0, monitor.stealTime) { "Steal time incorrect" } },
                { assertEquals(0, monitor.lostTime) { "Lost time incorrect" } },
                { assertEquals(2539987.394500494, monitor.powerDraw, 1E4) { "Incorrect power draw" } },
                { assertEquals(7.617527900379665E8, monitor.energyUsage, 1E4) { "Incorrect energy usage" } },
            )
        }

    /**
     * Test a small simulation setup with interference.
     * TODO: Interference is currently removed from OpenDC. Reactivate when interference is back in.
     */
    fun testInterference() =
        runSimulation {
            val seed = 0L
            val workload = createTestWorkload(1.0, seed)
            val topology = createTopology("single.json")

            Provisioner(dispatcher, seed).use { provisioner ->
                provisioner.runSteps(
                    setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
                    registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor),
                    setupHosts(serviceDomain = "compute.opendc.org", topology),
                )

                val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
                service.replay(timeSource, workload, seed = seed)
            }

            println(
                "Scheduler " +
                    "Success=${monitor.attemptsSuccess} " +
                    "Failure=${monitor.attemptsFailure} " +
                    "Error=${monitor.attemptsError} " +
                    "Pending=${monitor.tasksPending} " +
                    "Active=${monitor.tasksActive}",
            )

            // Note that these values have been verified beforehand
            assertAll(
                { assertEquals(42814948316, monitor.idleTime) { "Idle time incorrect" } },
                { assertEquals(40138266225, monitor.activeTime) { "Active time incorrect" } },
                { assertEquals(23489356981, monitor.stealTime) { "Steal time incorrect" } },
                { assertEquals(0, monitor.lostTime) { "Lost time incorrect" } },
            )
        }

    /**
     * Test a small simulation setup with failures.
     * FIXME: Currently failures do not work. reactivate this test when Failures are implemented again
     */
    fun testFailures() =
        runSimulation {
            val seed = 0L
            val topology = createTopology("single.json")
            val workload = createTestWorkload(0.25, seed)
            val monitor = monitor

            Provisioner(dispatcher, seed).use { provisioner ->
                provisioner.runSteps(
                    setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
                    registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor),
                    setupHosts(serviceDomain = "compute.opendc.org", topology),
                )

                val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
                service.replay(timeSource, workload, seed = seed, failureModelSpec = null)
            }

            // Note that these values have been verified beforehand
            assertAll(
                { assertEquals(1404277711, monitor.idleTime) { "Idle time incorrect" } },
                { assertEquals(1478675712, monitor.activeTime) { "Active time incorrect" } },
                { assertEquals(152, monitor.stealTime) { "Steal time incorrect" } },
                { assertEquals(0, monitor.lostTime) { "Lost time incorrect" } },
                { assertEquals(360369187, monitor.uptime) { "Uptime incorrect" } },
            )
        }

    /**
     * Obtain the trace reader for the test.
     */
    private fun createTestWorkload(
        fraction: Double,
        seed: Long,
    ): List<VirtualMachine> {
        val source = trace("bitbrains-small").sampleByLoad(fraction)
        return source.resolve(workloadLoader, Random(seed))
    }

    /**
     * Obtain the topology factory for the test.
     */
    private fun createTopology(name: String): List<HostSpec> {
        val stream = checkNotNull(object {}.javaClass.getResourceAsStream("/env/$name"))
        return stream.use { clusterTopology(stream) }
    }

    class TestComputeMonitor : ComputeMonitor {
        var attemptsSuccess = 0
        var attemptsFailure = 0
        var attemptsError = 0
        var tasksPending = 0
        var tasksActive = 0

        override fun record(reader: ServiceTableReader) {
            attemptsSuccess = reader.attemptsSuccess
            attemptsFailure = reader.attemptsFailure
            attemptsError = reader.attemptsError
            tasksPending = reader.tasksPending
            tasksActive = reader.tasksActive
        }

        var idleTime = 0L
        var activeTime = 0L
        var stealTime = 0L
        var lostTime = 0L
        var powerDraw = 0.0
        var energyUsage = 0.0
        var uptime = 0L

        override fun record(reader: HostTableReader) {
            idleTime += reader.cpuIdleTime
            activeTime += reader.cpuActiveTime
            stealTime += reader.cpuStealTime
            lostTime += reader.cpuLostTime
            powerDraw += reader.powerDraw
            energyUsage += reader.energyUsage
            uptime += reader.uptime
        }
    }
}
