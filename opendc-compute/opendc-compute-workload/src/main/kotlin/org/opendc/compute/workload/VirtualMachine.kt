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

import org.opendc.simulator.compute.kernel.interference.VmInterferenceProfile
import org.opendc.simulator.compute.workload.SimTrace
import java.time.Instant
import java.util.UUID

/**
 * A virtual machine workload.
 *
 * @param uid The unique identifier of the virtual machine.
 * @param name The name of the virtual machine.
 * @param cpuCapacity The required CPU capacity for the VM in MHz.
 * @param cpuCount The number of vCPUs in the VM.
 * @param memCapacity The provisioned memory for the VM in MB.
 * @param startTime The start time of the VM.
 * @param stopTime The stop time of the VM.
 * @param trace The trace that belong to this VM.
 * @param interferenceProfile The interference profile of this virtual machine.
 */
public data class VirtualMachine(
    val uid: UUID,
    val name: String,
    val cpuCount: Int,
    val cpuCapacity: Double,
    val memCapacity: Long,
    val totalLoad: Double,
    val startTime: Instant,
    val stopTime: Instant,
    val trace: SimTrace,
    val interferenceProfile: VmInterferenceProfile?,
) {
    val duration: Long = stopTime.toEpochMilli() - startTime.toEpochMilli()
}
