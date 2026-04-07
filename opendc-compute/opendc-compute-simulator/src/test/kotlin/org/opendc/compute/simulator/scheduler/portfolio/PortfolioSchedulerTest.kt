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
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.compute.simulator.host.HostModel
import org.opendc.compute.simulator.host.HostState
import org.opendc.compute.simulator.scheduler.ComputeScheduler
import org.opendc.compute.simulator.scheduler.SchedulingRequest
import org.opendc.compute.simulator.scheduler.SchedulingResultType
import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.compute.simulator.telemetry.HostCpuStats

internal class PortfolioSchedulerTest {
    private fun createMockHost(
        memoryCapacity: Long,
        availableMemory: Long,
        datacenterName: String = "DC1",
        type: String = "default",
    ): HostView {
        val hv = mockk<HostView>(relaxed = true)
        every { hv.host.getModel() } returns HostModel(1000.0, 4, memoryCapacity)
        every { hv.host.getState() } returns HostState.UP
        every { hv.host.isEmpty() } returns (availableMemory == memoryCapacity)
        every { hv.host.getType() } returns type
        every { hv.host.getClusterName() } returns "cluster1"
        every { hv.host.getDatacenterName() } returns datacenterName
        every { hv.availableMemory } returns availableMemory
        every { hv.datacenterName } returns datacenterName
        every { hv.host.getCpuStats() } returns HostCpuStats(0, 0, 0, 0, 1000.0, 500.0, 500.0, 0.5)
        return hv
    }

    private fun createMockTask(
        memorySize: Long = 1024,
        cpuCores: Int = 1,
    ): ServiceTask {
        val task = mockk<ServiceTask>(relaxed = true)
        every { task.memorySize } returns memorySize
        every { task.cpuCoreCount } returns cpuCores
        return task
    }

    @Test
    fun testEmptyPortfolioThrows() {
        assertThrows<IllegalArgumentException> {
            PortfolioScheduler(emptyList(), OperationalRiskUtility())
        }
    }

    @Test
    fun testDelegatesAddHostToAllPolicies() {
        val policy1 = mockk<ComputeScheduler>(relaxed = true)
        val policy2 = mockk<ComputeScheduler>(relaxed = true)
        val scheduler = PortfolioScheduler(listOf(policy1, policy2), OperationalRiskUtility())

        val host = createMockHost(8192, 8192)
        scheduler.addHost(host)

        verify { policy1.addHost(host) }
        verify { policy2.addHost(host) }
    }

    @Test
    fun testDelegatesRemoveHostToAllPolicies() {
        val policy1 = mockk<ComputeScheduler>(relaxed = true)
        val policy2 = mockk<ComputeScheduler>(relaxed = true)
        val scheduler = PortfolioScheduler(listOf(policy1, policy2), OperationalRiskUtility())

        val host = createMockHost(8192, 8192)
        scheduler.removeHost(host)

        verify { policy1.removeHost(host) }
        verify { policy2.removeHost(host) }
    }

    @Test
    fun testSelectEmptyQueue() {
        val policy1 = mockk<ComputeScheduler>(relaxed = true)
        val scheduler = PortfolioScheduler(listOf(policy1), OperationalRiskUtility())

        val result = scheduler.select(mutableListOf<SchedulingRequest>().iterator())
        assertEquals(SchedulingResultType.EMPTY, result.resultType)
    }

    @Test
    fun testSelectWithSinglePolicy() {
        val policy = mockk<ComputeScheduler>(relaxed = true)
        val host = createMockHost(8192, 4096, "DC1")
        val task = createMockTask()

        every { policy.evaluatePlacement(task) } returns host

        val scheduler = PortfolioScheduler(listOf(policy), OperationalRiskUtility())
        scheduler.addHost(host)

        val req = mockk<SchedulingRequest>()
        every { req.task } returns task
        every { req.isCancelled } returns false

        val result = scheduler.select(mutableListOf(req).iterator())
        assertEquals(SchedulingResultType.SUCCESS, result.resultType)
        assertEquals(host, result.host)
        assertEquals(0, scheduler.getLastSelectedPolicyIndex())
    }

