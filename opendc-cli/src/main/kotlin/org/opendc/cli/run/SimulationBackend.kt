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

package org.opendc.cli.run

import org.opendc.cli.progress.ProgressSource
import org.opendc.cli.render.OutputView
import org.opendc.cli.render.RunSummaryView
import org.opendc.sdk.model.experiment.ExperimentSpec
import java.nio.file.Path

/**
 * Runs experiments, decoupling the `run` command from where a simulation actually executes.
 * [LocalBackend] runs it in-process through the SDK; [RemoteBackend] submits it to an OpenDC server
 * and polls for progress. Both expose the same [SimulationSession], so the command and its dashboard
 * behave identically regardless of where the work happens.
 */
internal fun interface SimulationBackend {
    /** Resolves [request] and prepares a run without starting it. */
    fun prepare(request: RunRequest): SimulationSession
}

/** A prepared run: its up-front [overview], a pollable [progress] source, and a blocking [run]. */
internal interface SimulationSession {
    val overview: SimulationOverview
    val progress: ProgressSource

    /** Runs to completion (blocking) and returns the render-ready outcome. */
    fun run(): RunOutcome
}

/**
 * Everything a backend needs to prepare a run. A remote backend leaves [parallelism] to the server. [writerThreads] is
 * the number of threads that write each of the task and host Parquet files of a run.
 */
internal data class RunRequest(
    val experiment: ExperimentSpec,
    val inputRoot: Path,
    val output: Path,
    val parallelism: Int,
    val writerThreads: Int,
    val wantSummary: Boolean,
)

/** The render-ready result of a completed run: its per-run [summary] and the [outputs] location. */
internal data class RunOutcome(
    val summary: RunSummary,
    val outputs: OutputView,
)

/** The headline metrics of every run, which only an in-process run that was asked for them collects. */
internal sealed interface RunSummary {
    data class Measured(val view: RunSummaryView) : RunSummary

    data object NotMeasured : RunSummary
}

/** How many runs are simulated at once: a number this machine runs, or whatever the server grants. */
internal sealed interface Parallelism {
    data class Local(val runs: Int) : Parallelism

    data object Server : Parallelism
}

/**
 * The constant facts about a simulation, known before it starts and shown in the dashboard's top
 * panel. A backend computes these once when preparing a run (locally from the resolved experiment;
 * remotely from the experiment and the status the server reports once it is submitted).
 *
 * @property name The experiment name (or a placeholder when unset).
 * @property scenarios The number of scenarios the experiment expands to.
 * @property runs The total number of runs across all scenarios.
 * @property topologies The number of distinct topologies.
 * @property workloads The number of distinct workloads.
 * @property policies The number of distinct allocation policies.
 * @property totalTasks The total number of tasks across all runs (the progress denominator).
 * @property parallelism How many runs are simulated concurrently.
 * @property output The directory Parquet results are written to.
 * @property inputRoot The root against which named/relative references are resolved.
 */
internal data class SimulationOverview(
    val name: String,
    val scenarios: Int,
    val runs: Int,
    val topologies: Int,
    val workloads: Int,
    val policies: Int,
    val totalTasks: Long,
    val parallelism: Parallelism,
    val output: Path,
    val inputRoot: Path,
)
