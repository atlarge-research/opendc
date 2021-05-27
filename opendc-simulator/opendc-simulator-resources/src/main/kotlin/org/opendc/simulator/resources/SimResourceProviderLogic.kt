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

import kotlin.math.max

/**
 * The logic of a resource provider.
 */
public interface SimResourceProviderLogic {
    /**
     * This method is invoked when the resource will idle until the specified [deadline].
     *
     * @param ctx The context in which the provider runs.
     * @param deadline The deadline that was requested by the resource consumer.
     * @return The instant at which to resume the consumer.
     */
    public fun onIdle(ctx: SimResourceControllableContext, deadline: Long): Long

    /**
     * This method is invoked when the resource will be consumed until the specified [work] was processed or the
     * [deadline] was reached.
     *
     * @param ctx The context in which the provider runs.
     * @param work The amount of work that was requested by the resource consumer.
     * @param limit The limit on the work rate of the resource consumer.
     * @param deadline The deadline that was requested by the resource consumer.
     * @return The instant at which to resume the consumer.
     */
    public fun onConsume(ctx: SimResourceControllableContext, work: Double, limit: Double, deadline: Long): Long

    /**
     * This method is invoked when the resource consumer has finished.
     */
    public fun onFinish(ctx: SimResourceControllableContext)

    /**
     * Get the remaining work to process after a resource consumption.
     *
     * @param work The size of the resource consumption.
     * @param speed The speed of consumption.
     * @param duration The duration from the start of the consumption until now.
     * @return The amount of work remaining.
     */
    public fun getRemainingWork(ctx: SimResourceControllableContext, work: Double, speed: Double, duration: Long): Double {
        return if (duration > 0L) {
            val processed = duration / 1000.0 * speed
            max(0.0, work - processed)
        } else {
            0.0
        }
    }
}
