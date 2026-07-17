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

package org.opendc.sdk.model.serialization

import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.opendc.sdk.model.checkpoint.CheckpointSpec
import org.opendc.sdk.model.dsl.experiment
import org.opendc.sdk.model.dsl.filterScheduler
import org.opendc.sdk.model.dsl.minutes
import org.opendc.sdk.model.dsl.prefabScheduler
import org.opendc.sdk.model.dsl.scenario
import org.opendc.sdk.model.dsl.timeShiftScheduler
import org.opendc.sdk.model.dsl.traceWorkload
import org.opendc.sdk.model.export.AllColumns
import org.opendc.sdk.model.export.ColumnSelection
import org.opendc.sdk.model.export.ExportColumnsSpec
import org.opendc.sdk.model.export.ExportSpec
import org.opendc.sdk.model.export.OnlyColumns
import org.opendc.sdk.model.export.OutputFileSpec
import org.opendc.sdk.model.failure.ConstantDistributionSpec
import org.opendc.sdk.model.failure.CustomFailureSpec
import org.opendc.sdk.model.failure.DistributionSpec
import org.opendc.sdk.model.failure.ExponentialDistributionSpec
import org.opendc.sdk.model.failure.FailureModelSpec
import org.opendc.sdk.model.failure.FailurePrefabSpec
import org.opendc.sdk.model.failure.GammaDistributionSpec
import org.opendc.sdk.model.failure.LogNormalDistributionSpec
import org.opendc.sdk.model.failure.NoFailureSpec
import org.opendc.sdk.model.failure.NormalDistributionSpec
import org.opendc.sdk.model.failure.ParetoDistributionSpec
import org.opendc.sdk.model.failure.PrefabFailureSpec
import org.opendc.sdk.model.failure.TraceBasedFailureSpec
import org.opendc.sdk.model.failure.UniformDistributionSpec
import org.opendc.sdk.model.failure.WeibullDistributionSpec
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.resource.UriReference
import org.opendc.sdk.model.sampleTopology
import org.opendc.sdk.model.sampleWorkload
import org.opendc.sdk.model.scheduler.AllocationPolicySpec
import org.opendc.sdk.model.scheduler.ComputeHostFilterSpec
import org.opendc.sdk.model.scheduler.CoreRamWeigherSpec
import org.opendc.sdk.model.scheduler.DifferentHostFilterSpec
import org.opendc.sdk.model.scheduler.FilterAllocationPolicySpec
import org.opendc.sdk.model.scheduler.HostFilterSpec
import org.opendc.sdk.model.scheduler.HostWeigherSpec
import org.opendc.sdk.model.scheduler.InstanceCountFilterSpec
import org.opendc.sdk.model.scheduler.InstanceCountWeigherSpec
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicySpec
import org.opendc.sdk.model.scheduler.RamFilterSpec
import org.opendc.sdk.model.scheduler.RamWeigherSpec
import org.opendc.sdk.model.scheduler.SameHostFilterSpec
import org.opendc.sdk.model.scheduler.SchedulerNameSpec
import org.opendc.sdk.model.scheduler.TaskStopperSpec
import org.opendc.sdk.model.scheduler.TimeShiftAllocationPolicySpec
import org.opendc.sdk.model.scheduler.VCpuCapacityFilterSpec
import org.opendc.sdk.model.scheduler.VCpuCapacityWeigherSpec
import org.opendc.sdk.model.scheduler.VCpuFilterSpec
import org.opendc.sdk.model.scheduler.VCpuWeigherSpec
import org.opendc.sdk.model.topology.BatteryPolicy
import org.opendc.sdk.model.topology.BestEffort
import org.opendc.sdk.model.topology.ConstantVirtualizationOverheadSpec
import org.opendc.sdk.model.topology.DistributionPolicy
import org.opendc.sdk.model.topology.DoubleThresholdPolicy
import org.opendc.sdk.model.topology.EqualShare
import org.opendc.sdk.model.topology.FirstFit
import org.opendc.sdk.model.topology.FixedShare
import org.opendc.sdk.model.topology.MaxMinFairness
import org.opendc.sdk.model.topology.NoVirtualizationOverheadSpec
import org.opendc.sdk.model.topology.RunningMeanPlusPolicy
import org.opendc.sdk.model.topology.RunningMeanPolicy
import org.opendc.sdk.model.topology.RunningMedianPolicy
import org.opendc.sdk.model.topology.RunningQuartilesPolicy
import org.opendc.sdk.model.topology.ShareBasedVirtualizationOverheadSpec
import org.opendc.sdk.model.topology.SingleThresholdPolicy
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.topology.VirtualizationOverheadSpec
import org.opendc.sdk.model.workload.ScalingPolicySpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec
import org.opendc.sdk.model.workload.WorkloadSpec
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that every SDK model type survives a [SdkJson] encode/decode cycle unchanged, and that the
 * polymorphic wire format uses a `type` discriminator with the declared [@SerialName] values.
 *
 * Unit-valued fields ([org.opendc.common.units]) serialize through lossy string formatters, so the
 * fixtures deliberately use whole-unit magnitudes (integral GHz/GiB, whole-millisecond durations,
 * sub-kilowatt powers) that reconstruct to the exact same value.
 */
