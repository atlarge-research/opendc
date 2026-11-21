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
import com.github.ajalt.clikt.parameters.types.restrictTo
import org.opendc.cli.config.CliConfig
import org.opendc.cli.render.renderOutputs
import org.opendc.cli.render.renderSummary
import org.opendc.cli.render.renderValidation
import org.opendc.cli.run.ApiFailure
import org.opendc.cli.run.Credentials
import org.opendc.cli.run.LocalBackend
import org.opendc.cli.run.OpendcApi
import org.opendc.cli.run.ProjectChoice
import org.opendc.cli.run.RemoteBackend
import org.opendc.cli.run.RemoteRunFailed
import org.opendc.cli.run.RunRequest
import org.opendc.cli.run.RunSummary
import org.opendc.cli.run.SimulationBackend
import org.opendc.cli.tui.startDashboard
import java.io.IOException
import java.nio.file.Path

/** `opendc run` — simulate every scenario of an experiment and write per-run Parquet results. */
internal class RunCommand(config: CliConfig = CliConfig.DEFAULTS) : ExperimentCommand("run", config) {
    override fun help(context: Context): String =
        "Run an experiment: simulate every scenario and write the results to Parquet, showing a live progress dashboard."

    private val output by option(
        "--output",
        "-o",
        help = "Directory for the Parquet results.",
    )
        .path(canBeFile = false)
        .default(Path.of("output"))

    private val parallelism by option(
        "--parallelism",
        "-p",
        help = "Number of runs to simulate concurrently.",
    )
        .int()
        .default(1)

    private val writerThreads by option(
        "--writer-threads",
        help =
            "Number of threads that write each of the task and host Parquet files of a run. More threads speed up a " +
                "large run on an otherwise idle machine; keep 1 when running many runs in parallel.",
    )
        .int()
        .restrictTo(min = 1)
        .default(1)

    private val noProgress by option(
        "--no-progress",
        help = "Disable the live progress dashboard.",
    ).flag()

    private val noSummary by option(
        "--no-summary",
        help = "Skip the in-memory metrics summary (saves memory on very large sweeps).",
    ).flag()

    private val apiUrl by option(
        "--api-url",
        help = "Run on the OpenDC server at this URL instead of locally, and download the results.",
    )

    private val project by option(
        "--project",
        help = "With --api-url, the id of the project to run in. Defaults to a project called \"$DEFAULT_PROJECT\".",
    )

    private val token by option(
        "--token",
        envvar = "OPENDC_TOKEN",
        help = "With --api-url, a personal access token. Not needed for a server that signs nobody in.",
    )

    override fun run() {
        val experiment = loadExperiment()

        if (!renderValidation(terminal, experimentFile.name, experiment.validate(), config, showSuccess = false)) {
            throw ProgramResult(1)
        }

        val request =
            RunRequest(
                experiment = experiment,
                inputRoot = experimentBaseDirectory,
                output = output,
                parallelism = parallelism,
                writerThreads = writerThreads,
                wantSummary = !noSummary,
            )
        val backend = apiUrl?.let(::remoteBackend) ?: LocalBackend()

        val session =
            try {
                backend.prepare(request)
            } catch (e: ApiFailure) {
                echo(e.title, err = true)
                e.issues.forEach { echo("  $it", err = true) }
                throw ProgramResult(EXIT_REFUSED)
            } catch (e: IOException) {
                throw CliktError("Could not reach $apiUrl: ${e.message}", cause = e)
            }

        val reporter = if (noProgress) null else startDashboard(terminal, session.progress, session.overview, config)
        val outcome =
            try {
                session.run()
            } catch (e: RemoteRunFailed) {
                echo("The experiment ended ${e.state.wire}.", err = true)
                e.failures.forEach { echo("  $it", err = true) }
                if (e.outputs.runCount > 0) renderOutputs(terminal, e.outputs)
                throw ProgramResult(1)
            } catch (e: ApiFailure) {
                throw CliktError("The server stopped answering the run: ${e.title}", cause = e)
            } catch (e: IOException) {
                throw CliktError("Lost the connection to $apiUrl: ${e.message}", cause = e)
            } finally {
                reporter?.stop()
            }

        when (val summary = outcome.summary) {
            is RunSummary.Measured -> renderSummary(terminal, summary.view, config)
            RunSummary.NotMeasured -> {}
        }
        renderOutputs(terminal, outcome.outputs)
    }

    private fun remoteBackend(url: String): SimulationBackend {
        val credentials = token?.let(Credentials::Bearer) ?: Credentials.Anonymous
        val choice = project?.let(ProjectChoice::ById) ?: ProjectChoice.Named(DEFAULT_PROJECT)
        return RemoteBackend(OpendcApi(url, credentials), choice)
    }

    private companion object {
        /** Where remote runs go when no project is named, so they gather in one place. */
        const val DEFAULT_PROJECT = "opendc-cli"

        /** The server refused the experiment before running it: the document, a reference, or a budget. */
        const val EXIT_REFUSED = 2
    }
}
