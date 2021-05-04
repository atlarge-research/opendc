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
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeCapabilitiesFilter
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.weights.CoreMemoryWeigher
import org.opendc.experiments.capelin.model.Workload
import org.opendc.experiments.capelin.monitor.ExperimentMonitor
import org.opendc.experiments.capelin.trace.Sc20ParquetTraceReader
import org.opendc.experiments.capelin.trace.Sc20RawParquetTraceReader
import org.opendc.format.environment.EnvironmentReader
import org.opendc.format.environment.sc20.Sc20ClusterEnvironmentReader
import org.opendc.format.trace.TraceReader
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.core.runBlockingSimulation
import java.io.File

/**
 * An integration test suite for the SC20 experiments.
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

    @Test
    fun testLarge() = runBlockingSimulation {
        val failures = false
        val seed = 0
        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = FilterScheduler(
            filters = listOf(ComputeFilter(), ComputeCapabilitiesFilter()),
            weighers = listOf(CoreMemoryWeigher() to -1.0)
        )
        val traceReader = createTestTraceReader()
        val environmentReader = createTestEnvironmentReader()
        lateinit var monitorResults: ComputeMetrics

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

            withMonitor(monitor, clock, meterProvider as MetricProducer, scheduler) {
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

        monitorResults = collectMetrics(meterProvider as MetricProducer)
        println("Finish SUBMIT=${monitorResults.submittedVms} FAIL=${monitorResults.unscheduledVms} QUEUE=${monitorResults.queuedVms} RUNNING=${monitorResults.runningVms}")

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(50, monitorResults.submittedVms, "The trace contains 50 VMs") },
            { assertEquals(0, monitorResults.runningVms, "All VMs should finish after a run") },
            { assertEquals(0, monitorResults.unscheduledVms, "No VM should not be unscheduled") },
            { assertEquals(0, monitorResults.queuedVms, "No VM should not be in the queue") },
            { assertEquals(207389912923, monitor.totalRequestedBurst) { "Incorrect requested burst" } },
            { assertEquals(207122087280, monitor.totalGrantedBurst) { "Incorrect granted burst" } },
            { assertEquals(267825640, monitor.totalOvercommissionedBurst) { "Incorrect overcommitted burst" } },
            { assertEquals(0, monitor.totalInterferedBurst) { "Incorrect interfered burst" } }
        )
    }

    @Test
    fun testSmall() = runBlockingSimulation {
        val seed = 1
        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = FilterScheduler(
            filters = listOf(ComputeFilter(), ComputeCapabilitiesFilter()),
            weighers = listOf(CoreMemoryWeigher() to -1.0)
        )
        val traceReader = createTestTraceReader(0.5, seed)
        val environmentReader = createTestEnvironmentReader("single")

        val meterProvider = createMeterProvider(clock)

        withComputeService(clock, meterProvider, environmentReader, allocationPolicy) { scheduler ->
            withMonitor(monitor, clock, meterProvider as MetricProducer, scheduler) {
                processTrace(
                    clock,
                    traceReader,
                    scheduler,
                    chan,
                    monitor
                )
            }
        }

        val metrics = collectMetrics(meterProvider as MetricProducer)
        println("Finish SUBMIT=${metrics.submittedVms} FAIL=${metrics.unscheduledVms} QUEUE=${metrics.queuedVms} RUNNING=${metrics.runningVms}")

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(96350072517, monitor.totalRequestedBurst) { "Total requested work incorrect" } },
            { assertEquals(96330335057, monitor.totalGrantedBurst) { "Total granted work incorrect" } },
            { assertEquals(19737460, monitor.totalOvercommissionedBurst) { "Total overcommitted work incorrect" } },
            { assertEquals(0, monitor.totalInterferedBurst) { "Total interfered work incorrect" } }
        )
    }

    /**
     * Obtain the trace reader for the test.
     */
    private fun createTestTraceReader(fraction: Double = 1.0, seed: Int = 0): TraceReader<SimWorkload> {
        return Sc20ParquetTraceReader(
            listOf(Sc20RawParquetTraceReader(File("src/test/resources/trace"))),
            emptyMap(),
            Workload("test", fraction),
            seed
        )
    }

    /**
     * Obtain the environment reader for the test.
     */
    private fun createTestEnvironmentReader(name: String = "topology"): EnvironmentReader {
        val stream = object {}.javaClass.getResourceAsStream("/env/$name.txt")
        return Sc20ClusterEnvironmentReader(stream)
    }

    class TestExperimentReporter : ExperimentMonitor {
        var totalRequestedBurst = 0L
        var totalGrantedBurst = 0L
        var totalOvercommissionedBurst = 0L
        var totalInterferedBurst = 0L

        override fun reportHostSlice(
            time: Long,
            requestedBurst: Long,
            grantedBurst: Long,
            overcommissionedBurst: Long,
            interferedBurst: Long,
            cpuUsage: Double,
            cpuDemand: Double,
            powerDraw: Double,
            numberOfDeployedImages: Int,
            host: Host,
        ) {
            totalRequestedBurst += requestedBurst
            totalGrantedBurst += grantedBurst
            totalOvercommissionedBurst += overcommissionedBurst
            totalInterferedBurst += interferedBurst
        }

        override fun close() {}
    }
}
