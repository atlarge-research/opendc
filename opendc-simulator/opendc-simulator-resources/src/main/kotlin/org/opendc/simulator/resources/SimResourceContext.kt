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
 * The execution context in which a [SimResourceConsumer] runs. It facilitates the communication and control between a
 * resource and a resource consumer.
 */
public interface SimResourceContext {
    /**
     * The virtual clock tracking simulation time.
     */
    public val clock: Clock

    /**
     * The resource capacity available at this instant.
     */
    public val capacity: Double

    /**
     * The resource processing speed at this instant.
     */
    public val speed: Double

    /**
     * The amount of work still remaining at this instant.
     */
    public val remainingWork: Double

    /**
     * Ask the resource provider to interrupt its resource.
     */
    public fun interrupt()
}
