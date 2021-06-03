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

/**
 * A controllable [SimResourceContext].
 *
 * This interface is used by resource providers to control the resource context.
 */
public interface SimResourceControllableContext : SimResourceContext, AutoCloseable {
    /**
     * The state of the resource context.
     */
    public val state: SimResourceState

    /**
     * The capacity of the resource.
     */
    public override var capacity: Double

    /**
     * Start the resource context.
     */
    public fun start()

    /**
     * Stop the resource context.
     */
    public override fun close()

    /**
     * Invalidate the resource context's state.
     *
     * By invalidating the resource context's current state, the state is re-computed and the current progress is
     * materialized during the next interpreter cycle. As a result, this call run asynchronously. See [flush] for the
     * synchronous variant.
     */
    public fun invalidate()

    /**
     * Synchronously flush the progress of the resource context and materialize its current progress.
     */
    public fun flush()
}
