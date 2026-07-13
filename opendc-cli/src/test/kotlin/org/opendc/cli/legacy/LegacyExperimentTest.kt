/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.cli.legacy

import org.opendc.common.units.DataRate
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.Power
import org.opendc.common.units.TimeDelta
import org.opendc.sdk.model.checkpoint.CheckpointModel
import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.export.AllColumns
import org.opendc.sdk.model.export.OnlyColumns
import org.opendc.sdk.model.export.OutputFile
import org.opendc.sdk.model.failure.CustomFailure
import org.opendc.sdk.model.failure.ExponentialDistribution
import org.opendc.sdk.model.failure.FailurePrefab
import org.opendc.sdk.model.failure.LogNormalDistribution
import org.opendc.sdk.model.failure.NoFailure
import org.opendc.sdk.model.failure.PrefabFailure
import org.opendc.sdk.model.failure.TraceBasedFailure
import org.opendc.sdk.model.failure.UniformDistribution
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.scheduler.ComputeHostFilter
import org.opendc.sdk.model.scheduler.CoreRamWeigher
import org.opendc.sdk.model.scheduler.FilterAllocationPolicy
import org.opendc.sdk.model.scheduler.InstanceCountFilter
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicy
import org.opendc.sdk.model.scheduler.RamFilter
import org.opendc.sdk.model.scheduler.RamWeigher
import org.opendc.sdk.model.scheduler.SchedulerName
import org.opendc.sdk.model.scheduler.TaskStopper
import org.opendc.sdk.model.scheduler.TimeShiftAllocationPolicy
import org.opendc.sdk.model.scheduler.VCpuCapacityWeigher
import org.opendc.sdk.model.scheduler.VCpuFilter
import org.opendc.sdk.model.topology.BestEffort
import org.opendc.sdk.model.topology.ConstantVirtualizationOverhead
import org.opendc.sdk.model.topology.EqualShare
import org.opendc.sdk.model.topology.FixedShare
import org.opendc.sdk.model.topology.MaxMinFairness
import org.opendc.sdk.model.topology.PowerModelType
import org.opendc.sdk.model.topology.RunningMeanPolicy
import org.opendc.sdk.model.topology.ShareBasedVirtualizationOverhead
import org.opendc.sdk.model.workload.ScalingPolicy
import org.opendc.sdk.model.workload.TraceWorkload
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Proves that experiments written in the deprecated `opendc-experiments-base` JSON format still load.
 *
 * The four `opendc-demos` experiments are the validation set. They and their topologies are copied
 * here byte for byte, in the demo's own directory layout, so what is under test is the format exactly
 * as it was shipped; a fifth fixture covers the parts of the old format the demos never exercised.
 * Each is read against the root of that layout, which is the directory a demo was run from and hence
 * what the old runner resolved its relative paths against.
 *
 * These tests assert *loadability* — that an old document becomes an equivalent, valid SDK
 * [Experiment]. That the simulator then reproduces the old numbers is asserted separately, by
 * `opendc-sdk`.
 */
class LegacyExperimentTest {
    /**
     * Demo 1 — the plainest old experiment there is. It is also the sharpest test of the units,
     * because every magnitude in it is a bare number: `2100` must still mean 2100 MHz, `100000` must
     * still mean 100000 MiB, and `3600` must still mean an hour.
     */
    @Test
    fun `demo 1 loads with its units intact`() {
        val experiment = load("experiments/1.first_experiment_answers/simple_experiment.json")

        assertEquals("1.first_experiment", experiment.name)
        val cluster = experiment.topologies.single().clusters.single()
        val host = cluster.hosts.single()

        assertEquals(279, host.count)
        assertEquals(16, host.cpu.coreCount)
        assertEquals(Frequency.ofMHz(2100), host.cpu.coreSpeed, "a bare coreSpeed still counts MHz")
        assertEquals(DataSize.ofMiB(100000), host.memory.size, "a bare memorySize still counts MiB")
        assertEquals(PowerModelType.LINEAR, host.cpuPowerModel.type)
        assertEquals(Power.ofWatts(180), host.cpuPowerModel.maxPower, "a bare maxPower still counts Watts")
        assertEquals(Power.ofWatts(32), host.cpuPowerModel.idlePower)
        assertEquals(NamedReference("carbon_traces/NL_2021-2024.parquet"), cluster.powerSource.carbon)

        val workload = experiment.workloads.single()
        assertEquals(TraceWorkload(source = NamedReference("workload_traces/surf_week")), workload)

        val export = experiment.exportModels.single()
        assertEquals(
            TimeDelta.ofSec(3600),
            export.exportInterval,
            "the legacy export interval counts seconds, so 3600 is an hour — not 3.6 seconds",
        )
        assertEquals(24, export.printFrequency)
        assertEquals(listOf(OutputFile.HOST, OutputFile.POWER_SOURCE, OutputFile.SERVICE, OutputFile.TASK), export.filesToExport)
    }

