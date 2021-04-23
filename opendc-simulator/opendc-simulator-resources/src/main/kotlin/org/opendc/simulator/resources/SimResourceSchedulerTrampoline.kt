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

package org.opendc.simulator.resources

import org.opendc.utils.TimerScheduler
import java.time.Clock
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext

/**
 * A [SimResourceScheduler] queues all interrupts that occur during execution to be executed after.
 *
 * @param clock The virtual simulation clock.
 */
public class SimResourceSchedulerTrampoline(context: CoroutineContext, override val clock: Clock) : SimResourceScheduler {
    /**
     * The [TimerScheduler] to actually schedule the interrupts.
     */
    private val timers = TimerScheduler<Any>(context, clock)

    /**
     * A flag to indicate that an interrupt is currently running already.
     */
    private var isRunning: Boolean = false

    /**
     * The queue of resources to be flushed.
     */
    private val queue = ArrayDeque<Pair<SimResourceFlushable, Boolean>>()

    override fun schedule(flushable: SimResourceFlushable, isIntermediate: Boolean) {
        queue.add(flushable to isIntermediate)

        if (isRunning) {
            return
        }

        flush()
    }

    override fun schedule(flushable: SimResourceFlushable, timestamp: Long, isIntermediate: Boolean) {
        timers.startSingleTimerTo(flushable, timestamp) {
            schedule(flushable, isIntermediate)
        }
    }

    override fun batch(block: () -> Unit) {
        val wasAlreadyRunning = isRunning
        try {
            isRunning = true
            block()
        } finally {
            if (!wasAlreadyRunning) {
                isRunning = false
            }
        }
    }

    /**
     * Flush the scheduled queue.
     */
    private fun flush() {
        val visited = mutableSetOf<SimResourceFlushable>()
        try {
            isRunning = true
            while (queue.isNotEmpty()) {
                val (flushable, isIntermediate) = queue.poll()
                flushable.flush(isIntermediate)
                visited.add(flushable)
            }
        } finally {
            isRunning = false
        }
    }
}
