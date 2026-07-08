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

package org.opendc.sdk.runner.base.harness

import kotlinx.serialization.decodeFromString
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.TimeDelta
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.sdk.model.checkpoint.CheckpointModel
import org.opendc.sdk.model.experiment.Scenario
import org.opendc.sdk.model.export.ExportModel
import org.opendc.sdk.model.failure.FailureModel
import org.opendc.sdk.model.failure.NoFailure
import org.opendc.sdk.model.scheduler.AllocationPolicy
import org.opendc.sdk.model.scheduler.ComputeHostFilter
import org.opendc.sdk.model.scheduler.CoreRamWeigher
import org.opendc.sdk.model.scheduler.FilterAllocationPolicy
import org.opendc.sdk.model.scheduler.RamFilter
import org.opendc.sdk.model.scheduler.VCpuFilter
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.sdk.model.topology.Topology
import org.opendc.sdk.model.workload.InlineWorkload
import org.opendc.sdk.model.workload.ScalingPolicy
import org.opendc.sdk.model.workload.Task
import org.opendc.sdk.model.workload.TaskFragment
import org.opendc.sdk.runner.internal.runScenario
import org.opendc.sdk.runner.internal.toClusterSpecs
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.sink.MonitorSink
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneOffset

/** The default scheduler used by the ported tests: the legacy `FilterScheduler` default. */
internal val defaultPolicy: AllocationPolicy =
    FilterAllocationPolicy(
        filters = listOf(ComputeHostFilter, VCpuFilter(1.0), RamFilter(1.0)),
        weighers = listOf(CoreRamWeigher(1.0)),
    )

/** The test-resources root, used to resolve trace references against the classpath. */
internal val testResourcesRoot: Path by lazy { Path.of(object {}.javaClass.getResource("/topologies")!!.toURI()).parent }

private val provisioner = FileSystemResourceProvisioner(testResourcesRoot)

/** Loads an SDK-model [Topology] from `/topologies/<name>` on the test classpath. */
internal fun createTopology(name: String): Topology {
    val text = checkNotNull(object {}.javaClass.getResourceAsStream("/topologies/$name")).use { it.readBytes().decodeToString() }
    return SdkJson.json.decodeFromString(text)
}

/** Converts a [Topology] to engine [ClusterSpec]s for topology-parsing assertions. */
internal fun Topology.toClusters(): List<ClusterSpec> = toClusterSpecs { provisioner.provision(it).path }

/** Builds an SDK [TaskFragment] with durations in milliseconds and usages in MHz. */
internal fun fragment(
    duration: Long,
    cpuUsage: Double,
    gpuUsage: Double = 0.0,
): TaskFragment = TaskFragment(TimeDelta.ofMillis(duration), Frequency.ofMHz(cpuUsage), Frequency.ofMHz(gpuUsage))

/**
 * Builds an SDK [Task] reproducing the legacy `createTestTask`: per-core capacity is the peak
 * fragment usage, submission is parsed as a UTC instant, and memory is in MiB.
 */
internal fun createTestTask(
    id: Int,
    name: String = "",
    memCapacity: Long = 0L,
    submissionTime: String = "1970-01-01T00:00",
    duration: Long = 0L,
    cpuCoreCount: Int = 1,
    gpuCoreCount: Int = 0,
    fragments: List<TaskFragment>,
    parents: Set<Int> = emptySet(),
    children: Set<Int> = emptySet(),
): Task {
    val submitMs = LocalDateTime.parse(submissionTime).toInstant(ZoneOffset.UTC).toEpochMilli()
    return Task(
        id = id,
        name = name,
        submissionTime = TimeDelta.ofMillis(submitMs),
        duration = TimeDelta.ofMillis(duration),
        cpuCoreCount = cpuCoreCount,
        cpuCapacity = Frequency.ofMHz(fragments.maxOf { it.cpuUsage.toMHz() }),
        memory = DataSize.ofMiB(memCapacity),
        fragments = fragments,
        gpuCoreCount = gpuCoreCount,
        gpuCapacity = Frequency.ofMHz(fragments.maxOfOrNull { it.gpuUsage.toMHz() } ?: 0.0),
        gpuMemory = DataSize.ofBytes(0),
        deferrable = false,
        deadline = null,
        parents = parents,
        children = children,
    )
}

/**
 * Runs [workload] against [topology] on a fresh simulated clock with a one-minute export interval
 * and seed 0, returning the [TestComputeMonitor] that captured the run — the SDK-runner analogue of
 * the legacy `runTest`.
 */
internal fun runTest(
    topology: Topology,
    workload: List<Task>,
    failureModel: FailureModel = NoFailure,
    allocationPolicy: AllocationPolicy = defaultPolicy,
    checkpointModel: CheckpointModel? = null,
    scalingPolicy: ScalingPolicy = ScalingPolicy.NoDelay,
): TestComputeMonitor {
    val monitor = TestComputeMonitor()
    val scenario =
        Scenario(
            topology = topology,
            workload = InlineWorkload(workload, scalingPolicy),
            allocationPolicy = allocationPolicy,
            exportModel = ExportModel(exportInterval = TimeDelta.ofMin(1), printFrequency = null),
            failureModel = failureModel,
            checkpointModel = checkpointModel,
        )
    runScenario(scenario, "", 0, 0L, listOf(MonitorSink(monitor)), provisioner)
    return monitor
}