    /** Demo 2 — four topologies, each a `sqrt` power model in a different country. */
    @Test
    fun `demo 2 loads every topology it references`() {
        val experiment = load("experiments/2.datacenter_location_answers/location_experiment.json")

        assertEquals(4, experiment.topologies.size)
        val carbon = experiment.topologies.map { it.clusters.single().powerSource.carbon }
        assertEquals(
            listOf("AT", "AU", "BE", "NL").map { NamedReference("carbon_traces/${it}_2021-2024.parquet") }.toSet(),
            carbon.toSet(),
        )
        assertTrue(
            experiment.topologies.all { it.clusters.single().hosts.single().cpuPowerModel.type == PowerModelType.SQRT },
            "every location uses the sqrt power model",
        )
    }

    /** Demo 3 — the same datacenter at four sizes; the host `count` is what varies. */
    @Test
    fun `demo 3 loads each datacenter sizing`() {
        val experiment = load("experiments/3.horizontal_scaling_answers/scaling_experiment.json")

        assertEquals(
            setOf(100, 150, 200, 279),
            experiment.topologies.map { it.clusters.single().hosts.single().count }.toSet(),
        )
    }

    /** Demo 4 — the only demo with failure models, mixing the no-failure baseline with three traces. */
    @Test
    fun `demo 4 loads its failure models`() {
        val experiment = load("experiments/4.failures_answers/failure_experiment.json")

        assertContains(experiment.failureModels, NoFailure)
        assertEquals(
            listOf("Facebook", "Instagram", "Netflix")
                .map { NamedReference("failure_traces/${it}_user_reported.parquet") }
                .toSet(),
            experiment.failureModels.filterIsInstance<TraceBasedFailure>().map { it.source }.toSet(),
        )
    }

    /** Every demo experiment is a *valid* SDK experiment, not merely a parseable one. */
    @Test
    fun `every demo experiment validates`() {
        for (fixture in DEMOS) {
            assertEquals(emptyList(), load(fixture).validate(), "$fixture should validate cleanly")
        }
    }

    @Test
    fun `the experiment axes carry over`() {
        val experiment = load(FEATURES)

        assertEquals("features", experiment.name)
        assertEquals(3, experiment.runs)
        assertEquals(7, experiment.initialSeed)
        assertEquals(setOf(5, 20), experiment.maxNumFailures)
        assertEquals(emptyList(), experiment.validate())
    }

    @Test
    fun `a workload keeps its sampling, submission time and scaling policy`() {
        val experiment = load(FEATURES)

        assertEquals(
            TraceWorkload(
                source = NamedReference("workload_traces/surf_week"),
                sampleFraction = 0.5,
                submissionTime = "2024-03-01T00:00:00",
                scalingPolicy = ScalingPolicy.Perfect,
                deferAll = true,
            ),
            experiment.workloads.single(),
        )
    }

