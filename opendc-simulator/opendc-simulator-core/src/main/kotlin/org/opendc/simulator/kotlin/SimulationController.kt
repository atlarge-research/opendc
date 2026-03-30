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

package org.opendc.simulator.kotlin

import kotlinx.coroutines.CoroutineDispatcher
import org.opendc.simulator.SimulationDispatcher
import java.time.InstantSource

/**
 * Interface to control the virtual clock of a [CoroutineDispatcher].
 */
public interface SimulationController {
    /**
     * The current virtual clock as it is known to this Dispatcher.
     */
    public val timeSource: InstantSource

    /**
     * The current virtual timestamp of the dispatcher (in milliseconds since epoch).
     */
    public val currentTime: Long
        get() = timeSource.millis()

    /**
     * Return the [SimulationDispatcher] driving the simulation.
     */
    public val dispatcher: SimulationDispatcher

    /**
     * Immediately execute all pending tasks and advance the virtual clock-time to the last delay.
     *
     * If new tasks are scheduled due to advancing virtual time, they will be executed before `advanceUntilIdle`
     * returns.
     */
    public fun advanceUntilIdle()

    /**
     * Move the virtual clock of this dispatcher forward by the specified amount, running the scheduled tasks in the
     * meantime.
     *
     * @param delayMs The amount of time to move the virtual clock forward (in milliseconds).
     * @throws IllegalStateException if passed a negative <code>delay</code>.
     */
    public fun advanceBy(delayMs: Long)

    /**
     * Execute the tasks that are scheduled to execute at this moment of virtual time.
     */
    public fun runCurrent()
}
