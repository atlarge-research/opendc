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

package org.opendc.simulator;

import java.util.Arrays;

/**
 * Specialized priority queue for pending tasks.
 *
 * <p>
 * This class uses a specialized priority queue (as opposed to a generic {@link java.util.PriorityQueue}), which reduces
 * unnecessary allocations in the simulator's hot path.
 */
final class TaskQueue {
    /**
     * The deadlines of the pending tasks.
     */
    private long[] deadlines;

    /**
     * The identifiers of the pending tasks. Identifiers are used to provide a total order for pending tasks in case
     * the deadline of two tasks is the same.
     */
    private int[] ids;

    /**
     * The {@link Runnable}s representing the tasks that have been scheduled.
     */
    private Runnable[] tasks;

    /**
     * The number of elements in the priority queue.
     */
    private int size = 0;

    /**
     * Construct a {@link TaskQueue} with the specified initial capacity.
     *
     * @param initialCapacity The initial capacity of the queue.
     */
    public TaskQueue(int initialCapacity) {
        this.deadlines = new long[initialCapacity];
        this.ids = new int[initialCapacity];
        this.tasks = new Runnable[initialCapacity];
    }

    /**
     * Construct a {@link TaskQueue} with an initial capacity of 256 elements.
     */
    public TaskQueue() {
        this(256);
    }

    /**
     * Add a new task to this queue.
     *
     * @param deadline The deadline of the task.
     * @param id       The identifier of the task.
     * @param task     The {@link Runnable} representing the task to execute.
     */
    public void add(long deadline, int id, Runnable task) {
        int i = size;
        long[] deadlines = this.deadlines;

        if (i >= deadlines.length) {
            grow();

            // Re-fetch the resized array
            deadlines = this.deadlines;
        }

        siftUp(deadlines, ids, tasks, i, deadline, id, task);

        size = i + 1;
    }

    /**
     * Retrieve the next task to be executed.
     *
     * @return The head of the queue or <code>null</code> if the queue is empty.
     */
    public Runnable poll() {
        final Runnable[] tasks = this.tasks;
        final Runnable result = tasks[0];

        if (result != null) {
            int n = --size;

            if (n > 0) {
                long[] deadlines = this.deadlines;
                int[] ids = this.ids;

                siftDown(deadlines, ids, tasks, 0, n, deadlines[n], ids[n], tasks[n]);
            }

            // Clear the last element of the queue
            tasks[n] = null;
        }

        return result;
    }

    /**
     * Find the earliest deadline in the queue.
     *
     * @return The earliest deadline in the queue or {@link Long#MAX_VALUE} if the queue is empty.
     */
    public long peekDeadline() {
        if (size == 0) {
            return Long.MAX_VALUE;
        }

        return deadlines[0];
    }

    /**
     * Remove the timer entry with the specified <code>deadline</code> and <code>id</code>.
     */
    public boolean remove(long deadline, int id) {
        long[] deadlines = this.deadlines;
        int[] ids = this.ids;

        int size = this.size;
        int i = -1;

        for (int j = 0; j < size; j++) {
            if (deadlines[j] == deadline && ids[j] == id) {
                i = j;
                break;
            }
        }

        if (i < 0) {
            return false;
        }

        Runnable[] tasks = this.tasks;
        int s = size - 1;
        this.size = s;

        if (s == i) {
            tasks[i] = null;
        } else {
            long movedDeadline = deadlines[s];
            int movedId = ids[s];
            Runnable movedTask = tasks[s];

            tasks[s] = null;

            siftDown(deadlines, ids, tasks, i, s, movedDeadline, movedId, movedTask);
            if (tasks[i] == movedTask) {
                siftUp(deadlines, ids, tasks, i, movedDeadline, movedId, movedTask);
            }
        }

        return true;
    }

    /**
     * Increases the capacity of the priority queue.
     */
    private void grow() {
        int oldCapacity = deadlines.length;

        // Double size if small; else grow by 50%
        int newCapacity = oldCapacity + (oldCapacity < 64 ? oldCapacity + 2 : oldCapacity >> 1);

        deadlines = Arrays.copyOf(deadlines, newCapacity);
        ids = Arrays.copyOf(ids, newCapacity);
        tasks = Arrays.copyOf(tasks, newCapacity);
    }

    /**
     * Sift up an entry in the heap.
     */
    private static void siftUp(
            long[] deadlines, int[] ids, Runnable[] tasks, int k, long deadline, int id, Runnable task) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            long parentDeadline = deadlines[parent];
            int parentId = ids[parent];

            if (compare(deadline, id, parentDeadline, parentId) >= 0) {
                break;
            }

            deadlines[k] = parentDeadline;
            ids[k] = parentId;
            tasks[k] = tasks[parent];

            k = parent;
        }

        deadlines[k] = deadline;
        ids[k] = id;
        tasks[k] = task;
    }

    /**
     * Sift down an entry in the heap.
     */
    private static void siftDown(
            long[] deadlines, int[] ids, Runnable[] tasks, int k, int n, long deadline, int id, Runnable task) {
        int half = n >>> 1; // loop while a non-leaf

        while (k < half) {
            int child = (k << 1) + 1; // assume left child is least

            long childDeadline = deadlines[child];
            int childId = ids[child];

            int right = child + 1;
            if (right < n) {
                long rightDeadline = deadlines[right];
                int rightId = ids[right];

                if (compare(childDeadline, childId, rightDeadline, rightId) > 0) {
                    child = right;
                    childDeadline = rightDeadline;
                    childId = rightId;
                }
            }

            if (compare(deadline, id, childDeadline, childId) <= 0) {
                break;
            }

            deadlines[k] = childDeadline;
            ids[k] = childId;
            tasks[k] = tasks[child];

            k = child;
        }

        deadlines[k] = deadline;
        ids[k] = id;
        tasks[k] = task;
    }

    /**
     * Helper method to compare two task entries.
     */
    private static int compare(long leftDeadline, int leftId, long rightDeadline, int rightId) {
        int cmp = Long.compare(leftDeadline, rightDeadline);
        return cmp == 0 ? Integer.compare(leftId, rightId) : cmp;
    }
}
