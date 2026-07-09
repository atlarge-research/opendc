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
import org.opendc.cli.progress.ExperimentProgress
import org.opendc.cli.progress.ProgressSnapshot

/**
 * Renders the live three-panel dashboard (info · progress · logs) while a run executes. A single
 * daemon thread owns the Mordant animation and the log tail; simulation threads only ever touch the
 * thread-safe [ExperimentProgress] and the log capture. Restores logging on [stop].
 */
internal class DashboardReporter(
    private val terminal: Terminal,
    private val progress: ExperimentProgress,
    facts: SimulationFacts,
    private val logsMode: LogsPanel = LOGS,
) : RunReporter {
    private val logCapture = LogCapture()

    private val definition =
        progressBarLayout {
            progressBar(completeChar = "█", pendingChar = "━")
            percentage()
            completed(suffix = " tasks")
            timeRemaining()
        }

    // Used purely as the ETA/speed state machine; its own drawing (refresh) is never invoked.
    private val holder = MultiProgressBarAnimation(terminal)
    private val task = holder.addTask(definition, total = facts.totalTasks)

    private val infoPanel = infoPanel(terminal, facts)
    private val animation: Animation<DashboardFrame> =
        terminal.animation { frame ->
            val progressPanel = progressPanel(terminal, frame.progressBar)
            val panels =
                buildList {
                    add(infoPanel)
                    add(progressPanel)
                    logRows(progressPanel)?.let { add(logsPanel(terminal, frame.logs, it)) }
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

    override fun stop() {
        logCapture.use {
            running = false
            poller?.join(2 * POLL_INTERVAL_MS)
            val snap = progress.snapshot
            animation.update(frame(ProgressSnapshot(completedTasks = snap.totalTasks, totalTasks = snap.totalTasks)))
            animation.stop() // leave the final dashboard on screen
        }
    }

    private fun pollLoop() {
        while (running) {
            animation.update(frame(progress.snapshot))
            Thread.sleep(POLL_INTERVAL_MS)
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

    /** How many log rows to render this frame, or `null` to hide the logs panel. */
    private fun logRows(progressPanel: Widget): Int? =
        when (val mode = logsMode) {
            LogsPanel.Hidden -> null
            is LogsPanel.Fixed -> mode.rows
            LogsPanel.Fill -> {
                val width = terminal.size.width
                val used = infoPanel.render(terminal, width).height + progressPanel.render(terminal, width).height
                (terminal.size.height - used - LOG_PANEL_BORDERS).coerceAtLeast(1)
            }
        }

    private companion object {
        val LOGS: LogsPanel = LogsPanel.Fill
        const val POLL_INTERVAL_MS = 100L
        const val LOG_PANEL_BORDERS = 2
    }
}
