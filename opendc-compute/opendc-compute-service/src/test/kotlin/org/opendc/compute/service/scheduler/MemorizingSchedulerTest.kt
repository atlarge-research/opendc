package org.opendc.compute.service.scheduler

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.api.Task
import org.opendc.compute.service.HostView
import org.opendc.compute.service.driver.HostModel
import org.opendc.compute.service.driver.HostState
import org.opendc.compute.service.scheduler.filters.RamFilter
import java.util.Random
import java.util.random.RandomGenerator

internal class MemorizingSchedulerTest {
    @Test
    fun testNoHosts() {
        val scheduler =
            MemorizingScheduler(
                filters = emptyList(),
            )

        val task = mockk<Task>()
        every { task.flavor.coreCount } returns 2
        every { task.flavor.memorySize } returns 1024

        assertNull(scheduler.select(mutableListOf(task).iterator()))
    }

    @Test
    fun testNoFiltersAndSchedulersRandom() {
        val scheduler =
            MemorizingScheduler(
                filters = emptyList(),
                random = Random(1),
            )

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.DOWN

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val task = mockk<Task>()
        every { task.flavor.coreCount } returns 2
        every { task.flavor.memorySize } returns 1024

        // Make sure we get the first host both times
        assertAll(
            { assertEquals(hostB, scheduler.select(mutableListOf(task).iterator())) },
            { assertEquals(hostA, scheduler.select(mutableListOf(task).iterator())) },
        )
    }

    @Test
    fun testRamFilter() {
        // Make Random with predictable order of numbers to test max skipped logic
        val r = mockk<RandomGenerator>()
        val scheduler =
            MemorizingScheduler(
                filters = listOf(RamFilter(1.0)),
                random = r,
                maxTimesSkipped = 3
            )

        every { r.nextInt(any()) } returns 0

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.UP
        every { hostA.host.model } returns HostModel(4 * 2600.0, 1, 4, 2048)
        every { hostA.availableMemory } returns 512

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP
        every { hostB.host.model } returns HostModel(4 * 2600.0, 1, 4, 2048)
        every { hostB.availableMemory } returns 2048

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val task = mockk<Task>()
        every { task.flavor.coreCount } returns 2
        every { task.flavor.memorySize } returns 1024
        val skipped = slot<Int>()
        justRun { task.setProperty("timesSkipped") value capture(skipped)  }
        every { task.getProperty("timesSkipped") } answers { skipped.captured }
        task.timesSkipped = 0

        assertNull(scheduler.select(mutableListOf(task).iterator()))
        assertNull(scheduler.select(mutableListOf(task).iterator()))
        assertNull(scheduler.select(mutableListOf(task).iterator()))
        assertEquals(hostB, scheduler.select(mutableListOf(task).iterator()))
    }

    @Test
    fun testRamFilterOvercommit() {
        val scheduler =
            MemorizingScheduler(
                filters = listOf(RamFilter(1.5))
            )

        val host = mockk<HostView>()
        every { host.host.state } returns HostState.UP
        every { host.host.model } returns HostModel(4 * 2600.0, 1, 4, 2048)
        every { host.availableMemory } returns 2048

        scheduler.addHost(host)

        val task = mockk<Task>()
        every { task.flavor.coreCount } returns 2
        every { task.flavor.memorySize } returns 2300
        val skipped = slot<Int>()
        justRun { task.setProperty("timesSkipped") value capture(skipped)  }
        every { task.getProperty("timesSkipped") } answers { skipped.captured }
        task.timesSkipped = 0

        assertNull(scheduler.select(mutableListOf(task).iterator()))
    }
}
