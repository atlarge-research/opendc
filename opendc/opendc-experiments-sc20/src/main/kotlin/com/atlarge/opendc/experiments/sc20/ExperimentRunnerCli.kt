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

import com.atlarge.opendc.experiments.sc20.reporter.ExperimentParquetReporter
import com.atlarge.opendc.experiments.sc20.reporter.ExperimentPostgresReporter
import com.atlarge.opendc.experiments.sc20.reporter.ExperimentReporter
import com.atlarge.opendc.experiments.sc20.reporter.ExperimentReporterProvider
import com.atlarge.opendc.experiments.sc20.reporter.ParquetHostMetricsWriter
import com.atlarge.opendc.experiments.sc20.reporter.ParquetProvisionerMetricsWriter
import com.atlarge.opendc.experiments.sc20.reporter.PostgresHostMetricsWriter
import com.atlarge.opendc.experiments.sc20.reporter.PostgresProvisionerMetricsWriter
import com.atlarge.opendc.format.trace.sc20.Sc20PerformanceInterferenceReader
import com.atlarge.opendc.format.trace.sc20.Sc20VmPlacementReader
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import java.io.File
import java.io.InputStream
import javax.sql.DataSource

/**
 * The logger for this experiment.
 */
private val logger = KotlinLogging.logger {}

/**
 * Represents the command for running the experiment.
 */
class ExperimentCli : CliktCommand(name = "sc20-experiment") {
    /**
     * The JDBC connection url to use.
     */
    private val jdbcUrl by option("--jdbc-url", help = "JDBC connection url").required()

    /**
     * The path to the directory where the topology descriptions are located.
     */
    private val environmentPath by option("--environment-path", help = "path to the environment directory")
        .file(canBeFile = false)
        .required()

    /**
     * The path to the directory where the traces are located.
     */
    private val tracePath by option("--trace-path", help = "path to the traces directory")
        .file(canBeFile = false)
        .required()

    /**
     * The path to the performance interference model.
     */
    private val performanceInterferenceStream by option("--performance-interference-model", help = "path to the performance interference file")
        .file()
        .convert { it.inputStream() as InputStream }

    /**
     * The path to the original VM placements file.
     */
    private val vmPlacements by option("--vm-placements-file", help = "path to the VM placement file")
        .file()
        .convert {
            Sc20VmPlacementReader(it.inputStream().buffered()).construct()
        }
        .default(emptyMap())

    /**
     * The type of reporter to use.
     */
    private val reporter by option().groupChoice(
        "parquet" to Reporter.Parquet(),
        "postgres" to Reporter.Postgres()
    ).required()

    /**
     * The selected portfolios to run.
     */
    private val portfolios by option("--portfolio")
        .choice(
            "hor-ver" to HorVerPortfolio,
            "more-velocity" to MoreVelocityPortfolio,
            "more-hpc" to MoreHpcPortfolio,
            "operational-phenomena" to OperationalPhenomenaPortfolio,
            ignoreCase = true
        )
        .multiple()

    /**
     * The maximum number of worker threads to use.
     */
    private val workerParallelism by option("--worker-parallelism")
        .int()
        .default(Runtime.getRuntime().availableProcessors())

    /**
     * The maximum number of host writer threads to use.
     */
    private val hostWriterParallelism by option("--host-writer-parallelism")
        .int()
        .default(8)

    /**
     * The maximum number of provisioner writer threads to use.
     */
    private val provisionerWriterParallelism by option("--provisioner-writer-parallelism")
        .int()
        .default(1)

    /**
     * The buffer size for writing results.
     */
    private val bufferSize by option("--buffer-size")
        .int()
        .default(4096)

    override fun run() {
        val ds = HikariDataSource()
        ds.maximumPoolSize = Runtime.getRuntime().availableProcessors() * 3
        ds.jdbcUrl = jdbcUrl
        ds.addDataSourceProperty("reWriteBatchedInserts", "true")

        reporter.bufferSize = bufferSize
        reporter.hostParallelism = hostWriterParallelism
        reporter.provisionerParallelism = provisionerWriterParallelism

        val performanceInterferenceModel =
            performanceInterferenceStream?.let { Sc20PerformanceInterferenceReader(it).construct() }

        val runner = ExperimentRunner(portfolios, ds, reporter, environmentPath, tracePath, performanceInterferenceModel, workerParallelism)

        try {
            runner.run()
        } finally {
            runner.close()
            ds.close()
        }
    }
}

/**
 * An option for specifying the type of reporter to use.
 */
internal sealed class Reporter(name: String) : OptionGroup(name), ExperimentReporterProvider {
    var bufferSize = 4096
    var hostParallelism = 8
    var provisionerParallelism = 1

    class Parquet : Reporter("Options for reporting using Parquet") {
        private val path by option("--parquet-directory", help = "path to where the output should be stored")
            .file()
            .defaultLazy { File("data") }

        override fun createReporter(scenario: Long, run: Int): ExperimentReporter {
            val hostWriter = ParquetHostMetricsWriter(File(path, "$scenario-$run-host.parquet"), bufferSize)
            val provisionerWriter = ParquetProvisionerMetricsWriter(File(path, "$scenario-$run-provisioner.parquet"), bufferSize)
            return ExperimentParquetReporter(scenario, run, hostWriter, provisionerWriter)
        }

        override fun close() {}
    }

    class Postgres : Reporter("Options for reporting using PostgreSQL") {
        lateinit var hostWriter: PostgresHostMetricsWriter
        lateinit var provisionerWriter: PostgresProvisionerMetricsWriter

        override fun init(ds: DataSource) {
            hostWriter = PostgresHostMetricsWriter(ds, hostParallelism, bufferSize)
            provisionerWriter = PostgresProvisionerMetricsWriter(ds, provisionerParallelism, bufferSize)
        }

        override fun createReporter(scenario: Long, run: Int): ExperimentReporter {
            return ExperimentPostgresReporter(scenario, run, hostWriter, provisionerWriter)
        }

        override fun close() {
            hostWriter.close()
            provisionerWriter.close()
        }
    }
}

/**
 * Main entry point of the experiment.
 */
fun main(args: Array<String>) = ExperimentCli().main(args)
