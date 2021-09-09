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

import io.opentelemetry.sdk.metrics.export.MetricProducer
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
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
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.telemetry.compute.ComputeMonitor
import org.opendc.telemetry.compute.collectServiceMetrics
import org.opendc.telemetry.compute.table.HostData
import org.opendc.telemetry.compute.withMonitor
import java.io.File
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
     * Setup the experimental environment.
     */
    @BeforeEach
    fun setUp() {
        monitor = TestExperimentReporter()
    }

    /**
     * Test a large simulation setup.
     */
    @Test
    fun testLarge() = runBlockingSimulation {
        val failures = false
        val seed = 0
        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(16.0), RamFilter(1.0)),
            weighers = listOf(CoreRamWeigher(multiplier = 1.0))
        )
        val traceReader = createTestTraceReader()
        val environmentReader = createTestEnvironmentReader()

        val meterProvider = createMeterProvider(clock)
        withComputeService(clock, meterProvider, environmentReader, allocationPolicy) { scheduler ->
            val failureDomain = if (failures) {
                println("ENABLING failures")
                createFailureDomain(
                    this,
                    clock,
                    seed,
                    24.0 * 7,
                    scheduler,
                    chan
                )
            } else {
                null
            }

            withMonitor(scheduler, clock, meterProvider as MetricProducer, monitor) {
                processTrace(
                    clock,
                    traceReader,
                    scheduler,
                    chan,
                    monitor
                )
            }

            failureDomain?.cancel()
        }

        val serviceMetrics = collectServiceMetrics(clock.millis(), meterProvider as MetricProducer)
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
            { assertEquals(220346369753, monitor.totalWork) { "Incorrect requested burst" } },
            { assertEquals(206667809529, monitor.totalGrantedWork) { "Incorrect granted burst" } },
            { assertEquals(1151611104, monitor.totalOvercommittedWork) { "Incorrect overcommitted burst" } },
            { assertEquals(0, monitor.totalInterferedWork) { "Incorrect interfered burst" } },
            { assertEquals(1.7671768767192196E7, monitor.totalPowerDraw, 0.01) { "Incorrect power draw" } },
        )
    }

    /**
     * Test a small simulation setup.
     */
    @Test
    fun testSmall() = runBlockingSimulation {
        val seed = 1
        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(16.0), RamFilter(1.0)),
            weighers = listOf(CoreRamWeigher(multiplier = 1.0))
        )
        val traceReader = createTestTraceReader(0.25, seed)
        val environmentReader = createTestEnvironmentReader("single")

        val meterProvider = createMeterProvider(clock)

        withComputeService(clock, meterProvider, environmentReader, allocationPolicy) { scheduler ->
            withMonitor(scheduler, clock, meterProvider as MetricProducer, monitor) {
                processTrace(
                    clock,
                    traceReader,
                    scheduler,
                    chan,
                    monitor
                )
            }
        }

        val serviceMetrics = collectServiceMetrics(clock.millis(), meterProvider as MetricProducer)
        println(
            "Finish " +
                "SUBMIT=${serviceMetrics.instanceCount} " +
                "FAIL=${serviceMetrics.failedInstanceCount} " +
                "QUEUE=${serviceMetrics.queuedInstanceCount} " +
                "RUNNING=${serviceMetrics.runningInstanceCount}"
        )

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(39183961335, monitor.totalWork) { "Total requested work incorrect" } },
            { assertEquals(35649903197, monitor.totalGrantedWork) { "Total granted work incorrect" } },
            { assertEquals(1043641877, monitor.totalOvercommittedWork) { "Total overcommitted work incorrect" } },
            { assertEquals(0, monitor.totalInterferedWork) { "Total interfered work incorrect" } }
        )
    }

    /**
     * Test a small simulation setup with interference.
     */
    @Test
    fun testInterference() = runBlockingSimulation {
        val seed = 1
        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(16.0), RamFilter(1.0)),
            weighers = listOf(CoreRamWeigher(multiplier = 1.0))
        )
        val traceReader = createTestTraceReader(0.25, seed)
        val environmentReader = createTestEnvironmentReader("single")

        val perfInterferenceInput = checkNotNull(CapelinIntegrationTest::class.java.getResourceAsStream("/bitbrains-perf-interference.json"))
        val performanceInterferenceModel =
            PerformanceInterferenceReader()
                .read(perfInterferenceInput)
                .let { VmInterferenceModel(it, Random(seed.toLong())) }

        val meterProvider = createMeterProvider(clock)

        withComputeService(clock, meterProvider, environmentReader, allocationPolicy, performanceInterferenceModel) { scheduler ->
            withMonitor(scheduler, clock, meterProvider as MetricProducer, monitor) {
                processTrace(
                    clock,
                    traceReader,
                    scheduler,
                    chan,
                    monitor
                )
            }
        }

        val serviceMetrics = collectServiceMetrics(clock.millis(), meterProvider as MetricProducer)
        println(
            "Finish " +
                "SUBMIT=${serviceMetrics.instanceCount} " +
                "FAIL=${serviceMetrics.failedInstanceCount} " +
                "QUEUE=${serviceMetrics.queuedInstanceCount} " +
                "RUNNING=${serviceMetrics.runningInstanceCount}"
        )

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(39183961335, monitor.totalWork) { "Total requested work incorrect" } },
            { assertEquals(35649903197, monitor.totalGrantedWork) { "Total granted work incorrect" } },
            { assertEquals(1043641877, monitor.totalOvercommittedWork) { "Total overcommitted work incorrect" } },
            { assertEquals(2960970230, monitor.totalInterferedWork) { "Total interfered work incorrect" } }
        )
    }

    /**
     * Test a small simulation setup with failures.
     */
    @Test
    fun testFailures() = runBlockingSimulation {
        val seed = 1
        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(16.0), RamFilter(1.0)),
            weighers = listOf(CoreRamWeigher(multiplier = 1.0))
        )
        val traceReader = createTestTraceReader(0.25, seed)
        val environmentReader = createTestEnvironmentReader("single")

        val meterProvider = createMeterProvider(clock)

        withComputeService(clock, meterProvider, environmentReader, allocationPolicy) { scheduler ->
            val failureDomain =
                createFailureDomain(
                    this,
                    clock,
                    seed,
                    24.0 * 7,
                    scheduler,
                    chan
                )

            withMonitor(scheduler, clock, meterProvider as MetricProducer, monitor) {
                processTrace(
                    clock,
                    traceReader,
                    scheduler,
                    chan,
                    monitor
                )
            }

            failureDomain.cancel()
        }

        val serviceMetrics = collectServiceMetrics(clock.millis(), meterProvider as MetricProducer)
        println(
            "Finish " +
                "SUBMIT=${serviceMetrics.instanceCount} " +
                "FAIL=${serviceMetrics.failedInstanceCount} " +
                "QUEUE=${serviceMetrics.queuedInstanceCount} " +
                "RUNNING=${serviceMetrics.runningInstanceCount}"
        )

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(38385852453, monitor.totalWork) { "Total requested work incorrect" } },
            { assertEquals(34886665781, monitor.totalGrantedWork) { "Total granted work incorrect" } },
            { assertEquals(979997253, monitor.totalOvercommittedWork) { "Total overcommitted work incorrect" } },
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
            this.totalWork += data.totalWork.toLong()
            totalGrantedWork += data.grantedWork.toLong()
            totalOvercommittedWork += data.overcommittedWork.toLong()
            totalInterferedWork += data.interferedWork.toLong()
            totalPowerDraw += data.powerDraw
        }
    }
}
