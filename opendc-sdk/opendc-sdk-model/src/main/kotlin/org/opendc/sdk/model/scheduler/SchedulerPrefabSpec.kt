/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.scheduler

import kotlinx.serialization.Serializable

/** Keeps hosts that are up and have enough vCPUs and memory for the task. */
private val cpuRamFilters: List<HostFilterSpec> = listOf(ComputeHostFilterSpec, VCpuFilterSpec(), RamFilterSpec())

/** Like [cpuRamFilters], but also requires enough vGPUs. */
private val cpuGpuRamFilters: List<HostFilterSpec> = listOf(ComputeHostFilterSpec, VCpuFilterSpec(), VGpuFilterSpec(), RamFilterSpec())

/**
 * A named, built-in task scheduler. Each prefab is shorthand for a fully specified
 * [AllocationPolicySpec]; only its name is serialized.
 */
@Serializable
public enum class SchedulerPrefabSpec(
    /** The fully specified allocation policy this prefab stands for. */
    public val policy: AllocationPolicySpec,
) {
    Mem(
        FilterAllocationPolicySpec(
            cpuRamFilters,
            listOf(RamWeigherSpec(1.0)),
        ),
    ),
    MemInv(
        FilterAllocationPolicySpec(
            cpuRamFilters,
            listOf(RamWeigherSpec(-1.0)),
        ),
    ),
    CoreMem(
        FilterAllocationPolicySpec(
            cpuRamFilters,
            listOf(CoreRamWeigherSpec(1.0)),
        ),
    ),
    CoreMemInv(
        FilterAllocationPolicySpec(
            cpuRamFilters,
            listOf(CoreRamWeigherSpec(-1.0)),
        ),
    ),
    ActiveServers(
        FilterAllocationPolicySpec(
            cpuRamFilters,
            listOf(InstanceCountWeigherSpec(-1.0)),
        ),
    ),
    ActiveServersInv(
        FilterAllocationPolicySpec(
            cpuRamFilters,
            listOf(InstanceCountWeigherSpec(1.0)),
        ),
    ),
    ProvisionedCores(
        FilterAllocationPolicySpec(
            cpuRamFilters,
            listOf(VCpuWeigherSpec(1.0)),
        ),
    ),
    ProvisionedCoresInv(
        FilterAllocationPolicySpec(
            cpuRamFilters,
            listOf(VCpuWeigherSpec(-1.0)),
        ),
    ),

    /** Places each task on a fitting host running the fewest tasks. */
    TaskNumMemorizing(
        FilterAllocationPolicySpec(
            cpuRamFilters,
            listOf(InstanceCountWeigherSpec(-1.0)),
        ),
    ),
    Timeshift(
        FilterAllocationPolicySpec(
            cpuRamFilters,
            listOf(RamWeigherSpec(1.0)),
            timeshift = TimeshiftSpec(),
        ),
    ),
    ProvisionedCpuGpuCores(
        FilterAllocationPolicySpec(
            cpuGpuRamFilters,
            listOf(VCpuWeigherSpec(1.0), VGpuWeigherSpec(1.0)),
        ),
    ),
    ProvisionedCpuGpuCoresInv(
        FilterAllocationPolicySpec(
            cpuGpuRamFilters,
            listOf(VCpuWeigherSpec(-1.0), VGpuWeigherSpec(-1.0)),
        ),
    ),

    /** Places each task on a fitting host running the fewest tasks. */
    GpuTaskMemorizing(
        FilterAllocationPolicySpec(
            cpuGpuRamFilters,
            listOf(InstanceCountWeigherSpec(-1.0)),
        ),
    ),
}
