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

package org.opendc.harness.runner.console

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import mu.KotlinLogging
import org.opendc.harness.engine.ExperimentEngineLauncher
import org.opendc.harness.engine.discovery.DiscoveryProvider
import org.opendc.harness.engine.discovery.DiscoveryRequest
import org.opendc.harness.engine.discovery.DiscoverySelector
import org.opendc.harness.engine.scheduler.ThreadPoolExperimentScheduler

/**
 * The logger for this experiment runner.
 */
private val logger = KotlinLogging.logger {}

/**
 * The command line interface for the console experiment runner.
 */
public class ConsoleRunner : CliktCommand(name = "opendc-harness") {
    /**
     * The number of repeats per scenario.
     */
    private val repeats by option("-r", "--repeats", help = "Number of repeats per scenario")
        .int()
        .default(1)

    /**
     * The selected experiments to run by name.
     */
    private val experiments by option("-e", "--experiments", help = "Names of experiments to explore")
        .multiple(emptyList())

    /**
     * The maximum number of worker threads to use.
     */
    private val parallelism by option("-p", "--parallelism", help = "Maximum number of concurrent simulation runs")
        .int()
        .default(Runtime.getRuntime().availableProcessors())

    override fun run() {
        logger.info { "Starting OpenDC Console Experiment Runner" }

        val discovery = DiscoveryProvider.createComposite()
        val experiments = discovery.discover(
            DiscoveryRequest(
                selectors = experiments.map { DiscoverySelector.Name(it) }
            )
        )

        val reporter = ConsoleExperimentReporter()
        val scheduler = ThreadPoolExperimentScheduler(parallelism)

        try {
            ExperimentEngineLauncher()
                .withListener(reporter)
                .withRepeats(repeats)
                .withScheduler(scheduler)
                .runBlocking(experiments)
        } catch (e: Throwable) {
            logger.error(e) { "Failed to finish experiments" }
        } finally {
            reporter.close()
            scheduler.close()
        }

        logger.info { "Finished all experiments. Exiting." }
    }
}

/**
 * Main entry point of the experiment runner.
 */
public fun main(args: Array<String>): Unit = ConsoleRunner().main(args)
