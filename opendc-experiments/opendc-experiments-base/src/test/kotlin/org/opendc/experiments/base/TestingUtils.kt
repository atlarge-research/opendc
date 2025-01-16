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
import org.opendc.compute.simulator.telemetry.table.HostTableReader
import org.opendc.compute.simulator.telemetry.table.PowerSourceTableReader
import org.opendc.compute.simulator.telemetry.table.ServiceTableReader
import org.opendc.compute.simulator.telemetry.table.TaskTableReader
import org.opendc.compute.topology.clusterTopology
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.compute.workload.Task
import org.opendc.experiments.base.experiment.specs.FailureModelSpec
import org.opendc.experiments.base.runner.replay
import org.opendc.simulator.compute.workload.TraceFragment
import org.opendc.simulator.compute.workload.TraceWorkload
import org.opendc.simulator.kotlin.runSimulation
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
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
): Task {
    return Task(
        UUID.nameUUIDFromBytes(name.toByteArray()),
        name,
        fragments.maxOf { it.coreCount },
        fragments.maxOf { it.cpuUsage },
        memCapacity,
        1800000.0,
        LocalDateTime.parse(submissionTime).atZone(ZoneId.systemDefault()).toInstant(),
        duration,
        TraceWorkload(
            fragments,
            checkpointInterval,
            checkpointDuration,
            checkpointIntervalScaling,
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

            val startTimeLong = workload.minOf { it.submissionTime }.toEpochMilli()

            provisioner.runSteps(
                setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
                registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor, exportInterval = Duration.ofMinutes(1)),
                setupHosts(serviceDomain = "compute.opendc.org", topology, startTimeLong),
            )

            val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
            service.setTasksExpected(workload.size)
            service.setMetricReader(provisioner.getMonitor())

            service.replay(timeSource, workload, failureModelSpec = failureModelSpec)
        }
    }

    return monitor
}

class TestComputeMonitor : ComputeMonitor {
    var taskCpuDemands = mutableMapOf<String, ArrayList<Double>>()
    var taskCpuSupplied = mutableMapOf<String, ArrayList<Double>>()

    override fun record(reader: TaskTableReader) {
        val taskName: String = reader.taskInfo.name

        if (taskName in taskCpuDemands) {
            taskCpuDemands[taskName]?.add(reader.cpuDemand)
            taskCpuSupplied[taskName]?.add(reader.cpuUsage)
        } else {
            taskCpuDemands[taskName] = arrayListOf(reader.cpuDemand)
            taskCpuSupplied[taskName] = arrayListOf(reader.cpuUsage)
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

    var hostIdleTimes = mutableMapOf<String, ArrayList<Long>>()
    var hostActiveTimes = mutableMapOf<String, ArrayList<Long>>()
    var hostStealTimes = mutableMapOf<String, ArrayList<Long>>()
    var hostLostTimes = mutableMapOf<String, ArrayList<Long>>()

    var hostCpuDemands = mutableMapOf<String, ArrayList<Double>>()
    var hostCpuSupplied = mutableMapOf<String, ArrayList<Double>>()
    var hostPowerDraws = mutableMapOf<String, ArrayList<Double>>()
    var hostEnergyUsages = mutableMapOf<String, ArrayList<Double>>()

    override fun record(reader: HostTableReader) {
        val hostName: String = reader.host.name

        if (!(hostName in hostCpuDemands)) {
            hostIdleTimes[hostName] = ArrayList()
            hostActiveTimes[hostName] = ArrayList()
            hostStealTimes[hostName] = ArrayList()
            hostLostTimes[hostName] = ArrayList()

            hostCpuDemands[hostName] = ArrayList()
            hostCpuSupplied[hostName] = ArrayList()
            hostPowerDraws[hostName] = ArrayList()
            hostEnergyUsages[hostName] = ArrayList()
        }

        hostIdleTimes[hostName]?.add(reader.cpuIdleTime)
        hostActiveTimes[hostName]?.add(reader.cpuActiveTime)
        hostStealTimes[hostName]?.add(reader.cpuStealTime)
        hostLostTimes[hostName]?.add(reader.cpuLostTime)

        hostCpuDemands[hostName]?.add(reader.cpuDemand)
        hostCpuSupplied[hostName]?.add(reader.cpuUsage)
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
