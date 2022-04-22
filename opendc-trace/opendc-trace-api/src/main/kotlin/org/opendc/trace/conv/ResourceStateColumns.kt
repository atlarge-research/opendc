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

import org.opendc.trace.TableColumn
import org.opendc.trace.column
import java.time.Duration
import java.time.Instant

/**
 * The timestamp at which the state was recorded.
 */
@JvmField
public val RESOURCE_STATE_TIMESTAMP: TableColumn<Instant> = column("resource_state:timestamp")

/**
 * Duration for the state.
 */
@JvmField
public val RESOURCE_STATE_DURATION: TableColumn<Duration> = column("resource_state:duration")

/**
 * A flag to indicate that the resource is powered on.
 */
@JvmField
public val RESOURCE_STATE_POWERED_ON: TableColumn<Boolean> = column("resource_state:powered_on")

/**
 * Total CPU usage of the resource in MHz.
 */
@JvmField
public val RESOURCE_STATE_CPU_USAGE: TableColumn<Double> = column("resource_state:cpu_usage")

/**
 * Total CPU usage of the resource in percentage.
 */
@JvmField
public val RESOURCE_STATE_CPU_USAGE_PCT: TableColumn<Double> = column("resource_state:cpu_usage_pct")

/**
 * Total CPU demand of the resource in MHz.
 */
@JvmField
public val RESOURCE_STATE_CPU_DEMAND: TableColumn<Double> = column("resource_state:cpu_demand")

/**
 * CPU ready percentage.
 */
@JvmField
public val RESOURCE_STATE_CPU_READY_PCT: TableColumn<Double> = column("resource_state:cpu_ready_pct")

/**
 * Memory usage of the resource in KB.
 */
@JvmField
public val RESOURCE_STATE_MEM_USAGE: TableColumn<Double> = column("resource_state:mem_usage")

/**
 * Disk read throughput of the resource in KB/s.
 */
@JvmField
public val RESOURCE_STATE_DISK_READ: TableColumn<Double> = column("resource_state:disk_read")

/**
 * Disk write throughput of the resource in KB/s.
 */
@JvmField
public val RESOURCE_STATE_DISK_WRITE: TableColumn<Double> = column("resource_state:disk_write")

/**
 * Network receive throughput of the resource in KB/s.
 */
@JvmField
public val RESOURCE_STATE_NET_RX: TableColumn<Double> = column("resource_state:net_rx")

/**
 * Network transmit throughput of the resource in KB/s.
 */
@JvmField
public val RESOURCE_STATE_NET_TX: TableColumn<Double> = column("resource_state:net_tx")
