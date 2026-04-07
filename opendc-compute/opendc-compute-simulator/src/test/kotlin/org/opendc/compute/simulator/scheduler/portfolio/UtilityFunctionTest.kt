/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.compute.simulator.scheduler.portfolio

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.compute.simulator.host.HostModel
import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.telemetry.HostCpuStats

internal class UtilityFunctionTest {
    private fun createHostView(
        demand: Double,
        usage: Double,
        memoryCapacity: Long,
        availableMemory: Long,
        datacenterName: String = "",
    ): HostView {
        val hv = mockk<HostView>()
        val cpuStats = HostCpuStats(0, 0, 0, 0, 0.0, demand, usage, 0.0)
        every { hv.host.getCpuStats() } returns cpuStats
        every { hv.host.getModel() } returns HostModel(0.0, 0, memoryCapacity)
        every { hv.availableMemory } returns availableMemory
        every { hv.datacenterName } returns datacenterName
        return hv
    }

    // ========== Operational Risk Tests ==========

    @Test
    fun testORNoHosts() {
        val or = OperationalRiskUtility()
        assertEquals(0.0, or.evaluate(emptyList()))
    }

    @Test
    fun testORNoContention() {
        val or = OperationalRiskUtility()
        val hosts =
            listOf(
                createHostView(demand = 1000.0, usage = 1000.0, memoryCapacity = 8192, availableMemory = 4096),
                createHostView(demand = 500.0, usage = 500.0, memoryCapacity = 8192, availableMemory = 4096),
            )
        assertEquals(0.0, or.evaluate(hosts), 0.001)
    }

    @Test
    fun testORFullContention() {
        val or = OperationalRiskUtility()
        val hosts =
            listOf(
                createHostView(demand = 1000.0, usage = 0.0, memoryCapacity = 8192, availableMemory = 4096),
            )
        assertEquals(1.0, or.evaluate(hosts), 0.001)
    }

    @Test
    fun testORPartialContention() {
        val or = OperationalRiskUtility()
        // Host 1: 50% contention (demand=1000, usage=500)
        // Host 2: 0% contention (demand=1000, usage=1000)
        // Average: 25%
        val hosts =
            listOf(
                createHostView(demand = 1000.0, usage = 500.0, memoryCapacity = 8192, availableMemory = 4096),
                createHostView(demand = 1000.0, usage = 1000.0, memoryCapacity = 8192, availableMemory = 4096),
            )
        assertEquals(0.25, or.evaluate(hosts), 0.001)
    }

    @Test
    fun testORIgnoresIdleHosts() {
        val or = OperationalRiskUtility()
        // Idle host (demand=0) should be ignored
        val hosts =
            listOf(
                createHostView(demand = 0.0, usage = 0.0, memoryCapacity = 8192, availableMemory = 8192),
                createHostView(demand = 1000.0, usage = 500.0, memoryCapacity = 8192, availableMemory = 4096),
            )
        // Only 1 host with demand, 50% contention
        assertEquals(0.5, or.evaluate(hosts), 0.001)
    }

    // ========== Disaster Recovery Risk Tests ==========

    @Test
    fun testDRRNoHosts() {
        val drr = DisasterRecoveryRiskUtility()
        assertEquals(0.0, drr.evaluate(emptyList()))
    }

    @Test
    fun testDRRSingleDatacenter() {
        val drr = DisasterRecoveryRiskUtility()
        val hosts =
            listOf(
                createHostView(demand = 0.0, usage = 0.0, memoryCapacity = 8192, availableMemory = 4096, datacenterName = "DC1"),
            )
        // Single datacenter: DRR is 0 (not meaningful)
        assertEquals(0.0, drr.evaluate(hosts))
    }

    @Test
    fun testDRRBalancedDatacenters() {
        val drr = DisasterRecoveryRiskUtility()
        // Two datacenters, each with 8GB total, 4GB used, 4GB available
        // If DC1 fails: W_1=4096, E_complement_1=4096 (available in DC2)
        // rd_1 = (4096-4096)/4096 = 0 → normalized (0+1)/2 = 0.5
        // Same for DC2
        // product = 0.5 * 0.5 = 0.25, geometric mean = 0.25^(1/2) = 0.5
        val hosts =
            listOf(
                createHostView(demand = 0.0, usage = 0.0, memoryCapacity = 8192, availableMemory = 4096, datacenterName = "DC1"),
                createHostView(demand = 0.0, usage = 0.0, memoryCapacity = 8192, availableMemory = 4096, datacenterName = "DC2"),
            )
        assertEquals(0.5, drr.evaluate(hosts), 0.001)
    }

