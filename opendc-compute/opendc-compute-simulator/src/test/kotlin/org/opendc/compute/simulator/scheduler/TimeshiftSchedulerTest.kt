/*
 * Copyright (c) 2025 AtLarge Research
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
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.opendc.compute.simulator.scheduler.timeshift.TimeshiftScheduler
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
                forecast = false,
            )

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
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
                forecast = false,
            )

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
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
