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

package org.opendc.web.dispatcher.estimate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.sdk.model.dsl.ghz
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.dsl.minutes
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.scenario.ScenarioSpec
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicySpec
import org.opendc.sdk.model.telemetry.ExportSpec
import org.opendc.sdk.model.telemetry.OutputFileSpec
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.CpuSpec
import org.opendc.sdk.model.topology.DataCenterSpec
import org.opendc.sdk.model.topology.HostSpec
import org.opendc.sdk.model.topology.MemorySpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec

/**
 * The model exists to order units by what they will cost, so what is worth pinning is that it moves
 * the right way when an input moves, and that sampling is understood to buy time and not memory.
 */
class TraceSizeEstimatorTest {
    private val coefficients =
        EstimatorCoefficients(
            baseMemoryMb = 256.0,
            memoryPerHostMb = 1.0,
            memoryPerMillionFragmentsMb = 60.0,
            memoryPerMillionTasksMb = 200.0,
            baseSeconds = 5.0,
            loadSecondsPerMillionFragments = 20.0,
            simulateSecondsPerMillionFragments = 40.0,
            secondsPerMillionTasks = 10.0,
            exportCostPerHost = 0.002,
            failureOverhead = 0.25,
            checkpointOverhead = 0.15,
        )
    private val estimator = TraceSizeEstimator(coefficients)

    private fun scenario(
        hosts: Int = 1,
        sampleFraction: Double = 1.0,
        export: ExportSpec = ExportSpec(),
    ) = ScenarioSpec(
        topology =
            TopologySpec(
                listOf(
                    DataCenterSpec(
                        clusters =
                            listOf(
                                ClusterSpec(
                                    hosts =
                                        listOf(
                                            HostSpec(
                                                count = hosts,
                                                cpu = CpuSpec(coreCount = 8, coreSpeed = 3.ghz),
                                                memory = MemorySpec(size = 64.gib),
                                            ),
                                        ),
                                ),
                            ),
                    ),
                ),
            ),
        workload = TraceWorkloadSpec(source = NamedReference("trace"), sampleFraction = sampleFraction),
        allocationPolicy = PrefabAllocationPolicySpec(),
        exportModel = export,
    )

    private fun extent(
        tasks: Long = 10_000,
        fragments: Long = 2_000_000,
    ) = TraceExtent(taskCount = tasks, fragmentCount = fragments)

    @Test
    fun `costs more as the trace grows`() {
        val small = estimator.estimate(scenario(), extent(fragments = 1_000_000))
        val large = estimator.estimate(scenario(), extent(fragments = 8_000_000))

        assertTrue(large.peakMemoryMb > small.peakMemoryMb)
        assertTrue(large.cpuSeconds > small.cpuSeconds)
    }

    @Test
    fun `costs more as the topology grows`() {
        val narrow = estimator.estimate(scenario(hosts = 4), extent())
        val wide = estimator.estimate(scenario(hosts = 400), extent())

        assertTrue(wide.peakMemoryMb > narrow.peakMemoryMb)
        assertTrue(wide.cpuSeconds > narrow.cpuSeconds, "more hosts means more export rows per interval")
    }

    @Test
    fun `sampling buys time but not memory`() {
        val whole = estimator.estimate(scenario(sampleFraction = 1.0), extent())
        val sampled = estimator.estimate(scenario(sampleFraction = 0.01), extent())

        assertTrue(sampled.cpuSeconds < whole.cpuSeconds, "less of the trace is simulated")
        assertEquals(
            whole.peakMemoryMb,
            sampled.peakMemoryMb,
            "the whole trace is loaded before it is sampled, so it is all held either way",
        )
    }

    @Test
    fun `sampling never removes the cost of reading the trace`() {
        val estimate = estimator.estimate(scenario(sampleFraction = 0.000001), extent(fragments = 100_000_000))

        val loading = coefficients.loadSecondsPerMillionFragments * 100.0
        assertTrue(
            estimate.cpuSeconds >= loading,
            "a tiny sample of a huge trace still has to read all of it, ${estimate.cpuSeconds}s vs ${loading}s",
        )
    }

    @Test
    fun `a finer export interval costs more, and no host table costs nothing`() {
        val coarse = estimator.estimate(scenario(hosts = 100, export = ExportSpec(exportInterval = 5.minutes)), extent())
        val fine = estimator.estimate(scenario(hosts = 100, export = ExportSpec(exportInterval = 1.minutes)), extent())
        val none =
            estimator.estimate(
                scenario(hosts = 100, export = ExportSpec(filesToExport = listOf(OutputFileSpec.SERVICE))),
                extent(),
            )

        assertTrue(fine.cpuSeconds > coarse.cpuSeconds)
        assertTrue(none.cpuSeconds < coarse.cpuSeconds, "without the host table there is no per-host export cost")
    }

    @Test
    fun `an unmeasured trace still yields a usable estimate`() {
        val estimate = estimator.estimate(scenario(hosts = 10), TraceExtent.UNKNOWN)

        assertTrue(estimate.peakMemoryMb > 0.0)
        assertTrue(estimate.cpuSeconds > 0.0)
    }

    @Test
    fun `shipped coefficients predict a positive cost for an unmeasured trace`() {
        val estimate = TraceSizeEstimator(EstimatorCoefficients.SHIPPED).estimate(scenario(hosts = 10), TraceExtent.UNKNOWN)

        assertEquals(80.0 + 0.03 * 10, estimate.peakMemoryMb)
        assertTrue(estimate.cpuSeconds > 0.0)
    }
}
