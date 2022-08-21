/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.simulator.flow2

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Test suite for the [FlowTimerQueue] class.
 */
class FlowTimerQueueTest {
    private lateinit var queue: FlowTimerQueue

    @BeforeEach
    fun setUp() {
        queue = FlowTimerQueue(3)
    }

    /**
     * Test whether a call to [FlowTimerQueue.poll] returns `null` for an empty queue.
     */
    @Test
    fun testPollEmpty() {
        assertAll(
            { assertEquals(Long.MAX_VALUE, queue.peekDeadline()) },
            { assertNull(queue.poll(100L)) }
        )
    }

    /**
     * Test whether a call to [FlowTimerQueue.poll] returns the proper value for a queue with a single entry.
     */
    @Test
    fun testSingleEntry() {
        val entry = mockk<FlowStage>()
        entry.deadline = 100
        entry.timerIndex = -1

        queue.enqueue(entry)

        assertAll(
            { assertEquals(100, queue.peekDeadline()) },
            { assertNull(queue.poll(10L)) },
            { assertEquals(entry, queue.poll(200L)) },
            { assertNull(queue.poll(200L)) }
        )
    }

    /**
     * Test whether [FlowTimerQueue.poll] returns values in the queue in the proper order.
     */
    @Test
    fun testMultipleEntries() {
        val entryA = mockk<FlowStage>()
        entryA.deadline = 100
        entryA.timerIndex = -1

        queue.enqueue(entryA)

        val entryB = mockk<FlowStage>()
        entryB.deadline = 10
        entryB.timerIndex = -1

        queue.enqueue(entryB)

        val entryC = mockk<FlowStage>()
        entryC.deadline = 58
        entryC.timerIndex = -1

        queue.enqueue(entryC)

        assertAll(
            { assertEquals(10, queue.peekDeadline()) },
            { assertEquals(entryB, queue.poll(100L)) },
            { assertEquals(entryC, queue.poll(100L)) },
            { assertEquals(entryA, queue.poll(100L)) },
            { assertNull(queue.poll(100L)) }
        )
    }

    /**
     * Test that the queue is properly resized when the number of entries exceed the capacity.
     */
    @Test
    fun testResize() {
        val entryA = mockk<FlowStage>()
        entryA.deadline = 100
        entryA.timerIndex = -1

        queue.enqueue(entryA)

        val entryB = mockk<FlowStage>()
        entryB.deadline = 20
        entryB.timerIndex = -1

        queue.enqueue(entryB)

        val entryC = mockk<FlowStage>()
        entryC.deadline = 58
        entryC.timerIndex = -1

        queue.enqueue(entryC)

        val entryD = mockk<FlowStage>()
        entryD.deadline = 31
        entryD.timerIndex = -1

        queue.enqueue(entryD)

        assertAll(
            { assertEquals(20, queue.peekDeadline()) },
            { assertEquals(entryB, queue.poll(100L)) },
            { assertEquals(entryD, queue.poll(100L)) },
            { assertEquals(entryC, queue.poll(100L)) },
            { assertEquals(entryA, queue.poll(100L)) },
            { assertNull(queue.poll(100L)) }
        )
    }

    /**
     * Test to verify that we can change the deadline of the last element in the queue.
     */
    @Test
    fun testChangeDeadlineTail() {
        val entryA = mockk<FlowStage>()
        entryA.deadline = 100
        entryA.timerIndex = -1

        queue.enqueue(entryA)

        val entryB = mockk<FlowStage>()
        entryB.deadline = 20
        entryB.timerIndex = -1

        queue.enqueue(entryB)

        val entryC = mockk<FlowStage>()
        entryC.deadline = 58
        entryC.timerIndex = -1

        queue.enqueue(entryC)

        entryA.deadline = 10
        queue.enqueue(entryA)

        assertAll(
            { assertEquals(10, queue.peekDeadline()) },
            { assertEquals(entryA, queue.poll(100L)) },
            { assertEquals(entryB, queue.poll(100L)) },
            { assertEquals(entryC, queue.poll(100L)) },
            { assertNull(queue.poll(100L)) }
        )
    }

    /**
     * Test that we can change the deadline of the head entry in the queue.
     */
    @Test
    fun testChangeDeadlineMiddle() {
        val entryA = mockk<FlowStage>()
        entryA.deadline = 100
        entryA.timerIndex = -1

        queue.enqueue(entryA)

        val entryB = mockk<FlowStage>()
        entryB.deadline = 20
        entryB.timerIndex = -1

        queue.enqueue(entryB)

        val entryC = mockk<FlowStage>()
        entryC.deadline = 58
        entryC.timerIndex = -1

        queue.enqueue(entryC)

        entryC.deadline = 10
        queue.enqueue(entryC)

        assertAll(
            { assertEquals(10, queue.peekDeadline()) },
            { assertEquals(entryC, queue.poll(100L)) },
            { assertEquals(entryB, queue.poll(100L)) },
            { assertEquals(entryA, queue.poll(100L)) },
            { assertNull(queue.poll(100L)) }
        )
    }

