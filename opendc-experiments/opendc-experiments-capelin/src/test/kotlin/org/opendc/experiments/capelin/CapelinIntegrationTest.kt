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

package org.opendc.experiments.capelin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.api.Server
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.CoreRamWeigher
import org.opendc.compute.workload.*
import org.opendc.compute.workload.telemetry.ComputeMetricReader
import org.opendc.compute.workload.telemetry.ComputeMonitor
import org.opendc.compute.workload.telemetry.table.HostTableReader
import org.opendc.compute.workload.topology.Topology
import org.opendc.compute.workload.topology.apply
import org.opendc.experiments.capelin.topology.clusterTopology
import org.opendc.simulator.core.runBlockingSimulation
import java.io.File
import java.time.Duration
import java.util.*

/**
 * An integration test suite for the Capelin experiments.
 */
class CapelinIntegrationTest {
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
        computeScheduler = FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(16.0), RamFilter(1.0)),
            weighers = listOf(CoreRamWeigher(multiplier = 1.0))
        )
        workloadLoader = ComputeWorkloadLoader(File("src/test/resources/trace"))
    }

    /**
     * Test a large simulation setup.
     */
    @Test
    fun testLarge() = runBlockingSimulation {
        val (workload, _) = createTestWorkload(1.0)
        val runner = ComputeServiceHelper(
            coroutineContext,
            clock,
            computeScheduler
        )
        val topology = createTopology()

        val servers = mutableListOf<Server>()
        val reader = ComputeMetricReader(this, clock, runner.service, servers, monitor)

        try {
            runner.apply(topology)
            runner.run(workload, 0, servers)

            val serviceMetrics = runner.service.getSchedulerStats()
            println(
                "Scheduler " +
                    "Success=${serviceMetrics.attemptsSuccess} " +
                    "Failure=${serviceMetrics.attemptsFailure} " +
                    "Error=${serviceMetrics.attemptsError} " +
                    "Pending=${serviceMetrics.serversPending} " +
                    "Active=${serviceMetrics.serversActive}"
            )

            // Note that these values have been verified beforehand
            assertAll(
                { assertEquals(50, serviceMetrics.attemptsSuccess, "The scheduler should schedule 50 VMs") },
                { assertEquals(0, serviceMetrics.serversActive, "All VMs should finish after a run") },
                { assertEquals(0, serviceMetrics.attemptsFailure, "No VM should be unscheduled") },
                { assertEquals(0, serviceMetrics.serversPending, "No VM should not be in the queue") },
                { assertEquals(223393683, this@CapelinIntegrationTest.monitor.idleTime) { "Incorrect idle time" } },
                { assertEquals(66977508, this@CapelinIntegrationTest.monitor.activeTime) { "Incorrect active time" } },
                { assertEquals(3160381, this@CapelinIntegrationTest.monitor.stealTime) { "Incorrect steal time" } },
                { assertEquals(0, this@CapelinIntegrationTest.monitor.lostTime) { "Incorrect lost time" } },
                { assertEquals(5.840939264814157E9, this@CapelinIntegrationTest.monitor.energyUsage, 0.01) { "Incorrect power draw" } },
            )
        } finally {
            runner.close()
            reader.close()
        }
    }

    /**
     * Test a small simulation setup.
     */
    @Test
    fun testSmall() = runBlockingSimulation {
        val seed = 1
        val (workload, _) = createTestWorkload(0.25, seed)
        val runner = ComputeServiceHelper(
            coroutineContext,
            clock,
            computeScheduler
        )
        val topology = createTopology("single")
        val servers = mutableListOf<Server>()
        val reader = ComputeMetricReader(this, clock, runner.service, servers, monitor)

        try {
            runner.apply(topology)
            runner.run(workload, seed.toLong(), servers)

            val serviceMetrics = runner.service.getSchedulerStats()
            println(
                "Scheduler " +
                    "Success=${serviceMetrics.attemptsSuccess} " +
                    "Failure=${serviceMetrics.attemptsFailure} " +
                    "Error=${serviceMetrics.attemptsError} " +
                    "Pending=${serviceMetrics.serversPending} " +
                    "Active=${serviceMetrics.serversActive}"
            )
        } finally {
            runner.close()
            reader.close()
        }

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(10999592, this@CapelinIntegrationTest.monitor.idleTime) { "Idle time incorrect" } },
            { assertEquals(9741207, this@CapelinIntegrationTest.monitor.activeTime) { "Active time incorrect" } },
            { assertEquals(0, this@CapelinIntegrationTest.monitor.stealTime) { "Steal time incorrect" } },
            { assertEquals(0, this@CapelinIntegrationTest.monitor.lostTime) { "Lost time incorrect" } },
            { assertEquals(7.011676470304312E8, this@CapelinIntegrationTest.monitor.energyUsage, 0.01) { "Incorrect power draw" } }
        )
    }

    /**
     * Test a small simulation setup with interference.
     */
    @Test
    fun testInterference() = runBlockingSimulation {
        val seed = 0
        val (workload, interferenceModel) = createTestWorkload(1.0, seed)

        val simulator = ComputeServiceHelper(
            coroutineContext,
            clock,
            computeScheduler,
            interferenceModel = interferenceModel?.withSeed(seed.toLong())
        )
        val topology = createTopology("single")
        val servers = mutableListOf<Server>()
        val reader = ComputeMetricReader(this, clock, simulator.service, servers, monitor)

        try {
            simulator.apply(topology)
            simulator.run(workload, seed.toLong(), servers)

            val serviceMetrics = simulator.service.getSchedulerStats()
            println(
                "Scheduler " +
                    "Success=${serviceMetrics.attemptsSuccess} " +
                    "Failure=${serviceMetrics.attemptsFailure} " +
                    "Error=${serviceMetrics.attemptsError} " +
                    "Pending=${serviceMetrics.serversPending} " +
                    "Active=${serviceMetrics.serversActive}"
            )
        } finally {
            simulator.close()
            reader.close()
        }

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(6028050, this@CapelinIntegrationTest.monitor.idleTime) { "Idle time incorrect" } },
            { assertEquals(14712749, this@CapelinIntegrationTest.monitor.activeTime) { "Active time incorrect" } },
            { assertEquals(12532907, this@CapelinIntegrationTest.monitor.stealTime) { "Steal time incorrect" } },
            { assertEquals(467963, this@CapelinIntegrationTest.monitor.lostTime) { "Lost time incorrect" } }
        )
    }

    /**
     * Test a small simulation setup with failures.
     */
    @Test
    fun testFailures() = runBlockingSimulation {
        val seed = 1
        val simulator = ComputeServiceHelper(
            coroutineContext,
            clock,
            computeScheduler,
            grid5000(Duration.ofDays(7))
        )
        val topology = createTopology("single")
        val (workload, _) = createTestWorkload(0.25, seed)
        val servers = mutableListOf<Server>()
        val reader = ComputeMetricReader(this, clock, simulator.service, servers, monitor)

        try {
            simulator.apply(topology)
            simulator.run(workload, seed.toLong(), servers)

            val serviceMetrics = simulator.service.getSchedulerStats()
            println(
                "Scheduler " +
                    "Success=${serviceMetrics.attemptsSuccess} " +
                    "Failure=${serviceMetrics.attemptsFailure} " +
                    "Error=${serviceMetrics.attemptsError} " +
                    "Pending=${serviceMetrics.serversPending} " +
                    "Active=${serviceMetrics.serversActive}"
            )
        } finally {
            simulator.close()
            reader.close()
        }

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(10867345, monitor.idleTime) { "Idle time incorrect" } },
            { assertEquals(9607095, monitor.activeTime) { "Active time incorrect" } },
            { assertEquals(0, monitor.stealTime) { "Steal time incorrect" } },
            { assertEquals(0, monitor.lostTime) { "Lost time incorrect" } },
            { assertEquals(2559305056, monitor.uptime) { "Uptime incorrect" } }
        )
    }

    /**
     * Obtain the trace reader for the test.
     */
    private fun createTestWorkload(fraction: Double, seed: Int = 0): ComputeWorkload.Resolved {
        val source = trace("bitbrains-small").sampleByLoad(fraction)
        return source.resolve(workloadLoader, Random(seed.toLong()))
    }

    /**
     * Obtain the topology factory for the test.
     */
    private fun createTopology(name: String = "topology"): Topology {
        val stream = checkNotNull(object {}.javaClass.getResourceAsStream("/env/$name.txt"))
        return stream.use { clusterTopology(stream) }
    }

    class TestComputeMonitor : ComputeMonitor {
        var idleTime = 0L
        var activeTime = 0L
        var stealTime = 0L
        var lostTime = 0L
        var energyUsage = 0.0
        var uptime = 0L

        override fun record(reader: HostTableReader) {
            idleTime += reader.cpuIdleTime
            activeTime += reader.cpuActiveTime
            stealTime += reader.cpuStealTime
            lostTime += reader.cpuLostTime
            energyUsage += reader.powerTotal
            uptime += reader.uptime
        }
    }
}
