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
import org.opendc.compute.workload.ComputeWorkloadLoader
import org.opendc.sdk.model.checkpoint.CheckpointSpec
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.model.workload.InlineWorkloadSpec
import org.opendc.sdk.model.workload.ScalingPolicySpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.sdk.model.workload.WorkloadSpec
import org.opendc.simulator.compute.workload.trace.TraceFragment
import org.opendc.simulator.compute.workload.trace.scaling.NoDelayScaling
import org.opendc.simulator.compute.workload.trace.scaling.PerfectScaling
import java.nio.file.Path
import org.opendc.simulator.compute.workload.trace.TraceWorkload as EngineTraceWorkload
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy as EngineScalingPolicy

/**
 * Materializes an SDK [WorkloadSpec] into the engine's list of [ServiceTask]s. Trace workloads are
 * loaded from the resource resolved by [resolve]; inline workloads are built in memory.
 */
internal fun WorkloadSpec.toServiceTasks(
    checkpoint: CheckpointSpec?,
    resolve: (ResourceReference) -> Path,
): List<ServiceTask> =
    when (this) {
        is TraceWorkloadSpec -> loadTrace(resolve(source), checkpoint)
        is InlineWorkloadSpec -> tasks.map { it.toServiceTask(scalingPolicy.toEngine(), checkpoint) }
    }

private fun TraceWorkloadSpec.loadTrace(
    path: Path,
    checkpoint: CheckpointSpec?,
): List<ServiceTask> =
    ComputeWorkloadLoader(
        path.toFile(),
        submissionTime,
        checkpoint.intervalMs(),
        checkpoint.durationMs(),
        checkpoint.scaling(),
        scalingPolicy.toEngine(),
        deferAll,
    ).sampleByLoad(sampleFraction)

private fun TaskSpec.toServiceTask(
    scaling: EngineScalingPolicy,
    checkpoint: CheckpointSpec?,
): ServiceTask {
    val engineFragments =
        ArrayList(
            fragments.map { TraceFragment(it.duration.toMsLong(), it.cpuUsage.toMHz(), it.gpuUsage.toMHz(), it.gpuMemory.toMiB().toInt()) },
        )
    val usedResources =
        buildList {
            if (fragments.any { it.cpuUsage.toMHz() > 0.0 }) add(ResourceType.CPU)
            if (fragments.any { it.gpuUsage.toMHz() > 0.0 }) add(ResourceType.GPU)
        }.toTypedArray()
    val workload =
        EngineTraceWorkload(
            engineFragments,
            checkpoint.intervalMs(),
            checkpoint.durationMs(),
            checkpoint.scaling(),
            scaling,
            id,
            usedResources,
        )
    return ServiceTask(
        id,
        name,
        submissionTime.toMsLong(),
        duration.toMsLong(),
        cpuCoreCount,
        cpuCapacity.toMHz(),
        totalLoad(),
        memory.toMiB().toLong(),
        gpuCoreCount,
        gpuCapacity.toMHz(),
        gpuMemory.toMiB().toLong(),
        workload,
        deferrable,
        deadline?.toMsLong() ?: -1L,
        ArrayList(parents),
        children,
    )
}

private fun TaskSpec.totalLoad(): Double = fragments.sumOf { it.cpuUsage.toMHz() * it.duration.toHours() }

private fun ScalingPolicySpec.toEngine(): EngineScalingPolicy =
    when (this) {
        ScalingPolicySpec.NoDelay -> NoDelayScaling()
        ScalingPolicySpec.Perfect -> PerfectScaling()
    }

private fun CheckpointSpec?.intervalMs(): Long = this?.interval?.toMsLong() ?: 0L

private fun CheckpointSpec?.durationMs(): Long = this?.duration?.toMsLong() ?: 0L

private fun CheckpointSpec?.scaling(): Double = this?.intervalScaling ?: 1.0