class RoundTripTest {
    @Test
    fun `representative experiment round-trips`() {
        val builtExperiment =
            experiment {
                name = "capacity-sweep"
                runs = 3
                initialSeed = 42
                topology(sampleTopology)
                workload(
                    traceWorkload(
                        source = UriReference("file:///traces/bitbrains"),
                        sampleFraction = 0.5,
                        submissionTime = "2022-01-01T00:00",
                        scalingPolicy = ScalingPolicySpec.Perfect,
                        deferAll = true,
                    ),
                )
                workload(sampleWorkload)
                allocationPolicy(prefabScheduler(SchedulerNameSpec.CoreMem))
                allocationPolicy(
                    filterScheduler {
                        subsetSize = 2
                        filter(ComputeHostFilterSpec)
                        filter(RamFilterSpec(1.5))
                        weigher(RamWeigherSpec(2.0))
                    },
                )
                failureModel(PrefabFailureSpec(FailurePrefabSpec.G5k06Exp))
                failureModel(NoFailureSpec)
                exportModel(ExportSpec())
                checkpointModel(CheckpointSpec())
                checkpointModel(null)
                maxNumFailures(5)
                maxNumFailures(10)
            }

        val text = SdkJson.encodeToString(builtExperiment)
        assertEquals(builtExperiment, SdkJson.decodeExperiment(text))
    }

    @Test
    fun `representative scenario round-trips`() {
        val builtScenario =
            scenario {
                id = 7
                name = "s-7"
                runs = 2
                initialSeed = 9
                maxNumFailures = 3
                topology(sampleTopology)
                workload(sampleWorkload)
                allocationPolicy(
                    timeShiftScheduler {
                        windowSize = 200
                        subsetSize = 3
                        forecast = false
                        shortForecastThreshold = 0.25
                        longForecastThreshold = 0.4
                        forecastSize = 12
                        memorize = false
                        taskStopper = TaskStopperSpec(windowSize = 100, forecast = false, forecastThreshold = 0.5, forecastSize = 6)
                        filter(ComputeHostFilterSpec)
                        filter(VCpuFilterSpec(2.0))
                        weigher(CoreRamWeigherSpec(1.5))
                    },
                )
                exportModel =
                    ExportSpec(
                        exportInterval = 10.minutes,
                        printFrequency = null,
                        columns = ExportColumnsSpec(host = OnlyColumns(setOf("timestamp", "cpu_usage")), task = AllColumns),
                        filesToExport = listOf(OutputFileSpec.HOST, OutputFileSpec.TASK),
                    )
                failureModel =
                    CustomFailureSpec(
                        interArrival = ExponentialDistributionSpec(mean = 3600.0),
                        duration = ConstantDistributionSpec(value = 300.0),
                        hostFraction = UniformDistributionSpec(upper = 0.5, lower = 0.1),
                    )
                checkpointModel = CheckpointSpec()
            }

        val text = SdkJson.encodeToString(builtScenario)
        assertEquals(builtScenario, SdkJson.decodeScenario(text))
    }

    @TestFactory
    fun `allocation policies round-trip`(): List<DynamicTest> {
        val policies: List<AllocationPolicySpec> =
            listOf(
                PrefabAllocationPolicySpec(SchedulerNameSpec.Random),
                FilterAllocationPolicySpec(
                    filters = listOf(ComputeHostFilterSpec, RamFilterSpec(1.5)),
                    weighers = listOf(RamWeigherSpec(1.0), CoreRamWeigherSpec(2.0)),
                    subsetSize = 3,
                ),
                TimeShiftAllocationPolicySpec(
                    filters = listOf(ComputeHostFilterSpec),
                    weighers = listOf(VCpuWeigherSpec(1.0)),
                    taskStopper = TaskStopperSpec(),
                ),
            )
        return policies.map { policy ->
            dynamicTest(policy.toString()) { assertEquals(policy, roundTrip<AllocationPolicySpec>(policy)) }
        }
    }

