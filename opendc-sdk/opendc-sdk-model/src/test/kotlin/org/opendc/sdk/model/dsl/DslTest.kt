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

package org.opendc.sdk.model.dsl

import org.junit.jupiter.api.Test
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.Power
import org.opendc.common.units.TimeDelta
import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.experiment.Scenario
import org.opendc.sdk.model.export.ExportModel
import org.opendc.sdk.model.failure.NoFailure
import org.opendc.sdk.model.failure.TraceBasedFailure
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.sampleTopology
import org.opendc.sdk.model.sampleWorkload
import org.opendc.sdk.model.scheduler.ComputeHostFilter
import org.opendc.sdk.model.scheduler.CoreRamWeigher
import org.opendc.sdk.model.scheduler.FilterAllocationPolicy
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicy
import org.opendc.sdk.model.scheduler.RamFilter
import org.opendc.sdk.model.scheduler.RamWeigher
import org.opendc.sdk.model.scheduler.SchedulerName
import org.opendc.sdk.model.scheduler.TaskStopper
import org.opendc.sdk.model.scheduler.TimeShiftAllocationPolicy
import org.opendc.sdk.model.scheduler.VCpuFilter
import org.opendc.sdk.model.scheduler.VCpuWeigher
import org.opendc.sdk.model.topology.DoubleThresholdPolicy
import org.opendc.sdk.model.topology.EqualShare
import org.opendc.sdk.model.topology.FixedShare
import org.opendc.sdk.model.topology.PowerModelType
import org.opendc.sdk.model.workload.ScalingPolicy
import kotlin.test.assertEquals

/**
 * Verifies that the SDK builder DSLs yield exactly the model objects a caller would obtain by invoking
 * the data-class constructors directly, and that the unit extension helpers wrap the right values.
 */
class DslTest {
    @Test
    fun `topology DSL equals constructor-built topology`() {
        val built =
            topology {
                cluster(name = "cluster-a", count = 2) {
                    host(count = 4, name = "compute-host") {
                        cpu(coreCount = 8, coreSpeed = 3.ghz, count = 2, vendor = "AMD", modelName = "EPYC", architecture = "Zen4")
                        memory(size = 32.gib, speed = 3.ghz, vendor = "Samsung")
                        cpuDistribution = FixedShare(0.5)
                        gpuDistribution = EqualShare
                        power {
                            type = PowerModelType.SQUARE
                            maxPower = 500.watts
                            idlePower = 100.watts
                        }
                    }
                    powerSource(name = "grid", maxPower = 50.kwatts, carbon = NamedReference("carbon-trace"))
                    battery(
                        capacity = 100.0,
                        chargingSpeed = 1000.0,
                        policy = DoubleThresholdPolicy(lowerThreshold = 100.0, upperThreshold = 300.0),
                        name = "cell",
                        initialCharge = 20.0,
                        embodiedCarbon = 50.0,
                        expectedLifetime = 10.0,
                    )
                }
            }

        assertEquals(sampleTopology, built)
    }

    @Test
    fun `inline workload DSL equals constructor-built workload`() {
        val built =
            inlineWorkload {
                scalingPolicy = ScalingPolicy.Perfect
                task(
                    id = 0,
                    name = "t0",
                    submissionTime = 0.minutes,
                    duration = 10.minutes,
                    cpuCoreCount = 4,
                    cpuCapacity = 2.ghz,
                    memory = 8.gib,
                ) {
                    fragment(duration = 5.minutes, cpuUsage = 2.ghz)
                    fragment(duration = 5.minutes, cpuUsage = 1.ghz, gpuUsage = 1.ghz, gpuMemory = 2.gib)
                }
                task(
                    id = 1, name = "t1", submissionTime = 2.minutes, duration = 20.minutes, cpuCoreCount = 8,
                    cpuCapacity = 3.ghz, memory = 16.gib, deferrable = true, deadline = 60.minutes, parents = setOf(0),
                ) {
                    fragment(duration = 20.minutes, cpuUsage = 3.ghz)
                }
            }

        assertEquals(sampleWorkload, built)
    }

    @Test
    fun `filter scheduler DSL equals constructor-built policy`() {
        val built =
            filterScheduler {
                subsetSize = 3
                filter(ComputeHostFilter)
                filter(RamFilter(1.5))
                weigher(CoreRamWeigher(2.0))
                weigher(RamWeigher())
            }

        assertEquals(expectedFilterPolicy(), built)
    }

