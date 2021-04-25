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

import kotlinx.coroutines.CoroutineDispatcher
import java.time.Clock

/**
 * Control the virtual clock of a [CoroutineDispatcher].
 */
public interface SimulationController {
    /**
     * The current virtual clock as it is known to this Dispatcher.
     */
    public val clock: Clock

    /**
     * Immediately execute all pending tasks and advance the virtual clock-time to the last delay.
     *
     * If new tasks are scheduled due to advancing virtual time, they will be executed before `advanceUntilIdle`
     * returns.
     *
     * @return the amount of delay-time that this Dispatcher's clock has been forwarded in milliseconds.
     */
    public fun advanceUntilIdle(): Long
}
