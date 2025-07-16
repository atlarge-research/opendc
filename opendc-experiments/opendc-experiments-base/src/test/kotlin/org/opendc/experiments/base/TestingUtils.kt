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

package org.opendc.experiments.base

import org.opendc.common.ResourceType
import org.opendc.compute.simulator.provisioner.Provisioner
import org.opendc.compute.simulator.provisioner.registerComputeMonitor
import org.opendc.compute.simulator.provisioner.setupComputeService
import org.opendc.compute.simulator.provisioner.setupHosts
import org.opendc.compute.simulator.scheduler.ComputeScheduler
import org.opendc.compute.simulator.scheduler.FilterScheduler
import org.opendc.compute.simulator.scheduler.filters.ComputeFilter
import org.opendc.compute.simulator.scheduler.filters.RamFilter
import org.opendc.compute.simulator.scheduler.filters.VCpuFilter
import org.opendc.compute.simulator.scheduler.weights.CoreRamWeigher
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.telemetry.ComputeMonitor
import org.opendc.compute.simulator.telemetry.table.host.HostTableReader
import org.opendc.compute.simulator.telemetry.table.powerSource.PowerSourceTableReader
import org.opendc.compute.simulator.telemetry.table.service.ServiceTableReader
import org.opendc.compute.simulator.telemetry.table.task.TaskTableReader
import org.opendc.compute.topology.clusterTopology
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.compute.workload.Task
import org.opendc.experiments.base.experiment.specs.FailureModelSpec
import org.opendc.experiments.base.runner.replay
import org.opendc.simulator.compute.workload.trace.TraceFragment
import org.opendc.simulator.compute.workload.trace.TraceWorkload
import org.opendc.simulator.compute.workload.trace.scaling.NoDelayScaling
import org.opendc.simulator.compute.workload.trace.scaling.ScalingPolicy
import org.opendc.simulator.kotlin.runSimulation
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.collections.ArrayList

/**
 * Obtain the topology factory for the test.
 */
fun createTopology(name: String): List<ClusterSpec> {
    val stream = checkNotNull(object {}.javaClass.getResourceAsStream("/topologies/$name"))
    return stream.use { clusterTopology(stream) }
}

fun createTestTask(
    name: String,
    memCapacity: Long = 0L,
    submissionTime: String = "1970-01-01T00:00",
    duration: Long = 0L,
    fragments: ArrayList<TraceFragment>,
    checkpointInterval: Long = 0L,
    checkpointDuration: Long = 0L,
    checkpointIntervalScaling: Double = 1.0,
    scalingPolicy: ScalingPolicy = NoDelayScaling(),
): Task {
    var usedResources = arrayOf<ResourceType>()
    if (fragments.any { it.cpuUsage > 0.0 }) {
        usedResources += ResourceType.CPU
    }
    if (fragments.any { it.gpuUsage > 0.0 }) {
        usedResources += ResourceType.GPU
    }

    return Task(
        UUID.nameUUIDFromBytes(name.toByteArray()),
        name,
        LocalDateTime.parse(submissionTime).toInstant(ZoneOffset.UTC).toEpochMilli(),
        duration,
        emptySet(),
        emptySet(),
        fragments.maxOf { it.cpuCoreCount() },
        fragments.maxOf { it.cpuUsage },
        1800000.0,
        memCapacity,
        gpuCount = fragments.maxOfOrNull { it.gpuCoreCount() } ?: 0,
        gpuCapacity = fragments.maxOfOrNull { it.gpuUsage } ?: 0.0,
        gpuMemCapacity = fragments.maxOfOrNull { it.gpuMemoryUsage } ?: 0L,
        "",
        -1,
        TraceWorkload(
            fragments,
            checkpointInterval,
            checkpointDuration,
            checkpointIntervalScaling,
            scalingPolicy,
            name,
            usedResources,
        ),
    )
}

fun runTest(
    topology: List<ClusterSpec>,
    workload: ArrayList<Task>,
    failureModelSpec: FailureModelSpec? = null,
    computeScheduler: ComputeScheduler =
        FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(1.0), RamFilter(1.0)),
            weighers = listOf(CoreRamWeigher(multiplier = 1.0)),
        ),
): TestComputeMonitor {
    val monitor = TestComputeMonitor()

    runSimulation {
        val seed = 0L
        Provisioner(dispatcher, seed).use { provisioner ->

            val startTimeLong = workload.minOf { it.submissionTime }
            val startTime = Duration.ofMillis(startTimeLong)

            provisioner.runSteps(
                setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
                registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor, exportInterval = Duration.ofMinutes(1), startTime),
                setupHosts(serviceDomain = "compute.opendc.org", topology, startTimeLong),
            )

            val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
            service.setTasksExpected(workload.size)
            service.setMetricReader(provisioner.getMonitor())

            service.replay(timeSource, ArrayDeque(workload), failureModelSpec = failureModelSpec)
        }
    }

    return monitor
}

class TestComputeMonitor : ComputeMonitor {
    var taskCpuDemands = mutableMapOf<String, ArrayList<Double>>()
    var taskCpuSupplied = mutableMapOf<String, ArrayList<Double>>()
    var taskGpuDemands = mutableMapOf<String, ArrayList<Double?>?>()
    var taskGpuSupplied = mutableMapOf<String, ArrayList<Double?>?>()

