/*
 * Copyright (c) 2021 AtLarge Research
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

import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.MetricProducer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runBlockingTest
import mu.KotlinLogging
import org.opendc.compute.service.scheduler.*
import org.opendc.experiments.capelin.model.CompositeWorkload
import org.opendc.experiments.capelin.model.OperationalPhenomena
import org.opendc.experiments.capelin.model.Topology
import org.opendc.experiments.capelin.model.Workload
import org.opendc.experiments.capelin.monitor.ParquetExperimentMonitor
import org.opendc.experiments.capelin.trace.Sc20ParquetTraceReader
import org.opendc.experiments.capelin.trace.Sc20RawParquetTraceReader
import org.opendc.format.environment.sc20.Sc20ClusterEnvironmentReader
import org.opendc.format.trace.PerformanceInterferenceModelReader
import org.opendc.harness.dsl.Experiment
import org.opendc.harness.dsl.anyOf
import org.opendc.simulator.utils.DelayControllerClockAdapter
import org.opendc.telemetry.sdk.toOtelClock
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * A portfolio represents a collection of scenarios are tested for the work.
 *
 * @param name The name of the portfolio.
 */
public abstract class Portfolio(name: String) : Experiment(name) {
    /**
     * The logger for this portfolio instance.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The path to where the environments are located.
     */
    private val environmentPath by anyOf(File("input/environments/"))

    /**
     * The path to where the traces are located.
     */
    private val tracePath by anyOf(File("input/traces/"))

    /**
     * The path to where the output results should be written.
     */
    private val outputPath by anyOf(File("output/"))

    /**
     * The path to the original VM placements file.
     */
    private val vmPlacements by anyOf(emptyMap<String, String>())

    /**
     * The path to the performance interference model.
     */
    private val performanceInterferenceModel by anyOf<PerformanceInterferenceModelReader?>(null)

    /**
     * The topology to test.
     */
    public abstract val topology: Topology

    /**
     * The workload to test.
     */
    public abstract val workload: Workload

    /**
     * The operational phenomenas to consider.
     */
    public abstract val operationalPhenomena: OperationalPhenomena

    /**
     * The allocation policies to consider.
     */
    public abstract val allocationPolicy: String

    /**
     * A map of trace readers.
     */
    private val traceReaders = ConcurrentHashMap<String, Sc20RawParquetTraceReader>()

    /**
     * Perform a single trial for this portfolio.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun doRun(repeat: Int): Unit = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val seeder = Random(repeat)
        val environment = Sc20ClusterEnvironmentReader(File(environmentPath, "${topology.name}.txt"))

        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = createAllocationPolicy(seeder)

        val meterProvider: MeterProvider = SdkMeterProvider
            .builder()
            .setClock(clock.toOtelClock())
            .build()

        val workload = workload
        val workloadNames = if (workload is CompositeWorkload) {
            workload.workloads.map { it.name }
        } else {
            listOf(workload.name)
        }

        val rawReaders = workloadNames.map { workloadName ->
            traceReaders.computeIfAbsent(workloadName) {
                logger.info { "Loading trace $workloadName" }
                Sc20RawParquetTraceReader(File(tracePath, workloadName))
            }
        }

        val performanceInterferenceModel = performanceInterferenceModel
            ?.takeIf { operationalPhenomena.hasInterference }
            ?.construct(seeder) ?: emptyMap()
        val trace = Sc20ParquetTraceReader(rawReaders, performanceInterferenceModel, workload, seeder.nextInt())

        val monitor = ParquetExperimentMonitor(
            outputPath,
            "portfolio_id=$name/scenario_id=$id/run_id=$repeat",
            4096
        )

        withComputeService(clock, meterProvider, environment, allocationPolicy) { scheduler ->
            val failureDomain = if (operationalPhenomena.failureFrequency > 0) {
                logger.debug("ENABLING failures")
                createFailureDomain(
                    this,
                    clock,
                    seeder.nextInt(),
                    operationalPhenomena.failureFrequency,
                    scheduler,
                    chan
                )
            } else {
                null
            }

            withMonitor(monitor, clock, meterProvider as MetricProducer, scheduler) {
                processTrace(
                    clock,
                    trace,
                    scheduler,
                    chan,
                    monitor
                )
            }

            failureDomain?.cancel()
        }

        val monitorResults = collectMetrics(meterProvider as MetricProducer)
        logger.debug { "Finish SUBMIT=${monitorResults.submittedVms} FAIL=${monitorResults.unscheduledVms} QUEUE=${monitorResults.queuedVms} RUNNING=${monitorResults.runningVms}" }
    }

    /**
     * Create the [AllocationPolicy] instance to use for the trial.
     */
    private fun createAllocationPolicy(seeder: Random): AllocationPolicy {
        return when (allocationPolicy) {
            "mem" -> AvailableMemoryAllocationPolicy()
            "mem-inv" -> AvailableMemoryAllocationPolicy(true)
            "core-mem" -> AvailableCoreMemoryAllocationPolicy()
            "core-mem-inv" -> AvailableCoreMemoryAllocationPolicy(true)
            "active-servers" -> NumberOfActiveServersAllocationPolicy()
            "active-servers-inv" -> NumberOfActiveServersAllocationPolicy(true)
            "provisioned-cores" -> ProvisionedCoresAllocationPolicy()
            "provisioned-cores-inv" -> ProvisionedCoresAllocationPolicy(true)
            "random" -> RandomAllocationPolicy(Random(seeder.nextInt()))
            "replay" -> ReplayAllocationPolicy(vmPlacements)
            else -> throw IllegalArgumentException("Unknown policy $allocationPolicy")
        }
    }
}
