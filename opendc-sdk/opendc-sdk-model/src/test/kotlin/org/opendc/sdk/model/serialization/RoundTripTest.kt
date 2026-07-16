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

import kotlinx.serialization.decodeFromString
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
import org.opendc.sdk.model.dsl.topology
import org.opendc.sdk.model.dsl.traceWorkload
import org.opendc.sdk.model.export.AllColumns
import org.opendc.sdk.model.export.ColumnSelection
import org.opendc.sdk.model.export.ExportColumnsSpec
import org.opendc.sdk.model.export.ExportSpec
import org.opendc.sdk.model.export.OnlyColumns
import org.opendc.sdk.model.export.OutputFile
import org.opendc.sdk.model.failure.ConstantDistribution
import org.opendc.sdk.model.failure.CustomFailure
import org.opendc.sdk.model.failure.Distribution
import org.opendc.sdk.model.failure.ExponentialDistribution
import org.opendc.sdk.model.failure.FailureModel
import org.opendc.sdk.model.failure.FailurePrefab
import org.opendc.sdk.model.failure.GammaDistribution
import org.opendc.sdk.model.failure.LogNormalDistribution
import org.opendc.sdk.model.failure.NoFailure
import org.opendc.sdk.model.failure.NormalDistribution
import org.opendc.sdk.model.failure.ParetoDistribution
import org.opendc.sdk.model.failure.PrefabFailure
import org.opendc.sdk.model.failure.TraceBasedFailure
import org.opendc.sdk.model.failure.UniformDistribution
import org.opendc.sdk.model.failure.WeibullDistribution
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.resource.UriReference
import org.opendc.sdk.model.sampleTopology
import org.opendc.sdk.model.sampleWorkload
import org.opendc.sdk.model.scheduler.AllocationPolicy
import org.opendc.sdk.model.scheduler.ComputeHostFilter
import org.opendc.sdk.model.scheduler.CoreRamWeigher
import org.opendc.sdk.model.scheduler.DifferentHostFilter
import org.opendc.sdk.model.scheduler.FilterAllocationPolicy
import org.opendc.sdk.model.scheduler.HostFilter
import org.opendc.sdk.model.scheduler.HostWeigher
import org.opendc.sdk.model.scheduler.InstanceCountFilter
import org.opendc.sdk.model.scheduler.InstanceCountWeigher
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicy
import org.opendc.sdk.model.scheduler.RamFilter
import org.opendc.sdk.model.scheduler.RamWeigher
import org.opendc.sdk.model.scheduler.SameHostFilter
import org.opendc.sdk.model.scheduler.SchedulerName
import org.opendc.sdk.model.scheduler.TaskStopperSpec
import org.opendc.sdk.model.scheduler.TimeShiftAllocationPolicy
import org.opendc.sdk.model.scheduler.VCpuCapacityFilter
import org.opendc.sdk.model.scheduler.VCpuCapacityWeigher
import org.opendc.sdk.model.scheduler.VCpuFilter
import org.opendc.sdk.model.scheduler.VCpuWeigher
import org.opendc.sdk.model.topology.BatteryPolicy
import org.opendc.sdk.model.topology.BestEffort
import org.opendc.sdk.model.topology.ConstantVirtualizationOverhead
import org.opendc.sdk.model.topology.DistributionPolicy
import org.opendc.sdk.model.topology.DoubleThresholdPolicy
import org.opendc.sdk.model.topology.EqualShare
import org.opendc.sdk.model.topology.FirstFit
import org.opendc.sdk.model.topology.FixedShare
import org.opendc.sdk.model.topology.MaxMinFairness
import org.opendc.sdk.model.topology.NoVirtualizationOverhead
import org.opendc.sdk.model.topology.RunningMeanPlusPolicy
import org.opendc.sdk.model.topology.RunningMeanPolicy
import org.opendc.sdk.model.topology.RunningMedianPolicy
import org.opendc.sdk.model.topology.RunningQuartilesPolicy
import org.opendc.sdk.model.topology.ShareBasedVirtualizationOverhead
import org.opendc.sdk.model.topology.SingleThresholdPolicy
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.topology.VirtualizationOverhead
import org.opendc.sdk.model.workload.ScalingPolicy
import org.opendc.sdk.model.workload.TraceWorkload
import org.opendc.sdk.model.workload.Workload
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
                        scalingPolicy = ScalingPolicy.Perfect,
                        deferAll = true,
                    ),
                )
                workload(sampleWorkload)
                allocationPolicy(prefabScheduler(SchedulerName.CoreMem))
                allocationPolicy(
                    filterScheduler {
                        subsetSize = 2
                        filter(ComputeHostFilter)
                        filter(RamFilter(1.5))
                        weigher(RamWeigher(2.0))
                    },
                )
                failureModel(PrefabFailure(FailurePrefab.G5k06Exp))
                failureModel(NoFailure)
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
                        filter(ComputeHostFilter)
                        filter(VCpuFilter(2.0))
                        weigher(CoreRamWeigher(1.5))
                    },
                )
                exportModel =
                    ExportSpec(
                        exportInterval = 10.minutes,
                        printFrequency = null,
                        columns = ExportColumnsSpec(host = OnlyColumns(setOf("timestamp", "cpu_usage")), task = AllColumns),
                        filesToExport = listOf(OutputFile.HOST, OutputFile.TASK),
                    )
                failureModel =
                    CustomFailure(
                        interArrival = ExponentialDistribution(mean = 3600.0),
                        duration = ConstantDistribution(value = 300.0),
                        hostFraction = UniformDistribution(upper = 0.5, lower = 0.1),
                    )
                checkpointModel = CheckpointSpec()
            }

        val text = SdkJson.encodeToString(builtScenario)
        assertEquals(builtScenario, SdkJson.decodeScenario(text))
    }

    @TestFactory
    fun `allocation policies round-trip`(): List<DynamicTest> {
        val policies: List<AllocationPolicy> =
            listOf(
                PrefabAllocationPolicy(SchedulerName.Random),
                FilterAllocationPolicy(
                    filters = listOf(ComputeHostFilter, RamFilter(1.5)),
                    weighers = listOf(RamWeigher(1.0), CoreRamWeigher(2.0)),
                    subsetSize = 3,
                ),
                TimeShiftAllocationPolicy(
                    filters = listOf(ComputeHostFilter),
                    weighers = listOf(VCpuWeigher(1.0)),
                    taskStopper = TaskStopperSpec(),
                ),
            )
        return policies.map { policy ->
            dynamicTest(policy.toString()) { assertEquals(policy, roundTrip<AllocationPolicy>(policy)) }
        }
    }

    @TestFactory
    fun `host filters round-trip`(): List<DynamicTest> {
        val filters: List<HostFilter> =
            listOf(
                ComputeHostFilter,
                SameHostFilter,
                DifferentHostFilter,
                InstanceCountFilter(limit = 4),
                RamFilter(allocationRatio = 1.5),
                VCpuCapacityFilter,
                VCpuFilter(allocationRatio = 2.0),
            )
        return filters.map { filter ->
            dynamicTest(filter.toString()) { assertEquals(filter, roundTrip<HostFilter>(filter)) }
        }
    }

    @TestFactory
    fun `host weighers round-trip`(): List<DynamicTest> {
        val weighers: List<HostWeigher> =
            listOf(
                RamWeigher(1.0),
                CoreRamWeigher(2.0),
                InstanceCountWeigher(0.5),
                VCpuCapacityWeigher(1.5),
                VCpuWeigher(3.0),
            )
        return weighers.map { weigher ->
            dynamicTest(weigher.toString()) { assertEquals(weigher, roundTrip<HostWeigher>(weigher)) }
        }
    }

    @TestFactory
    fun `failure models round-trip`(): List<DynamicTest> {
        val models: List<FailureModel> =
            listOf(
                NoFailure,
                TraceBasedFailure(source = NamedReference("availability"), startPoint = 0.25, repeat = false),
                PrefabFailure(FailurePrefab.Lanl05Wbl),
                CustomFailure(
                    interArrival = ExponentialDistribution(3600.0),
                    duration = ConstantDistribution(300.0),
                    hostFraction = UniformDistribution(upper = 0.5, lower = 0.1),
                ),
            )
        return models.map { model ->
            dynamicTest(model.toString()) { assertEquals(model, roundTrip<FailureModel>(model)) }
        }
    }

    @TestFactory
    fun `distributions round-trip`(): List<DynamicTest> =
        distributionCases.map { (distribution, _) ->
            dynamicTest(distribution.toString()) { assertEquals(distribution, roundTrip<Distribution>(distribution)) }
        }

    @TestFactory
    fun `workloads round-trip`(): List<DynamicTest> {
        val workloads: List<Workload> =
            listOf(
                TraceWorkload(
                    source = NamedReference("bitbrains"),
                    sampleFraction = 0.75,
                    submissionTime = "2022-01-01T00:00",
                    scalingPolicy = ScalingPolicy.Perfect,
                    deferAll = true,
                ),
                sampleWorkload,
            )
        return workloads.map { workload ->
            dynamicTest(workload::class.simpleName ?: "workload") { assertEquals(workload, roundTrip<Workload>(workload)) }
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
        val overheads: List<VirtualizationOverhead> =
            listOf(
                NoVirtualizationOverhead,
                ConstantVirtualizationOverhead(percentageOverhead = 12.5),
                ConstantVirtualizationOverhead(percentageOverhead = null),
                ShareBasedVirtualizationOverhead,
            )
        return overheads.map { overhead ->
            dynamicTest(overhead.toString()) { assertEquals(overhead, roundTrip<VirtualizationOverhead>(overhead)) }
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
            assertFieldOrder(encode<Distribution>(distribution), fields)
        }
    }

    @Test
    fun `polymorphic types use a type discriminator with the declared serial names`() {
        assertTrue(encode<AllocationPolicy>(PrefabAllocationPolicy()).contains("\"type\": \"prefab\""))
        assertTrue(encode<HostFilter>(ComputeHostFilter).contains("\"type\": \"compute\""))
        assertTrue(encode<HostWeigher>(CoreRamWeigher()).contains("\"type\": \"coreRam\""))
        assertTrue(encode<FailureModel>(NoFailure).contains("\"type\": \"none\""))
        assertTrue(encode<Distribution>(LogNormalDistribution(1.0, 2.0)).contains("\"type\": \"log-normal\""))
        assertTrue(encode<Workload>(TraceWorkload(NamedReference("t"))).contains("\"type\": \"trace\""))
        assertTrue(encode<DistributionPolicy>(MaxMinFairness).contains("\"type\": \"maxMinFairness\""))
        assertTrue(encode<BatteryPolicy>(SingleThresholdPolicy(100.0)).contains("\"type\": \"single\""))
        assertTrue(encode<VirtualizationOverhead>(ShareBasedVirtualizationOverhead).contains("\"type\": \"shareBased\""))
        assertTrue(encode<ColumnSelection>(AllColumns).contains("\"type\": \"all\""))
    }

    private val distributionCases: List<Pair<Distribution, List<String>>> =
        listOf(
            ConstantDistribution(value = 5.0) to listOf("type", "value"),
            ExponentialDistribution(mean = 2.0) to listOf("type", "mean"),
            GammaDistribution(shape = 2.0, scale = 3.0) to listOf("type", "shape", "scale"),
            LogNormalDistribution(scale = 1.0, shape = 2.0) to listOf("type", "scale", "shape"),
            NormalDistribution(mean = 1.0, std = 2.0) to listOf("type", "mean", "std"),
            ParetoDistribution(scale = 1.0, shape = 2.0) to listOf("type", "scale", "shape"),
            UniformDistribution(upper = 0.9, lower = 0.1) to listOf("type", "upper", "lower"),
            WeibullDistribution(alpha = 1.0, beta = 2.0) to listOf("type", "alpha", "beta"),
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