    @Test
    fun `prefab, filter and timeshift allocation policies carry over`() {
        val policies = load(FEATURES).allocationPolicies

        assertContains(policies, PrefabAllocationPolicy(SchedulerName.CoreMem))
        assertContains(
            policies,
            FilterAllocationPolicy(
                filters =
                    listOf(
                        ComputeHostFilter,
                        RamFilter(allocationRatio = 1.5),
                        VCpuFilter(allocationRatio = 16.0),
                        InstanceCountFilter(limit = 8),
                    ),
                weighers = listOf(CoreRamWeigher(multiplier = 1.0), VCpuCapacityWeigher(multiplier = -1.0)),
                subsetSize = 2,
            ),
        )
        assertContains(
            policies,
            TimeShiftAllocationPolicy(
                filters = listOf(ComputeHostFilter),
                weighers = listOf(RamWeigher(multiplier = 1.0)),
                memorize = false,
                taskStopper = TaskStopper(windowSize = 168, forecast = true, forecastThreshold = 0.6, forecastSize = 24),
            ),
        )
    }

    @Test
    fun `prefab, trace-based and custom failure models carry over, and a null one means no failure`() {
        val failures = load(FEATURES).failureModels

        assertContains(failures, NoFailure, "a null failure model, like an explicit 'no' one, injects no failures")
        assertContains(failures, PrefabFailure(FailurePrefab.G5k06Exp))
        assertContains(
            failures,
            TraceBasedFailure(
                source = NamedReference("failure_traces/Facebook_user_reported.parquet"),
                startPoint = 0.25,
                repeat = false,
            ),
        )
        assertContains(
            failures,
            CustomFailure(
                interArrival = ExponentialDistribution(mean = 3600.0),
                duration = LogNormalDistribution(scale = 2.0, shape = 0.5),
                hostFraction = UniformDistribution(upper = 0.5, lower = 0.1),
            ),
        )
    }

    /** Checkpoint intervals already counted milliseconds, which is what a bare `TimeDelta` number means. */
    @Test
    fun `checkpoint models keep their milliseconds`() {
        val checkpoints = load(FEATURES).checkpointModels

        assertContains(checkpoints, null)
        assertContains(
            checkpoints,
            CheckpointModel(interval = TimeDelta.ofHours(1), duration = TimeDelta.ofMin(5), intervalScaling = 1.5),
        )
    }

    @Test
    fun `an export model keeps its interval in seconds and its hand-picked columns`() {
        val export = load(FEATURES).exportModels.single()

        assertEquals(TimeDelta.ofSec(300), export.exportInterval)
        assertEquals(OnlyColumns(setOf("timestamp", "host_name", "cpu_utilization")), export.columns.host)
        assertEquals(OnlyColumns(setOf("timestamp", "task_name")), export.columns.task)
        assertEquals(AllColumns, export.columns.service, "a file the legacy config never mentions exports every column")
    }

    @Test
    fun `a GPU host carries over its accelerator, overhead and distribution policy`() {
        val hosts = load(FEATURES).topologies.first { it.clusters.single().name == "GpuCluster" }.clusters.single().hosts
        val (constant, shareBased, unset) = hosts

        val gpu = checkNotNull(constant.gpu)
        assertEquals(4, gpu.count)
        assertEquals(5120, gpu.coreCount)
        assertEquals(Frequency.ofMHz(5000), gpu.coreSpeed)
        assertEquals(DataSize.ofMiB(30517578125), gpu.memory, "a bare GPU memorySize still counts MiB")
        assertEquals(DataRate.ofGBps(900), gpu.memoryBandwidth, "a spelled-out bandwidth is honoured as written")
        assertEquals("Volta", gpu.architecture)
        assertEquals(ConstantVirtualizationOverhead(percentageOverhead = 0.05), gpu.virtualizationOverhead)
        assertEquals(PowerModelType.SQRT, constant.gpuPowerModel.type)
        assertEquals(MaxMinFairness, constant.cpuDistribution)
        assertEquals(BestEffort(updateIntervalMs = 60000), constant.gpuDistribution)

        // The CPU and memory keep the fields the two formats spell differently.
        assertEquals(2, constant.cpu.count)
        assertEquals("Zen2", constant.cpu.architecture)
        assertEquals("DDR4", constant.memory.architecture)
        assertEquals(Frequency.ofMHz(3200), constant.memory.speed)

        assertEquals(ShareBasedVirtualizationOverhead, checkNotNull(shareBased.gpu).virtualizationOverhead)
        assertEquals(FixedShare(shareRatio = 0.5), shareBased.gpuDistribution)

        val unsetOverhead = checkNotNull(unset.gpu).virtualizationOverhead as ConstantVirtualizationOverhead
        assertNull(unsetOverhead.percentageOverhead, "the legacy -1.0 sentinel meant 'unset'")
        assertEquals(EqualShare, unset.gpuDistribution)
    }

