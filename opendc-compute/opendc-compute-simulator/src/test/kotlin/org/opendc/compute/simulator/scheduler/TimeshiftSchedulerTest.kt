package org.opendc.compute.simulator.scheduler

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.opendc.compute.simulator.service.TaskNature
import java.time.Duration
import java.time.Instant
import java.time.InstantSource

class TimeshiftSchedulerTest {
    @Test
    fun testBasicDeferring() {
        val clock = mockk<InstantSource>()
        every { clock.instant() } returns Instant.ofEpochMilli(10)

        val scheduler =
            TimeshiftScheduler(
                filters = emptyList(),
                weighers = emptyList(),
                windowSize = 2,
                clock = clock,
            )

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.coreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
        every { req.isCancelled } returns false
        every { req.task.nature } returns TaskNature(true)
        every { req.task.duration } returns Duration.ofMillis(10)
        every { req.task.deadline } returns 50

        scheduler.updateCarbonIntensity(100.0)
        scheduler.updateCarbonIntensity(200.0)

        assertEquals(SchedulingResultType.EMPTY, scheduler.select(mutableListOf(req).iterator()).resultType)
    }

    @Test
    fun testRespectDeadline() {
        val clock = mockk<InstantSource>()
        every { clock.instant() } returns Instant.ofEpochMilli(10)

        val scheduler =
            TimeshiftScheduler(
                filters = emptyList(),
                weighers = emptyList(),
                windowSize = 2,
                clock = clock,
            )

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.coreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
        every { req.isCancelled } returns false
        every { req.task.nature } returns TaskNature(true)
        every { req.task.duration } returns Duration.ofMillis(10)
        every { req.task.deadline } returns 20

        scheduler.updateCarbonIntensity(100.0)
        scheduler.updateCarbonIntensity(200.0)

        // The scheduler tries to schedule the task, but fails as there are no hosts.
        assertEquals(SchedulingResultType.FAILURE, scheduler.select(mutableListOf(req).iterator()).resultType)
    }
}
