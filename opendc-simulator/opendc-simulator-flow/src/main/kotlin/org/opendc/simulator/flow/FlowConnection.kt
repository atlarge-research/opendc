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

package org.opendc.simulator.flow

/**
 * An active connection between a [FlowSource] and [FlowConsumer].
 */
public interface FlowConnection : AutoCloseable {
    /**
     * The capacity of the connection.
     */
    public val capacity: Double

    /**
     * The flow rate over the connection.
     */
    public val rate: Double

    /**
     * The flow demand of the source.
     */
    public val demand: Double

    /**
     * A flag to control whether [FlowSource.onConverge] should be invoked for this source.
     */
    public var shouldSourceConverge: Boolean

    /**
     * Pull the source.
     */
    public fun pull()

    /**
     * Pull the source.
     *
     * @param now The timestamp at which the connection is pulled.
     */
    public fun pull(now: Long)

    /**
     * Push the given flow [rate] over this connection.
     *
     * @param rate The rate of the flow to push.
     */
    public fun push(rate: Double)

    /**
     * Disconnect the consumer from its source.
     */
    public override fun close()
}
