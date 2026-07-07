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

package org.opendc.sdk.model

import org.opendc.sdk.model.checkpoint.CheckpointModel
import org.opendc.sdk.model.dsl.ghz
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.dsl.kwatts
import org.opendc.sdk.model.dsl.minutes
import org.opendc.sdk.model.dsl.watts
import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.experiment.Scenario
import org.opendc.sdk.model.export.ExportModel
import org.opendc.sdk.model.failure.NoFailure
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicy
import org.opendc.sdk.model.scheduler.SchedulerName
import org.opendc.sdk.model.topology.Battery
import org.opendc.sdk.model.topology.Cluster
import org.opendc.sdk.model.topology.Cpu
import org.opendc.sdk.model.topology.DoubleThresholdPolicy
import org.opendc.sdk.model.topology.EqualShare
import org.opendc.sdk.model.topology.FixedShare
import org.opendc.sdk.model.topology.Host
import org.opendc.sdk.model.topology.Memory
import org.opendc.sdk.model.topology.PowerModel
import org.opendc.sdk.model.topology.PowerModelType
import org.opendc.sdk.model.topology.PowerSource
import org.opendc.sdk.model.topology.Topology
import org.opendc.sdk.model.workload.InlineWorkload
import org.opendc.sdk.model.workload.ScalingPolicy
import org.opendc.sdk.model.workload.Task
import org.opendc.sdk.model.workload.TaskFragment

/**
 * Shared, constructor-built test data. This is the single source of truth for the SDK-model test suite;
 * the `valid*` fixtures are minimal well-formed models and the `sample*` fixtures exercise many fields.
 * All fixtures are built with whole-unit magnitudes so they survive a JSON round-trip unchanged.
 */

public val validMemory: Memory = Memory(size = 64.gib)

public val validCpu: Cpu = Cpu(coreCount = 8, coreSpeed = 3.ghz)

public val validHost: Host = Host(cpu = validCpu, memory = validMemory)

public val validTopology: Topology = Topology(listOf(Cluster(hosts = listOf(validHost))))

public val validTask: Task =
    Task(
        id = 0,
        name = "task",
        submissionTime = 0.minutes,
        duration = 10.minutes,
        cpuCoreCount = 1,
        cpuCapacity = 1.ghz,
        memory = 1.gib,
        fragments = listOf(TaskFragment(duration = 10.minutes, cpuUsage = 1.ghz)),
    )

public val validWorkload: InlineWorkload = InlineWorkload(listOf(validTask))

public val validExperiment: Experiment = Experiment(topologies = setOf(validTopology), workloads = setOf(validWorkload))

public val sampleHost: Host =
    Host(
        name = "compute-host",
        count = 4,
        cpu = Cpu(coreCount = 8, coreSpeed = 3.ghz, count = 2, vendor = "AMD", modelName = "EPYC", architecture = "Zen4"),
        memory = Memory(size = 32.gib, speed = 3.ghz, vendor = "Samsung"),
        cpuPowerModel = PowerModel(PowerModelType.SQUARE, 500.watts, 100.watts, 350.watts),
        cpuDistribution = FixedShare(0.5),
        gpuDistribution = EqualShare,
    )

public val sampleCluster: Cluster =
    Cluster(
        name = "cluster-a",
        count = 2,
        hosts = listOf(sampleHost),
        powerSource = PowerSource(name = "grid", maxPower = 50.kwatts, carbon = NamedReference("carbon-trace")),
        battery =
            Battery(
                name = "cell",
                capacity = 100.0,
                chargingSpeed = 1000.0,
                initialCharge = 20.0,
                policy = DoubleThresholdPolicy(lowerThreshold = 100.0, upperThreshold = 300.0),
                embodiedCarbon = 50.0,
                expectedLifetime = 10.0,
            ),
    )

public val sampleTopology: Topology = Topology(listOf(sampleCluster))

public val sampleRootTask: Task =
    Task(
        id = 0,
        name = "t0",
        submissionTime = 0.minutes,
        duration = 10.minutes,
        cpuCoreCount = 4,
        cpuCapacity = 2.ghz,
        memory = 8.gib,
        fragments =
            listOf(
                TaskFragment(duration = 5.minutes, cpuUsage = 2.ghz),
                TaskFragment(duration = 5.minutes, cpuUsage = 1.ghz, gpuUsage = 1.ghz, gpuMemory = 2.gib),
            ),
    )

public val sampleLeafTask: Task =
    Task(
        id = 1,
        name = "t1",
        submissionTime = 2.minutes,
        duration = 20.minutes,
        cpuCoreCount = 8,
        cpuCapacity = 3.ghz,
        memory = 16.gib,
        fragments = listOf(TaskFragment(duration = 20.minutes, cpuUsage = 3.ghz)),
        deferrable = true,
        deadline = 60.minutes,
        parents = setOf(0),
    )

public val sampleWorkload: InlineWorkload = InlineWorkload(listOf(sampleRootTask, sampleLeafTask), ScalingPolicy.Perfect)

public val sampleScenario: Scenario =
    Scenario(
        topology = sampleTopology,
        workload = sampleWorkload,
        allocationPolicy = PrefabAllocationPolicy(SchedulerName.CoreMem),
        exportModel = ExportModel(exportInterval = 10.minutes),
        failureModel = NoFailure,
        checkpointModel = CheckpointModel(),
        maxNumFailures = 5,
        runs = 3,
        initialSeed = 42,
        id = 0,
        name = "sample",
    )

public val sampleExperiment: Experiment =
    Experiment(
        topologies = setOf(sampleTopology),
        workloads = setOf(sampleWorkload),
        allocationPolicies = setOf(PrefabAllocationPolicy(SchedulerName.Mem), PrefabAllocationPolicy(SchedulerName.CoreMem)),
        failureModels = setOf(NoFailure),
        maxNumFailures = setOf(5, 10),
        runs = 3,
        initialSeed = 1,
        name = "sample-sweep",
    )
