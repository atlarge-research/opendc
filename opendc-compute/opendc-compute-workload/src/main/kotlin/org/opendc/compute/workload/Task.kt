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

package org.opendc.compute.workload

import org.opendc.simulator.compute.workload.trace.TraceWorkload

/**
 * A virtual machine workload.
 *
 * @param id The unique identifier of the virtual machine.
 * @param name The name of the virtual machine.
 * @param cpuCapacity The required CPU capacity for the VM in MHz.
 * @param cpuCount The number of vCPUs in the VM.
 * @param memCapacity The provisioned memory for the VM in MB.
 * @param submissionTime The start time of the VM.
 * @param trace The trace that belong to this VM.
 */
public data class Task(
    val id: Int,
    val name: String,
    var submissionTime: Long,
    val duration: Long,
    val parents: Set<Int> = emptySet(),
    val children: Set<Int> = emptySet(),
    val cpuCount: Int,
    val cpuCapacity: Double,
    val totalCpuLoad: Double,
    val memCapacity: Long,
    val gpuCount: Int = 0,
    val gpuCapacity: Double = 0.0,
    val gpuMemCapacity: Long = 0L,
    val nature: String?,
    var deadline: Long,
    val trace: TraceWorkload,
)
