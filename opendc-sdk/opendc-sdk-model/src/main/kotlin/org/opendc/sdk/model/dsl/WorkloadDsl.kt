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

package org.opendc.sdk.model.dsl

import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.TimeDelta
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.model.workload.InlineWorkloadSpec
import org.opendc.sdk.model.workload.ScalingPolicySpec
import org.opendc.sdk.model.workload.TaskFragmentSpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec

/**
 * Builds a [TraceWorkloadSpec] backed by an external trace resource.
 *
 * @param source Handle to the trace data resolved at runtime.
 */
public fun traceWorkload(
    source: ResourceReference,
    sampleFraction: Double = 1.0,
    submissionTime: String? = null,
    scalingPolicy: ScalingPolicySpec = ScalingPolicySpec.NoDelay,
    deferAll: Boolean = false,
): TraceWorkloadSpec = TraceWorkloadSpec(source, sampleFraction, submissionTime, scalingPolicy, deferAll)

/**
 * Builds an [InlineWorkloadSpec] from tasks defined directly in the [block].
 *
 * @param block Configures the workload through an [InlineWorkloadBuilder].
 */
public fun inlineWorkload(block: InlineWorkloadBuilder.() -> Unit): InlineWorkloadSpec = InlineWorkloadBuilder().apply(block).build()

/** Collects the tasks composing an [InlineWorkloadSpec]. */
@SdkDsl
public class InlineWorkloadBuilder {
    private val tasks = mutableListOf<TaskSpec>()

    /** How tasks react to resource contention. */
    public var scalingPolicy: ScalingPolicySpec = ScalingPolicySpec.NoDelay

    public fun task(
        id: Int,
        name: String,
        submissionTime: TimeDelta,
        duration: TimeDelta,
        cpuCoreCount: Int,
        cpuCapacity: Frequency,
        memory: DataSize,
        gpuCoreCount: Int = 0,
        gpuCapacity: Frequency = Frequency.ofMHz(0),
        gpuMemory: DataSize = DataSize.ofBytes(0),
        deferrable: Boolean = false,
        deadline: TimeDelta? = null,
        parents: Set<Int> = emptySet(),
        children: Set<Int> = emptySet(),
        block: TaskBuilder.() -> Unit,
    ) {
        val fragments = TaskBuilder().apply(block).build()
        tasks +=
            TaskSpec(
                id, name, submissionTime, duration, cpuCoreCount, cpuCapacity, memory, fragments,
                gpuCoreCount, gpuCapacity, gpuMemory, deferrable, deadline, parents, children,
            )
    }

    internal fun build(): InlineWorkloadSpec = InlineWorkloadSpec(tasks.toList(), scalingPolicy)
}

/** Collects the execution fragments of a [TaskSpec]. */
@SdkDsl
public class TaskBuilder {
    private val fragments = mutableListOf<TaskFragmentSpec>()

    public fun fragment(
        duration: TimeDelta,
        cpuUsage: Frequency,
        gpuUsage: Frequency = Frequency.ofMHz(0),
        gpuMemory: DataSize = DataSize.ofBytes(0),
    ) {
        fragments += TaskFragmentSpec(duration, cpuUsage, gpuUsage, gpuMemory)
    }

    internal fun build(): List<TaskFragmentSpec> = fragments.toList()
}
