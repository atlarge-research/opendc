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
import org.opendc.compute.simulator.provisioner.Provisioner
import org.opendc.compute.simulator.provisioner.registerComputeMonitor
import org.opendc.compute.simulator.provisioner.setupComputeService
import org.opendc.compute.simulator.provisioner.setupHosts
import org.opendc.compute.simulator.scheduler.FilterScheduler
import org.opendc.compute.simulator.scheduler.filters.ComputeFilter
import org.opendc.compute.simulator.scheduler.filters.RamFilter
import org.opendc.compute.simulator.scheduler.filters.VCpuFilter
import org.opendc.compute.simulator.scheduler.weights.CoreRamWeigher
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.telemetry.ComputeMonitor
import org.opendc.compute.simulator.telemetry.table.HostTableReader
import org.opendc.compute.simulator.telemetry.table.ServiceTableReader
import org.opendc.compute.topology.clusterTopology
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.compute.workload.ComputeWorkloadLoader
import org.opendc.compute.workload.Task
import org.opendc.compute.workload.sampleByLoad
import org.opendc.compute.workload.trace
import org.opendc.experiments.base.experiment.specs.TraceBasedFailureModelSpec
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
        workloadLoader = ComputeWorkloadLoader(File("src/test/resources/traces"), 0L, 0L, 0.0)
    }

    /**
     * Test a small simulation setup.
     */
    @Test
    fun testSingleTask() =
        runSimulation {
            val seed = 1L
            val workload = createTestWorkload("single_task", 1.0, seed)
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
                { assertEquals(0, monitor.idleTime) { "Idle time incorrect" } },
                { assertEquals(3000000, monitor.activeTime) { "Active time incorrect" } },
                { assertEquals(0, monitor.stealTime) { "Steal time incorrect" } },
                { assertEquals(0, monitor.lostTime) { "Lost time incorrect" } },
                { assertEquals(1200000.0, monitor.energyUsage, 1E4) { "Incorrect energy usage" } },
            )
        }

    /**
     * Test a small simulation setup.
     */
    @Test
    fun testSingleTaskSingleFailure() =
        runSimulation {
            val seed = 1L
            val workload = createTestWorkload("single_task", 1.0, seed)
            val topology = createTopology("single.json")
            val monitor = monitor
            val failureModelSpec =
                TraceBasedFailureModelSpec(
                    "src/test/resources/failureTraces/single_failure.parquet",
                    repeat = false,
                )

            Provisioner(dispatcher, seed).use { provisioner ->
                provisioner.runSteps(
                    setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
                    registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor),
                    setupHosts(serviceDomain = "compute.opendc.org", topology),
                )

                val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
                service.setTasksExpected(workload.size)

                service.replay(timeSource, workload, failureModelSpec = failureModelSpec, seed = seed)
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
                { assertEquals(2200000, monitor.idleTime) { "Idle time incorrect" } },
                { assertEquals(5000000, monitor.activeTime) { "Active time incorrect" } },
                { assertEquals(0, monitor.stealTime) { "Steal time incorrect" } },
                { assertEquals(0, monitor.lostTime) { "Lost time incorrect" } },
                { assertEquals(2440000.0, monitor.energyUsage, 1E4) { "Incorrect energy usage" } },
            )
        }

    /**
     * Test a small simulation setup.
     */
    @Test
    fun testSingleTask11Failures() =
        runSimulation {
            val seed = 1L
            val workload = createTestWorkload("single_task", 1.0, seed)
            val topology = createTopology("single.json")
            val monitor = monitor
            val failureModelSpec = TraceBasedFailureModelSpec("src/test/resources/failureTraces/11_failures.parquet")

            Provisioner(dispatcher, seed).use { provisioner ->
                provisioner.runSteps(
                    setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
                    registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor),
                    setupHosts(serviceDomain = "compute.opendc.org", topology),
                )

                val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
                service.replay(timeSource, workload, failureModelSpec = failureModelSpec, seed = seed)
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
                { assertEquals(1, monitor.tasksTerminated) { "Idle time incorrect" } },
                { assertEquals(18100000, monitor.idleTime) { "Idle time incorrect" } },
                { assertEquals(20000000, monitor.activeTime) { "Active time incorrect" } },
                { assertEquals(0, monitor.stealTime) { "Steal time incorrect" } },
                { assertEquals(0, monitor.lostTime) { "Lost time incorrect" } },
                { assertEquals(1.162E7, monitor.energyUsage, 1E4) { "Incorrect energy usage" } },
            )
        }

    /**
     * Test a small simulation setup.
     */
    @Test
    fun testSingleTaskCheckpoint() =
        runSimulation {
            val seed = 1L
            workloadLoader = ComputeWorkloadLoader(File("src/test/resources/traces"), 1000000L, 1000L, 1.0)
            val workload = createTestWorkload("single_task", 1.0, seed)
            val topology = createTopology("single.json")
            val monitor = monitor
            val failureModelSpec = TraceBasedFailureModelSpec("src/test/resources/failureTraces/11_failures.parquet")

            Provisioner(dispatcher, seed).use { provisioner ->
                provisioner.runSteps(
                    setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
                    registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor),
                    setupHosts(serviceDomain = "compute.opendc.org", topology),
                )

                val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
                service.replay(timeSource, workload, failureModelSpec = failureModelSpec, seed = seed)
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
                { assertEquals(0, monitor.tasksTerminated) { "Idle time incorrect" } },
                { assertEquals(1, monitor.tasksCompleted) { "Idle time incorrect" } },
                { assertEquals(4297000, monitor.idleTime) { "Idle time incorrect" } },
                { assertEquals(5003000, monitor.activeTime) { "Active time incorrect" } },
                { assertEquals(0, monitor.stealTime) { "Steal time incorrect" } },
                { assertEquals(0, monitor.lostTime) { "Lost time incorrect" } },
                { assertEquals(2860800.0, monitor.energyUsage, 1E4) { "Incorrect energy usage" } },
            )
        }

    /**
     * Test a small simulation setup.
     */
    @Test
    fun testSmall() =
        runSimulation {
            val seed = 1L
            val workload = createTestWorkload("bitbrains-small", 0.25, seed)
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
                { assertEquals(1803918473, monitor.idleTime) { "Idle time incorrect" } },
                { assertEquals(787181527, monitor.activeTime) { "Active time incorrect" } },
                { assertEquals(0, monitor.stealTime) { "Steal time incorrect" } },
                { assertEquals(0, monitor.lostTime) { "Lost time incorrect" } },
                { assertEquals(6.7565629E8, monitor.energyUsage, 1E4) { "Incorrect energy usage" } },
            )
        }

    /**
     * Test a large simulation setup.
     */
    @Test
    fun testLarge() =
        runSimulation {
            val seed = 0L
            val workload = createTestWorkload("bitbrains-small", 1.0, seed)
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
                { assertEquals(50, monitor.tasksCompleted, "The scheduler should schedule 50 VMs") },
                { assertEquals(0, monitor.tasksTerminated, "The scheduler should schedule 50 VMs") },
                { assertEquals(0, monitor.tasksActive, "All VMs should finish after a run") },
                { assertEquals(0, monitor.attemptsFailure, "No VM should be unscheduled") },
                { assertEquals(0, monitor.tasksPending, "No VM should not be in the queue") },
                { assertEquals(43101787433, monitor.idleTime) { "Incorrect idle time" } },
                { assertEquals(3489412567, monitor.activeTime) { "Incorrect active time" } },
                { assertEquals(0, monitor.stealTime) { "Incorrect steal time" } },
                { assertEquals(0, monitor.lostTime) { "Incorrect lost time" } },
                { assertEquals(1.0016123392181786E10, monitor.energyUsage, 1E4) { "Incorrect energy usage" } },
            )
        }

    /**
     * Obtain the trace reader for the test.
     */
    private fun createTestWorkload(
        traceName: String,
        fraction: Double,
        seed: Long,
    ): List<Task> {
        val source = trace(traceName).sampleByLoad(fraction)
        return source.resolve(workloadLoader, Random(seed))
    }

    /**
     * Obtain the topology factory for the test.
     */
    private fun createTopology(name: String): List<ClusterSpec> {
        val stream = checkNotNull(object {}.javaClass.getResourceAsStream("/topologies/$name"))
        return stream.use { clusterTopology(stream) }
    }

    class TestComputeMonitor : ComputeMonitor {
        var attemptsSuccess = 0
        var attemptsFailure = 0
        var attemptsError = 0
        var tasksPending = 0
        var tasksActive = 0
        var tasksTerminated = 0
        var tasksCompleted = 0

        override fun record(reader: ServiceTableReader) {
            attemptsSuccess = reader.attemptsSuccess
            attemptsFailure = reader.attemptsFailure
            attemptsError = 0
            tasksPending = reader.tasksPending
            tasksActive = reader.tasksActive
            tasksTerminated = reader.tasksTerminated
            tasksCompleted = reader.tasksCompleted
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
