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

package org.opendc.common;

import java.time.InstantSource;

/**
 * A {@link Dispatcher} is used in OpenDC to schedule the execution of future tasks over potentially multiple threads.
 */
public interface Dispatcher {
    /**
     * Return the time source of the dispatcher as an {@link InstantSource}.
     */
    InstantSource getTimeSource();

    /**
     * Schedule the specified {@link Runnable} to run as soon as possible.
     *
     * @param command The task to execute.
     */
    default void schedule(Runnable command) {
        schedule(0, command);
    }

    /**
     * Schedule the specified {@link Runnable} to run after the specified <code>delay</code>.
     * <p>
     * Use this method to eliminate potential allocations in case the task does not need to be cancellable.
     *
     * @param delayMs The time from now to the delayed execution (in milliseconds).
     * @param command The task to execute.
     */
    void schedule(long delayMs, Runnable command);

    /**
     * Schedule the specified {@link Runnable} to run after the specified <code>delay</code>.
     *
     * @param delayMs The time from now to the delayed execution (in milliseconds).
     * @param command The task to execute.
     * @return A {@link DispatcherHandle} representing pending completion of the task.
     */
    DispatcherHandle scheduleCancellable(long delayMs, Runnable command);
}
