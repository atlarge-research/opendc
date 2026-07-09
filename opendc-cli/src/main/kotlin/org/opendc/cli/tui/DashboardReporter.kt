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

import com.github.ajalt.mordant.animation.Animation
import com.github.ajalt.mordant.animation.animation
import com.github.ajalt.mordant.animation.progress.MultiProgressBarAnimation
import com.github.ajalt.mordant.animation.progress.addTask
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.progress.MultiProgressBarWidgetMaker
import com.github.ajalt.mordant.widgets.progress.build
import com.github.ajalt.mordant.widgets.progress.completed
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.github.ajalt.mordant.widgets.progress.timeRemaining
import org.opendc.cli.config.CliConfig
import org.opendc.cli.progress.ProgressSnapshot
import org.opendc.cli.progress.ProgressSource
import org.opendc.cli.run.SimulationOverview

/** The top and bottom border rows of the logs panel, excluded when sizing it to fill the terminal. */
private const val LOG_PANEL_BORDERS = 2

/**
 * Starts a live dashboard for the run when the terminal can host one, or returns null so the run
 * proceeds with plain streaming logs. The dashboard needs full multi-line in-place redraw: Mordant
 * reports `supportsAnsiCursor` true ONLY for the IntelliJ console (which can rewrite just one line),
 * so a real interactive terminal reports it false — precisely where the dashboard works. A
 * non-interactive or cursor-limited terminal cannot host it, and no live progress UI is shown.
 */
internal fun startDashboard(
    terminal: Terminal,
    progress: ProgressSource,
    overview: SimulationOverview,
    config: CliConfig,
): DashboardReporter? {
    val info = terminal.terminalInfo
    if (!info.outputInteractive || info.supportsAnsiCursor) return null
    return DashboardReporter(terminal, progress, overview, config).also { it.start() }
}

/**
 * Renders the live three-panel dashboard (info · progress · logs) while a run executes. A single
 * daemon thread owns the Mordant animation and the log tail; simulation threads only ever touch the
 * thread-safe progress source and the log capture. Restores logging on [stop].
 */
internal class DashboardReporter(
    private val terminal: Terminal,
    private val progress: ProgressSource,
    overview: SimulationOverview,
    private val config: CliConfig,
) {
    private val logCapture = LogCapture(config.logging)

    private val definition =
        progressBarLayout {
            progressBar(completeChar = config.symbols.barComplete, pendingChar = config.symbols.barPending)
            percentage()
            completed(suffix = config.bar.completedSuffix)
            timeRemaining()
        }

    // Used purely as the ETA/speed state machine; its own drawing (refresh) is never invoked.
    private val holder = MultiProgressBarAnimation(terminal)
    private val task = holder.addTask(definition, total = overview.totalTasks)

    private val infoPanel = infoPanel(terminal.theme, terminal.size.width, overview, config)
    private val animation: Animation<DashboardFrame> =
        terminal.animation { frame ->
            val progressPanel = progressPanel(terminal.theme, frame.progressBar, config)
            val panels =
                buildList {
                    add(infoPanel)
                    add(progressPanel)
                    add(logsPanel(terminal.theme, terminal.size.width, frame.logs, logRows(progressPanel), config))
                }
            dashboardWidget(panels)
        }

    @Volatile
    private var running = true
    private var poller: Thread? = null

    fun start() {
        logCapture.attach()
        animation.update(frame(progress.snapshot)) // initial frame on the caller thread, before the poller exists
        poller =
            Thread({ pollLoop() }, "opendc-dashboard").apply {
                isDaemon = true
                start()
            }
    }

    fun stop() {
        logCapture.use {
            running = false
            poller?.join(2 * config.timing.pollIntervalMs)
            val snap = progress.snapshot
            animation.update(frame(ProgressSnapshot(completedTasks = snap.totalTasks, totalTasks = snap.totalTasks)))
            animation.stop() // leave the final dashboard on screen
        }
    }

    private fun pollLoop() {
        while (running) {
            animation.update(frame(progress.snapshot))
            Thread.sleep(config.timing.pollIntervalMs)
        }
    }

    private fun frame(snap: ProgressSnapshot): DashboardFrame {
        task.update {
            completed = snap.completedTasks
            total = if (snap.totalTasks > 0) snap.totalTasks else null
        }
        val bar = MultiProgressBarWidgetMaker.build(definition to task.makeState())
        return DashboardFrame(bar, logCapture.snapshot())
    }

    /** How many log rows to render this frame: the logs panel grows to fill the leftover terminal height. */
    private fun logRows(progressPanel: Widget): Int {
        val width = terminal.size.width
        val used = infoPanel.render(terminal, width).height + progressPanel.render(terminal, width).height
        return (terminal.size.height - used - LOG_PANEL_BORDERS).coerceAtLeast(1)
    }
}

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
