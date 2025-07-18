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
public val resourceSubmissionTime: String = "submission_time"

/**
 * Carbon intensity of the resource.
 */
@JvmField
public val resourceCarbonIntensity: String = "carbon_intensity"

/**
 * End time for the resource.
 */
@JvmField
public val resourceDuration: String = "duration"

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

/**
 * Number of GPU cores for the resource.
 */
@JvmField
public val resourceGpuCount: String = "gpu_count"

/**
 * Total GPU capacity of the resource in MHz.
 */
@JvmField
public val resourceGpuCapacity: String = "gpu_capacity"

/**
 * Total GPU memory capacity of the resource in MB.
 */
@JvmField
public val resourceGpuMemCapacity: String = "gpu_mem_capacity"

/**
 * The parents of the resource that need to be completed before this resource can be used.
 */
@JvmField
public val resourceParents: String = "parents"

/**
 * The children of the resource that cannot be started before this is completed.
 */
@JvmField
public val resourceChildren: String = "children"

/**
 * Nature of the task. Delayable, interruptible, etc.
 */
@JvmField
public val resourceNature: String = "nature"

/**
 * Deadline of the task.
 */
@JvmField
public val resourceDeadline: String = "deadline"
