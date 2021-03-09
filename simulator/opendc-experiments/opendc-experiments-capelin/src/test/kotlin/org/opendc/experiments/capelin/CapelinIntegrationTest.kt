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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.internal.ComputeServiceImpl
import org.opendc.compute.service.scheduler.AvailableCoreMemoryAllocationPolicy
import org.opendc.experiments.capelin.model.Workload
import org.opendc.experiments.capelin.monitor.ExperimentMonitor
import org.opendc.experiments.capelin.trace.Sc20ParquetTraceReader
import org.opendc.experiments.capelin.trace.Sc20RawParquetTraceReader
import org.opendc.format.environment.EnvironmentReader
import org.opendc.format.environment.sc20.Sc20ClusterEnvironmentReader
import org.opendc.format.trace.TraceReader
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.utils.DelayControllerClockAdapter
import org.opendc.trace.core.EventTracer
import java.io.File
import java.time.Clock

/**
 * An integration test suite for the SC20 experiments.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CapelinIntegrationTest {
    /**
     * The [TestCoroutineScope] to use.
     */
    private lateinit var testScope: TestCoroutineScope

    /**
     * The simulation clock to use.
     */
    private lateinit var clock: Clock

    /**
     * The monitor used to keep track of the metrics.
     */
    private lateinit var monitor: TestExperimentReporter

    /**
     * Setup the experimental environment.
     */
    @BeforeEach
    fun setUp() {
        testScope = TestCoroutineScope()
        clock = DelayControllerClockAdapter(testScope)

        monitor = TestExperimentReporter()
    }

    /**
     * Tear down the experimental environment.
     */
    @AfterEach
    fun tearDown() = testScope.cleanupTestCoroutines()

    @Test
    fun testLarge() {
        val failures = false
        val seed = 0
        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = AvailableCoreMemoryAllocationPolicy()
        val traceReader = createTestTraceReader()
        val environmentReader = createTestEnvironmentReader()
        lateinit var scheduler: ComputeServiceImpl
        val tracer = EventTracer(clock)

        testScope.launch {
            scheduler = createComputeService(
                this,
                clock,
                environmentReader,
                allocationPolicy,
                tracer
            )

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

            attachMonitor(this, clock, scheduler, monitor)
            processTrace(
                this,
                clock,
                traceReader,
                scheduler,
                chan,
                monitor
            )

            println("Finish SUBMIT=${scheduler.submittedVms} FAIL=${scheduler.unscheduledVms} QUEUE=${scheduler.queuedVms} RUNNING=${scheduler.runningVms} FINISH=${scheduler.finishedVms}")

            failureDomain?.cancel()
            scheduler.close()
            monitor.close()
        }

        runSimulation()

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(50, scheduler.submittedVms, "The trace contains 50 VMs") },
            { assertEquals(50, scheduler.finishedVms, "All VMs should finish after a run") },
            { assertEquals(1678587333640, monitor.totalRequestedBurst) },
            { assertEquals(438118200924, monitor.totalGrantedBurst) },
            { assertEquals(1220323969993, monitor.totalOvercommissionedBurst) },
            { assertEquals(0, monitor.totalInterferedBurst) }
        )
    }

    @Test
    fun testSmall() {
        val seed = 1
        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = AvailableCoreMemoryAllocationPolicy()
        val traceReader = createTestTraceReader(0.5, seed)
        val environmentReader = createTestEnvironmentReader("single")
        val tracer = EventTracer(clock)

        testScope.launch {
            val scheduler = createComputeService(
                this,
                clock,
                environmentReader,
                allocationPolicy,
                tracer
            )
            attachMonitor(this, clock, scheduler, monitor)
            processTrace(
                this,
                clock,
                traceReader,
                scheduler,
                chan,
                monitor
            )

            println("Finish SUBMIT=${scheduler.submittedVms} FAIL=${scheduler.unscheduledVms} QUEUE=${scheduler.queuedVms} RUNNING=${scheduler.runningVms} FINISH=${scheduler.finishedVms}")

            scheduler.close()
            monitor.close()
        }

        runSimulation()

        // Note that these values have been verified beforehand
        assertAll(
            { assertEquals(705128393966, monitor.totalRequestedBurst) { "Total requested work incorrect" } },
            { assertEquals(173489747029, monitor.totalGrantedBurst) { "Total granted work incorrect" } },
            { assertEquals(526858997740, monitor.totalOvercommissionedBurst) { "Total overcommitted work incorrect" } },
            { assertEquals(0, monitor.totalInterferedBurst) { "Total interfered work incorrect" } }
        )
    }

    /**
     * Run the simulation.
     */
    private fun runSimulation() = testScope.advanceUntilIdle()

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
            numberOfDeployedImages: Int,
            host: Host,
            duration: Long
        ) {
            totalRequestedBurst += requestedBurst
            totalGrantedBurst += grantedBurst
            totalOvercommissionedBurst += overcommissionedBurst
            totalInterferedBurst += interferedBurst
        }

        override fun close() {}
    }
}
