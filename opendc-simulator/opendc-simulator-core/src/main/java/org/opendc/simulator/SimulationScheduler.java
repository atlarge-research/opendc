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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Executor;

/**
 * A scheduler is used by simulations to manage execution of (future) tasks, providing a controllable (virtual) clock to
 * skip over delays.
 *
 * <p>
 * The scheduler can be queried to advance the time (via {@link #advanceBy}), run all the scheduled tasks advancing the
 * virtual time as needed (via {@link #advanceUntilIdle}), or run the tasks that are scheduled to run as soon as
 * possible but have not yet been dispatched (via {@link #runCurrent}). These methods execute the pending tasks using
 * a single thread.
 *
 * <p>
 * This class is not thread-safe and must not be used concurrently by multiple threads.
 */
public final class SimulationScheduler implements Executor {
    /**
     * The {@link TaskQueue} containing the pending tasks.
     */
    private final TaskQueue queue = new TaskQueue();

    /**
     * The current time of the scheduler in milliseconds since epoch.
     */
    private long currentTime;

    /**
     * A counter to establish total order on the events that happen at the same virtual time.
     */
    private int count = 0;

    /**
     * The {@link Clock} instance linked to this scheduler.
     */
    private final SimulationClock clock = new SimulationClock(this, ZoneId.systemDefault());

    /**
     * Construct a {@link SimulationScheduler} instance with the specified initial time.
     *
     * @param initialTimeMs The initial virtual time of the scheduler in milliseconds since epoch.
     */
    public SimulationScheduler(long initialTimeMs) {
       this.currentTime = initialTimeMs;
    }

    /**
     * Construct a {@link SimulationScheduler} instance with the initial time set to UNIX Epoch 0.
     */
    public SimulationScheduler() {
        this(0);
    }

    /**
     * Return the virtual clock associated with this dispatcher.
     *
     * @return A {@link Clock} tracking the virtual time of the dispatcher.
     */
    public Clock getClock() {
        return clock;
    }

    /**
     * Return the current virtual timestamp of the dispatcher (in milliseconds since epoch).
     *
     * @return A long value representing the virtual timestamp of the dispatcher in milliseconds since epoch.
     */
    public long getCurrentTime() {
        return currentTime;
    }

    /**
     * Schedule a <code>task</code> that executes after the specified <code>delayMs</code>.
     *
     * @param delayMs The time from now until the execution of the task (in milliseconds).
     * @param task The task to execute after the delay.
     * @return The identifier of the task that can be used together with the timestamp of the task to cancel it.
     */
    public int schedule(long delayMs, Runnable task) {
        if (delayMs < 0) {
            throw new IllegalArgumentException("Attempted scheduling an event earlier in time (delay " + delayMs + " ms)");
        }

        long target = currentTime + delayMs;
        if (target < 0) {
            target = Long.MAX_VALUE;
        }

        int id = count++;
        queue.add(target, id, task);
        return id;
    }

    /**
     * Cancel a pending task.
     *
     * @param deadline The deadline of the task.
     * @param id The identifier of the task (returned by {@link #schedule(long, Runnable)}).
     * @return A boolean indicating whether a task was actually cancelled.
     */
    public boolean cancel(long deadline, int id) {
        return queue.remove(deadline, id);
    }

    /**
     * Run the enqueued tasks in the specified order, advancing the virtual time as needed until there are no more
     * tasks in the queue of this scheduler.
     */
    public void advanceUntilIdle() {
        final TaskQueue queue = this.queue;

        while (true) {
            long deadline = queue.peekDeadline();
            Runnable task = queue.poll();

            if (task == null) {
                break;
            }

            currentTime = deadline;
            task.run();
        }
    }

    /**
     * Move the virtual clock of this dispatcher forward by the specified amount, running the scheduled tasks in the
     * meantime.
     *
     * @param delayMs The amount of time to move the virtual clock forward (in milliseconds).
     * @throws IllegalStateException if passed a negative <code>delay</code>.
     */
    public void advanceBy(long delayMs) {
        if (delayMs < 0) {
            throw new IllegalArgumentException("Can not advance time by a negative delay: " + delayMs + " ms");
        }

        long target = currentTime + delayMs;
        if (target < 0) {
            target = Long.MAX_VALUE;
        }

        final TaskQueue queue = this.queue;
        long deadline;

        while ((deadline = queue.peekDeadline()) < target) {
            Runnable task = queue.poll(); // Cannot be null since while condition is always false on an empty queue

            task.run();
            currentTime = deadline;
        }

        currentTime = target;
    }

    /**
     * Execute the tasks that are scheduled to execute at this moment of virtual time.
     */
    public void runCurrent() {
        final TaskQueue queue = this.queue;
        long currentTime = this.currentTime;

        while (queue.peekDeadline() == currentTime) {
            Runnable task = queue.poll();

            if (task == null) {
                break;
            }

            task.run();
        }
    }

    /**
     * Schedule the specified command to run at this moment of virtual time.
     *
     * @param command The command to execute.
     */
    @Override
    public void execute(Runnable command) {
        schedule(0, command);
    }

    /**
     * A {@link Clock} implementation for a {@link SimulationScheduler}.
     */
    private static class SimulationClock extends Clock {
        private final SimulationScheduler scheduler;
        private final ZoneId zone;

        SimulationClock(SimulationScheduler scheduler, ZoneId zone) {
            this.scheduler = scheduler;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zoneId) {
            return new SimulationClock(scheduler, zone);
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(scheduler.currentTime);
        }

        @Override
        public long millis() {
            return scheduler.currentTime;
        }

        @Override
        public String toString() {
            return "SimulationClock[time=" + millis() + "ms]";
        }
    }
}
