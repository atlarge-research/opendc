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

package org.opendc.simulator

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Test suite for the [TaskQueue] class.
 */
class TaskQueueTest {
    private lateinit var queue: TaskQueue

    @BeforeEach
    fun setUp() {
        queue = TaskQueue(3)
    }

    /**
     * Test whether a call to [TaskQueue.poll] returns `null` for an empty queue.
     */
    @Test
    fun testPollEmpty() {
        assertAll(
            { assertEquals(Long.MAX_VALUE, queue.peekDeadline()) },
            { assertNull(queue.poll()) }
        )
    }

    /**
     * Test whether a call to [TaskQueue.poll] returns the proper value for a queue with a single entry.
     */
    @Test
    fun testSingleEntry() {
        val entry = Runnable {}

        queue.add(100, 1, entry)

        assertAll(
            { assertEquals(100, queue.peekDeadline()) },
            { assertEquals(entry, queue.poll()) },
            { assertNull(queue.poll()) }
        )
    }

    /**
     * Test whether [TaskQueue.poll] returns values in the queue in the proper order.
     */
    @Test
    fun testMultipleEntries() {
        val entryA = Runnable {}
        queue.add(100, 1, entryA)

        val entryB = Runnable {}
        queue.add(48, 1, entryB)

        val entryC = Runnable {}
        queue.add(58, 1, entryC)

        assertAll(
            { assertEquals(48, queue.peekDeadline()) },
            { assertEquals(entryB, queue.poll()) },
            { assertEquals(entryC, queue.poll()) },
            { assertEquals(entryA, queue.poll()) },
            { assertNull(queue.poll()) }
        )
    }

    /**
     * Test whether [TaskQueue.poll] returns values in the queue in the proper order with duplicates.
     */
    @Test
    fun testMultipleEntriesDuplicate() {
        val entryA = Runnable {}
        queue.add(48, 0, entryA)

        val entryB = Runnable {}
        queue.add(48, 1, entryB)

        val entryC = Runnable {}
        queue.add(48, 2, entryC)

        assertAll(
            { assertEquals(48, queue.peekDeadline()) },
            { assertEquals(entryA, queue.poll()) },
            { assertEquals(entryB, queue.poll()) },
            { assertEquals(entryC, queue.poll()) },
            { assertNull(queue.poll()) }
        )
    }

    /**
     * Test that the queue is properly resized when the number of entries exceed the capacity.
     */
    @Test
    fun testResize() {
        val entryA = Runnable {}
        queue.add(100, 1, entryA)

        val entryB = Runnable {}
        queue.add(20, 1, entryB)

        val entryC = Runnable {}
        queue.add(58, 1, entryC)

        val entryD = Runnable {}
        queue.add(38, 1, entryD)

        assertAll(
            { assertEquals(20, queue.peekDeadline()) },
            { assertEquals(entryB, queue.poll()) },
            { assertEquals(entryD, queue.poll()) },
            { assertEquals(entryC, queue.poll()) },
            { assertEquals(entryA, queue.poll()) },
            { assertNull(queue.poll()) }
        )
    }

    /**
     * Test that we can remove an entry from the end of the queue.
     */
    @Test
    fun testRemoveEntryTail() {
        val entryA = Runnable {}
        queue.add(100, 1, entryA)

        val entryB = Runnable {}
        queue.add(20, 1, entryB)

        val entryC = Runnable {}
        queue.add(58, 1, entryC)

        queue.remove(100, 1)

        assertAll(
            { assertEquals(20, queue.peekDeadline()) },
            { assertEquals(entryB, queue.poll()) },
            { assertEquals(entryC, queue.poll()) },
            { assertNull(queue.poll()) }
        )
    }

    /**
     * Test that we can remove an entry from the head of the queue.
     */
    @Test
    fun testRemoveEntryHead() {
        val entryA = Runnable {}
        queue.add(100, 1, entryA)

        val entryB = Runnable {}
        queue.add(20, 1, entryB)

        val entryC = Runnable {}
        queue.add(58, 1, entryC)

        queue.remove(20, 1)

        assertAll(
            { assertEquals(58, queue.peekDeadline()) },
            { assertEquals(entryC, queue.poll()) },
            { assertEquals(entryA, queue.poll()) },
            { assertNull(queue.poll()) }
        )
    }

    /**
     * Test that we can remove an entry from the middle of a queue.
     */
    @Test
    fun testRemoveEntryMiddle() {
        val entryA = Runnable {}
        queue.add(100, 1, entryA)

        val entryB = Runnable {}
        queue.add(20, 1, entryB)

        val entryC = Runnable {}
        queue.add(58, 1, entryC)

        queue.remove(58, 1)

        assertAll(
            { assertEquals(20, queue.peekDeadline()) },
            { assertEquals(entryB, queue.poll()) },
            { assertEquals(entryA, queue.poll()) },
            { assertNull(queue.poll()) }
        )
    }

    /**
     * Test that we can "remove" an unknown entry without error.
     */
    @Test
    fun testRemoveUnknown() {
        val entryA = Runnable {}
        queue.add(100, 1, entryA)

        val entryB = Runnable {}
        queue.add(20, 1, entryB)

        val entryC = Runnable {}
        queue.add(58, 1, entryC)

        assertAll(
            { assertFalse(queue.remove(10, 1)) },
            { assertFalse(queue.remove(58, 2)) }
        )
    }
}
