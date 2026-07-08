/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.runner

import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.experiment.Scenario
import org.opendc.sdk.model.experiment.expand
import org.opendc.sdk.model.resource.ResourceProvisioner
import org.opendc.sdk.runner.internal.runScenario
import org.opendc.sdk.runner.sink.OutputSink
import org.opendc.sdk.runner.sink.ParquetSink
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ForkJoinPool

/**
 * The entry point for running OpenDC simulations from the SDK model.
 *
 * Build an instance with [builder], supplying a [ResourceProvisioner] to resolve external trace
 * references and any number of output sinks, then call [simulate]:
 *
 * ```kotlin
 * val report = OpenDC.builder()
 *     .provisioner(FileSystemResourceProvisioner(inputRoot))
 *     .output(outputRoot)      // parquet
 *     .sink(InMemorySink())    // typed in-memory metrics
 *     .build()
 *     .simulate(experiment)
 * ```
 */
public class OpenDC private constructor(
    private val provisioner: ResourceProvisioner,
    private val sinks: List<OutputSink>,
    private val parallelism: Int,
) {
    /** Expands [experiment] into scenarios and simulates each repetition of each. */
    public fun simulate(experiment: Experiment): SimulationReport = run(experiment.name, experiment.expand())

    /** Simulates a single fully-resolved [scenario]. */
    public fun simulate(scenario: Scenario): SimulationReport = run(scenario.name, listOf(scenario))

    private fun run(
        experimentName: String,
        scenarios: List<Scenario>,
    ): SimulationReport {
        val work = scenarios.flatMap { scenario -> scenario.seeds().map { seed -> scenario to seed } }
        val completed = execute(experimentName, work)
        return SimulationReport(scenarios.map { it.collectRuns(completed) })
    }

    private fun execute(
        experimentName: String,
        work: List<Pair<Scenario, Long>>,
    ): List<Pair<Scenario, RunResult>> {
        val pool = ForkJoinPool(parallelism)
        try {
            return pool.submit(
                Callable {
                    work.parallelStream()
                        .map { (scenario, seed) ->
                            scenario to runScenario(scenario, experimentName, scenario.id, seed, sinks, provisioner)
                        }
                        .toList()
                },
            ).get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        } finally {
            pool.shutdown()
        }
    }

    private fun Scenario.seeds(): List<Long> = (0 until runs).map { initialSeed.toLong() + it }

    private fun Scenario.collectRuns(completed: List<Pair<Scenario, RunResult>>): ScenarioResult =
        ScenarioResult(this, completed.filter { it.first === this }.map { it.second }.sortedBy { it.seed })

    /** Assembles an [OpenDC] instance from a provisioner, output sinks and a parallelism level. */
    public class Builder {
        private var provisioner: ResourceProvisioner? = null
        private val sinks = mutableListOf<OutputSink>()
        private var parallelism: Int = Runtime.getRuntime().availableProcessors()

        /** Sets the provisioner that resolves external trace references (required). */
        public fun provisioner(provisioner: ResourceProvisioner): Builder = apply { this.provisioner = provisioner }

        /** Adds a [ParquetSink] writing per-run parquet files under [root]. */
        public fun output(root: Path): Builder = apply { sinks += ParquetSink(root) }

        /** Adds an output [sink]; sinks compose and all observe every run. */
        public fun sink(sink: OutputSink): Builder = apply { sinks += sink }

        /** Sets how many runs execute concurrently (defaults to the available processor count). */
        public fun parallelism(threads: Int): Builder =
            apply {
                require(threads >= 1) { "parallelism must be >= 1" }
                this.parallelism = threads
            }

        public fun build(): OpenDC =
            OpenDC(
                requireNotNull(provisioner) { "a ResourceProvisioner is required" },
                sinks.toList(),
                parallelism,
            )
    }

    public companion object {
        /** Creates a new [Builder]. */
        @JvmStatic
        public fun builder(): Builder = Builder()
    }
}
