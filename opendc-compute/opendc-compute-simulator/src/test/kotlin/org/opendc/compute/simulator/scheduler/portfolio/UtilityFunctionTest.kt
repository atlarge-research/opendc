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
import org.opendc.compute.simulator.internal.Guest
import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.compute.simulator.telemetry.HostCpuStats
import org.opendc.simulator.compute.workload.trace.TraceFragment
import org.opendc.simulator.compute.workload.trace.TraceWorkload
import java.util.ArrayList

internal class UtilityFunctionTest {
    private fun createGuest(
        fragments: List<TraceFragment>,
        cpuCoreCount: Int = 1,
    ): Guest {
        val guest = mockk<Guest>()
        val task = mockk<ServiceTask>(relaxed = true)
        val workload = mockk<TraceWorkload>()
        every { workload.fragments } returns ArrayList(fragments)
        every { task.getWorkload() } returns workload
        every { task.getCpuCoreCount() } returns cpuCoreCount
        every { guest.task } returns task
        return guest
    }

    private fun createHostView(
        capacity: Double = 1000.0,
        coreCount: Int = 4,
        memoryCapacity: Long = 8192,
        availableMemory: Long = 4096,
        datacenterName: String = "",
        guests: List<Guest> = emptyList(),
    ): HostView {
        val hv = mockk<HostView>()
        val cpuStats = HostCpuStats(0, 0, 0, 0, capacity, 0.0, 0.0, 0.0)
        every { hv.host.getCpuStats() } returns cpuStats
        every { hv.host.getModel() } returns HostModel(capacity, coreCount, memoryCapacity)
        every { hv.host.getGuests() } returns guests
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
    fun testORNoGuests() {
        val or = OperationalRiskUtility()
        val hosts = listOf(createHostView(capacity = 1000.0, coreCount = 4, guests = emptyList()))
        assertEquals(0.0, or.evaluate(hosts), 0.001)
    }

    @Test
    fun testORSingleGuestNoOvercommit() {
        val or = OperationalRiskUtility()
        // 1 guest with 4 vCPUs on 4 physical cores → overcommit = 1.0
        // Guest uses 1000 MHz on 1000 MHz host → utilization = 1.0
        // Contention = 1.0 × 1.0 = 1.0
        val guest = createGuest(listOf(TraceFragment(300_000L, 1000.0)), cpuCoreCount = 4)
        val hosts = listOf(createHostView(capacity = 1000.0, coreCount = 4, guests = listOf(guest)))
        assertEquals(1.0, or.evaluate(hosts), 0.001)
    }

    @Test
    fun testOROvercommit() {
        val or = OperationalRiskUtility()
        // 2 guests with 4 vCPUs each on 4 physical cores → overcommit = 2.0
        // Each uses 250 MHz on 1000 MHz → total demand = 500 MHz → utilization = 0.5
        // Contention = 0.5 × 2.0 = 1.0
        val guest1 = createGuest(listOf(TraceFragment(300_000L, 250.0)), cpuCoreCount = 4)
        val guest2 = createGuest(listOf(TraceFragment(300_000L, 250.0)), cpuCoreCount = 4)
        val hosts = listOf(createHostView(capacity = 1000.0, coreCount = 4, guests = listOf(guest1, guest2)))
        assertEquals(1.0, or.evaluate(hosts), 0.001)
    }

    @Test
    fun testORMeanAcrossHosts() {
        val or = OperationalRiskUtility()
        // Host 1: 1 guest, 4 vCPUs / 4 cores = 1.0 overcommit, 500/1000 = 0.5 util → contention 0.5
        // Host 2: no guests → contention 0
        // Mean = 0.25
        val guest = createGuest(listOf(TraceFragment(300_000L, 500.0)), cpuCoreCount = 4)
        val hosts =
            listOf(
                createHostView(capacity = 1000.0, coreCount = 4, guests = listOf(guest)),
                createHostView(capacity = 1000.0, coreCount = 4, guests = emptyList()),
            )
        assertEquals(0.25, or.evaluate(hosts), 0.001)
    }

    @Test
    fun testORMaxAcrossIntervals() {
        val or = OperationalRiskUtility(forwardLookMs = 600_000L)
        // Two 5-min intervals: first at 1000 MHz, second at 200 MHz
        // overcommit = 4/4 = 1.0
        // interval 0: util = 1000/1000 = 1.0, contention = 1.0
        // interval 1: util = 200/1000 = 0.2, contention = 0.2
        // MAX = 1.0
        val guest =
            createGuest(
                listOf(TraceFragment(300_000L, 1000.0), TraceFragment(300_000L, 200.0)),
                cpuCoreCount = 4,
            )
        val hosts = listOf(createHostView(capacity = 1000.0, coreCount = 4, guests = listOf(guest)))
        assertEquals(1.0, or.evaluate(hosts), 0.001)
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
                createHostView(memoryCapacity = 8192, availableMemory = 4096, datacenterName = "DC1"),
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
                createHostView(memoryCapacity = 8192, availableMemory = 4096, datacenterName = "DC1"),
                createHostView(memoryCapacity = 8192, availableMemory = 4096, datacenterName = "DC2"),
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
                createHostView(memoryCapacity = 8192, availableMemory = 1024, datacenterName = "DC1"),
                createHostView(memoryCapacity = 8192, availableMemory = 7168, datacenterName = "DC2"),
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
                createHostView(memoryCapacity = 8192, availableMemory = 1024, datacenterName = "DC1"),
                createHostView(memoryCapacity = 8192, availableMemory = 1024, datacenterName = "DC2"),
            )
        val score = drr.evaluate(hosts)
        assertTrue(score > 0.9, "DRR should be high when both DCs are nearly full, got $score")
    }

