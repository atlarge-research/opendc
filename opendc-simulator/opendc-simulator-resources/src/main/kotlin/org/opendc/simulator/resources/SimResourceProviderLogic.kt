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
 * The logic of a resource provider.
 */
public interface SimResourceProviderLogic {
    /**
     * This method is invoked when the consumer ask to consume the resource for the specified [duration].
     *
     * @param ctx The context in which the provider runs.
     * @param now The virtual timestamp in milliseconds at which the update is occurring.
     * @param limit The limit on the work rate of the resource consumer.
     * @param duration The duration of the consumption in milliseconds.
     * @return The deadline of the resource consumption.
     */
    public fun onConsume(ctx: SimResourceControllableContext, now: Long, limit: Double, duration: Long): Long {
        return if (duration == Long.MAX_VALUE) {
            return Long.MAX_VALUE
        } else {
            now + duration
        }
    }

    /**
     * This method is invoked when the progress of the resource consumer is materialized.
     *
     * @param ctx The context in which the provider runs.
     * @param limit The limit on the work rate of the resource consumer.
     * @param willOvercommit A flag to indicate that the remaining work is overcommitted.
     */
    public fun onUpdate(ctx: SimResourceControllableContext, delta: Long, limit: Double, willOvercommit: Boolean) {}

    /**
     * This method is invoked when the resource consumer has finished.
     */
    public fun onFinish(ctx: SimResourceControllableContext) {}
}
