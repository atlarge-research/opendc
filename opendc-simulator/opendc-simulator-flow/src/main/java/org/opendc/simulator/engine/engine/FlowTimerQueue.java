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

package org.opendc.simulator.engine.engine;

import java.util.Arrays;
import org.opendc.simulator.engine.graph.FlowNode;

/**
 * A specialized priority queue for timers of {@link FlowNode}s.
 * <p>
 * By using a specialized priority queue, we reduce the overhead caused by the default priority queue implementation
 * being generic.
 */
public final class FlowTimerQueue {
    /**
     * Array representation of binary heap of {@link FlowNode} instances.
     */
    private FlowNode[] queue;

    /**
     * The number of elements in the priority queue.
     */
    private int size = 0;

    /**
     * Construct a {@link FlowTimerQueue} with the specified initial capacity.
     *
     * @param initialCapacity The initial capacity of the queue.
     */
    public FlowTimerQueue(int initialCapacity) {
        this.queue = new FlowNode[initialCapacity];
    }

    /**
     * Enqueue a timer for the specified context or update the existing timer.
     */
    public void enqueue(FlowNode node) {
        FlowNode[] es = queue;
        int k = node.getTimerIndex();

        if (node.getDeadline() != Long.MAX_VALUE) {
            if (k >= 0) {
                update(es, node, k);
            } else {
                add(es, node);
            }
        } else if (k >= 0) {
            delete(es, k);
        }
    }

    /**
     * Retrieve the head of the queue if its deadline does not exceed <code>now</code>.
     *
     * @param now The timestamp that the deadline of the head of the queue should not exceed.
     * @return The head of the queue if its deadline does not exceed <code>now</code>, otherwise <code>null</code>.
     */
    public FlowNode poll(long now) {
        if (this.size == 0) {
            return null;
        }

        final FlowNode[] es = queue;
        final FlowNode head = es[0];

        if (now < head.getDeadline()) {
            return null;
        }

        int n = size - 1;
        this.size = n;
        final FlowNode next = es[n];
        es[n] = null; // Clear the last element of the queue

        if (n > 0) {
            siftDown(0, next, es, n);
        }

        head.setTimerIndex(-1);
        return head;
    }

    /**
     * Find the earliest deadline in the queue.
     */
    public long peekDeadline() {
        if (this.size > 0) {
            return this.queue[0].getDeadline();
        }

        return Long.MAX_VALUE;
    }

    /**
     * Add a new entry to the queue.
     */
    private void add(FlowNode[] es, FlowNode node) {
        if (this.size >= es.length) {
            // Re-fetch the resized array
            es = grow();
        }

        siftUp(this.size, node, es);

        this.size++;
    }

    /**
     * Update the deadline of an existing entry in the queue.
     */
    private void update(FlowNode[] es, FlowNode node, int k) {
        if (k > 0) {
            int parent = (k - 1) >>> 1;
            if (es[parent].getDeadline() > node.getDeadline()) {
                siftUp(k, node, es);
                return;
            }
        }

        siftDown(k, node, es, this.size);
    }

    /**
     * Deadline an entry from the queue.
     */
    private void delete(FlowNode[] es, int k) {
        int s = --this.size;
        if (s == k) {
            es[k] = null; // Element is last in the queue
        } else {
            FlowNode moved = es[s];
            es[s] = null;

            siftDown(k, moved, es, s);

            if (es[k] == moved) {
                siftUp(k, moved, es);
            }
        }
    }

    /**
     * Increases the capacity of the array.
     */
    private FlowNode[] grow() {
        FlowNode[] queue = this.queue;
        int oldCapacity = queue.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);

        queue = Arrays.copyOf(queue, newCapacity);
        this.queue = queue;
        return queue;
    }

    private static void siftUp(int k, FlowNode key, FlowNode[] es) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            FlowNode e = es[parent];
            if (key.getDeadline() >= e.getDeadline()) break;
            es[k] = e;
            e.setTimerIndex(k);
            k = parent;
        }
        es[k] = key;
        key.setTimerIndex(k);
    }

    private static void siftDown(int k, FlowNode key, FlowNode[] es, int n) {
        int half = n >>> 1; // loop while a non-leaf
        while (k < half) {
            int child = (k << 1) + 1; // assume left child is least
            FlowNode c = es[child];
            int right = child + 1;
            if (right < n && c.getDeadline() > es[right].getDeadline()) c = es[child = right];

            if (key.getDeadline() <= c.getDeadline()) break;

            es[k] = c;
            c.setTimerIndex(k);
            k = child;
        }

        es[k] = key;
        key.setTimerIndex(k);
    }
}
