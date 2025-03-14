package org.opendc.compute.simulator.scheduler

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TimeShiftSchedulerTest {
    @Test
    fun testNoHosts() {
        val scheduler =
            TimeShiftScheduler(
                filters = emptyList(),
                weighers = emptyList(),
                windowSize = 3,
                clock =
            )

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.coreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(SchedulingResultType.FAILURE, scheduler.select(mutableListOf(req).iterator()).resultType)
    }
}
