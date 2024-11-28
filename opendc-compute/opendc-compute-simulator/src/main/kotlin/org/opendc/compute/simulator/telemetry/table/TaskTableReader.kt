/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.compute.simulator.telemetry.table

import org.opendc.compute.api.TaskState
import org.opendc.compute.simulator.telemetry.parquet.DfltTaskExportColumns
import org.opendc.trace.util.parquet.exporter.Exportable
import java.time.Instant

/**
 * An interface that is used to read a row of a task trace entry.
 */
public interface TaskTableReader : Exportable {
    public fun copy(): TaskTableReader

    public fun setValues(table: TaskTableReader)

    public fun record(now: Instant)

    public fun reset()

    /**
     * The timestamp of the current entry of the reader relative to the start of the workload.
     */
    public val timestamp: Instant

    /**
     * The timestamp of the current entry of the reader.
     */
    public val timestampAbsolute: Instant

    /**
     * The [TaskInfo] of the task to which the row belongs to.
     */
    public val taskInfo: TaskInfo

    /**
     * The [HostInfo] of the host on which the task is hosted or `null` if it has no host.
     */
    public val host: HostInfo?

    /**
     * The uptime of the host since last time in ms.
     */
    public val uptime: Long

    /**
     * The downtime of the host since last time in ms.
     */
    public val downtime: Long

    /**
     * The [Instant] at which the task was enqueued for the scheduler.
     */
    public val provisionTime: Instant?

    /**
     * The [Instant] at which the task booted relative to the start of the workload.
     */
    public val bootTime: Instant?

    /**
     * The [Instant] at which the task booted.
     */
    public val bootTimeAbsolute: Instant?

    /**
     * The [Instant] at which the task booted relative to the start of the workload.
     */
    public val creationTime: Instant?

    /**
     * The [Instant] at which the task booted relative to the start of the workload.
     */
    public val finishTime: Instant?

    /**
     * The capacity of the CPUs of Host on which the task is running (in MHz).
     */
    public val cpuLimit: Double

    /**
     * The CPU given to this task (in MHz).
     */
    public val cpuUsage: Double

    /**
     * The CPU demanded by this task (in MHz).
     */
    public val cpuDemand: Double

    /**
     * The duration (in seconds) that a CPU was active in the task.
     */
    public val cpuActiveTime: Long

    /**
     * The duration (in seconds) that a CPU was idle in the task.
     */
    public val cpuIdleTime: Long

    /**
     * The duration (in seconds) that a vCPU wanted to run, but no capacity was available.
     */
    public val cpuStealTime: Long

    /**
     * The duration (in seconds) of CPU time that was lost due to interference.
     */
    public val cpuLostTime: Long

    /**
     * The state of the task
     */
    public val taskState: TaskState?
}

// Loads the default export fields for deserialization whenever this file is loaded.
private val _ignore = DfltTaskExportColumns
