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
import com.atlarge.opendc.experiments.sc20.util.DatabaseHelper
import com.atlarge.opendc.format.environment.EnvironmentReader
import com.atlarge.opendc.format.environment.sc20.Sc20ClusterEnvironmentReader
import com.atlarge.opendc.format.trace.TraceReader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.Closeable
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource
import kotlin.random.Random
import kotlin.system.measureTimeMillis

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
    private val performanceInterferenceModel: PerformanceInterferenceModel
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
     * Create a trace reader for the specified trace.
     */
    private fun createTraceReader(
        name: String,
        performanceInterferenceModel: PerformanceInterferenceModel,
        seed: Int
    ): TraceReader<VmWorkload> {
        return Sc20ParquetTraceReader(
            File(tracePath, name),
            performanceInterferenceModel,
            emptyList(),
            Random(seed)
        )
    }

    /**
     * Create the environment reader for the specified environment.
     */
    private fun createEnvironmentReader(name: String): EnvironmentReader {
        return Sc20ClusterEnvironmentReader(File(environmentPath, "$name.txt"))
    }

    /**
     * Run the specified run.
     */
    private fun run(run: Run) {
        val reporter = reporterProvider.createReporter(ds, experimentId)
        val traceReader = createTraceReader(run.scenario.workload.name, performanceInterferenceModel, run.seed)
        val environmentReader = createEnvironmentReader(run.scenario.topology.name)
        run.scenario(run, reporter, environmentReader, traceReader)
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
        val finished = AtomicInteger()
        val dispatcher = Executors.newWorkStealingPool().asCoroutineDispatcher()

        runBlocking {
            val mainDispatcher = coroutineContext[CoroutineDispatcher.Key]!!
            for (run in plan) {
                val scenarioId = scenarioIds[run.scenario]!!
                launch(dispatcher) {
                    launch(mainDispatcher) {
                        helper.startRun(scenarioId, run.id)
                    }

                    logger.info { "[${finished.get()}/$total] Starting run ($scenarioId, ${run.id})" }

                    try {

                        val duration = measureTimeMillis {
                            run(run)
                        }

                        finished.incrementAndGet()
                        logger.info { "[${finished.get()}/$total] Finished run ($scenarioId, ${run.id}) in $duration milliseconds" }

                        withContext(mainDispatcher) {
                            helper.finishRun(scenarioId, run.id, hasFailed = false)
                        }
                    } catch (e: Throwable) {
                        logger.error("A run has failed", e)
                        finished.incrementAndGet()
                        withContext(mainDispatcher) {
                            helper.finishRun(scenarioId, run.id, hasFailed = true)
                        }
                    }
                }
            }
        }
    }

    override fun close() {
        helper.close()
    }
}