    @Test
    fun `filter scheduler DSL without filters defaults to the compute filter`() {
        val built = filterScheduler { weigher(RamWeigher()) }

        assertEquals(FilterAllocationPolicy(listOf(ComputeHostFilter), listOf(RamWeigher()), 1), built)
    }

    @Test
    fun `time-shift scheduler DSL equals constructor-built policy`() {
        val built =
            timeShiftScheduler {
                windowSize = 100
                subsetSize = 2
                forecast = false
                shortForecastThreshold = 0.1
                longForecastThreshold = 0.5
                forecastSize = 12
                taskStopper = TaskStopper(windowSize = 50)
                memorize = false
                filter(VCpuFilter(2.0))
                weigher(VCpuWeigher())
            }

        val expected =
            TimeShiftAllocationPolicy(
                filters = listOf(VCpuFilter(2.0)), weighers = listOf(VCpuWeigher()), windowSize = 100, subsetSize = 2,
                forecast = false, shortForecastThreshold = 0.1, longForecastThreshold = 0.5, forecastSize = 12,
                taskStopper = TaskStopper(windowSize = 50), memorize = false,
            )
        assertEquals(expected, built)
    }

    @Test
    fun `prefab scheduler DSL equals constructor-built policy`() {
        assertEquals(PrefabAllocationPolicy(SchedulerName.CoreMem), prefabScheduler(SchedulerName.CoreMem))
        assertEquals(PrefabAllocationPolicy(SchedulerName.Mem), prefabScheduler())
    }

    @Test
    fun `scenario DSL equals constructor-built scenario`() {
        val topology = sampleTopology
        val workload = sampleWorkload
        val policy = expectedFilterPolicy()

        val built =
            scenario {
                id = 7
                name = "baseline"
                topology(topology)
                workload(workload)
                allocationPolicy(policy)
                exportModel = ExportModel(exportInterval = 15.minutes, printFrequency = 12)
                failureModel = TraceBasedFailure(NamedReference("failure-trace"))
                maxNumFailures = 5
                runs = 3
                initialSeed = 42
            }

        val expected =
            Scenario(
                topology = topology, workload = workload, allocationPolicy = policy,
                exportModel = ExportModel(exportInterval = 15.minutes, printFrequency = 12),
                failureModel = TraceBasedFailure(NamedReference("failure-trace")),
                maxNumFailures = 5, runs = 3, initialSeed = 42, id = 7, name = "baseline",
            )
        assertEquals(expected, built)
    }

    @Test
    fun `experiment DSL equals constructor-built experiment`() {
        val topology = sampleTopology
        val workload = sampleWorkload

        val built =
            experiment {
                name = "sweep"
                runs = 5
                initialSeed = 1
                topology(topology)
                workload(workload)
                allocationPolicy(prefabScheduler(SchedulerName.Mem))
                allocationPolicy(prefabScheduler(SchedulerName.CoreMem))
                failureModel(NoFailure)
                exportModel(ExportModel())
                maxNumFailures(5)
                maxNumFailures(10)
            }

        val expected =
            Experiment(
                topologies = setOf(topology), workloads = setOf(workload),
                allocationPolicies = setOf(PrefabAllocationPolicy(SchedulerName.Mem), PrefabAllocationPolicy(SchedulerName.CoreMem)),
                failureModels = setOf(NoFailure), maxNumFailures = setOf(5, 10), checkpointModels = setOf(null),
                exportModels = setOf(ExportModel()), runs = 5, initialSeed = 1, name = "sweep",
            )
        assertEquals(expected, built)
    }

    @Test
    fun `unit extension helpers wrap the expected values`() {
        assertEquals(Frequency.ofGHz(2), 2.ghz)
        assertEquals(2000.0, 2.ghz.toMHz())
        assertEquals(2.ghz, 2000.mhz)

        assertEquals(DataSize.ofGiB(4), 4.gib)
        assertEquals(4096.0, 4.gib.toMiB())
        assertEquals(1.gib, 1024.mib)

        assertEquals(Power.ofWatts(250), 250.watts)
        assertEquals(250.0, 250.watts.toWatts())
        assertEquals(1.kwatts, 1000.watts)

        assertEquals(TimeDelta.ofMin(5), 5.minutes)
        assertEquals(300000.0, 5.minutes.toMs())
        assertEquals(1.hours, 60.minutes)
    }

    private fun expectedFilterPolicy(): FilterAllocationPolicy =
        FilterAllocationPolicy(
            filters = listOf(ComputeHostFilter, RamFilter(1.5)),
            weighers = listOf(CoreRamWeigher(2.0), RamWeigher()),
            subsetSize = 3,
        )
}
