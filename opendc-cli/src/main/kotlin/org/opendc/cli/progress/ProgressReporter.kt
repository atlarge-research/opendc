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

package org.opendc.cli.progress

import com.github.ajalt.mordant.animation.progress.animateOnThread
import com.github.ajalt.mordant.animation.progress.execute
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.progress.completed
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.github.ajalt.mordant.widgets.progress.text
import com.github.ajalt.mordant.widgets.progress.timeRemaining
import org.opendc.cli.tui.RunReporter
import java.util.concurrent.Future

/**
 * Renders a live, task-granularity progress bar for a running experiment. A daemon thread polls an
 * [ExperimentProgress] and pushes the aggregate into the single Mordant animation; it is the only
 * thread that touches the animator, keeping the many simulation callbacks fully decoupled.
 *
 * This is the plain fallback used when the terminal cannot host the richer [org.opendc.cli.tui.DashboardReporter].
 */
internal class ProgressReporter(terminal: Terminal, private val progress: ExperimentProgress) : RunReporter {
    private val animator =
        progressBarLayout {
            text("simulating")
            progressBar()
            percentage()
            completed(suffix = " tasks")
            timeRemaining()
        }.animateOnThread(terminal)

    @Volatile private var running = true
    private var render: Future<*>? = null
    private var poller: Thread? = null

    fun start() {
        render = animator.execute()
        poller =
            Thread({ pollLoop() }, "opendc-progress").apply {
                isDaemon = true
                start()
            }
    }

    override fun stop() {
        running = false
        poller?.join(2 * POLL_INTERVAL_MS)
        val snap = progress.snapshot
        animator.update {
            completed = snap.totalTasks
            total = snap.totalTasks
        }
        runCatching { render?.get() }
    }

    private fun pollLoop() {
        while (running) {
            pushSnapshot(progress.snapshot)
            Thread.sleep(POLL_INTERVAL_MS)
        }
    }

    private fun pushSnapshot(snap: ProgressSnapshot) {
        animator.update {
            completed = snap.completedTasks
            total = if (snap.totalTasks > 0) snap.totalTasks else null
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 100L
    }
}