    @TestFactory
    fun `host filters round-trip`(): List<DynamicTest> {
        val filters: List<HostFilterSpec> =
            listOf(
                ComputeHostFilterSpec,
                SameHostFilterSpec,
                DifferentHostFilterSpec,
                InstanceCountFilterSpec(limit = 4),
                RamFilterSpec(allocationRatio = 1.5),
                VCpuCapacityFilterSpec,
                VCpuFilterSpec(allocationRatio = 2.0),
            )
        return filters.map { filter ->
            dynamicTest(filter.toString()) { assertEquals(filter, roundTrip<HostFilterSpec>(filter)) }
        }
    }

    @TestFactory
    fun `host weighers round-trip`(): List<DynamicTest> {
        val weighers: List<HostWeigherSpec> =
            listOf(
                RamWeigherSpec(1.0),
                CoreRamWeigherSpec(2.0),
                InstanceCountWeigherSpec(0.5),
                VCpuCapacityWeigherSpec(1.5),
                VCpuWeigherSpec(3.0),
            )
        return weighers.map { weigher ->
            dynamicTest(weigher.toString()) { assertEquals(weigher, roundTrip<HostWeigherSpec>(weigher)) }
        }
    }

    @TestFactory
    fun `failure models round-trip`(): List<DynamicTest> {
        val models: List<FailureModelSpec> =
            listOf(
                NoFailureSpec,
                TraceBasedFailureSpec(source = NamedReference("availability"), startPoint = 0.25, repeat = false),
                PrefabFailureSpec(FailurePrefabSpec.Lanl05Wbl),
                CustomFailureSpec(
                    interArrival = ExponentialDistributionSpec(3600.0),
                    duration = ConstantDistributionSpec(300.0),
                    hostFraction = UniformDistributionSpec(upper = 0.5, lower = 0.1),
                ),
            )
        return models.map { model ->
            dynamicTest(model.toString()) { assertEquals(model, roundTrip<FailureModelSpec>(model)) }
        }
    }

    @TestFactory
    fun `distributions round-trip`(): List<DynamicTest> =
        distributionCases.map { (distribution, _) ->
            dynamicTest(distribution.toString()) { assertEquals(distribution, roundTrip<DistributionSpec>(distribution)) }
        }

    @TestFactory
    fun `workloads round-trip`(): List<DynamicTest> {
        val workloads: List<WorkloadSpec> =
            listOf(
                TraceWorkloadSpec(
                    source = NamedReference("bitbrains"),
                    sampleFraction = 0.75,
                    submissionTime = "2022-01-01T00:00",
                    scalingPolicy = ScalingPolicySpec.Perfect,
                    deferAll = true,
                ),
                sampleWorkload,
            )
        return workloads.map { workload ->
            dynamicTest(workload::class.simpleName ?: "workload") { assertEquals(workload, roundTrip<WorkloadSpec>(workload)) }
        }
    }

    @TestFactory
    fun `topologies round-trip`(): List<DynamicTest> {
        val topologies: List<TopologySpec> = listOf(sampleTopology)
        return topologies.map { topology ->
            dynamicTest(topology::class.simpleName ?: "topology") { assertEquals(topology, roundTrip<TopologySpec>(topology)) }
        }
    }

    @TestFactory
    fun `battery policies round-trip`(): List<DynamicTest> {
        val policies: List<BatteryPolicy> =
            listOf(
                SingleThresholdPolicy(carbonThreshold = 150.0),
                DoubleThresholdPolicy(lowerThreshold = 100.0, upperThreshold = 300.0),
                RunningMeanPolicy(startingThreshold = 200.0, windowSize = 24),
                RunningMeanPlusPolicy(startingThreshold = 200.0, windowSize = 24),
                RunningMedianPolicy(startingThreshold = 200.0, windowSize = 24),
                RunningQuartilesPolicy(startingThreshold = 200.0, windowSize = 24),
            )
        return policies.map { policy ->
            dynamicTest(policy.toString()) { assertEquals(policy, roundTrip<BatteryPolicy>(policy)) }
        }
    }

