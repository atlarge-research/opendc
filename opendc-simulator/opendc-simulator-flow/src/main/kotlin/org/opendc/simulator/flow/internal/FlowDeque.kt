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

import java.util.*

/**
 * A specialized [ArrayDeque] that tracks the [FlowConsumerContextImpl] instances that have updated in an interpreter
 * cycle.
 *
 * By using a specialized class, we reduce the overhead caused by type-erasure.
 */
internal class FlowDeque(initialCapacity: Int = 256) {
    /**
     * The array of elements in the queue.
     */
    private var _elements: Array<FlowConsumerContextImpl?> = arrayOfNulls(initialCapacity)
    private var _head = 0
    private var _tail = 0

    /**
     * Determine whether this queue is not empty.
     */
    fun isNotEmpty(): Boolean {
        return _head != _tail
    }

    /**
     * Add the specified [ctx] to the queue.
     */
    fun add(ctx: FlowConsumerContextImpl) {
        val es = _elements
        var tail = _tail

        es[tail] = ctx

        tail = inc(tail, es.size)
        _tail = tail

        if (_head == tail) {
            doubleCapacity()
        }
    }

    /**
     * Remove a [FlowConsumerContextImpl] from the queue or `null` if the queue is empty.
     */
    fun poll(): FlowConsumerContextImpl? {
        val es = _elements
        val head = _head
        val ctx = es[head]

        if (ctx != null) {
            es[head] = null
            _head = inc(head, es.size)
        }

        return ctx
    }

    /**
     * Clear the queue.
     */
    fun clear() {
        _elements.fill(null)
        _head = 0
        _tail = 0
    }

    private fun inc(i: Int, modulus: Int): Int {
        var x = i
        if (++x >= modulus) {
            x = 0
        }
        return x
    }

    /**
     * Doubles the capacity of this deque
     */
    private fun doubleCapacity() {
        assert(_head == _tail)
        val p = _head
        val n = _elements.size
        val r = n - p // number of elements to the right of p

        val newCapacity = n shl 1
        check(newCapacity >= 0) { "Sorry, deque too big" }

        val a = arrayOfNulls<FlowConsumerContextImpl>(newCapacity)

        _elements.copyInto(a, 0, p, n)
        _elements.copyInto(a, r, 0, p)

        _elements = a
        _head = 0
        _tail = n
    }
}
