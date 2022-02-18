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

package org.opendc.common.util

import kotlinx.coroutines.*
import java.time.Clock
import java.util.*
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * A TimerScheduler facilitates scheduled execution of future tasks.
 *
 * @param context The [CoroutineContext] to run the tasks with.
 * @param clock The clock to keep track of the time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class TimerScheduler<T>(private val context: CoroutineContext, private val clock: Clock) {
    /**
     * The [Delay] instance that provides scheduled execution of [Runnable]s.
     */
    @OptIn(InternalCoroutinesApi::class)
    private val delay =
        requireNotNull(context[ContinuationInterceptor] as? Delay) { "Invalid CoroutineDispatcher: no delay implementation" }

    /**
     * The stack of the invocations to occur in the future.
     */
    private val invocations = ArrayDeque<Invocation>()

    /**
     * A priority queue containing the tasks to be scheduled in the future.
     */
    private val queue = PriorityQueue<Timer<T>>()

    /**
     * A map that keeps track of the timers.
     */
    private val timers = mutableMapOf<T, Timer<T>>()

    /**
     * Start a timer that will invoke the specified [block] after [delay].
     *
     * Each timer has a key and if a new timer with same key is started the previous is cancelled.
     *
     * @param key The key of the timer to start.
     * @param delay The delay before invoking the block.
     * @param block The block to invoke.
     */
    public fun startSingleTimer(key: T, delay: Long, block: () -> Unit) {
        startSingleTimerTo(key, clock.millis() + delay, block)
    }

    /**
     * Start a timer that will invoke the specified [block] at [timestamp].
     *
     * Each timer has a key and if a new timer with same key is started the previous is cancelled.
     *
     * @param key The key of the timer to start.
     * @param timestamp The timestamp at which to invoke the block.
     * @param block The block to invoke.
     */
    public fun startSingleTimerTo(key: T, timestamp: Long, block: () -> Unit) {
        val now = clock.millis()
        val queue = queue
        val invocations = invocations

        require(timestamp >= now) { "Timestamp must be in the future" }

        timers.compute(key) { _, old ->
            if (old != null && old.timestamp == timestamp) {
                // Fast-path: timer for the same timestamp already exists
                old.block = block
                old
            } else {
                // Slow-path: cancel old timer and replace it with new timer
                val timer = Timer(key, timestamp, block)

                old?.isCancelled = true
                queue.add(timer)
                trySchedule(now, invocations, timestamp)

                timer
            }
        }
    }

    /**
     * Check if a timer with a given key is active.
     *
     * @param key The key to check if active.
     * @return `true` if the timer with the specified [key] is active, `false` otherwise.
     */
    public fun isTimerActive(key: T): Boolean = key in timers

    /**
     * Cancel a timer with a given key.
     *
     * If canceling a timer that was already canceled, or key never was used to start
     * a timer this operation will do nothing.
     *
     * @param key The key of the timer to cancel.
     */
    public fun cancel(key: T) {
        val timer = timers.remove(key)

        // Mark the timer as cancelled
        timer?.isCancelled = true
    }

    /**
     * Cancel all timers.
     */
    public fun cancelAll() {
        queue.clear()
        timers.clear()

        // Cancel all pending invocations
        for (invocation in invocations) {
            invocation.cancel()
        }
        invocations.clear()
    }

    /**
     * Try to schedule an engine invocation at the specified [target].
     *
     * @param now The current virtual timestamp.
     * @param target The virtual timestamp at which the engine invocation should happen.
     * @param scheduled The queue of scheduled invocations.
     */
    private fun trySchedule(now: Long, scheduled: ArrayDeque<Invocation>, target: Long) {
        val head = scheduled.peek()

        // Only schedule a new scheduler invocation in case the target is earlier than all other pending
        // scheduler invocations
        if (head == null || target < head.timestamp) {
            @OptIn(InternalCoroutinesApi::class)
            val handle = delay.invokeOnTimeout(target - now, ::doRunTimers, context)
            scheduled.addFirst(Invocation(target, handle))
        }
    }

    /**
     * This method is invoked when the earliest timer expires.
     */
    private fun doRunTimers() {
        val invocations = invocations
        val invocation = checkNotNull(invocations.poll()) // Clear invocation from future invocation queue
        val now = invocation.timestamp

        while (queue.isNotEmpty()) {
            val timer = queue.peek()

            val timestamp = timer.timestamp
            val isCancelled = timer.isCancelled

            assert(timestamp >= now) { "Found task in the past" }

            if (timestamp > now && !isCancelled) {
                // Schedule a task for the next event to occur.
                trySchedule(now, invocations, timestamp)
                break
            }

            queue.poll()

            if (!isCancelled) {
                timers.remove(timer.key)
                timer()
            }
        }
    }

    /**
     * A task that is scheduled to run in the future.
     */
    private class Timer<T>(val key: T, val timestamp: Long, var block: () -> Unit) : Comparable<Timer<T>> {
        /**
         * A flag to indicate that the task has been cancelled.
         */
        @JvmField
        var isCancelled: Boolean = false

        /**
         * Run the task.
         */
        operator fun invoke(): Unit = block()

        override fun compareTo(other: Timer<T>): Int = timestamp.compareTo(other.timestamp)
    }

    /**
     * A future engine invocation.
     *
     * This class is used to keep track of the future engine invocations created using the [Delay] instance. In case
     * the invocation is not needed anymore, it can be cancelled via [cancel].
     */
    private class Invocation(
        @JvmField val timestamp: Long,
        @JvmField val handle: DisposableHandle
    ) {
        /**
         * Cancel the engine invocation.
         */
        fun cancel() = handle.dispose()
    }
}
