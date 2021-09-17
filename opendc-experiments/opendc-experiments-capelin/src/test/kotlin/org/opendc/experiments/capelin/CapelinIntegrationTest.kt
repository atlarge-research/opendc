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
import org.opendc.compute.workload.ComputeWorkloadRunner
import org.opendc.compute.workload.grid5000
import org.opendc.compute.workload.trace.RawParquetTraceReader
import org.opendc.compute.workload.trace.TraceReader
import org.opendc.compute.workload.util.PerformanceInterferenceReader
import org.opendc.experiments.capelin.env.ClusterEnvironmentReader
import org.opendc.experiments.capelin.env.EnvironmentReader
import org.opendc.experiments.capelin.model.Workload
import org.opendc.experiments.capelin.trace.ParquetTraceReader
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.telemetry.compute.ComputeMetricExporter
import org.opendc.telemetry.compute.ComputeMonitor
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
    private lateinit var monitor: TestExperimentReporter

    /**
     * The [FilterScheduler] to use for all experiments.
     */
    private lateinit var computeScheduler: FilterScheduler

    /**
     * Setup the experimental environment.
     */
    @BeforeEach
    fun setUp() {
        monitor = TestExperimentReporter()
        computeScheduler = FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(16.0), RamFilter(1.0)),
            weighers = listOf(CoreRamWeigher(multiplier = 1.0))
        )
    }

    /**
     * Test a large simulation setup.
     */
    @Test
    fun testLarge() = runBlockingSimulation {
        val traceReader = createTestTraceReader()
        val environmentReader = createTestEnvironmentReader()

        val simulator = ComputeWorkloadRunner(
            coroutineContext,
            clock,
            computeScheduler,
            environmentReader.read(),
        )

        val metricReader = CoroutineMetricReader(this, simulator.producers, ComputeMetricExporter(clock, monitor))

        try {
            simulator.run(traceReader)
        } finally {
            simulator.close()
            metricReader.close()
        }

        val serviceMetrics = collectServiceMetrics(clock.instant(), simulator.producers[0])
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
            { assertEquals(223856043, monitor.idleTime) { "Incorrect idle time" } },
            { assertEquals(66481557, monitor.activeTime) { "Incorrect active time" } },
            { assertEquals(360441, monitor.stealTime) { "Incorrect steal time" } },
            { assertEquals(0, monitor.lostTime) { "Incorrect lost time" } },
            { assertEquals(5.418336360461193E9, monitor.energyUsage, 0.01) { "Incorrect power draw" } },
        )
    }

    /**
     * Test a small simulation setup.
     */
    @Test
    fun testSmall() = runBlockingSimulation {
        val seed = 1
        val traceReader = createTestTraceReader(0.25, seed)
        val environmentReader = createTestEnvironmentReader("single")

        val simulator = ComputeWorkloadRunner(
            coroutineContext,
            clock,
            computeScheduler,
            environmentReader.read(),
        )

        val metricReader = CoroutineMetricReader(this, simulator.producers, ComputeMetricExporter(clock, monitor))

        try {
            simulator.run(traceReader)
        } finally {
            simulator.close()
            metricReader.close()
        }

        val serviceMetrics = collectServiceMetrics(clock.instant(), simulator.producers[0])
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
            { assertEquals(9597804, monitor.idleTime) { "Idle time incorrect" } },
            { assertEquals(11140596, monitor.activeTime) { "Active time incorrect" } },
            { assertEquals(326138, monitor.stealTime) { "Steal time incorrect" } },
            { assertEquals(0, monitor.lostTime) { "Lost time incorrect" } }
        )
    }

    /**
     * Test a small simulation setup with interference.
     */
    @Test
    fun testInterference() = runBlockingSimulation {
        val seed = 1
        val traceReader = createTestTraceReader(0.25, seed)
        val environmentReader = createTestEnvironmentReader("single")

        val perfInterferenceInput = checkNotNull(CapelinIntegrationTest::class.java.getResourceAsStream("/bitbrains-perf-interference.json"))
        val performanceInterferenceModel =
            PerformanceInterferenceReader()
                .read(perfInterferenceInput)
                .let { VmInterferenceModel(it, Random(seed.toLong())) }

        val simulator = ComputeWorkloadRunner(
            coroutineContext,
            clock,
            computeScheduler,
            environmentReader.read(),
            interferenceModel = performanceInterferenceModel
        )

        val metricReader = CoroutineMetricReader(this, simulator.producers, ComputeMetricExporter(clock, monitor))

        try {
            simulator.run(traceReader)
        } finally {
            simulator.close()
            metricReader.close()
        }

        val serviceMetrics = collectServiceMetrics(clock.instant(), simulator.producers[0])
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
            { assertEquals(9597804, monitor.idleTime) { "Idle time incorrect" } },
            { assertEquals(11140596, monitor.activeTime) { "Active time incorrect" } },
            { assertEquals(326138, monitor.stealTime) { "Steal time incorrect" } },
            { assertEquals(925305, monitor.lostTime) { "Lost time incorrect" } }
        )
    }

    /**
     * Test a small simulation setup with failures.
     */
    @Test
    fun testFailures() = runBlockingSimulation {
        val seed = 1
        val traceReader = createTestTraceReader(0.25, seed)
        val environmentReader = createTestEnvironmentReader("single")

        val simulator = ComputeWorkloadRunner(
            coroutineContext,
            clock,
            computeScheduler,
            environmentReader.read(),
            grid5000(Duration.ofDays(7), seed)
        )

        val metricReader = CoroutineMetricReader(this, simulator.producers, ComputeMetricExporter(clock, monitor))

        try {
            simulator.run(traceReader)
        } finally {
            simulator.close()
            metricReader.close()
        }

        val serviceMetrics = collectServiceMetrics(clock.instant(), simulator.producers[0])
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
            { assertEquals(9836315, monitor.idleTime) { "Idle time incorrect" } },
            { assertEquals(10902085, monitor.activeTime) { "Active time incorrect" } },
            { assertEquals(306249, monitor.stealTime) { "Steal time incorrect" } },
            { assertEquals(0, monitor.lostTime) { "Lost time incorrect" } },
            { assertEquals(2540877457, monitor.uptime) { "Uptime incorrect" } }
        )
    }

    /**
     * Obtain the trace reader for the test.
     */
    private fun createTestTraceReader(fraction: Double = 1.0, seed: Int = 0): TraceReader<SimWorkload> {
        return ParquetTraceReader(
            listOf(RawParquetTraceReader(File("src/test/resources/trace"))),
            Workload("test", fraction),
            seed
        )
    }

    /**
     * Obtain the environment reader for the test.
     */
    private fun createTestEnvironmentReader(name: String = "topology"): EnvironmentReader {
        val stream = checkNotNull(object {}.javaClass.getResourceAsStream("/env/$name.txt"))
        return ClusterEnvironmentReader(stream)
    }

    class TestExperimentReporter : ComputeMonitor {
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
