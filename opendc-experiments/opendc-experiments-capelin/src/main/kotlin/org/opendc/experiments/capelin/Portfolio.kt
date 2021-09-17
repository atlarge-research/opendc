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

import com.typesafe.config.ConfigFactory
import mu.KotlinLogging
import org.opendc.experiments.capelin.env.ClusterEnvironmentReader
import org.opendc.experiments.capelin.export.parquet.ParquetExportMonitor
import org.opendc.experiments.capelin.model.CompositeWorkload
import org.opendc.experiments.capelin.model.OperationalPhenomena
import org.opendc.experiments.capelin.model.Topology
import org.opendc.experiments.capelin.model.Workload
import org.opendc.experiments.capelin.trace.ParquetTraceReader
import org.opendc.experiments.capelin.trace.PerformanceInterferenceReader
import org.opendc.experiments.capelin.trace.RawParquetTraceReader
import org.opendc.experiments.capelin.util.ComputeServiceSimulator
import org.opendc.experiments.capelin.util.createComputeScheduler
import org.opendc.harness.dsl.Experiment
import org.opendc.harness.dsl.anyOf
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.telemetry.compute.ComputeMetricExporter
import org.opendc.telemetry.compute.collectServiceMetrics
import org.opendc.telemetry.sdk.metrics.export.CoroutineMetricReader
import java.io.File
import java.io.FileInputStream
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/**
 * A portfolio represents a collection of scenarios are tested for the work.
 *
 * @param name The name of the portfolio.
 */
abstract class Portfolio(name: String) : Experiment(name) {
    /**
     * The logger for this portfolio instance.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The configuration to use.
     */
    private val config = ConfigFactory.load().getConfig("opendc.experiments.capelin")

    /**
     * The path to the original VM placements file.
     */
    private val vmPlacements by anyOf(emptyMap<String, String>())

    /**
     * The topology to test.
     */
    abstract val topology: Topology

    /**
     * The workload to test.
     */
    abstract val workload: Workload

    /**
     * The operational phenomenas to consider.
     */
    abstract val operationalPhenomena: OperationalPhenomena

    /**
     * The allocation policies to consider.
     */
    abstract val allocationPolicy: String

    /**
     * A map of trace readers.
     */
    private val traceReaders = ConcurrentHashMap<String, RawParquetTraceReader>()

    /**
     * Perform a single trial for this portfolio.
     */
    override fun doRun(repeat: Int): Unit = runBlockingSimulation {
        val seeder = Random(repeat.toLong())
        val environment = ClusterEnvironmentReader(File(config.getString("env-path"), "${topology.name}.txt"))

        val workload = workload
        val workloadNames = if (workload is CompositeWorkload) {
            workload.workloads.map { it.name }
        } else {
            listOf(workload.name)
        }
        val rawReaders = workloadNames.map { workloadName ->
            traceReaders.computeIfAbsent(workloadName) {
                logger.info { "Loading trace $workloadName" }
                RawParquetTraceReader(File(config.getString("trace-path"), workloadName))
            }
        }
        val trace = ParquetTraceReader(rawReaders, workload, seeder.nextInt())
        val performanceInterferenceModel = if (operationalPhenomena.hasInterference)
            PerformanceInterferenceReader()
                .read(FileInputStream(config.getString("interference-model")))
                .let { VmInterferenceModel(it, Random(seeder.nextLong())) }
        else
            null

        val computeScheduler = createComputeScheduler(allocationPolicy, seeder, vmPlacements)
        val failureModel =
            if (operationalPhenomena.failureFrequency > 0)
                grid5000(Duration.ofSeconds((operationalPhenomena.failureFrequency * 60).roundToLong()), seeder.nextInt())
            else
                null
        val simulator = ComputeServiceSimulator(
            coroutineContext,
            clock,
            computeScheduler,
            environment.read(),
            failureModel,
            performanceInterferenceModel
        )

        val monitor = ParquetExportMonitor(
            File(config.getString("output-path")),
            "portfolio_id=$name/scenario_id=$id/run_id=$repeat",
            4096
        )
        val metricReader = CoroutineMetricReader(this, simulator.producers, ComputeMetricExporter(clock, monitor))

        try {
            simulator.run(trace)
        } finally {
            simulator.close()
            metricReader.close()
            monitor.close()
        }

        val monitorResults = collectServiceMetrics(clock.instant(), simulator.producers[0])
        logger.debug {
            "Scheduler " +
                "Success=${monitorResults.attemptsSuccess} " +
                "Failure=${monitorResults.attemptsFailure} " +
                "Error=${monitorResults.attemptsError} " +
                "Pending=${monitorResults.serversPending} " +
                "Active=${monitorResults.serversActive}"
        }
    }
}
