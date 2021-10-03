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
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.CoreRamWeigher
import org.opendc.compute.workload.*
import org.opendc.compute.workload.topology.Topology
import org.opendc.compute.workload.topology.apply
import org.opendc.compute.workload.util.PerformanceInterferenceReader
import org.opendc.experiments.capelin.topology.clusterTopology
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.telemetry.compute.ComputeMetricExporter
import org.opendc.telemetry.compute.collectServiceMetrics
import org.opendc.telemetry.compute.table.HostData
import org.opendc.telemetry.sdk.metrics.export.CoroutineMetricReader
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
    private lateinit var exporter: TestComputeMetricExporter

    /**
     * The [FilterScheduler] to use for all experiments.
     */
    private lateinit var computeScheduler: FilterScheduler

    /**
     * The [ComputeWorkloadLoader] responsible for loading the traces.
     */
    private lateinit var workloadLoader: ComputeWorkloadLoader

    /**
     * Setup the experimental environment.
     */
    @BeforeEach
    fun setUp() {
        exporter = TestComputeMetricExporter()
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
        val workload = createTestWorkload(1.0)
        val runner = ComputeWorkloadRunner(
            coroutineContext,
            clock,
            computeScheduler
        )
        val topology = createTopology()
        val metricReader = CoroutineMetricReader(this, runner.producers, exporter)

        try {
            runner.apply(topology)
            runner.run(workload, 0)
        } finally {
            runner.close()
            metricReader.close()
        }

        val serviceMetrics = collectServiceMetrics(runner.producers[0])
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
            { assertEquals(223325655, this@CapelinIntegrationTest.exporter.idleTime) { "Incorrect idle time" } },
            { assertEquals(67006560, this@CapelinIntegrationTest.exporter.activeTime) { "Incorrect active time" } },
            { assertEquals(3159377, this@CapelinIntegrationTest.exporter.stealTime) { "Incorrect steal time" } },
            { assertEquals(0, this@CapelinIntegrationTest.exporter.lostTime) { "Incorrect lost time" } },
            { assertEquals(5.840207707767459E9, this@CapelinIntegrationTest.exporter.energyUsage, 0.01) { "Incorrect power draw" } },
        )
    }

    /**
     * Test a small simulation setup.
     */
    @Test
    fun testSmall() = runBlockingSimulation {
        val seed = 1
        val workload = createTestWorkload(0.25, seed)

        val simulator = ComputeWorkloadRunner(
            coroutineContext,
            clock,
            computeScheduler
        )
        val topology = createTopology("single")
        val metricReader = CoroutineMetricReader(this, simulator.producers, exporter)

        try {
            simulator.apply(topology)
            simulator.run(workload, seed.toLong())
        } finally {
            simulator.close()
            metricReader.close()
        }

        val serviceMetrics = collectServiceMetrics(simulator.producers[0])
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
            { assertEquals(10997726, this@CapelinIntegrationTest.exporter.idleTime) { "Idle time incorrect" } },
            { assertEquals(9740289, this@CapelinIntegrationTest.exporter.activeTime) { "Active time incorrect" } },
            { assertEquals(0, this@CapelinIntegrationTest.exporter.stealTime) { "Steal time incorrect" } },
            { assertEquals(0, this@CapelinIntegrationTest.exporter.lostTime) { "Lost time incorrect" } },
            { assertEquals(7.009945802750012E8, this@CapelinIntegrationTest.exporter.energyUsage, 0.01) { "Incorrect power draw" } }
        )
    }

    /**
     * Test a small simulation setup with interference.
     */
    @Test
    fun testInterference() = runBlockingSimulation {
        val seed = 0
        val workload = createTestWorkload(1.0, seed)
        val perfInterferenceInput = checkNotNull(CapelinIntegrationTest::class.java.getResourceAsStream("/bitbrains-perf-interference.json"))
        val performanceInterferenceModel =
            PerformanceInterferenceReader()
                .read(perfInterferenceInput)
                .let { VmInterferenceModel(it, Random(seed.toLong())) }

        val simulator = ComputeWorkloadRunner(
            coroutineContext,
            clock,
            computeScheduler,
            interferenceModel = performanceInterferenceModel
        )
        val topology = createTopology("single")
        val metricReader = CoroutineMetricReader(this, simulator.producers, exporter)

        try {
            simulator.apply(topology)
            simulator.run(workload, seed.toLong())
        } finally {
            simulator.close()
            metricReader.close()
        }

        val serviceMetrics = collectServiceMetrics(simulator.producers[0])
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
            { assertEquals(6013515, this@CapelinIntegrationTest.exporter.idleTime) { "Idle time incorrect" } },
            { assertEquals(14724500, this@CapelinIntegrationTest.exporter.activeTime) { "Active time incorrect" } },
            { assertEquals(12530742, this@CapelinIntegrationTest.exporter.stealTime) { "Steal time incorrect" } },
            { assertEquals(480866, this@CapelinIntegrationTest.exporter.lostTime) { "Lost time incorrect" } }
        )
    }

    /**
     * Test a small simulation setup with failures.
     */
    @Test
    fun testFailures() = runBlockingSimulation {
        val seed = 1
        val simulator = ComputeWorkloadRunner(
            coroutineContext,
            clock,
            computeScheduler,
            grid5000(Duration.ofDays(7))
        )
        val topology = createTopology("single")
        val workload = createTestWorkload(0.25, seed)
        val metricReader = CoroutineMetricReader(this, simulator.producers, exporter)

        try {
            simulator.apply(topology)
            simulator.run(workload, seed.toLong())
        } finally {
            simulator.close()
            metricReader.close()
        }

        val serviceMetrics = collectServiceMetrics(simulator.producers[0])
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
            { assertEquals(10865478, exporter.idleTime) { "Idle time incorrect" } },
            { assertEquals(9606177, exporter.activeTime) { "Active time incorrect" } },
            { assertEquals(0, exporter.stealTime) { "Steal time incorrect" } },
            { assertEquals(0, exporter.lostTime) { "Lost time incorrect" } },
            { assertEquals(2559005056, exporter.uptime) { "Uptime incorrect" } }
        )
    }

    /**
     * Obtain the trace reader for the test.
     */
    private fun createTestWorkload(fraction: Double, seed: Int = 0): List<VirtualMachine> {
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

    class TestComputeMetricExporter : ComputeMetricExporter() {
        var idleTime = 0L
        var activeTime = 0L
        var stealTime = 0L
        var lostTime = 0L
        var energyUsage = 0.0
        var uptime = 0L

        override fun record(data: HostData) {
            idleTime += data.cpuIdleTime
            activeTime += data.cpuActiveTime
            stealTime += data.cpuStealTime
            lostTime += data.cpuLostTime
            energyUsage += data.powerTotal
            uptime += data.uptime
        }
    }
}