    @Test
    fun `a battery cluster carries over its power source, battery and power models`() {
        val cluster = load(FEATURES).topologies.first { it.clusters.single().name == "BatteryCluster" }.clusters.single()

        assertEquals("grid", cluster.powerSource.name)
        assertEquals(Power.ofWatts(50000), cluster.powerSource.maxPower)
        assertEquals(NamedReference("carbon_traces/NL_2021-2024.parquet"), cluster.powerSource.carbon)

        val battery = checkNotNull(cluster.battery)
        assertEquals(0.1, battery.capacity)
        assertEquals(1000.0, battery.chargingSpeed)
        assertEquals(0.05, battery.initialCharge)
        assertEquals(RunningMeanPolicy(startingThreshold = 150.0, windowSize = 24), battery.policy)
        assertEquals(1200.0, battery.embodiedCarbon)
        assertEquals(10.0, battery.expectedLifetime)

        val (mse, asymptotic, constant) = cluster.hosts
        assertEquals(PowerModelType.MSE, mse.cpuPowerModel.type)
        assertEquals(1.5, mse.cpuPowerModel.calibrationFactor)
        assertEquals(PowerModelType.ASYMPTOTIC, asymptotic.cpuPowerModel.type)
        assertEquals(0.7, asymptotic.cpuPowerModel.asymUtil)
        assertEquals(false, asymptotic.cpuPowerModel.dvfs)
        assertEquals(PowerModelType.CONSTANT, constant.cpuPowerModel.type)
        assertEquals(Power.ofWatts(350), constant.cpuPowerModel.power)
    }

    /** A topology is read relative to the given root, not to wherever the experiment file happens to sit. */
    @Test
    fun `relative paths resolve against the root, not the experiment file`() {
        val experiment =
            readLegacyExperiment(File(legacyRoot, "experiments/1.first_experiment_answers/simple_experiment.json"), legacyRoot)

        // The experiment lives two directories below the root, yet its "topologies/..." path resolved.
        assertEquals(1, experiment.topologies.single().clusters.size)
    }

    @Test
    fun `an unreadable legacy document is reported against the field that caused it`() {
        assertFailsWith<LegacyFormatException> { read("""{"workloads": []}""") }
            .also { assertContains(it.message.orEmpty(), "topologies") }

        assertFailsWith<LegacyFormatException> {
            read("""{"topologies": [{"pathToFile": "nowhere.json"}], "workloads": []}""")
        }.also { assertContains(it.message.orEmpty(), "nowhere.json") }

        assertFailsWith<LegacyFormatException> {
            read("""{"topologies": [], "workloads": [], "allocationPolicies": [{"type": "psychic"}]}""")
        }.also { assertContains(it.message.orEmpty(), "psychic") }
    }

    private fun load(fixture: String): Experiment = readLegacyExperiment(File(legacyRoot, fixture), legacyRoot)

    /** Reads a throwaway legacy document, used to pin down the error a malformed one produces. */
    private fun read(json: String): Experiment {
        val file =
            File.createTempFile("legacy-experiment", ".json").apply {
                deleteOnExit()
                writeText(json)
            }
        return readLegacyExperiment(file, checkNotNull(file.parentFile))
    }

    /** The root of the copied demo layout: what a demo was run from, and so what its paths resolve against. */
    private val legacyRoot: File =
        File(checkNotNull(javaClass.classLoader.getResource("legacy")) { "missing legacy fixtures" }.toURI())

    private companion object {
        val DEMOS =
            listOf(
                "experiments/1.first_experiment_answers/simple_experiment.json",
                "experiments/2.datacenter_location_answers/location_experiment.json",
                "experiments/3.horizontal_scaling_answers/scaling_experiment.json",
                "experiments/4.failures_answers/failure_experiment.json",
            )
        const val FEATURES = "experiments/features/features_experiment.json"
    }
}
