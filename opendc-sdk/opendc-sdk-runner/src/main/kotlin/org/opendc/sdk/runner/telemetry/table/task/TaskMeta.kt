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

package org.opendc.sdk.runner.telemetry.table.task

import org.opendc.simulator.compute.task.SimTask
import org.opendc.trace.parquet.exporter.Exportable
import java.time.Duration

/**
 * The static attributes of a task. Task samples only carry what changes during the run, so these are exported once per
 * task.
 *
 * @property memCapacity The memory requested by the task in MiB.
 * @property submissionTime The time the task was submitted, in milliseconds since the start of the simulation.
 */
public data class TaskMeta(
    public val taskId: Int,
    public val cpuCount: Int,
    public val memCapacity: Long,
    public val gpuCount: Int,
    public val submissionTime: Long,
) : Exportable {
    public companion object {
        /**
         * Capture the static attributes of [task] in a simulation that starts at [startTime].
         */
        public fun of(
            task: SimTask,
            startTime: Duration,
        ): TaskMeta =
            TaskMeta(
                taskId = task.id,
                cpuCount = task.cpuCoreCount,
                memCapacity = task.memorySize.toMiB().toLong(),
                gpuCount = task.gpuCoreCount,
                // The trace keeps its absolute submission times; only the simulation clock starts at zero
                submissionTime = task.submittedAt.toEpochMs().toLong() - startTime.toMillis(),
            )
    }
}