    @Test
    fun testDRRUnbalancedDatacenters() {
        val drr = DisasterRecoveryRiskUtility()
        // DC1: 8GB total, 7GB used (1GB available) → workload=7168
        // DC2: 8GB total, 1GB used (7GB available) → workload=1024
        // If DC1 fails: W_1=7168, E_complement_1=7168 (available in DC2)
        //   rd_1 = (7168-7168)/7168 = 0 → normalized 0.5
        // If DC2 fails: W_2=1024, E_complement_2=1024 (available in DC1)
        //   rd_2 = (1024-1024)/1024 = 0 → normalized 0.5
        // geometric mean = 0.5
        val hosts =
            listOf(
                createHostView(demand = 0.0, usage = 0.0, memoryCapacity = 8192, availableMemory = 1024, datacenterName = "DC1"),
                createHostView(demand = 0.0, usage = 0.0, memoryCapacity = 8192, availableMemory = 7168, datacenterName = "DC2"),
            )
        assertEquals(0.5, drr.evaluate(hosts), 0.001)
    }

    @Test
    fun testDRROverloaded() {
        val drr = DisasterRecoveryRiskUtility()
        // DC1: 8GB total, 7GB used (1GB available) → workload=7168
        // DC2: 8GB total, 7GB used (1GB available) → workload=7168
        // If DC1 fails: W_1=7168, E_complement_1=1024
        //   W_1 > E_complement_1 → rd_1 = (7168-1024)/7168 ≈ 0.857
        //   normalized = (0.857+1)/2 = 0.929
        // Same for DC2
        // geometric mean = 0.929
        val hosts =
            listOf(
                createHostView(demand = 0.0, usage = 0.0, memoryCapacity = 8192, availableMemory = 1024, datacenterName = "DC1"),
                createHostView(demand = 0.0, usage = 0.0, memoryCapacity = 8192, availableMemory = 1024, datacenterName = "DC2"),
            )
        val score = drr.evaluate(hosts)
        assertTrue(score > 0.9, "DRR should be high when both DCs are nearly full, got $score")
    }

    @Test
    fun testDRRFullyEmpty() {
        val drr = DisasterRecoveryRiskUtility()
        // Two datacenters, both empty (no workload)
        // W_i = 0, E_complement_i = 8192
        // rd_i = (0-8192)/8192 = -1 → normalized = 0
        // geometric mean = 0
        val hosts =
            listOf(
                createHostView(demand = 0.0, usage = 0.0, memoryCapacity = 8192, availableMemory = 8192, datacenterName = "DC1"),
                createHostView(demand = 0.0, usage = 0.0, memoryCapacity = 8192, availableMemory = 8192, datacenterName = "DC2"),
            )
        assertEquals(0.0, drr.evaluate(hosts), 0.001)
    }

    // ========== Combined DOR Tests ==========

    @Test
    fun testDORInvalidWeights() {
        assertThrows<IllegalArgumentException> { CombinedDORUtility(-1.0, 1.0) }
        assertThrows<IllegalArgumentException> { CombinedDORUtility(0.0, 0.0) }
    }

    @Test
    fun testDOREqualWeights() {
        val dor = CombinedDORUtility(1.0, 1.0)
        // Two DCs, no contention, balanced
        val hosts =
            listOf(
                createHostView(demand = 1000.0, usage = 1000.0, memoryCapacity = 8192, availableMemory = 4096, datacenterName = "DC1"),
                createHostView(demand = 1000.0, usage = 1000.0, memoryCapacity = 8192, availableMemory = 4096, datacenterName = "DC2"),
            )
        val score = dor.evaluate(hosts)
        // OR=0 (no contention), DRR=0.5 (balanced)
        // DOR = (1*0 + 1*0.5) / 2 = 0.25
        assertEquals(0.25, score, 0.001)
    }

    @Test
    fun testDORWeightedTowardsOR() {
        val dor = CombinedDORUtility(3.0, 1.0)
        val hosts =
            listOf(
                createHostView(demand = 1000.0, usage = 500.0, memoryCapacity = 8192, availableMemory = 4096, datacenterName = "DC1"),
                createHostView(demand = 1000.0, usage = 1000.0, memoryCapacity = 8192, availableMemory = 4096, datacenterName = "DC2"),
            )
        val score = dor.evaluate(hosts)
        // OR = 0.25 (avg of 0.5 and 0.0), DRR ≈ 0.5
        // DOR = (3*0.25 + 1*0.5) / 4 = 1.25/4 = 0.3125
        assertEquals(0.3125, score, 0.001)
    }
}
