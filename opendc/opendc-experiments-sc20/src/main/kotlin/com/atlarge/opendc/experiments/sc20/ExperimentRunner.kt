/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.experiments.sc20

import com.atlarge.opendc.compute.core.workload.PerformanceInterferenceModel
import com.atlarge.opendc.compute.core.workload.VmWorkload
import com.atlarge.opendc.experiments.sc20.reporter.ExperimentReporterProvider
import com.atlarge.opendc.experiments.sc20.trace.Sc20ParquetTraceReader
import com.atlarge.opendc.experiments.sc20.trace.Sc20RawParquetTraceReader
import com.atlarge.opendc.experiments.sc20.util.DatabaseHelper
import com.atlarge.opendc.format.environment.EnvironmentReader
import com.atlarge.opendc.format.environment.sc20.Sc20ClusterEnvironmentReader
import com.atlarge.opendc.format.trace.TraceReader
import me.tongfei.progressbar.ProgressBar
import mu.KotlinLogging
import java.io.Closeable
import java.io.File
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.sql.DataSource

/**
 * The logger for the experiment runner.
 */
private val logger = KotlinLogging.logger {}

/**
 * The experiment runner is responsible for orchestrating the simulation runs of an experiment.
 *
 * @param portfolios The portfolios to consider.
 * @param ds The data source to write the experimental results to.
 */
public class ExperimentRunner(
    private val portfolios: List<Portfolio>,
    private val ds: DataSource,
    private val reporterProvider: ExperimentReporterProvider,
    private val environmentPath: File,
    private val tracePath: File,
    private val performanceInterferenceModel: PerformanceInterferenceModel?,
    private val parallelism: Int = Runtime.getRuntime().availableProcessors()
) : Closeable {
    /**
     * The database helper to write the execution plan.
     */
    private val helper = DatabaseHelper(ds.connection)

    /**
     * The experiment identifier.
     */
    private var experimentId = -1L

    /**
     * The mapping of portfolios to their ids.
     */
    private val portfolioIds = mutableMapOf<Portfolio, Long>()

    /**
     * The mapping of scenarios to their ids.
     */
    private val scenarioIds = mutableMapOf<Scenario, Long>()

    init {
        reporterProvider.init(ds)
    }

    /**
     * Create an execution plan
     */
    private fun createPlan(): List<Run> {
        val runs = mutableListOf<Run>()

        for (portfolio in portfolios) {
            val portfolioId = helper.persist(portfolio, experimentId)
            portfolioIds[portfolio] = portfolioId
            var scenarios = 0
            var runCount = 0

            for (scenario in portfolio.scenarios) {
                val scenarioId = helper.persist(scenario, portfolioId)
                scenarioIds[scenario] = scenarioId
                scenarios++

                for (run in scenario.runs) {
                    helper.persist(run, scenarioId)
                    runCount++
                    runs.add(run)
                }
            }

            logger.info { "Portfolio $portfolioId: ${portfolio.name} ($scenarios scenarios, $runCount runs total)" }
        }

        return runs
    }

    /**
     * The raw parquet trace readers that are shared across simulations.
     */
    private val rawTraceReaders = mutableMapOf<String, Sc20RawParquetTraceReader>()

    /**
     * Create a trace reader for the specified trace.
     */
    private fun createTraceReader(
        name: String,
        performanceInterferenceModel: PerformanceInterferenceModel?,
        run: Run
    ): TraceReader<VmWorkload> {
        val raw = rawTraceReaders.getValue(name)
        return Sc20ParquetTraceReader(
            raw,
            performanceInterferenceModel,
            run
        )
    }

    /**
     * Create the environment reader for the specified environment.
     */
    private fun createEnvironmentReader(name: String): EnvironmentReader {
        return Sc20ClusterEnvironmentReader(File(environmentPath, "$name.txt"))
    }

    /**
     * Run the portfolios.
     */
    @OptIn(ExperimentalStdlibApi::class)
    public fun run() {
        experimentId = helper.createExperiment()
        logger.info { "Creating execution plan for experiment $experimentId" }

        val plan = createPlan()
        val total = plan.size
        val completionService = ExecutorCompletionService<Unit>(Executors.newCachedThreadPool())
        val pb = ProgressBar("Experiment", total.toLong())

        var running = 0

        for (run in plan) {
            if (running >= parallelism) {
                completionService.take()
                running--
            }

            val scenarioId = scenarioIds[run.scenario]!!

            rawTraceReaders.computeIfAbsent(run.scenario.workload.name) { name ->
                logger.info { "Loading trace $name" }
                Sc20RawParquetTraceReader(File(tracePath, name))
            }

            completionService.submit {
                pb.extraMessage = "($scenarioId, ${run.id}) START"

                var hasFailed = false
                synchronized(helper) {
                    helper.startRun(scenarioId, run.id)
                }

                try {
                    val reporter = reporterProvider.createReporter(scenarioIds[run.scenario]!!, run.id)
                    val traceReader =
                        createTraceReader(run.scenario.workload.name, performanceInterferenceModel, run)
                    val environmentReader = createEnvironmentReader(run.scenario.topology.name)

                    try {
                        run.scenario(run, reporter, environmentReader, traceReader)
                    } finally {
                        reporter.close()
                    }

                    pb.extraMessage = "($scenarioId, ${run.id}) OK"
                } catch (e: Throwable) {
                    logger.error("A run has failed", e)
                    hasFailed = true
                    pb.extraMessage = "($scenarioId, ${run.id}) FAIL"
                } finally {
                    synchronized(helper) {
                        helper.finishRun(scenarioId, run.id, hasFailed = hasFailed)
                    }

                    pb.step()
                }
            }

            running++
        }
    }

    override fun close() {
        reporterProvider.close()
        helper.close()
    }
}
