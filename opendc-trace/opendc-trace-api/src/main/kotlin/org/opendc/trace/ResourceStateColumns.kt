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

@file:JvmName("ResourceStateColumns")
package org.opendc.trace

import java.time.Duration
import java.time.Instant

/**
 * Identifier of the resource.
 */
public val RESOURCE_STATE_ID: TableColumn<String> = stringColumn("resource_state:id")

/**
 * The cluster to which the resource belongs.
 */
public val RESOURCE_STATE_CLUSTER_ID: TableColumn<String> = stringColumn("resource_state:cluster_id")

/**
 * Timestamp for the state.
 */
public val RESOURCE_STATE_TIMESTAMP: TableColumn<Instant> = TableColumn("resource_state:timestamp", Instant::class.java)

/**
 * Duration for the state.
 */
public val RESOURCE_STATE_DURATION: TableColumn<Duration> = TableColumn("resource_state:duration", Duration::class.java)

/**
 * A flag to indicate that the resource is powered on.
 */
public val RESOURCE_STATE_POWERED_ON: TableColumn<Boolean> = booleanColumn("resource_state:powered_on")

/**
 * Number of CPUs for the resource.
 */
public val RESOURCE_STATE_NCPUS: TableColumn<Int> = intColumn("resource_state:ncpus")

/**
 * Total CPU capacity of the resource in MHz.
 */
public val RESOURCE_STATE_CPU_CAPACITY: TableColumn<Double> = doubleColumn("resource_state:cpu_capacity")

/**
 * Total CPU usage of the resource in MHz.
 */
public val RESOURCE_STATE_CPU_USAGE: TableColumn<Double> = doubleColumn("resource_state:cpu_usage")

/**
 * Total CPU usage of the resource in percentage.
 */
public val RESOURCE_STATE_CPU_USAGE_PCT: TableColumn<Double> = doubleColumn("resource_state:cpu_usage_pct")

/**
 * Total CPU demand of the resource in MHz.
 */
public val RESOURCE_STATE_CPU_DEMAND: TableColumn<Double> = doubleColumn("resource_state:cpu_demand")

/**
 * CPU ready percentage.
 */
public val RESOURCE_STATE_CPU_READY_PCT: TableColumn<Double> = doubleColumn("resource_state:cpu_ready_pct")

/**
 * Memory capacity of the resource in KB.
 */
public val RESOURCE_STATE_MEM_CAPACITY: TableColumn<Double> = doubleColumn("resource_state:mem_capacity")

/**
 * Memory usage of the resource in KB.
 */
public val RESOURCE_STATE_MEM_USAGE: TableColumn<Double> = doubleColumn("resource_state:mem_usage")

/**
 * Disk read throughput of the resource in KB/s.
 */
public val RESOURCE_STATE_DISK_READ: TableColumn<Double> = doubleColumn("resource_state:disk_read")

/**
 * Disk write throughput of the resource in KB/s.
 */
public val RESOURCE_STATE_DISK_WRITE: TableColumn<Double> = doubleColumn("resource_state:disk_write")

/**
 * Network receive throughput of the resource in KB/s.
 */
public val RESOURCE_STATE_NET_RX: TableColumn<Double> = doubleColumn("resource_state:net_rx")

/**
 * Network transmit throughput of the resource in KB/s.
 */
public val RESOURCE_STATE_NET_TX: TableColumn<Double> = doubleColumn("resource_state:net_tx")
