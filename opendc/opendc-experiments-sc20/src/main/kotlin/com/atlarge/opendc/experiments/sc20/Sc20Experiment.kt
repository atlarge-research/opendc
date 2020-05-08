/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.opendc.compute.virt.service.allocation.AvailableCoreMemoryAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.AvailableMemoryAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.NumberOfActiveServersAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.ProvisionedCoresAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.RandomAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.ReplayAllocationPolicy
import com.atlarge.opendc.format.environment.sc20.Sc20ClusterEnvironmentReader
import com.atlarge.opendc.format.trace.sc20.Sc20PerformanceInterferenceReader
import com.atlarge.opendc.format.trace.sc20.Sc20VmPlacementReader
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.File
import java.io.FileReader
import java.io.InputStream
import java.sql.DriverManager
import java.util.ServiceLoader
import kotlin.random.Random

/**
 * The logger for this experiment.
 */
private val logger = KotlinLogging.logger {}

/**
 * Represents the command for running the experiment.
 */
class ExperimentCommand : CliktCommand(name = "sc20-experiment") {
    private val environment by option("--environment-file", help = "path to the environment file")
        .file()
        .required()
    private val performanceInterferenceStream by option("--performance-interference-file", help = "path to the performance interference file")
        .file()
        .convert { it.inputStream() as InputStream }
        .defaultLazy { ExperimentCommand::class.java.getResourceAsStream("/env/performance-interference.json") }

    private val vmPlacements by option("--vm-placements-file", help = "path to the VM placement file")
        .file()
        .convert {
            Sc20VmPlacementReader(it.inputStream().buffered()).construct()
        }
        .default(emptyMap())

    private val selectedVms by mutuallyExclusiveOptions(
        option("--selected-vms", help = "the VMs to run").convert { parseVMs(it) },
        option("--selected-vms-file").file().convert { parseVMs(FileReader(it).readText()) }
    ).default(emptyList())

    private val seed by option(help = "the random seed")
        .int()
        .default(0)
    private val failures by option("-x", "--failures", help = "enable (correlated) machine failures")
        .flag()
    private val failureInterval by option(help = "expected number of hours between failures")
        .int()
        .default(24 * 7) // one week
    private val allocationPolicy by option(help = "name of VM allocation policy to use")
        .choice(
            "mem", "mem-inv",
            "core-mem", "core-mem-inv",
            "active-servers", "active-servers-inv",
            "provisioned-cores", "provisioned-cores-inv",
            "random", "replay"
        )
        .default("core-mem")

    private val trace by option("--trace-directory", help = "path to the trace directory")
        .file(canBeFile = false)
        .required()

    private val reporter by option().groupChoice(
        "parquet" to Parquet(),
        "postgres" to Postgres()
    ).required()

    private fun parseVMs(string: String): List<String> {
        // Handle case where VM list contains a VM name with an (escaped) single-quote in it
        val sanitizedString = string.replace("\\'", "\\\\[")
            .replace("'", "\"")
            .replace("\\\\[", "'")
        val vms: List<String> = jacksonObjectMapper().readValue(sanitizedString)
        return vms
    }

    override fun run() {
        logger.info("seed: $seed")
        logger.info("failures: $failures")
        logger.info("allocation-policy: $allocationPolicy")

        val start = System.currentTimeMillis()
        val reporter: Sc20Reporter = reporter.createReporter()

        val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
        val system = provider("test")
        val root = system.newDomain("root")

        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = when (this.allocationPolicy) {
            "mem" -> AvailableMemoryAllocationPolicy()
            "mem-inv" -> AvailableMemoryAllocationPolicy(true)
            "core-mem" -> AvailableCoreMemoryAllocationPolicy()
            "core-mem-inv" -> AvailableCoreMemoryAllocationPolicy(true)
            "active-servers" -> NumberOfActiveServersAllocationPolicy()
            "active-servers-inv" -> NumberOfActiveServersAllocationPolicy(true)
            "provisioned-cores" -> ProvisionedCoresAllocationPolicy()
            "provisioned-cores-inv" -> ProvisionedCoresAllocationPolicy(true)
            "random" -> RandomAllocationPolicy(Random(seed))
            "replay" -> ReplayAllocationPolicy(vmPlacements)
            else -> throw IllegalArgumentException("Unknown policy ${this.allocationPolicy}")
        }

        val performanceInterferenceModel = try {
            Sc20PerformanceInterferenceReader(performanceInterferenceStream).construct()
        } catch (e: Throwable) {
            reporter.close()
            throw e
        }
        val environmentReader = Sc20ClusterEnvironmentReader(environment)
        val traceReader = try {
            createTraceReader(trace, performanceInterferenceModel, selectedVms, seed)
        } catch (e: Throwable) {
            reporter.close()
            throw e
        }

        root.launch {
            val (bareMetalProvisioner, scheduler) = createProvisioner(root, environmentReader, allocationPolicy)

            val failureDomain = if (failures) {
                logger.info("ENABLING failures")
                createFailureDomain(seed, failureInterval, bareMetalProvisioner, chan)
            } else {
                null
            }

            attachMonitor(scheduler, reporter)
            processTrace(traceReader, scheduler, chan, reporter, vmPlacements)

            logger.debug("SUBMIT=${scheduler.submittedVms}")
            logger.debug("FAIL=${scheduler.unscheduledVms}")
            logger.debug("QUEUED=${scheduler.queuedVms}")
            logger.debug("RUNNING=${scheduler.runningVms}")
            logger.debug("FINISHED=${scheduler.finishedVms}")

            failureDomain?.cancel()
            scheduler.terminate()
            logger.info("Simulation took ${System.currentTimeMillis() - start} milliseconds")
        }

        runBlocking {
            system.run()
            system.terminate()
        }

        // Explicitly close the monitor to flush its buffer
        reporter.close()
    }
}

sealed class Reporter(name: String) : OptionGroup(name) {
    /**
     * Create the [Sc20Reporter] for this option.
     */
    abstract fun createReporter(): Sc20Reporter
}

class Parquet : Reporter("Options for reporting using Parquet") {
    private val path by option(help = "path to where the output should be stored")
        .file()
        .defaultLazy { File("data/results-${System.currentTimeMillis()}.parquet") }

    override fun createReporter(): Sc20Reporter = Sc20ParquetReporter(path)
}

class Postgres : Reporter("Options for reporting using PostgreSQL") {
    private val url by option(help = "JDBC connection url").required()
    private val experimentId by option(help = "Experiment ID").long().required()

    override fun createReporter(): Sc20Reporter {
        val conn = DriverManager.getConnection(url)
        return Sc20PostgresReporter(conn, experimentId)
    }
}

/**
 * Main entry point of the experiment.
 */
fun main(args: Array<String>) = ExperimentCommand().main(args)
