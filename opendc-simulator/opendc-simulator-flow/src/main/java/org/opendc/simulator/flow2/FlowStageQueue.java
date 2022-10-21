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

package org.opendc.simulator.flow2;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * A specialized {@link ArrayDeque} implementation that contains the {@link FlowStageLogic}s
 * that have been updated during the engine cycle and should converge.
 * <p>
 * By using a specialized class, we reduce the overhead caused by type-erasure.
 */
final class FlowStageQueue {
    /**
     * The array of elements in the queue.
     */
    private FlowStage[] elements;

    private int head = 0;
    private int tail = 0;

    public FlowStageQueue(int initialCapacity) {
        elements = new FlowStage[initialCapacity];
    }

    /**
     * Add the specified context to the queue.
     */
    void add(FlowStage ctx) {
        final FlowStage[] es = elements;
        int tail = this.tail;

        es[tail] = ctx;

        tail = inc(tail, es.length);
        this.tail = tail;

        if (head == tail) {
            doubleCapacity();
        }
    }

    /**
     * Remove a {@link FlowStage} from the queue or <code>null</code> if the queue is empty.
     */
    FlowStage poll() {
        final FlowStage[] es = elements;
        int head = this.head;
        FlowStage ctx = es[head];

        if (ctx != null) {
            es[head] = null;
            this.head = inc(head, es.length);
        }

        return ctx;
    }

    /**
     * Doubles the capacity of this deque
     */
    private void doubleCapacity() {
        int oldCapacity = elements.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity < 0) {
            throw new IllegalStateException("Sorry, deque too big");
        }

        final FlowStage[] es = elements = Arrays.copyOf(elements, newCapacity);

        // Exceptionally, here tail == head needs to be disambiguated
        if (tail < head || (tail == head && es[head] != null)) {
            // wrap around; slide first leg forward to end of array
            int newSpace = newCapacity - oldCapacity;
            System.arraycopy(es, head, es, head + newSpace, oldCapacity - head);
            for (int i = head, to = (head += newSpace); i < to; i++) es[i] = null;
        }
    }

    /**
     * Circularly increments i, mod modulus.
     * Precondition and postcondition: 0 <= i < modulus.
     */
    private static int inc(int i, int modulus) {
        if (++i >= modulus) i = 0;
        return i;
    }
}
