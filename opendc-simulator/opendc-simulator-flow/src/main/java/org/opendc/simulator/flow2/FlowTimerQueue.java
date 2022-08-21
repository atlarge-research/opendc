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

import java.util.Arrays;

/**
 * A specialized priority queue for timers of {@link FlowStageLogic}s.
 * <p>
 * By using a specialized priority queue, we reduce the overhead caused by the default priority queue implementation
 * being generic.
 */
final class FlowTimerQueue {
    /**
     * Array representation of binary heap of {@link FlowStage} instances.
     */
    private FlowStage[] queue;

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
        this.queue = new FlowStage[initialCapacity];
    }

    /**
     * Enqueue a timer for the specified context or update the existing timer.
     */
    void enqueue(FlowStage ctx) {
        FlowStage[] es = queue;
        int k = ctx.timerIndex;

        if (ctx.deadline != Long.MAX_VALUE) {
            if (k >= 0) {
                update(es, ctx, k);
            } else {
                add(es, ctx);
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
    FlowStage poll(long now) {
        int size = this.size;
        if (size == 0) {
            return null;
        }

        final FlowStage[] es = queue;
        final FlowStage head = es[0];

        if (now < head.deadline) {
            return null;
        }

        int n = size - 1;
        this.size = n;
        final FlowStage next = es[n];
        es[n] = null; // Clear the last element of the queue

        if (n > 0) {
            siftDown(0, next, es, n);
        }

        head.timerIndex = -1;
        return head;
    }

    /**
     * Find the earliest deadline in the queue.
     */
    long peekDeadline() {
        if (size > 0) {
            return queue[0].deadline;
        }

        return Long.MAX_VALUE;
    }

    /**
     * Add a new entry to the queue.
     */
    private void add(FlowStage[] es, FlowStage ctx) {
        int i = size;

        if (i >= es.length) {
            // Re-fetch the resized array
            es = grow();
        }

        siftUp(i, ctx, es);

        size = i + 1;
    }

    /**
     * Update the deadline of an existing entry in the queue.
     */
    private void update(FlowStage[] es, FlowStage ctx, int k) {
        if (k > 0) {
            int parent = (k - 1) >>> 1;
            if (es[parent].deadline > ctx.deadline) {
                siftUp(k, ctx, es);
                return;
            }
        }

        siftDown(k, ctx, es, size);
    }

    /**
     * Deadline an entry from the queue.
     */
    private void delete(FlowStage[] es, int k) {
        int s = --size;
        if (s == k) {
            es[k] = null; // Element is last in the queue
        } else {
            FlowStage moved = es[s];
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
    private FlowStage[] grow() {
        FlowStage[] queue = this.queue;
        int oldCapacity = queue.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);

        queue = Arrays.copyOf(queue, newCapacity);
        this.queue = queue;
        return queue;
    }

    private static void siftUp(int k, FlowStage key, FlowStage[] es) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            FlowStage e = es[parent];
            if (key.deadline >= e.deadline) break;
            es[k] = e;
            e.timerIndex = k;
            k = parent;
        }
        es[k] = key;
        key.timerIndex = k;
    }

    private static void siftDown(int k, FlowStage key, FlowStage[] es, int n) {
        int half = n >>> 1; // loop while a non-leaf
        while (k < half) {
            int child = (k << 1) + 1; // assume left child is least
            FlowStage c = es[child];
            int right = child + 1;
            if (right < n && c.deadline > es[right].deadline) c = es[child = right];

            if (key.deadline <= c.deadline) break;

            es[k] = c;
            c.timerIndex = k;
            k = child;
        }

        es[k] = key;
        key.timerIndex = k;
    }
}
