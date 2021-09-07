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

package org.opendc.experiments.radice

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.typesafe.config.Config
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics
import org.opendc.experiments.radice.scenario.ScenarioSpec
import org.opendc.experiments.radice.scenario.mapper.YamlScenarioMapper
import org.opendc.experiments.radice.util.ci
import org.opendc.telemetry.risk.RiskFactor
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ForkJoinPool
import java.util.stream.LongStream

/**
 * A [CliktCommand] for running portfolios from file.
 */
internal class RadiceRunCommand(private val config: Config) : CliktCommand(name = "run") {
    /**
     * The path to the trace directory.
     */
    private val tracePath by option("--trace-path", help = "path to trace directory")
        .file(canBeDir = true, canBeFile = false)
        .defaultLazy { File(config.getString("trace-path")) }

    /**
     * The path to the experiment output.
     */
    private val outputPath by option("-O", "--output", help = "path to experiment output")
        .file(canBeDir = true, canBeFile = false)
        .defaultLazy { File(config.getString("output-path")) }

    /**
     * Disable writing output.
     */
    private val disableOutput by option("--disable-output", help = "disable output").flag()

    /**
     * The number of threads to use for parallelism.
     */
    private val parallelism by option("-p", "--parallelism", help = "number of worker threads")
        .int()
        .default(Runtime.getRuntime().availableProcessors() - 1)

    /**
     * The number of repeats.
     */
    private val repeats by option("-r", "--repeats", help = "number of repeats")
        .int()
        .default(4096)

    /**
     * The seed for seeding the random instances.
     */
    private val seed by option("-s", "--seed", help = "initial seed for randomness")
        .long()
        .default(0)

    /**
     * The [YamlScenarioMapper] that is used to load the scenario specifications.
     */
    private val mapper = YamlScenarioMapper()

    /**
     * The portfolio to replay.
     */
    private val portfolio by argument(help = "portfolio to replay").file(mustExist = true)

    /**
     * The base partitions to use for the invocation
     */
    private val basePartitions: Map<String, String> by option("-P", "--base-partitions").associate()

    override fun run() {
        val runner = RadiceRunner(tracePath, outputPath.takeUnless { disableOutput })
        val scenarios = mapper.loadAll(portfolio)

        val pool = ForkJoinPool(parallelism)

        echo("Detected ${scenarios.size} scenarios [$repeats replications]")

        for (scenario in scenarios) {
            runScenario(runner, pool, scenario)
        }

        pool.shutdown()
    }

    /**
     * Run a single scenario.
     */
    private fun runScenario(runner: RadiceRunner, pool: ForkJoinPool, scenario: ScenarioSpec) {
        val pb = ProgressBarBuilder()
            .setInitialMax(repeats.toLong())
            .setStyle(ProgressBarStyle.ASCII)
            .setTaskName("Simulating...")
            .build()

        val stats = SynchronizedDescriptiveStatistics()
        val factorStats = ConcurrentHashMap<RiskFactor, DescriptiveStatistics>()

        pool.submit {
            LongStream.range(0, repeats.toLong())
                .parallel()
                .forEach { repeat ->
                    val augmentedScenario = scenario.copy(partitions = basePartitions + scenario.partitions)
                    val costs = runner.runScenario(augmentedScenario, seed + repeat)

                    for ((factor, cost) in costs) {
                        val factorStat = factorStats.computeIfAbsent(factor) { SynchronizedDescriptiveStatistics() }
                        factorStat.addValue(cost)
                    }

                    val totalCosts = costs.values.sum()
                    stats.addValue(totalCosts)

                    pb.step()

                    val ci = stats.ci(0.95)
                    pb.extraMessage = "Average cost: %.2f +- %.2f".format(stats.mean, ci)
                }

            pb.close()

            echo("Total costs: %.2f (%.2f) +- %.2f".format(stats.mean, stats.getPercentile(50.0), stats.ci(0.95)))

            for ((factor, stat) in factorStats) {
                val ci = stat.ci(0.95)
                val median = stat.getPercentile(50.0)
                echo("Costs of %s: %.2f (%.2f) +- %.2f".format(factor.id, stat.mean, median, ci))
            }
        }.join()
    }
}
