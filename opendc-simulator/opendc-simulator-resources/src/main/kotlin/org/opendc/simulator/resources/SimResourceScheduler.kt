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

import java.time.Clock

/**
 * A resource scheduler is responsible for scheduling the communication and synchronization between multiple resource
 * providers and consumers.
 *
 * By centralizing the scheduling logic, updates of resources within a single system can be scheduled and tracked more
 * efficiently, reducing the overall work needed per update.
 */
public interface SimResourceScheduler {
    /**
     * The [Clock] associated with this scheduler.
     */
    public val clock: Clock

    /**
     * Schedule a direct interrupt for the resource context represented by [flushable].
     *
     * @param flushable The resource context that needs to be flushed.
     * @param isIntermediate A flag to indicate that the intermediate progress of the resource consumer should be
     * flushed, but without interrupting the resource consumer to submit a new command. If false, the resource consumer
     * will be asked to deliver a new command and is essentially interrupted.
     */
    public fun schedule(flushable: SimResourceFlushable, isIntermediate: Boolean = false)

    /**
     * Schedule an interrupt in the future for the resource context represented by [flushable].
     *
     * This method will override earlier calls to this method for the same [flushable].
     *
     * @param flushable The resource context that needs to be flushed.
     * @param timestamp The timestamp when the interrupt should happen.
     * @param isIntermediate A flag to indicate that the intermediate progress of the resource consumer should be
     * flushed, but without interrupting the resource consumer to submit a new command. If false, the resource consumer
     * will be asked to deliver a new command and is essentially interrupted.
     */
    public fun schedule(flushable: SimResourceFlushable, timestamp: Long, isIntermediate: Boolean = false)

    /**
     * Batch the execution of several interrupts into a single call.
     *
     * This method is useful if you want to propagate the start of multiple resources (e.g., CPUs) in a single update.
     */
    public fun batch(block: () -> Unit)
}
