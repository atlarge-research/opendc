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

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import org.opendc.cli.config.CliConfig
import org.opendc.cli.render.renderOutputs
import org.opendc.cli.render.renderSummary
import org.opendc.cli.render.renderValidation
import org.opendc.cli.run.LocalBackend
import org.opendc.cli.run.RemoteBackend
import org.opendc.cli.run.RunRequest
import org.opendc.cli.run.SimulationBackend
import org.opendc.cli.tui.startDashboard
import java.nio.file.Path

/** `opendc run` — simulate every scenario of an experiment and write per-run Parquet results. */
internal class RunCommand(config: CliConfig = CliConfig.DEFAULTS) : ExperimentCommand("run", config) {
    override fun help(context: Context): String =
        "Run an experiment: simulate every scenario and write the results to Parquet, showing a live progress dashboard."

    private val output by option("--output", "-o", help = "Directory for the Parquet results.")
        .path(canBeFile = false)
        .default(Path.of("output"))

    private val inputRoot by option(
        "--input-root",
        help =
            "Root for resolving named/relative topology, workload and trace references " +
                "(default: the experiment file's directory, or the working directory under --legacy).",
    ).path(canBeFile = false, mustExist = true)

    private val parallelism by option(
        "--parallelism",
        "-p",
        help = "Number of runs to simulate concurrently (default: available processors).",
    )
        .int()

    private val noProgress by option("--no-progress", help = "Disable the live progress dashboard.").flag()

    private val noSummary by option(
        "--no-summary",
        help = "Skip the in-memory metrics summary (saves memory on very large sweeps).",
    ).flag()

    private val apiUrl by option(
        "--api-url",
        help = "Run remotely against this OpenDC API instead of locally (experimental; not yet implemented).",
    )

    override fun run() {
        // The topologies inlined while loading and the traces provisioned while running are resolved
        // against the same root, so `--input-root` moves the whole experiment rather than half of it.
        val root = inputRoot ?: defaultInputRoot
        val experiment = loadExperiment(root)
        if (!renderValidation(terminal, experimentFile.name, experiment.validate(), config, showSuccess = false)) {
            throw ProgramResult(1)
        }

        val request =
            RunRequest(
                experiment = experiment,
                inputRoot = root,
                output = output,
                parallelism = parallelism,
                wantSummary = !noSummary,
            )
        val backend: SimulationBackend = apiUrl?.let { RemoteBackend(it) } ?: LocalBackend()
        // The backend stays framework-agnostic; translate its "not implemented yet" into a clean CLI error.
        val session =
            try {
                backend.prepare(request)
            } catch (e: NotImplementedError) {
                throw CliktError(e.message ?: "Not implemented.")
            }

        val reporter = if (noProgress) null else startDashboard(terminal, session.progress, session.overview, config)
        val outcome =
            try {
                session.run()
            } finally {
                reporter?.stop()
            }

        outcome.summary?.let { renderSummary(terminal, it, config) }
        renderOutputs(terminal, outcome.outputs)
    }
}
