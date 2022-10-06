/*
 * Copyright (c) 2022 AtLarge Research
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

@file:JvmName("CapelinCli")

package org.opendc.experiments.capelin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import org.opendc.experiments.capelin.model.Scenario
import org.opendc.experiments.capelin.portfolio.CompositeWorkloadPortfolio
import org.opendc.experiments.capelin.portfolio.HorVerPortfolio
import org.opendc.experiments.capelin.portfolio.MoreHpcPortfolio
import org.opendc.experiments.capelin.portfolio.MoreVelocityPortfolio
import org.opendc.experiments.capelin.portfolio.OperationalPhenomenaPortfolio
import org.opendc.experiments.capelin.portfolio.TestPortfolio
import java.io.File
import java.util.concurrent.ForkJoinPool
import java.util.stream.LongStream

/**
 * Main entrypoint of the application.
 */
fun main(args: Array<String>): Unit = CapelinCommand().main(args)

/**
 * Represents the command for the Capelin experiments.
 */
internal class CapelinCommand : CliktCommand(name = "capelin") {
    /**
     * The path to the environment directory.
     */
    private val envPath by option("--env-path", help = "path to environment directory")
        .file(canBeDir = true, canBeFile = false)
        .defaultLazy { File("input/environments") }

    /**
     * The path to the trace directory.
     */
    private val tracePath by option("--trace-path", help = "path to trace directory")
        .file(canBeDir = true, canBeFile = false)
        .defaultLazy { File("input/traces") }

    /**
     * The path to the experiment output.
     */
    private val outputPath by option("-O", "--output", help = "path to experiment output")
        .file(canBeDir = true, canBeFile = false)
        .defaultLazy { File("output") }

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
        .default(128)

    /**
     * The seed for seeding the random instances.
     */
    private val seed by option("-s", "--seed", help = "initial seed for randomness")
        .long()
        .default(0)

    /**
     * The portfolio to replay.
     */
    private val portfolio by argument(help = "portfolio to replay")
        .choice(
            "test" to { TestPortfolio() },
            "composite-workload" to { CompositeWorkloadPortfolio() },
            "hor-ver" to { HorVerPortfolio() },
            "more-hpc" to { MoreHpcPortfolio() },
            "more-velocity" to { MoreVelocityPortfolio() },
            "op-phen" to { OperationalPhenomenaPortfolio() }
        )

    /**
     * The base partitions to use for the invocation
     */
    private val basePartitions: Map<String, String> by option("-P", "--base-partitions").associate()

    override fun run() {
        val runner = CapelinRunner(envPath, tracePath, outputPath.takeUnless { disableOutput })
        val scenarios = portfolio().scenarios.toList()

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
    private fun runScenario(runner: CapelinRunner, pool: ForkJoinPool, scenario: Scenario) {
        val pb = ProgressBarBuilder()
            .setInitialMax(repeats.toLong())
            .setStyle(ProgressBarStyle.ASCII)
            .setTaskName("Simulating...")
            .build()

        pool.submit {
            LongStream.range(0, repeats.toLong())
                .parallel()
                .forEach { repeat ->
                    val augmentedScenario = scenario.copy(partitions = basePartitions + scenario.partitions)
                    runner.runScenario(augmentedScenario, seed + repeat)
                    pb.step()
                }

            pb.close()
        }.join()
    }
}
