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

package org.opendc.cli.tui

import com.github.ajalt.mordant.rendering.Widget
import java.nio.file.Path

/**
 * The constant facts shown in the dashboard's top panel, computed once at run start.
 *
 * @property name The experiment name (or a placeholder when unset).
 * @property scenarios The number of scenarios the experiment expands to.
 * @property runs The total number of runs across all scenarios.
 * @property topologies The number of distinct topologies.
 * @property workloads The number of distinct workloads.
 * @property policies The number of distinct allocation policies.
 * @property totalTasks The total number of tasks across all runs (the progress denominator).
 * @property parallelism The number of runs simulated concurrently.
 * @property output The directory Parquet results are written to.
 * @property inputRoot The root against which named/relative references are resolved.
 */
internal data class SimulationFacts(
    val name: String,
    val scenarios: Int,
    val runs: Int,
    val topologies: Int,
    val workloads: Int,
    val policies: Int,
    val totalTasks: Long,
    val parallelism: Int,
    val output: Path,
    val inputRoot: Path,
)

/**
 * One frame of the live dashboard: the freshly rendered progress bar and the tail of the log buffer.
 *
 * @property progressBar The progress bar rendered to a widget for this frame.
 * @property logs The most recent log lines to show, oldest first.
 */
internal data class DashboardFrame(
    val progressBar: Widget,
    val logs: List<String>,
)

/** How the dashboard's logs panel is sized. */
internal sealed interface LogsPanel {
    /** Omit the logs panel entirely. */
    data object Hidden : LogsPanel

    /** Grow the logs panel to fill whatever terminal height the info and progress panels leave. */
    data object Fill : LogsPanel

    /** Show a fixed number of log [rows]. */
    data class Fixed(val rows: Int) : LogsPanel
}