    /**
     * Test that we can change the deadline of the head entry in the queue.
     */
    @Test
    fun testChangeDeadlineHead() {
        val entryA = mockk<FlowStage>()
        entryA.deadline = 100
        entryA.timerIndex = -1

        queue.enqueue(entryA)

        val entryB = mockk<FlowStage>()
        entryB.deadline = 20
        entryB.timerIndex = -1

        queue.enqueue(entryB)

        val entryC = mockk<FlowStage>()
        entryC.deadline = 58
        entryC.timerIndex = -1

        queue.enqueue(entryC)

        entryB.deadline = 30
        queue.enqueue(entryB)

        assertAll(
            { assertEquals(30, queue.peekDeadline()) },
            { assertEquals(entryB, queue.poll(100L)) },
            { assertEquals(entryC, queue.poll(100L)) },
            { assertEquals(entryA, queue.poll(100L)) },
            { assertNull(queue.poll(100L)) }
        )
    }

    /**
     * Test that an unchanged deadline results in a no-op.
     */
    @Test
    fun testChangeDeadlineNop() {
        val entryA = mockk<FlowStage>()
        entryA.deadline = 100
        entryA.timerIndex = -1

        queue.enqueue(entryA)

        val entryB = mockk<FlowStage>()
        entryB.deadline = 20
        entryB.timerIndex = -1

        queue.enqueue(entryB)

        val entryC = mockk<FlowStage>()
        entryC.deadline = 58
        entryC.timerIndex = -1

        queue.enqueue(entryC)

        // Should be a no-op
        queue.enqueue(entryA)

        assertAll(
            { assertEquals(20, queue.peekDeadline()) },
            { assertEquals(entryB, queue.poll(100L)) },
            { assertEquals(entryC, queue.poll(100L)) },
            { assertEquals(entryA, queue.poll(100L)) },
            { assertNull(queue.poll(100L)) }
        )
    }

    /**
     * Test that we can remove an entry from the end of the queue.
     */
    @Test
    fun testRemoveEntryTail() {
        val entryA = mockk<FlowStage>()
        entryA.deadline = 100
        entryA.timerIndex = -1

        queue.enqueue(entryA)

        val entryB = mockk<FlowStage>()
        entryB.deadline = 20
        entryB.timerIndex = -1

        queue.enqueue(entryB)

        val entryC = mockk<FlowStage>()
        entryC.deadline = 58
        entryC.timerIndex = -1

        queue.enqueue(entryC)

        entryC.deadline = Long.MAX_VALUE
        queue.enqueue(entryC)

        assertAll(
            { assertEquals(20, queue.peekDeadline()) },
            { assertEquals(entryB, queue.poll(100L)) },
            { assertEquals(entryA, queue.poll(100L)) },
            { assertNull(queue.poll(100L)) }
        )
    }

    /**
     * Test that we can remove an entry from the head of the queue.
     */
    @Test
    fun testRemoveEntryHead() {
        val entryA = mockk<FlowStage>()
        entryA.deadline = 100
        entryA.timerIndex = -1

        queue.enqueue(entryA)

        val entryB = mockk<FlowStage>()
        entryB.deadline = 20
        entryB.timerIndex = -1

        queue.enqueue(entryB)

        val entryC = mockk<FlowStage>()
        entryC.deadline = 58
        entryC.timerIndex = -1

        queue.enqueue(entryC)

        entryB.deadline = Long.MAX_VALUE
        queue.enqueue(entryB)

        assertAll(
            { assertEquals(58, queue.peekDeadline()) },
            { assertEquals(entryC, queue.poll(100L)) },
            { assertEquals(entryA, queue.poll(100L)) },
            { assertNull(queue.poll(100L)) }
        )
    }

    /**
     * Test that we can remove an entry from the middle of a queue.
     */
    @Test
    fun testRemoveEntryMiddle() {
        val entryA = mockk<FlowStage>()
        entryA.deadline = 100
        entryA.timerIndex = -1

        queue.enqueue(entryA)

        val entryB = mockk<FlowStage>()
        entryB.deadline = 20
        entryB.timerIndex = -1

        queue.enqueue(entryB)

        val entryC = mockk<FlowStage>()
        entryC.deadline = 58
        entryC.timerIndex = -1

        queue.enqueue(entryC)

        entryC.deadline = Long.MAX_VALUE
        queue.enqueue(entryC)

        assertAll(
            { assertEquals(20, queue.peekDeadline()) },
            { assertEquals(entryB, queue.poll(100L)) },
            { assertEquals(entryA, queue.poll(100L)) },
            { assertNull(queue.poll(100L)) }
        )
    }
}
