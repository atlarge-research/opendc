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

package org.opendc.common.util;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.PriorityQueue;
import org.jetbrains.annotations.NotNull;
import org.opendc.common.Dispatcher;
import org.opendc.common.DispatcherHandle;

/**
 * A {@link TimerScheduler} facilitates scheduled execution of future tasks.
 */
public final class TimerScheduler<T> {
    private final Dispatcher dispatcher;

    /**
     * The stack of the invocations to occur in the future.
     */
    private final ArrayDeque<Invocation> invocations = new ArrayDeque<>();

    /**
     * A priority queue containing the tasks to be scheduled in the future.
     */
    private final PriorityQueue<Timer<T>> queue = new PriorityQueue<Timer<T>>();

    /**
     * A map that keeps track of the timers.
     */
    private final HashMap<T, Timer<T>> timers = new HashMap<>();

    /**
     * Construct a {@link TimerScheduler} instance.
     *
     * @param dispatcher The {@link Dispatcher} to schedule future invocations.
     */
    public TimerScheduler(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Start a timer that will invoke the specified [block] after [delay].
     * <p>
     * Each timer has a key and if a new timer with same key is started the previous is cancelled.
     *
     * @param key The key of the timer to start.
     * @param delay The delay before invoking the block.
     * @param block The block to invoke.
     */
    public void startSingleTimer(T key, long delay, Runnable block) {
        startSingleTimerTo(key, dispatcher.getTimeSource().millis() + delay, block);
    }

    /**
     * Start a timer that will invoke the specified [block] at [timestamp].
     * <p>
     * Each timer has a key and if a new timer with same key is started the previous is cancelled.
     *
     * @param key The key of the timer to start.
     * @param timestamp The timestamp at which to invoke the block.
     * @param block The block to invoke.
     */
    public void startSingleTimerTo(T key, long timestamp, Runnable block) {
        long now = dispatcher.getTimeSource().millis();
        final PriorityQueue<Timer<T>> queue = this.queue;
        final ArrayDeque<Invocation> invocations = this.invocations;

        if (timestamp < now) {
            throw new IllegalArgumentException("Timestamp must be in the future");
        }

        timers.compute(key, (k, old) -> {
            if (old != null && old.timestamp == timestamp) {
                // Fast-path: timer for the same timestamp already exists
                old.block = block;
                return old;
            } else {
                // Slow-path: cancel old timer and replace it with new timer
                Timer<T> timer = new Timer<T>(key, timestamp, block);

                if (old != null) {
                    old.isCancelled = true;
                }
                queue.add(timer);
                trySchedule(now, invocations, timestamp);

                return timer;
            }
        });
    }

    /**
     * Check if a timer with a given key is active.
     *
     * @param key The key to check if active.
     * @return `true` if the timer with the specified [key] is active, `false` otherwise.
     */
    public boolean isTimerActive(T key) {
        return timers.containsKey(key);
    }

    /**
     * Cancel a timer with a given key.
     * <p>
     * If canceling a timer that was already canceled, or key never was used to start
     * a timer this operation will do nothing.
     *
     * @param key The key of the timer to cancel.
     */
    public void cancel(T key) {
        final Timer<T> timer = timers.remove(key);

        // Mark the timer as cancelled
        if (timer != null) {
            timer.isCancelled = true;
        }
    }

    /**
     * Cancel all timers.
     */
    public void cancelAll() {
        queue.clear();
        timers.clear();

        // Cancel all pending invocations
        for (final Invocation invocation : invocations) {
            invocation.cancel();
        }

        invocations.clear();
    }

    /**
     * Try to schedule an engine invocation at the specified [target].
     *
     * @param now The current virtual timestamp.
     * @param target The virtual timestamp at which the engine invocation should happen.
     * @param scheduled The queue of scheduled invocations.
     */
    private void trySchedule(long now, ArrayDeque<Invocation> scheduled, long target) {
        final Invocation head = scheduled.peek();

        // Only schedule a new scheduler invocation in case the target is earlier than all other pending
        // scheduler invocations
        if (head == null || target < head.timestamp) {
            final DispatcherHandle handle = dispatcher.scheduleCancellable(target - now, this::doRunTimers);
            scheduled.addFirst(new Invocation(target, handle));
        }
    }

    /**
     * This method is invoked when the earliest timer expires.
     */
    private void doRunTimers() {
        final ArrayDeque<Invocation> invocations = this.invocations;
        final Invocation invocation = invocations.remove();

        final PriorityQueue<Timer<T>> queue = this.queue;
        final HashMap<T, Timer<T>> timers = this.timers;
        long now = invocation.timestamp;

        while (!queue.isEmpty()) {
            final Timer<T> timer = queue.peek();

            long timestamp = timer.timestamp;
            boolean isCancelled = timer.isCancelled;

            assert timestamp >= now : "Found task in the past";

            if (timestamp > now && !isCancelled) {
                // Schedule a task for the next event to occur.
                trySchedule(now, invocations, timestamp);
                break;
            }

            queue.poll();

            if (!isCancelled) {
                timers.remove(timer.key);
                timer.run();
            }
        }
    }

    /**
     * A task that is scheduled to run in the future.
     */
    private static class Timer<T> implements Comparable<Timer<T>> {
        final T key;
        final long timestamp;
        Runnable block;

        /**
         * A flag to indicate that the task has been cancelled.
         */
        boolean isCancelled;

        /**
         * Construct a {@link Timer} instance.
         */
        public Timer(T key, long timestamp, Runnable block) {
            this.key = key;
            this.timestamp = timestamp;
            this.block = block;
        }

        /**
         * Run the task.
         */
        void run() {
            block.run();
        }

        @Override
        public int compareTo(@NotNull Timer<T> other) {
            return Long.compare(timestamp, other.timestamp);
        }
    }

    /**
     * A future engine invocation.
     * <p>
     * This class is used to keep track of the future engine invocations created using the {@link Dispatcher} instance.
     * In case the invocation is not needed anymore, it can be cancelled via [cancel].
     */
    private record Invocation(long timestamp, DispatcherHandle handle) {
        /**
         * Cancel the engine invocation.
         */
        void cancel() {
            handle.cancel();
        }
    }
}
