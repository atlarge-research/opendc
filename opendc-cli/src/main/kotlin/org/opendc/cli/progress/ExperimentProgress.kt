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

import java.util.concurrent.ConcurrentHashMap

/** Identifies one repetition of one scenario within a running experiment. */
internal data class RunKey(val scenarioId: Int, val seed: Long)

/** An immutable, aggregate view of how far an experiment has progressed at task granularity. */
internal data class ProgressSnapshot(
    val completedTasks: Long,
    val totalTasks: Long,
    val runsOpened: Int,
    val totalRuns: Int,
)

/**
 * Thread-safe aggregate of per-task completion across every (possibly parallel) run of an
 * experiment. Each run reports its *stable* task count once when it opens and its running tally of
 * completed + terminated tasks thereafter, so the total is a fixed denominator rather than the
 * moving "tasks seen so far" the service reports mid-run.
 */
internal class ExperimentProgress(private val totalRuns: Int) {
    private val totals = ConcurrentHashMap<RunKey, Int>()
    private val completed = ConcurrentHashMap<RunKey, Long>()

    fun registerRun(
        key: RunKey,
        taskCount: Int,
    ) {
        totals[key] = taskCount
        completed.putIfAbsent(key, 0L)
    }

    fun update(
        key: RunKey,
        done: Long,
    ) {
        completed[key] = done
    }

    val snapshot: ProgressSnapshot
        get() =
            ProgressSnapshot(
                completedTasks = completed.values.sumOf { it },
                totalTasks = totals.values.sumOf { it.toLong() },
                runsOpened = totals.size,
                totalRuns = totalRuns,
            )
}
