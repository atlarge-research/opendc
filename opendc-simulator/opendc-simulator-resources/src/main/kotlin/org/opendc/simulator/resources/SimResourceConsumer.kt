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
 * A [SimResourceConsumer] characterizes how a resource is consumed.
 *
 * Implementors of this interface should be considered stateful and must be assumed not to be re-usable (concurrently)
 * for multiple resource providers, unless explicitly said otherwise.
 */
public interface SimResourceConsumer {
    /**
     * This method is invoked when the resource provider is pulling this resource consumer.
     *
     * @param ctx The execution context in which the consumer runs.
     * @param now The virtual timestamp in milliseconds at which the update is occurring.
     * @param delta The virtual duration between this call and the last call in milliseconds.
     * @return The duration after which the resource consumer should be pulled again.
     */
    public fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long

    /**
     * This method is invoked when an event has occurred.
     *
     * @param ctx The execution context in which the consumer runs.
     * @param event The event that has occurred.
     */
    public fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {}

    /**
     * This method is invoked when a resource consumer throws an exception.
     *
     * @param ctx The execution context in which the consumer runs.
     * @param cause The cause of the failure.
     */
    public fun onFailure(ctx: SimResourceContext, cause: Throwable) {}
}
