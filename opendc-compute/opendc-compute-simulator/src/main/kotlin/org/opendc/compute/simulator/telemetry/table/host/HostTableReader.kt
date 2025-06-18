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

package org.opendc.compute.simulator.telemetry.table.host

import org.opendc.trace.util.parquet.exporter.Exportable
import java.time.Instant

/**
 * An interface that is used to read a row of a host trace entry.
 */
public interface HostTableReader : Exportable {
    public fun copy(): HostTableReader

    public fun setValues(table: HostTableReader)

    public fun record(now: Instant)

    public fun reset()

    /**
     * The [HostInfo] of the host to which the row belongs to.
     */
    public val hostInfo: HostInfo

    /**
     * The timestamp of the current entry of the reader relative to the start of the workload.
     */
    public val timestamp: Instant

    /**
     * The timestamp of the current entry of the reader.
     */
    public val timestampAbsolute: Instant

    /**
     * The number of guests that are in a terminated state.
     */
    public val tasksTerminated: Int

    /**
     * The number of guests that are active on the Host state.
     */
    public val tasksActive: Int

    /**
     * The number of guests that are in an error state.
     */
    public val guestsError: Int

    /**
     * The number of guests that are in an unknown state.
     */
    public val guestsInvalid: Int

    /**
     * The capacity of the CPUs in the host (in MHz).
     */
    public val cpuCapacity: Double

    /**
     * The usage of all CPUs in the host (in MHz).
     */
    public val cpuUsage: Double

    /**
     * The demand of all vCPUs of the guests (in MHz)
     */
    public val cpuDemand: Double

    /**
     * The CPU utilization of the host.
     */
    public val cpuUtilization: Double

    /**
     * The duration (in ms) that a CPU was active in the host.
     */
    public val cpuActiveTime: Long

    /**
     * The duration (in ms) that a CPU was idle in the host.
     */
    public val cpuIdleTime: Long

    /**
     * The duration (in ms) that a vCPU wanted to run, but no capacity was available.
     */
    public val cpuStealTime: Long

    /**
     * The duration (in ms) of CPU time that was lost due to interference.
     */
    public val cpuLostTime: Long

    /**
     * The capacity of the CPUs in the host (in MHz).
     */
    public val gpuCapacities: ArrayList<Double>

    /**
     * The capacity of the GPUs in the host (in MHz). They inserted by GPU ID.
     */
    public val gpuLimits: ArrayList<Double>

    /**
     * The usage per GPU in the host (in MHz). They inserted by GPU ID
     */
    public val gpuUsages: ArrayList<Double>

    /**
     * The demand per GPU of the guests (in MHz). They inserted by GPU ID
     */
    public val gpuDemands: ArrayList<Double>

    /**
     * The GPU utilization of the host of each GPU. They inserted by GPU ID.
     */
    public val gpuUtilizations: ArrayList<Double>

    /**
     * The duration (in ms) that the respective GPU was active in the host. They inserted by GPU ID
     */
    public val gpuActiveTimes: ArrayList<Long>

    /**
     * The duration (in ms) that a GPU was idle in the host. They inserted by GPU ID
     */
    public val gpuIdleTimes: ArrayList<Long>

    /**
     * The duration (in ms) that a vGPU wanted to run, but no capacity was available. They inserted by GPU ID.
     */
    public val gpuStealTimes: ArrayList<Long>

    /**
     * The duration (in ms) of GPU time that was lost due to interference. They inserted by GPU ID
     */
    public val gpuLostTimes: ArrayList<Long>

    /**
     * The current power draw of the host in W.
     */
    public val powerDraw: Double

    /**
     * The total energy consumption of the host since last sample in J.
     */
    public val energyUsage: Double

    /**
     * The embodied carbon emitted since the last sample in gram.
     */
    public val embodiedCarbon: Double

    /**
     * The uptime of the host since last time in ms.
     */
    public val uptime: Long

    /**
     * The downtime of the host since last time in ms.
     */
    public val downtime: Long

    /**
     * The [Instant] at which the host booted relative to the start of the workload.
     */
    public val bootTime: Instant?
}
