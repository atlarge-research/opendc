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

package org.opendc.compute.simulator.scheduler

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.simulator.host.HostModel
import org.opendc.compute.simulator.host.HostState
import org.opendc.compute.simulator.scheduler.filters.RamFilter
import org.opendc.compute.simulator.service.HostView

internal class MemorizingSchedulerTest {
    @Test
    fun testNoHosts() {
        val scheduler =
            MemorizingScheduler(
                filters = emptyList(),
            )

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.coreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(SchedulingResultType.FAILURE, scheduler.select(mutableListOf(req).iterator()).resultType)
    }

    @Test
    fun testNoFiltersAndSchedulersRandom() {
        val scheduler =
            MemorizingScheduler(
                filters = emptyList(),
            )

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.DOWN

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.coreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
        every { req.isCancelled } returns false

        // Make sure we get the first host both times
        assertAll(
            { assertEquals(hostA, scheduler.select(mutableListOf(req).iterator()).host) },
            { assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host) },
        )
    }

    @Test
    fun testRamFilter() {
        // Make Random with predictable order of numbers to test max skipped logic
        val scheduler =
            MemorizingScheduler(
                filters = listOf(RamFilter(1.0)),
                maxTimesSkipped = 3,
            )

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.UP
        every { hostA.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.availableMemory } returns 512

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP
        every { hostB.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.availableMemory } returns 512

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.coreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
        every { req.isCancelled } returns false
        val skipped = slot<Int>()
        justRun { req.setProperty("timesSkipped") value capture(skipped) }
        every { req.getProperty("timesSkipped") } answers { skipped.captured }
        req.timesSkipped = 0

        assertEquals(SchedulingResultType.EMPTY, scheduler.select(mutableListOf(req).iterator()).resultType)
        every { hostB.availableMemory } returns 2048
        assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host)
    }

    @Test
    fun testRamFilterOvercommit() {
        val scheduler =
            MemorizingScheduler(
                filters = listOf(RamFilter(1.5)),
            )

        val host = mockk<HostView>()
        every { host.host.getState() } returns HostState.UP
        every { host.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { host.availableMemory } returns 2048

        scheduler.addHost(host)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.coreCount } returns 2
        every { req.task.flavor.memorySize } returns 2300
        every { req.isCancelled } returns false
        val skipped = slot<Int>()
        justRun { req.setProperty("timesSkipped") value capture(skipped) }
        every { req.getProperty("timesSkipped") } answers { skipped.captured }
        req.timesSkipped = 0

        assertEquals(SchedulingResultType.EMPTY, scheduler.select(mutableListOf(req).iterator()).resultType)
    }
}
