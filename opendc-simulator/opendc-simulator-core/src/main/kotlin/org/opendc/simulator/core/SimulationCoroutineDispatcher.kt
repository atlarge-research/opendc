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

package org.opendc.simulator.core

import kotlinx.coroutines.*
import java.lang.Runnable
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineDispatcher] that performs both immediate execution of coroutines on the main thread and uses a virtual
 * clock for time management.
 */
@OptIn(InternalCoroutinesApi::class)
public class SimulationCoroutineDispatcher : CoroutineDispatcher(), SimulationController, Delay {
    /**
     * Queue of ordered tasks to run.
     */
    private val queue = PriorityQueue<TimedRunnable>()

    /**
     * Global order counter.
     */
    private var _counter = 0L

    /**
     * The current virtual time of simulation
     */
    private var _clock = SimClock()

    /**
     * The virtual clock of this dispatcher.
     */
    override val clock: Clock = ClockAdapter(_clock)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }

    override fun dispatchYield(context: CoroutineContext, block: Runnable) {
        post(block)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        postDelayed(CancellableContinuationRunnable(continuation) { resumeUndispatched(Unit) }, timeMillis)
    }

    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        val node = postDelayed(block, timeMillis)
        return object : DisposableHandle {
            override fun dispose() {
                queue.remove(node)
            }
        }
    }

    override fun toString(): String {
        return "SimulationCoroutineDispatcher[time=${_clock.time}ms, queued=${queue.size}]"
    }

    private fun post(block: Runnable) =
        queue.add(TimedRunnable(block, _counter++))

    private fun postDelayed(block: Runnable, delayTime: Long) =
        TimedRunnable(block, _counter++, safePlus(_clock.time, delayTime))
            .also {
                queue.add(it)
            }

    private fun safePlus(currentTime: Long, delayTime: Long): Long {
        check(delayTime >= 0)
        val result = currentTime + delayTime
        if (result < currentTime) return Long.MAX_VALUE // clamp on overflow
        return result
    }

    override fun advanceUntilIdle(): Long {
        val queue = queue
        val clock = _clock
        val oldTime = clock.time

        while (true) {
            val current = queue.poll() ?: break

            // If the scheduled time is 0 (immediate) use current virtual time
            if (current.time != 0L) {
                clock.time = current.time
            }

            current.run()
        }

        return clock.time - oldTime
    }

    /**
     * A helper class that holds the time of the simulation.
     */
    private class SimClock(@JvmField var time: Long = 0)

    /**
     * A helper class to expose a [Clock] instance for this dispatcher.
     */
    private class ClockAdapter(private val clock: SimClock, private val zone: ZoneId = ZoneId.systemDefault()) : Clock() {
        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = ClockAdapter(clock, zone)

        override fun instant(): Instant = Instant.ofEpochMilli(millis())

        override fun millis(): Long = clock.time

        override fun toString(): String = "SimulationCoroutineDispatcher.ClockAdapter[time=${clock.time}]"
    }

    /**
     * This class exists to allow cleanup code to avoid throwing for cancelled continuations scheduled
     * in the future.
     */
    private class CancellableContinuationRunnable<T>(
        @JvmField val continuation: CancellableContinuation<T>,
        private val block: CancellableContinuation<T>.() -> Unit
    ) : Runnable {
        override fun run() = continuation.block()
    }

    /**
     * A Runnable for our event loop that represents a task to perform at a time.
     */
    private class TimedRunnable(
        @JvmField val runnable: Runnable,
        private val count: Long = 0,
        @JvmField val time: Long = 0
    ) : Comparable<TimedRunnable>, Runnable by runnable {
        override fun compareTo(other: TimedRunnable) = if (time == other.time) {
            count.compareTo(other.count)
        } else {
            time.compareTo(other.time)
        }

        override fun toString() = "TimedRunnable[time=$time, run=$runnable]"
    }
}
