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

package org.opendc.experiments.compute.telemetry.table

import java.time.Instant

/**
 * An interface that is used to read a row of a host trace entry.
 */
public interface HostTableReader {
    /**
     * The timestamp of the current entry of the reader.
     */
    public val timestamp: Instant

    /**
     * The [HostInfo] of the host to which the row belongs to.
     */
    public val host: HostInfo

    /**
     * The number of guests that are in a terminated state.
     */
    public val guestsTerminated: Int

    /**
     * The number of guests that are in a running state.
     */
    public val guestsRunning: Int

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
    public val cpuLimit: Double

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
     * The duration (in seconds) that a CPU was active in the host.
     */
    public val cpuActiveTime: Long

    /**
     * The duration (in seconds) that a CPU was idle in the host.
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
     * The current power usage of the host in W.
     */
    public val powerUsage: Double

    /**
     * The total power consumption of the host since last time in J.
     */
    public val powerTotal: Double

    /**
     * The uptime of the host since last time in ms.
     */
    public val uptime: Long

    /**
     * The downtime of the host since last time in ms.
     */
    public val downtime: Long

    /**
     * The [Instant] at which the host booted.
     */
    public val bootTime: Instant?
}
