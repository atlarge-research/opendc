/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import org.opendc.cli.progress.ExperimentProgress
import org.opendc.cli.progress.ProgressReporter
import org.opendc.cli.progress.ProgressSink
import org.opendc.cli.render.renderOutputs
import org.opendc.cli.render.renderSummary
import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.experiment.expand
import org.opendc.sdk.model.export.OutputFile
import org.opendc.sdk.runner.OpenDC
import org.opendc.sdk.runner.SimulationReport
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.sink.InMemorySink
import java.nio.file.Path

/** `opendc run` — simulate every scenario of an experiment and write per-run Parquet results. */
internal class RunCommand : CliktCommand(name = "run") {
    override fun help(context: Context): String =
        "Run an experiment: simulate every scenario and write the results to Parquet, showing a live progress bar."

    private val experimentFile by argument(name = "experiment", help = "Path to the experiment JSON file.")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)

    private val output by option("--output", "-o", help = "Directory for the Parquet results.")
        .path(canBeFile = false)
        .default(Path.of("output"))

    private val inputRoot by option(
        "--input-root",
        help = "Root for resolving named/relative workload and trace references (default: the experiment file's directory).",
    ).path(canBeFile = false, mustExist = true)

    private val parallelism by option(
        "--parallelism",
        "-p",
        help = "Number of runs to simulate concurrently (default: available processors).",
    )
        .int()

    private val noProgress by option("--no-progress", help = "Disable the live progress bar.").flag()

    private val noSummary by option("--no-summary", help = "Skip the in-memory metrics summary (saves memory on very large sweeps).").flag()

    override fun run() {
        val experiment = loadExperiment(experimentFile)
        rejectInvalid(experiment)

        val report = simulate(experiment)

        if (!noSummary) renderSummary(terminal, report)
        renderOutputs(terminal, report, output)
    }

    private fun rejectInvalid(experiment: Experiment) {
        val issues = experiment.validate()
        if (issues.isEmpty()) return
        terminal.println(terminal.theme.danger("✗ ${experimentFile.name} has ${issues.size} issue(s):"))
        issues.forEach { terminal.println("  • ${it.path}: ${it.message}") }
        throw ProgramResult(1)
    }

    private fun simulate(experiment: Experiment): SimulationReport {
        val root = inputRoot ?: experimentFile.absoluteFile.parentFile.toPath()
        val builder = OpenDC.builder().provisioner(FileSystemResourceProvisioner(root)).output(output)
        parallelism?.let { builder.parallelism(it) }
        if (!noSummary) builder.sink(InMemorySink(setOf(OutputFile.HOST, OutputFile.SERVICE, OutputFile.POWER_SOURCE)))

        val reporter = attachProgress(experiment, builder)
        return try {
            builder.build().simulate(experiment)
        } finally {
            reporter?.stop()
        }
    }

    private fun attachProgress(
        experiment: Experiment,
        builder: OpenDC.Builder,
    ): ProgressReporter? {
        if (noProgress) return null
        val progress = ExperimentProgress(experiment.expand().sumOf { it.runs })
        builder.sink(ProgressSink(progress))
        return ProgressReporter(terminal, progress).also { it.start() }
    }
}
