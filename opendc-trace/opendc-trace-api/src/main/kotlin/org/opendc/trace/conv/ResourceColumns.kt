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

@file:JvmName("ResourceColumns")

package org.opendc.trace.conv

/**
 * Identifier of the resource.
 */
@JvmField
public val resourceID: String = "id"

/**
 * The cluster to which the resource belongs.
 */
@JvmField
public val resourceClusterID: String = "cluster_id"

/**
 * Start time for the resource.
 */
@JvmField
public val resourceStartTime: String = "start_time"

/**
 * Start time for the resource.
 */
@JvmField
public val resourceCarbonIntensity: String = "carbon_intensity"

/**
 * End time for the resource.
 */
@JvmField
public val resourceStopTime: String = "stop_time"

/**
 * Number of CPUs for the resource.
 */
@JvmField
public val resourceCpuCount: String = "cpu_count"

/**
 * Total CPU capacity of the resource in MHz.
 */
@JvmField
public val resourceCpuCapacity: String = "cpu_capacity"

/**
 * Memory capacity for the resource in KB.
 */
@JvmField
public val resourceMemCapacity: String = "mem_capacity"
