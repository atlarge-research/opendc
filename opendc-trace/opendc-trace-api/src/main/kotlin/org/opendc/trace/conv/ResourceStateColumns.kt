/*
 * Copyright (c) 2022 AtLarge Research
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

@file:JvmName("ResourceStateColumns")

package org.opendc.trace.conv

/**
 * The timestamp at which the state was recorded.
 */
@JvmField
public val resourceStateTimestamp: String = "timestamp"

/**
 * Duration for the state.
 */
@JvmField
public val resourceStateDuration: String = "duration"

/**
 * A flag to indicate that the resource is powered on.
 */
@JvmField
public val resourceStatePoweredOn: String = "powered_on"

/**
 * Total CPU usage of the resource in MHz.
 */
@JvmField
public val resourceStateCpuUsage: String = "cpu_usage"

/**
 * Total CPU usage of the resource in percentage.
 */
@JvmField
public val resourceStateCpuUsagePct: String = "cpu_usage_pct"

/**
 * Total CPU demand of the resource in MHz.
 */
@JvmField
public val resourceStateCpuDemand: String = "cpu_demand"

/**
 * CPU ready percentage.
 */
@JvmField
public val resourceStateCpuReadyPct: String = "cpu_ready_pct"

/**
 * Memory usage of the resource in KB.
 */
@JvmField
public val resourceStateMemUsage: String = "mem_usage"

/**
 * Disk read throughput of the resource in KB/s.
 */
@JvmField
public val resourceStateDiskRead: String = "disk_read"

/**
 * Disk write throughput of the resource in KB/s.
 */
@JvmField
public val resourceStateDiskWrite: String = "disk_write"

/**
 * Network receive throughput of the resource in KB/s.
 */
@JvmField
public val resourceStateNetRx: String = "net_rx"

/**
 * Network transmit throughput of the resource in KB/s.
 */
@JvmField
public val resourceStateNetTx: String = "net_tx"

/**
 * Total GPU capacity of the resource in MHz.
 */
@JvmField
public val resourceStateGpuUsage: String = "gpu_usage"
