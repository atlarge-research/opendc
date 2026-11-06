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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertAll
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.Power
import org.opendc.common.units.TimeDelta
import org.opendc.sdk.model.checkpoint.CheckpointModelSpec
import org.opendc.sdk.model.failure.FailureModelSpec
import org.opendc.sdk.model.failure.FailureSpec
import org.opendc.sdk.model.failure.InlineTraceBasedFailureSpec
import org.opendc.sdk.model.failure.NoFailureSpec
import org.opendc.sdk.model.scenario.ScenarioSpec
import org.opendc.sdk.model.scheduler.AllocationPolicySpec
import org.opendc.sdk.model.scheduler.ComputeHostFilterSpec
import org.opendc.sdk.model.scheduler.CoreRamWeigherSpec
import org.opendc.sdk.model.scheduler.FilterAllocationPolicySpec
import org.opendc.sdk.model.scheduler.RamFilterSpec
import org.opendc.sdk.model.scheduler.VCpuFilterSpec
import org.opendc.sdk.model.serialization.SdkJson
import org.opendc.sdk.model.telemetry.ExportSpec
import org.opendc.sdk.model.topology.BatterySpec
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.CpuSpec
import org.opendc.sdk.model.topology.DataCenterSpec
import org.opendc.sdk.model.topology.HostSpec
import org.opendc.sdk.model.topology.LinearPowerModelSpec
import org.opendc.sdk.model.topology.MemorySpec
import org.opendc.sdk.model.topology.PowerSourceSpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.InlineWorkloadSpec
import org.opendc.sdk.model.workload.ScalingPolicySpec
import org.opendc.sdk.model.workload.TaskFragmentSpec
import org.opendc.sdk.model.workload.TaskSpec
import org.opendc.sdk.runner.executor.runScenario
import org.opendc.sdk.runner.provision.FileSystemResourceProvisioner
import org.opendc.sdk.runner.telemetry.sink.MonitorSink
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneOffset

/** The default scheduler used by the ported tests: the legacy `FilterScheduler` default. */
internal val defaultPolicy: AllocationPolicySpec =
    FilterAllocationPolicySpec(
        filters = listOf(ComputeHostFilterSpec, VCpuFilterSpec(1.0), RamFilterSpec(1.0)),
        weighers = listOf(CoreRamWeigherSpec(1.0)),
    )

/** The test-resources root, used to resolve trace references against the classpath. */
internal val testResourcesRoot: Path by lazy { Path.of(object {}.javaClass.getResource("/topologies")!!.toURI()).parent }

private val provisioner = FileSystemResourceProvisioner(testResourcesRoot)

/** Loads an SDK-model [TopologySpec] from `/topologies/<name>` on the test classpath. */
internal fun createTopology(name: String): TopologySpec {
    val text = checkNotNull(object {}.javaClass.getResourceAsStream("/topologies/$name")).use { it.readBytes().decodeToString() }
    return SdkJson.json.decodeFromString(text)
}

/** All times in the tests are in milliseconds. */
internal const val SECOND = 1_000L
internal const val MINUTE = 60 * SECOND

/** The name of the hosts in [singleClusterTopology]. */
internal const val HOST = "H01"

/** The power draw of [HOST] when idle, which includes while it is failed. Its linear power model goes from 100 to 200 W. */
internal const val IDLE_POWER = 100.0

/** The power draw of [HOST] when running at 1000 MHz, half of its capacity. */
internal const val HALF_LOAD_POWER = 150.0

/** The power draw of [HOST] when running at 2000 MHz, its full capacity. */
internal const val FULL_LOAD_POWER = 200.0

/**
 * A topology with a single datacenter and cluster, with [hostCount] identical hosts ([HOST]). Each host has one 2 GHz
 * core, and a linear power model from [IDLE_POWER] to [FULL_LOAD_POWER].
 * The datacenter draws its power from [powerSource], and can store energy in [battery].
 */
internal fun singleClusterTopology(
    hostCount: Int = 1,
    powerSource: PowerSourceSpec = PowerSourceSpec(),
    battery: BatterySpec? = null,
): TopologySpec =
    TopologySpec(
        datacenters =
            listOf(
                DataCenterSpec(
                    clusters =
                        listOf(
                            ClusterSpec(
                                name = "C01",
                                hosts =
                                    listOf(
                                        HostSpec(
                                            name = HOST,
                                            count = hostCount,
                                            cpu = CpuSpec(coreCount = 1, coreSpeed = Frequency.ofGHz(2)),
                                            memory = MemorySpec(size = DataSize.ofGB(128)),
                                            cpuPowerModel =
                                                LinearPowerModelSpec(
                                                    idlePower = Power.ofWatts(IDLE_POWER),
                                                    maxPower = Power.ofWatts(FULL_LOAD_POWER),
                                                ),
                                        ),
                                    ),
                            ),
                        ),
                    powerSource = powerSource,
                    battery = battery,
                ),
            ),
    )

