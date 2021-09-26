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
 * A SimResourceCommand communicates to a resource how it is consumed by a [SimResourceConsumer].
 */
public sealed class SimResourceCommand {
    /**
     * A request to the resource to perform work for the specified [duration].
     *
     * @param limit The maximum amount of work to be processed per second.
     * @param duration The duration of the resource consumption in milliseconds.
     */
    public data class Consume(val limit: Double, val duration: Long = Long.MAX_VALUE) : SimResourceCommand() {
        init {
            require(limit >= 0.0) { "Negative limit is not allowed" }
            require(duration >= 0) { "Duration must be positive" }
        }
    }

    /**
     * An indication to the resource that the consumer has finished.
     */
    public object Exit : SimResourceCommand()
}
