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
 * A specialized monotonic stack implementation for tracking the scheduled engine invocations.
 * <p>
 * By using a specialized class, we reduce the overhead caused by type-erasure.
 */
final class InvocationStack {
    /**
     * The array of elements in the stack.
     */
    private long[] elements;

    private int head = -1;

    public InvocationStack(int initialCapacity) {
        elements = new long[initialCapacity];
        Arrays.fill(elements, Long.MIN_VALUE);
    }

    /**
     * Try to add the specified invocation to the monotonic stack.
     *
     * @param invocation The timestamp of the invocation.
     * @return <code>true</code> if the invocation was added, <code>false</code> otherwise.
     */
    boolean tryAdd(long invocation) {
        final long[] es = elements;
        int head = this.head;

        if (head < 0 || es[head] > invocation) {
            es[head + 1] = invocation;
            this.head = head + 1;

            if (head + 2 == es.length) {
                doubleCapacity();
            }

            return true;
        }

        return false;
    }

    /**
     * Remove the head invocation from the stack or return {@link Long#MAX_VALUE} if the stack is empty.
     */
    long poll() {
        final long[] es = elements;
        int head = this.head--;

        if (head >= 0) {
            return es[head];
        }

        return Long.MAX_VALUE;
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

        elements = Arrays.copyOf(elements, newCapacity);
    }
}
