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

package org.opendc.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.selects.select
import java.time.Clock
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

/**
 * A TimerScheduler facilitates scheduled execution of future tasks.
 *
 * @param context The [CoroutineContext] to run the tasks with.
 * @param clock The clock to keep track of the time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class TimerScheduler<T>(context: CoroutineContext, private val clock: Clock) : AutoCloseable {
    /**
     * The scope in which the scheduler runs.
     */
    private val scope = CoroutineScope(context + Job())

    /**
     * A priority queue containing the tasks to be scheduled in the future.
     */
    private val queue = PriorityQueue<Timer>()

    /**
     * A map that keeps track of the timers.
     */
    private val timers = mutableMapOf<T, Timer>()

    /**
     * The channel to communicate with the scheduling job.
     */
    private val channel = Channel<Long?>(Channel.CONFLATED)

    /**
     * The scheduling job.
     */
    private val job = scope.launch {
        val timers = timers
        val queue = queue
        val clock = clock
        val job = requireNotNull(coroutineContext[Job])
        val exceptionHandler = coroutineContext[CoroutineExceptionHandler]
        var next: Long? = channel.receive()

        while (true) {
            next = select {
                channel.onReceive { it }

                val delay = next?.let { max(0L, it - clock.millis()) } ?: return@select

                onTimeout(delay) {
                    while (queue.isNotEmpty() && job.isActive) {
                        val timer = queue.peek()
                        val timestamp = clock.millis()

                        assert(timer.timestamp >= timestamp) { "Found task in the past" }

                        if (timer.timestamp > timestamp && !timer.isCancelled) {
                            // Schedule a task for the next event to occur.
                            return@onTimeout timer.timestamp
                        }

                        queue.poll()

                        if (!timer.isCancelled) {
                            timers.remove(timer.key)
                            try {
                                timer()
                            } catch (e: Throwable) {
                                exceptionHandler?.handleException(coroutineContext, e)
                            }
                        }
                    }

                    null
                }
            }
        }
    }

    /**
     * Stop the scheduler.
     */
    override fun close() {
        cancelAll()
        scope.cancel()
    }

    /**
     * Cancel a timer with a given key.
     *
     * If canceling a timer that was already canceled, or key never was used to start
     * a timer this operation will do nothing.
     *
     * @param key The key of the timer to cancel.
     */
    public fun cancel(key: T) {
        if (!job.isActive) {
            return
        }

        val timer = timers.remove(key)

        // Mark the timer as cancelled
        timer?.isCancelled = true

        // Optimization: check whether we are the head of the queue
        val queue = queue
        val peek = queue.peek()
        if (peek == timer) {
            queue.poll()

            if (queue.isNotEmpty()) {
                channel.sendBlocking(peek.timestamp)
            } else {
                channel.sendBlocking(null)
            }
        }
    }

    /**
     * Cancel all timers.
     */
    public fun cancelAll() {
        queue.clear()
        timers.clear()
    }

    /**
     * Check if a timer with a given key is active.
     *
     * @param key The key to check if active.
     * @return `true` if the timer with the specified [key] is active, `false` otherwise.
     */
    public fun isTimerActive(key: T): Boolean = key in timers

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

        require(timestamp >= now) { "Timestamp must be in the future" }
        check(job.isActive) { "Timer is stopped" }

        timers.compute(key) { _, old ->
            if (old?.timestamp == timestamp) {
                // Fast-path: timer for the same timestamp already exists
                old
            } else {
                // Slow-path: cancel old timer and replace it with new timer
                val timer = Timer(key, timestamp, block)

                old?.isCancelled = true
                queue.add(timer)

                // Check if we need to push the interruption forward
                if (queue.peek() == timer) {
                    channel.sendBlocking(timer.timestamp)
                }

                timer
            }
        }
    }

    /**
     * A task that is scheduled to run in the future.
     */
    private inner class Timer(val key: T, val timestamp: Long, val block: () -> Unit) : Comparable<Timer> {
        /**
         * A flag to indicate that the task has been cancelled.
         */
        var isCancelled: Boolean = false

        /**
         * Run the task.
         */
        operator fun invoke(): Unit = block()

        override fun compareTo(other: Timer): Int = timestamp.compareTo(other.timestamp)
    }
}