    @TestFactory
    fun `distribution policies round-trip`(): List<DynamicTest> {
        val policies: List<DistributionPolicy> =
            listOf(
                MaxMinFairness,
                BestEffort(updateIntervalMs = 2000),
                EqualShare,
                FirstFit,
                FixedShare(shareRatio = 0.5),
            )
        return policies.map { policy ->
            dynamicTest(policy.toString()) { assertEquals(policy, roundTrip<DistributionPolicy>(policy)) }
        }
    }

    @TestFactory
    fun `virtualization overheads round-trip`(): List<DynamicTest> {
        val overheads: List<VirtualizationOverheadSpec> =
            listOf(
                NoVirtualizationOverheadSpec,
                ConstantVirtualizationOverheadSpec(percentageOverhead = 12.5),
                ConstantVirtualizationOverheadSpec(percentageOverhead = null),
                ShareBasedVirtualizationOverheadSpec,
            )
        return overheads.map { overhead ->
            dynamicTest(overhead.toString()) { assertEquals(overhead, roundTrip<VirtualizationOverheadSpec>(overhead)) }
        }
    }

    @TestFactory
    fun `column selections round-trip`(): List<DynamicTest> {
        val selections: List<ColumnSelection> =
            listOf(
                AllColumns,
                OnlyColumns(columns = setOf("timestamp", "cpu_usage", "power_draw")),
            )
        return selections.map { selection ->
            dynamicTest(selection.toString()) { assertEquals(selection, roundTrip<ColumnSelection>(selection)) }
        }
    }

    @Test
    fun `distributions serialize their fields in declaration order`() {
        distributionCases.forEach { (distribution, fields) ->
            assertFieldOrder(encode<DistributionSpec>(distribution), fields)
        }
    }

    @Test
    fun `polymorphic types use a type discriminator with the declared serial names`() {
        assertTrue(encode<AllocationPolicySpec>(PrefabAllocationPolicySpec()).contains("\"type\": \"prefab\""))
        assertTrue(encode<HostFilterSpec>(ComputeHostFilterSpec).contains("\"type\": \"compute\""))
        assertTrue(encode<HostWeigherSpec>(CoreRamWeigherSpec()).contains("\"type\": \"coreRam\""))
        assertTrue(encode<FailureModelSpec>(NoFailureSpec).contains("\"type\": \"none\""))
        assertTrue(encode<DistributionSpec>(LogNormalDistributionSpec(1.0, 2.0)).contains("\"type\": \"log-normal\""))
        assertTrue(encode<WorkloadSpec>(TraceWorkloadSpec(NamedReference("t"))).contains("\"type\": \"trace\""))
        assertTrue(encode<DistributionPolicy>(MaxMinFairness).contains("\"type\": \"maxMinFairness\""))
        assertTrue(encode<BatteryPolicy>(SingleThresholdPolicy(100.0)).contains("\"type\": \"single\""))
        assertTrue(encode<VirtualizationOverheadSpec>(ShareBasedVirtualizationOverheadSpec).contains("\"type\": \"shareBased\""))
        assertTrue(encode<ColumnSelection>(AllColumns).contains("\"type\": \"all\""))
    }

    private val distributionCases: List<Pair<DistributionSpec, List<String>>> =
        listOf(
            ConstantDistributionSpec(value = 5.0) to listOf("type", "value"),
            ExponentialDistributionSpec(mean = 2.0) to listOf("type", "mean"),
            GammaDistributionSpec(shape = 2.0, scale = 3.0) to listOf("type", "shape", "scale"),
            LogNormalDistributionSpec(scale = 1.0, shape = 2.0) to listOf("type", "scale", "shape"),
            NormalDistributionSpec(mean = 1.0, std = 2.0) to listOf("type", "mean", "std"),
            ParetoDistributionSpec(scale = 1.0, shape = 2.0) to listOf("type", "scale", "shape"),
            UniformDistributionSpec(upper = 0.9, lower = 0.1) to listOf("type", "upper", "lower"),
            WeibullDistributionSpec(alpha = 1.0, beta = 2.0) to listOf("type", "alpha", "beta"),
        )
}

private inline fun <reified T> encode(value: T): String = SdkJson.json.encodeToString(value)

private inline fun <reified T> roundTrip(value: T): T = SdkJson.json.decodeFromString(encode(value))

private fun assertFieldOrder(
    json: String,
    fields: List<String>,
) {
    val positions =
        fields.map { field ->
            val index = json.indexOf("\"$field\"")
            assertTrue(index >= 0, "missing field '$field' in:\n$json")
            index
        }
    assertEquals(positions.sorted(), positions, "fields not in declaration order in:\n$json")
}