    override fun record(reader: TaskTableReader) {
        val taskName: String = reader.taskInfo.name

        if (taskName in taskCpuDemands) {
            taskCpuDemands[taskName]?.add(reader.cpuDemand)
            taskCpuSupplied[taskName]?.add(reader.cpuUsage)
        } else {
            taskCpuDemands[taskName] = arrayListOf(reader.cpuDemand)
            taskCpuSupplied[taskName] = arrayListOf(reader.cpuUsage)
        }
        if (taskName in taskGpuDemands) {
            taskGpuDemands[taskName]?.add(reader.gpuDemand)
            taskGpuSupplied[taskName]?.add(reader.gpuUsage)
        } else {
            taskGpuDemands[taskName] = arrayListOf(reader.gpuDemand)
            taskGpuSupplied[taskName] = arrayListOf(reader.gpuUsage)
        }
    }

    var attemptsSuccess = 0
    var attemptsFailure = 0
    var attemptsError = 0
    var tasksPending = 0
    var tasksActive = 0
    var tasksTerminated = 0
    var tasksCompleted = 0

    var timestamps = ArrayList<Long>()
    var absoluteTimestamps = ArrayList<Long>()

    var maxTimestamp = 0L

    override fun record(reader: ServiceTableReader) {
        attemptsSuccess = reader.attemptsSuccess
        attemptsFailure = reader.attemptsFailure
        attemptsError = 0
        tasksPending = reader.tasksPending
        tasksActive = reader.tasksActive
        tasksTerminated = reader.tasksTerminated
        tasksCompleted = reader.tasksCompleted

        timestamps.add(reader.timestamp.toEpochMilli())
        absoluteTimestamps.add(reader.timestampAbsolute.toEpochMilli())
        maxTimestamp = reader.timestamp.toEpochMilli()
    }

    var hostCpuDemands = mutableMapOf<String, ArrayList<Double>>()
    var hostCpuSupplied = mutableMapOf<String, ArrayList<Double>>()
    var hostCpuIdleTimes = mutableMapOf<String, ArrayList<Long>>()
    var hostCpuActiveTimes = mutableMapOf<String, ArrayList<Long>>()
    var hostCpuStealTimes = mutableMapOf<String, ArrayList<Long>>()
    var hostCpuLostTimes = mutableMapOf<String, ArrayList<Long>>()

    var hostGpuDemands = mutableMapOf<String, ArrayList<ArrayList<Double>>>()
    var hostGpuSupplied = mutableMapOf<String, ArrayList<ArrayList<Double>>>()
    var hostGpuIdleTimes = mutableMapOf<String, ArrayList<ArrayList<Long>>>()
    var hostGpuActiveTimes = mutableMapOf<String, ArrayList<ArrayList<Long>>>()
    var hostGpuStealTimes = mutableMapOf<String, ArrayList<ArrayList<Long>>>()
    var hostGpuLostTimes = mutableMapOf<String, ArrayList<ArrayList<Long>>>()

    var hostPowerDraws = mutableMapOf<String, ArrayList<Double>>()
    var hostEnergyUsages = mutableMapOf<String, ArrayList<Double>>()

    override fun record(reader: HostTableReader) {
        val hostName: String = reader.hostInfo.name

        if (!(hostName in hostCpuDemands)) {
            hostCpuIdleTimes[hostName] = ArrayList()
            hostCpuActiveTimes[hostName] = ArrayList()
            hostCpuStealTimes[hostName] = ArrayList()
            hostCpuLostTimes[hostName] = ArrayList()

            hostCpuDemands[hostName] = ArrayList()
            hostCpuSupplied[hostName] = ArrayList()
            hostPowerDraws[hostName] = ArrayList()
            hostEnergyUsages[hostName] = ArrayList()
        }
        if (hostName !in hostGpuDemands) {
            hostGpuDemands[hostName] = ArrayList()
            hostGpuSupplied[hostName] = ArrayList()
            hostGpuIdleTimes[hostName] = ArrayList()
            hostGpuActiveTimes[hostName] = ArrayList()
            hostGpuStealTimes[hostName] = ArrayList()
            hostGpuLostTimes[hostName] = ArrayList()
        }

        hostCpuDemands[hostName]?.add(reader.cpuDemand)
        hostCpuSupplied[hostName]?.add(reader.cpuUsage)
        hostCpuIdleTimes[hostName]?.add(reader.cpuIdleTime)
        hostCpuActiveTimes[hostName]?.add(reader.cpuActiveTime)
        hostCpuStealTimes[hostName]?.add(reader.cpuStealTime)
        hostCpuLostTimes[hostName]?.add(reader.cpuLostTime)

        hostGpuDemands[hostName]?.add(reader.gpuDemands)
        hostGpuSupplied[hostName]?.add(reader.gpuUsages)
        hostGpuIdleTimes[hostName]?.add(reader.gpuIdleTimes)
        hostGpuActiveTimes[hostName]?.add(reader.gpuActiveTimes)
        hostGpuStealTimes[hostName]?.add(reader.gpuStealTimes)
        hostGpuLostTimes[hostName]?.add(reader.gpuLostTimes)

        hostPowerDraws[hostName]?.add(reader.powerDraw)
        hostEnergyUsages[hostName]?.add(reader.energyUsage)
    }

    var powerDraws = ArrayList<Double>()
    var energyUsages = ArrayList<Double>()

    var carbonIntensities = ArrayList<Double>()
    var carbonEmissions = ArrayList<Double>()

    override fun record(reader: PowerSourceTableReader) {
        powerDraws.add(reader.powerDraw)
        energyUsages.add(reader.energyUsage)

        carbonIntensities.add(reader.carbonIntensity)
        carbonEmissions.add(reader.carbonEmission)
    }
}
