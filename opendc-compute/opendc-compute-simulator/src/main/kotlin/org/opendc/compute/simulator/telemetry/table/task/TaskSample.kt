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

package org.opendc.compute.simulator.telemetry.table.task

import org.opendc.compute.api.TaskState
import org.opendc.trace.util.parquet.exporter.Exportable
import java.time.Instant

public data class TaskSample(
    public val taskId: Int = -1,
    public val memCapacity: Long = -1L,
    public val cpuCount: Int = -1,
    public val gpuCount: Int = -1,
    public val hostName: String? = null,
    public val timestamp: Instant = Instant.MIN,
    public val timestampAbsolute: Instant = Instant.MIN,
    public val uptime: Long = -1L,
    public val downtime: Long = -1L,
    public val numFailures: Int = -1,
    public val numPauses: Int = -1,
    public val submissionTime: Long? = null,
    public val scheduleTime: Long? = null,
    public val finishTime: Long? = null,
    public val schedulingDelay: Long = -1L,
    public val failureDelay: Long = -1L,
    public val checkpointDelay: Long = -1L,
    public val taskState: TaskState? = null,
    public val cpuLimit: Double = -1.0,
    public val cpuUsage: Double = -1.0,
    public val cpuDemand: Double = -1.0,
    public val cpuActiveTime: Long = -1L,
    public val cpuIdleTime: Long = -1L,
    public val cpuStealTime: Long = -1L,
    public val cpuLostTime: Long = -1L,
    public val gpuLimit: Double? = -1.0,
    public val gpuUsage: Double? = -1.0,
    public val gpuDemand: Double? = -1.0,
    public val gpuActiveTime: Long = -1L,
    public val gpuIdleTime: Long = -1L,
    public val gpuStealTime: Long = -1L,
    public val gpuLostTime: Long = -1L,
) : Exportable
