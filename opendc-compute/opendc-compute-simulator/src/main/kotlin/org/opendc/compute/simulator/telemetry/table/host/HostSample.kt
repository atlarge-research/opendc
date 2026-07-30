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

package org.opendc.compute.simulator.telemetry.table.host

import org.opendc.trace.util.parquet.exporter.Exportable
import java.time.Instant

public data class HostSample(
    public val hostName: String? = null,
    public val clusterName: String? = null,
    // TODO: update this metric, unclear what it does
    public val coreCount: Int = -1,
    public val memCapacity: Long = -1L,
    public val timestamp: Instant = Instant.MIN,
    public val timestampAbsolute: Instant = Instant.MIN,
    public val tasksTerminated: Int = -1,
    public val tasksActive: Int = -1,
    public val guestsError: Int = -1,
    public val guestsInvalid: Int = -1,
    public val cpuCapacity: Double = -1.0,
    public val cpuUsage: Double = -1.0,
    public val cpuDemand: Double = -1.0,
    public val cpuUtilization: Double = -1.0,
    public val cpuActiveTime: Long = -1L,
    public val cpuIdleTime: Long = -1L,
    public val cpuStealTime: Long = -1L,
    public val cpuLostTime: Long = -1L,
    public var gpuCapacities: ArrayList<Double> = ArrayList(),
    public var gpuUsages: ArrayList<Double> = ArrayList(),
    public var gpuDemands: ArrayList<Double> = ArrayList(),
    public var gpuUtilizations: ArrayList<Double> = ArrayList(),
    public val gpuActiveTimes: ArrayList<Long> = ArrayList(),
    public val gpuIdleTimes: ArrayList<Long> = ArrayList(),
    public val gpuStealTimes: ArrayList<Long> = ArrayList(),
    public val gpuLostTimes: ArrayList<Long> = ArrayList(),
    public val gpuPowerDraws: ArrayList<Double> = ArrayList(),
    public val powerDraw: Double = -1.0,
    public val energyUsage: Double = -1.0,
    public val embodiedCarbon: Double = -1.0,
    public val uptime: Long = -1L,
    public val downtime: Long = -1L,
    public val bootTime: Instant = Instant.MIN,
) : Exportable
