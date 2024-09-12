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
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.driver.HostModel
import org.opendc.compute.service.driver.HostState
import org.opendc.compute.service.scheduler.filters.RamFilter
import java.util.Random
import java.util.random.RandomGenerator

internal class TwoChoiceSchedulerTest {
    @Test
    fun testNoHosts() {
        val scheduler =
            TwoChoiceScheduler(
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
            TwoChoiceScheduler(
                filters = emptyList(),
                random = Random(1),
            )

        val hostSource = mockk<Host>()
        every { hostSource.getModel().memoryCapacity() } returns 1

        val hostA = HostView(hostSource)
        val hostB = HostView(hostSource)

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
        val scheduler =
            TwoChoiceScheduler(
                filters = listOf(RamFilter(1.0)),
                random = Random(1),
            )

        val hostSourceA = mockk<Host>()
        every { hostSourceA.getModel() } returns HostModel(4 * 2600.0, 1, 4, 512)
        val hostSourceB = mockk<Host>()
        every { hostSourceB.getModel() } returns HostModel(4 * 2600.0, 1, 4, 2048)

        val hostA = HostView(hostSourceA)
        val hostB = HostView(hostSourceB)

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val task = mockk<Task>()
        every { task.flavor.coreCount } returns 2
        every { task.flavor.memorySize } returns 1024

        assertEquals(hostB, scheduler.select(mutableListOf(task).iterator()))
    }
}