/** The host fails after 5 min, for 5 min. */
internal val singleFailure =
    InlineTraceBasedFailureSpec(listOf(FailureSpec(TimeDelta.ofMin(5), TimeDelta.ofMin(5), 1.0)), repeat = false)

/** Makes a 1 s snapshot after every minute of work. */
internal val checkpointEveryMinute = CheckpointModelSpec(interval = TimeDelta.ofMin(1), duration = TimeDelta.ofSec(1))

/** The energy in J used when drawing [power] W for [time] ms. */
internal fun energy(
    power: Double,
    time: Long,
): Double = power * time / SECOND

/** Asserts the delays in ms that task [taskId] reported in its last sample. */
internal fun assertDelays(
    monitor: TestMetricExporter,
    taskId: Int,
    scheduling: Long,
    failure: Long,
    checkpoint: Long,
) {
    assertAll(
        "Delays of task $taskId",
        { assertEquals(scheduling, monitor.taskSchedulingDelay.getValue(taskId).last()) { "Scheduling delay incorrect" } },
        { assertEquals(failure, monitor.taskFailureDelay.getValue(taskId).last()) { "Failure delay incorrect" } },
        { assertEquals(checkpoint, monitor.taskCheckpointDelay.getValue(taskId).last()) { "Checkpoint delay incorrect" } },
    )
}

/** Builds an SDK [TaskFragmentSpec] with durations in milliseconds and usages in MHz. */
internal fun fragment(
    duration: Long,
    cpuUsage: Double,
    gpuUsage: Double = 0.0,
): TaskFragmentSpec = TaskFragmentSpec(TimeDelta.ofMs(duration), Frequency.ofMHz(cpuUsage), Frequency.ofMHz(gpuUsage))

/**
 * Builds an SDK [TaskSpec] reproducing the legacy `createTestTask`: per-core capacity is the peak
 * fragment usage, submission is parsed as a UTC instant, and memory is in MiB.
 * TODO: rewrite this to use the dsl
 */
internal fun createTestTask(
    id: Int,
    memCapacity: Long = 0L,
    submissionTime: String = "1970-01-01T00:00",
    duration: Long = 0L,
    cpuCoreCount: Int = 1,
    gpuCoreCount: Int = 0,
    fragments: List<TaskFragmentSpec>,
    parents: Set<Int> = emptySet(),
    children: Set<Int> = emptySet(),
): TaskSpec {
    val submitMs = LocalDateTime.parse(submissionTime).toInstant(ZoneOffset.UTC).toEpochMilli()
    return TaskSpec(
        id = id,
        submissionTime = TimeDelta.ofMs(submitMs),
        duration = TimeDelta.ofMs(duration),
        cpuCoreCount = cpuCoreCount.toShort(),
        cpuCapacity = Frequency.ofMHz(fragments.maxOf { it.cpuUsage.toMHz() }),
        memory = DataSize.ofMiB(memCapacity),
        fragments = fragments,
        gpuCoreCount = gpuCoreCount.toShort(),
        gpuCapacity = Frequency.ofMHz(fragments.maxOfOrNull { it.gpuUsage.toMHz() } ?: 0.0),
        gpuMemory = DataSize.ofBytes(0),
        deferrable = false,
        parents = parents.toIntArray(),
        children = children.toIntArray(),
    )
}

/**
 * Runs [workload] against [topology] on a fresh simulated clock with a one-minute export interval
 * and seed 0, returning the [TestMetricExporter] that captured the run — the SDK-runner analogue of
 * the legacy `runTest`.
 */
internal fun runTest(
    topology: TopologySpec,
    workload: List<TaskSpec>,
    failureModel: FailureModelSpec = NoFailureSpec,
    allocationPolicy: AllocationPolicySpec = defaultPolicy,
    checkpointModel: CheckpointModelSpec? = null,
    scalingPolicy: ScalingPolicySpec = ScalingPolicySpec.NoDelay,
    exportInterval: TimeDelta = TimeDelta.ofMin(1),
): TestMetricExporter {
    val monitor = TestMetricExporter()
//    val monitorSink = ParquetSink(Path.of("output"))
    val scenario =
        ScenarioSpec(
            topology = topology,
            workload = InlineWorkloadSpec(workload, scalingPolicy),
            allocationPolicy = allocationPolicy,
            exportModel = ExportSpec(exportInterval = exportInterval, printFrequency = null),
            failureModel = failureModel,
            checkpointModel = checkpointModel,
        )
    runScenario(scenario, "", listOf(MonitorSink(monitor)), provisioner)
//    runScenario(scenario, "", listOf(monitorSink), provisioner)
    return monitor
}