    @Test
    fun testDRRFullyEmpty() {
        val drr = DisasterRecoveryRiskUtility()
        // Two datacenters, both empty (no workload)
        // W_i = 0 → skipped in risk computation → product stays 1.0
        // geometric mean = 1.0^(1/2) = 1.0
        val hosts =
            listOf(
                createHostView(memoryCapacity = 8192, availableMemory = 8192, datacenterName = "DC1"),
                createHostView(memoryCapacity = 8192, availableMemory = 8192, datacenterName = "DC2"),
            )
        assertEquals(1.0, drr.evaluate(hosts), 0.001)
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
        // Two DCs, no guests (OR=0), balanced memory (DRR=0.5)
        val hosts =
            listOf(
                createHostView(capacity = 1000.0, memoryCapacity = 8192, availableMemory = 4096, datacenterName = "DC1"),
                createHostView(capacity = 1000.0, memoryCapacity = 8192, availableMemory = 4096, datacenterName = "DC2"),
            )
        val score = dor.evaluate(hosts)
        // OR=0 (no guests), DRR=0.5 (balanced)
        // DOR = (1*0 + 1*0.5) / 2 = 0.25
        assertEquals(0.25, score, 0.001)
    }

    @Test
    fun testDORWeightedTowardsOR() {
        val dor = CombinedDORUtility(3.0, 1.0)
        // DC1: 1 guest with 4 vCPUs on 4 cores (overcommit=1.0), 500/1000 util → contention 0.5
        // DC2: no guests → contention 0. Mean OR = 0.25
        val guest = createGuest(listOf(TraceFragment(300_000L, 500.0)), cpuCoreCount = 4)
        val hosts =
            listOf(
                createHostView(
                    capacity = 1000.0,
                    coreCount = 4,
                    memoryCapacity = 8192,
                    availableMemory = 4096,
                    datacenterName = "DC1",
                    guests = listOf(guest),
                ),
                createHostView(capacity = 1000.0, coreCount = 4, memoryCapacity = 8192, availableMemory = 4096, datacenterName = "DC2"),
            )
        val score = dor.evaluate(hosts)
        // OR = 0.25, DRR ≈ 0.5
        // DOR = (3*0.25 + 1*0.5) / 4 = 1.25/4 = 0.3125
        assertEquals(0.3125, score, 0.001)
    }
}
