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

package org.opendc.simulator.compute.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.opendc.simulator.compute.infrastructure.SimHost
import org.opendc.simulator.compute.scheduler.ComputeScheduler
import org.opendc.simulator.compute.scheduler.SchedulingRequest
import org.opendc.simulator.compute.scheduler.SchedulingResult
import org.opendc.simulator.compute.scheduler.SchedulingResultType
import org.opendc.simulator.compute.scheduler.priority.ScorePriority
import org.opendc.simulator.compute.task.SimTask
import org.opendc.simulator.core.SimulationDispatcher

/**
 * Tests for the order in which [ComputeService] offers waiting tasks to its scheduler.
 */
internal class ComputeServiceQueueTest {
    /**
     * A scheduler that places nothing and records the ids of the waiting tasks, in the order the service offers them.
     */
    private open class RecordingScheduler : ComputeScheduler {
        val offered = mutableListOf<List<Int>>()

        override fun addHost(host: SimHost) {}

        override fun removeHost(host: SimHost) {}

        override fun failHost(host: SimHost) {}

        override fun restartHost(host: SimHost) {}

        override fun updateHost(host: SimHost) {}

        override fun select(iter: MutableIterator<SchedulingRequest>): SchedulingResult {
            val ids = mutableListOf<Int>()
            iter.forEach { ids += it.task.id }
            offered += ids
            return SchedulingResult(SchedulingResultType.EMPTY)
        }
    }

    private class PriorityRecordingScheduler(
        override val taskPriority: ScorePriority,
    ) : RecordingScheduler()

    private fun task(id: Int): SimTask {
        val task = mockk<SimTask>()
        every { task.id } returns id
        every { task.numFailures } returns 0
        every { task.hasParents() } returns false
        return task
    }

    @Test
    fun testFirstInFirstOut() {
        val dispatcher = SimulationDispatcher()
        val scheduler = RecordingScheduler()
        val service = ComputeService(dispatcher, scheduler)

        service.schedule(task(1))
        service.schedule(task(2))
        service.schedule(task(3), atFront = true)
        dispatcher.advanceUntilIdle()
        service.close()

        assertNull(scheduler.taskPriority)
        assertEquals(listOf(3, 1, 2), scheduler.offered.single())
    }

    @Test
    fun testPriority() {
        val dispatcher = SimulationDispatcher()
        val scheduler = PriorityRecordingScheduler(ScorePriority(mapOf(1 to 1.0, 2 to 1.0, 3 to 1.0, 4 to 2.0)))
        val service = ComputeService(dispatcher, scheduler)

        service.schedule(task(1))
        service.schedule(task(2))
        service.schedule(task(3), atFront = true)
        service.schedule(task(4))
        dispatcher.advanceUntilIdle()
        service.close()

        // Task 4 has the highest priority; among the equal ones, task 3 was put at the front
        assertEquals(listOf(4, 3, 1, 2), scheduler.offered.single())
    }
}