    @Test
    fun testSelectPicksBestPolicy() {
        val policy1 = mockk<ComputeScheduler>(relaxed = true)
        val policy2 = mockk<ComputeScheduler>(relaxed = true)

        // Two hosts in different datacenters
        val hostA = createMockHost(8192, 2048, "DC1") // Less available memory
        val hostB = createMockHost(8192, 6144, "DC2") // More available memory

        val task = createMockTask(1024)

        // Policy1 picks hostA (less optimal for DRR)
        every { policy1.evaluatePlacement(task) } returns hostA
        // Policy2 picks hostB (more optimal for DRR)
        every { policy2.evaluatePlacement(task) } returns hostB

        val scheduler =
            PortfolioScheduler(
                listOf(policy1, policy2),
                DisasterRecoveryRiskUtility(),
            )
        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task } returns task
        every { req.isCancelled } returns false

        val result = scheduler.select(mutableListOf(req).iterator())
        assertEquals(SchedulingResultType.SUCCESS, result.resultType)
        assertNotNull(result.host)
    }

    @Test
    fun testSelectNoPolicyFindsHost() {
        val policy1 = mockk<ComputeScheduler>(relaxed = true)
        val task = createMockTask()

        every { policy1.evaluatePlacement(task) } returns null

        val scheduler = PortfolioScheduler(listOf(policy1), OperationalRiskUtility())

        val req = mockk<SchedulingRequest>()
        every { req.task } returns task
        every { req.isCancelled } returns false

        val result = scheduler.select(mutableListOf(req).iterator())
        assertEquals(SchedulingResultType.FAILURE, result.resultType)
    }

    @Test
    fun testSyncsAllPoliciesAfterSelect() {
        val policy1 = mockk<ComputeScheduler>(relaxed = true)
        val policy2 = mockk<ComputeScheduler>(relaxed = true)

        val host = createMockHost(8192, 4096, "DC1")
        val task = createMockTask()

        every { policy1.evaluatePlacement(task) } returns host
        every { policy2.evaluatePlacement(task) } returns null // Can't place

        val scheduler =
            PortfolioScheduler(
                listOf(policy1, policy2),
                OperationalRiskUtility(),
            )
        scheduler.addHost(host)

        val req = mockk<SchedulingRequest>()
        every { req.task } returns task
        every { req.isCancelled } returns false

        scheduler.select(mutableListOf(req).iterator())

        // Both policies should be notified of the placement
        verify { policy1.notifyPlacement(host, task) }
        verify { policy2.notifyPlacement(host, task) }
    }

    @Test
    fun testDelegatesRemoveTaskToAllPolicies() {
        val policy1 = mockk<ComputeScheduler>(relaxed = true)
        val policy2 = mockk<ComputeScheduler>(relaxed = true)
        val scheduler = PortfolioScheduler(listOf(policy1, policy2), OperationalRiskUtility())

        val host = createMockHost(8192, 4096)
        val task = createMockTask()
        scheduler.removeTask(task, host)

        verify { policy1.removeTask(task, host) }
        verify { policy2.removeTask(task, host) }
    }

    @Test
    fun testSkipsCancelledRequests() {
        val policy = mockk<ComputeScheduler>(relaxed = true)
        val host = createMockHost(8192, 4096)
        val task = createMockTask()

        every { policy.evaluatePlacement(task) } returns host

        val scheduler = PortfolioScheduler(listOf(policy), OperationalRiskUtility())
        scheduler.addHost(host)

        val cancelledReq = mockk<SchedulingRequest>()
        every { cancelledReq.isCancelled } returns true

        val validReq = mockk<SchedulingRequest>()
        every { validReq.task } returns task
        every { validReq.isCancelled } returns false

        val result = scheduler.select(mutableListOf(cancelledReq, validReq).iterator())
        assertEquals(SchedulingResultType.SUCCESS, result.resultType)
    }
}
