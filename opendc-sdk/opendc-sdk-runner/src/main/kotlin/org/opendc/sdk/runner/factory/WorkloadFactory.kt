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

package org.opendc.sdk.runner.factory

import org.opendc.common.ResourceType
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.sdk.model.checkpoint.CheckpointSpec
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.model.workload.EfficientTraceWorkloadSpec
import org.opendc.sdk.model.workload.InlineWorkloadSpec
import org.opendc.sdk.model.workload.ScalingPolicySpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.sdk.model.workload.WorkloadSpec
import org.opendc.sdk.model.workload.loader.ComputeWorkloadLoader
import org.opendc.sdk.model.workload.loader.EfficientWorkloadLoader
import org.opendc.simulator.compute.workload.trace.TraceFragment
import org.opendc.simulator.compute.workload.trace.scaling.NoDelayScaling
import org.opendc.simulator.compute.workload.trace.scaling.PerfectScaling
import java.nio.file.Path
import java.util.ArrayDeque
import java.util.Queue
import org.opendc.simulator.compute.workload.trace.TraceWorkload as EngineTraceWorkload
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy as EngineScalingPolicy

/**
 * Materializes an SDK [WorkloadSpec] into a submission-order queue of [ServiceTask]s. Trace workloads
 * are loaded from the resource resolved by [resolve]; inline workloads are built in memory. Tasks are
 * meant to be [Queue.poll]ed off as they are submitted, so the replayer never holds onto tasks it has
 * already handed off.
 */
public fun WorkloadSpec.toServiceTasks(
    checkpoint: CheckpointSpec?,
    resolve: (ResourceReference) -> Path,
): Queue<ServiceTask> =
    when (this) {
        is TraceWorkloadSpec -> loadTrace(resolve(source), checkpoint)
        is EfficientTraceWorkloadSpec -> loadTrace(resolve(source), checkpoint)
        is InlineWorkloadSpec ->
            ArrayDeque(
                tasks
                    .sortedBy { it.submissionTime.toMsLong() }
                    .map { it.toServiceTask(scalingPolicy.toEngine(), checkpoint) },
            )
    }

public fun TraceWorkloadSpec.loadTrace(
    path: Path,
    checkpoint: CheckpointSpec?,
): Queue<ServiceTask> =
    ArrayDeque(
        ComputeWorkloadLoader(
            path.toFile(),
            submissionTime,
            checkpoint.intervalMs(),
            checkpoint.durationMs(),
            checkpoint.scaling(),
            scalingPolicy.toEngine(),
            deferAll,
        ).sampleByLoad(sampleFraction),
    )

public fun EfficientTraceWorkloadSpec.loadTrace(
    path: Path,
    checkpoint: CheckpointSpec?,
): Queue<ServiceTask> =
    ArrayDeque(
        EfficientWorkloadLoader(
            path.toFile(),
            submissionTime,
            checkpoint.intervalMs(),
            checkpoint.durationMs(),
            checkpoint.scaling(),
            scalingPolicy.toEngine(),
            deferAll,
        ).sampleByLoad(sampleFraction),
    )

public fun TaskSpec.toServiceTask(
    scaling: EngineScalingPolicy,
    checkpoint: CheckpointSpec?,
): ServiceTask {
    val engineFragments =
        ArrayList(
            fragments.map { TraceFragment(it.duration.toMsLong(), it.cpuUsage.toMHz(), it.gpuUsage.toMHz(), it.gpuMemory.toMiB().toInt()) },
        )
    val durationsArray = LongArray(fragments.size)
    val cpuUsagesArray = DoubleArray(fragments.size)
    val gpuUsagesArray = DoubleArray(fragments.size)
    val gpuMemoryUsagesArray = IntArray(fragments.size)

    var maxCpuUsage = 0.0
    var maxGpuUsage = 0.0

    val usedResources = BooleanArray(ResourceType.values().size)
    usedResources[ResourceType.CPU.ordinal] = true

    for ((i, fragment) in fragments.withIndex()) {
        durationsArray[i] = fragment.duration.toMsLong()
        cpuUsagesArray[i] = fragment.cpuUsage.toMHz()
        gpuUsagesArray[i] = fragment.gpuUsage.toMHz()
        gpuMemoryUsagesArray[i] = fragment.gpuMemory.toMiB().toInt()

        if (fragment.cpuUsage.toMHz() > maxCpuUsage) {
            maxCpuUsage = fragment.cpuUsage.toMHz()
        }
        if (fragment.gpuUsage.toMHz() > maxGpuUsage) {
            usedResources[ResourceType.GPU.ordinal] = true
            maxGpuUsage = fragment.gpuUsage.toMHz()
        }
    }

    val workload =
        EngineTraceWorkload(
            durationsArray,
            cpuUsagesArray,
            gpuUsagesArray,
            gpuMemoryUsagesArray,
            maxCpuUsage,
            maxGpuUsage,
            checkpoint.intervalMs(),
            checkpoint.durationMs(),
            checkpoint.scaling(),
            scaling,
            id,
            usedResources,
        )
    return ServiceTask(
        id,
        submissionTime.toMsLong(),
        duration.toMsLong(),
        cpuCoreCount.toInt(),
        cpuCapacity.toMHz(),
        memory.toMiB().toLong(),
        gpuCoreCount.toInt(),
        gpuCapacity.toMHz(),
        gpuMemory.toMiB().toLong(),
        workload,
        deferrable,
        deadline.toMsLong(),
        if (parents.isEmpty()) null else parents,
        if (children.isEmpty()) null else children,
    )
}

private fun ScalingPolicySpec.toEngine(): EngineScalingPolicy =
    when (this) {
        ScalingPolicySpec.NoDelay -> NoDelayScaling()
        ScalingPolicySpec.Perfect -> PerfectScaling()
    }

private fun CheckpointSpec?.intervalMs(): Long = this?.interval?.toMsLong() ?: 0L

private fun CheckpointSpec?.durationMs(): Long = this?.duration?.toMsLong() ?: 0L

private fun CheckpointSpec?.scaling(): Double = this?.intervalScaling ?: 1.0
