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
import org.opendc.experiments.capelin.env.ClusterEnvironmentReader
import org.opendc.experiments.capelin.env.EnvironmentReader
import org.opendc.experiments.capelin.model.Workload
import org.opendc.experiments.capelin.trace.ParquetTraceReader
import org.opendc.experiments.capelin.trace.PerformanceInterferenceReader
import org.opendc.experiments.capelin.trace.RawParquetTraceReader
import org.opendc.experiments.capelin.trace.TraceReader
import org.opendc.experiments.capelin.util.ComputeServiceSimulator
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
import kotlin.math.roundToLong

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

        val simulator = ComputeServiceSimulator(
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

        val serviceMetrics = collectServiceMetrics(clock.millis(), simulator.producers[0])
        println(
            "Finish " +
                "SUBMIT=${serviceMetrics.instanceCount} " +
                "FAIL=${serviceMetrics.failedInstanceCount} " +
                "QUEUE=${serviceMetrics.queuedInstanceCount} " +
                "RUNNING=${serviceMetrics.runningInstanceCount}"
        )

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(50, serviceMetrics.instanceCount, "The trace contains 50 VMs") },
            { assertEquals(0, serviceMetrics.runningInstanceCount, "All VMs should finish after a run") },
            { assertEquals(0, serviceMetrics.failedInstanceCount, "No VM should not be unscheduled") },
            { assertEquals(0, serviceMetrics.queuedInstanceCount, "No VM should not be in the queue") },
            { assertEquals(220346412191, monitor.totalWork) { "Incorrect requested burst" } },
            { assertEquals(206667852689, monitor.totalGrantedWork) { "Incorrect granted burst" } },
            { assertEquals(1151612221, monitor.totalOvercommittedWork) { "Incorrect overcommitted burst" } },
            { assertEquals(0, monitor.totalInterferedWork) { "Incorrect interfered burst" } },
            { assertEquals(9.088769763540529E7, monitor.totalPowerDraw, 0.01) { "Incorrect power draw" } },
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

        val simulator = ComputeServiceSimulator(
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

        val serviceMetrics = collectServiceMetrics(clock.millis(), simulator.producers[0])
        println(
            "Finish " +
                "SUBMIT=${serviceMetrics.instanceCount} " +
                "FAIL=${serviceMetrics.failedInstanceCount} " +
                "QUEUE=${serviceMetrics.queuedInstanceCount} " +
                "RUNNING=${serviceMetrics.runningInstanceCount}"
        )

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(39183965664, monitor.totalWork) { "Total work incorrect" } },
            { assertEquals(35649907631, monitor.totalGrantedWork) { "Total granted work incorrect" } },
            { assertEquals(1043642275, monitor.totalOvercommittedWork) { "Total overcommitted work incorrect" } },
            { assertEquals(0, monitor.totalInterferedWork) { "Total interfered work incorrect" } }
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

        val simulator = ComputeServiceSimulator(
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

        val serviceMetrics = collectServiceMetrics(clock.millis(), simulator.producers[0])
        println(
            "Finish " +
                "SUBMIT=${serviceMetrics.instanceCount} " +
                "FAIL=${serviceMetrics.failedInstanceCount} " +
                "QUEUE=${serviceMetrics.queuedInstanceCount} " +
                "RUNNING=${serviceMetrics.runningInstanceCount}"
        )

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(39183965664, monitor.totalWork) { "Total work incorrect" } },
            { assertEquals(35649907631, monitor.totalGrantedWork) { "Total granted work incorrect" } },
            { assertEquals(1043642275, monitor.totalOvercommittedWork) { "Total overcommitted work incorrect" } },
            { assertEquals(2960974524, monitor.totalInterferedWork) { "Total interfered work incorrect" } }
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

        val simulator = ComputeServiceSimulator(
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

        val serviceMetrics = collectServiceMetrics(clock.millis(), simulator.producers[0])
        println(
            "Finish " +
                "SUBMIT=${serviceMetrics.instanceCount} " +
                "FAIL=${serviceMetrics.failedInstanceCount} " +
                "QUEUE=${serviceMetrics.queuedInstanceCount} " +
                "RUNNING=${serviceMetrics.runningInstanceCount}"
        )

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(38385856700, monitor.totalWork) { "Total requested work incorrect" } },
            { assertEquals(34886670127, monitor.totalGrantedWork) { "Total granted work incorrect" } },
            { assertEquals(979997628, monitor.totalOvercommittedWork) { "Total overcommitted work incorrect" } },
            { assertEquals(0, monitor.totalInterferedWork) { "Total interfered work incorrect" } }
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
        var totalWork = 0L
        var totalGrantedWork = 0L
        var totalOvercommittedWork = 0L
        var totalInterferedWork = 0L
        var totalPowerDraw = 0.0

        override fun record(data: HostData) {
            this.totalWork += data.totalWork.roundToLong()
            totalGrantedWork += data.grantedWork.roundToLong()
            totalOvercommittedWork += data.overcommittedWork.roundToLong()
            totalInterferedWork += data.interferedWork.roundToLong()
            totalPowerDraw += data.powerDraw
        }
    }
}
