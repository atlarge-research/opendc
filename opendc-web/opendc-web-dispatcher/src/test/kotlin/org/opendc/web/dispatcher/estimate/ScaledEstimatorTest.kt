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
import org.junit.jupiter.api.Test
import org.opendc.sdk.model.dsl.ghz
import org.opendc.sdk.model.dsl.gib
import org.opendc.sdk.model.resource.NamedReference
import org.opendc.sdk.model.scenario.ScenarioSpec
import org.opendc.sdk.model.scheduler.PrefabAllocationPolicySpec
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.CpuSpec
import org.opendc.sdk.model.topology.DataCenterSpec
import org.opendc.sdk.model.topology.HostSpec
import org.opendc.sdk.model.topology.MemorySpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.workload.TraceWorkloadSpec

/**
 * A correction that grew or shrank without bound would turn the one lever a deployment has into the
 * cause of the failures it exists to prevent.
 */
class ScaledEstimatorTest {
    private val base = UnitEstimate(peakMemoryMb = 1000.0, cpuSeconds = 100.0)
    private val delegate = ResourceEstimator { _, _ -> base }

    private val scenario =
        ScenarioSpec(
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
                                                    cpu = CpuSpec(coreCount = 8, coreSpeed = 3.ghz),
                                                    memory = MemorySpec(size = 64.gib),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
                ),
            workload = TraceWorkloadSpec(source = NamedReference("trace")),
            allocationPolicy = PrefabAllocationPolicySpec(),
        )

    private fun estimate(
        runtime: Double,
        memory: Double,
    ) = delegate.scaledBy(runtime, memory).estimate(scenario, TraceExtent.UNKNOWN)

    @Test
    fun `leaves an estimate alone when the deployment says nothing`() {
        assertEquals(base, estimate(runtime = 1.0, memory = 1.0))
    }

    @Test
    fun `scales runtime and memory independently`() {
        val corrected = estimate(runtime = 2.0, memory = 1.0)

        assertEquals(base.cpuSeconds * 2.0, corrected.cpuSeconds)
        assertEquals(base.peakMemoryMb, corrected.peakMemoryMb, "slow cores do not imply a need for more memory")
    }

    @Test
    fun `stops shrinking rather than packing against nothing`() {
        val corrected = estimate(runtime = 0.0, memory = 0.0)

        assertEquals(base.cpuSeconds * 0.25, corrected.cpuSeconds)
        assertEquals(base.peakMemoryMb * 0.25, corrected.peakMemoryMb)
    }

    @Test
    fun `stops growing rather than asking for an absurd grant`() {
        val corrected = estimate(runtime = 100.0, memory = 100.0)

        assertEquals(base.cpuSeconds * 4.0, corrected.cpuSeconds)
        assertEquals(base.peakMemoryMb * 4.0, corrected.peakMemoryMb)
    }

    @Test
    fun `treats a corrupt multiplier as no correction`() {
        assertEquals(base, estimate(runtime = Double.NaN, memory = Double.NaN))
    }
}
