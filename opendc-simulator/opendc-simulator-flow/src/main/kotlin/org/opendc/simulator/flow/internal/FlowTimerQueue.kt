/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.simulator.flow.internal

/**
 * Specialized priority queue for flow timers.
 */
internal class FlowTimerQueue(initialCapacity: Int = 256) {
    /**
     * The binary heap of deadlines.
     */
    private var _deadlines = LongArray(initialCapacity) { Long.MIN_VALUE }

    /**
     * The binary heap of [FlowConsumerContextImpl]s.
     */
    private var _pending = arrayOfNulls<FlowConsumerContextImpl>(initialCapacity)

    /**
     * The number of elements in the priority queue.
     */
    private var size = 0

    /**
     * Register a timer for [ctx] with [deadline].
     */
    fun add(ctx: FlowConsumerContextImpl, deadline: Long) {
        val i = size
        val deadlines = _deadlines
        if (i >= deadlines.size) {
            grow()
        }

        siftUp(deadlines, _pending, i, ctx, deadline)

        size = i + 1
    }

    /**
     * Update all pending [FlowConsumerContextImpl]s at the timestamp [now].
     */
    fun poll(now: Long): FlowConsumerContextImpl? {
        if (size == 0) {
            return null
        }

        val deadlines = _deadlines
        val deadline = deadlines[0]

        if (now < deadline) {
            return null
        }

        val pending = _pending
        val res = pending[0]
        val s = --size

        val nextDeadline = deadlines[s]
        val next = pending[s]!!

        // Clear the last element of the queue
        pending[s] = null
        deadlines[s] = Long.MIN_VALUE

        if (s != 0) {
            siftDown(deadlines, pending, next, nextDeadline)
        }

        return res
    }

    /**
     * Find the earliest deadline in the queue.
     */
    fun peekDeadline(): Long {
        return if (size == 0) Long.MAX_VALUE else _deadlines[0]
    }

    /**
     * Increases the capacity of the array.
     */
    private fun grow() {
        val oldCapacity = _deadlines.size
        // Double size if small; else grow by 50%
        val newCapacity = oldCapacity + if (oldCapacity < 64) oldCapacity + 2 else oldCapacity shr 1

        _deadlines = _deadlines.copyOf(newCapacity)
        _pending = _pending.copyOf(newCapacity)
    }

    /**
     * Insert item [ctx] at position [pos], maintaining heap invariant by promoting [ctx] up the tree until it is
     * greater than or equal to its parent, or is the root.
     *
     * @param deadlines The heap of deadlines.
     * @param pending The heap of contexts.
     * @param pos The position to fill.
     * @param ctx The [FlowConsumerContextImpl] to insert.
     * @param deadline The deadline of the context.
     */
    private fun siftUp(
        deadlines: LongArray,
        pending: Array<FlowConsumerContextImpl?>,
        pos: Int,
        ctx: FlowConsumerContextImpl,
        deadline: Long
    ) {
        var k = pos

        while (k > 0) {
            val parent = (k - 1) ushr 1
            val parentDeadline = deadlines[parent]

            if (deadline >= parentDeadline) {
                break
            }

            deadlines[k] = parentDeadline
            pending[k] = pending[parent]

            k = parent
        }

        deadlines[k] = deadline
        pending[k] = ctx
    }

    /**
     * Inserts [ctx] at the top, maintaining heap invariant by demoting [ctx] down the tree repeatedly until it
     * is less than or equal to its children or is a leaf.
     *
     * @param deadlines The heap of deadlines.
     * @param pending The heap of contexts.
     * @param ctx The [FlowConsumerContextImpl] to insert.
     * @param deadline The deadline of the context.
     */
    private fun siftDown(
        deadlines: LongArray,
        pending: Array<FlowConsumerContextImpl?>,
        ctx: FlowConsumerContextImpl,
        deadline: Long
    ) {
        var k = 0
        val size = size
        val half = size ushr 1

        while (k < half) {
            var child = (k shl 1) + 1

            var childDeadline = deadlines[child]
            val right = child + 1

            if (right < size) {
                val rightDeadline = deadlines[right]

                if (childDeadline > rightDeadline) {
                    child = right
                    childDeadline = rightDeadline
                }
            }

            if (deadline <= childDeadline) {
                break
            }

            deadlines[k] = childDeadline
            pending[k] = pending[child]

            k = child
        }

        deadlines[k] = deadline
        pending[k] = ctx
    }
}
