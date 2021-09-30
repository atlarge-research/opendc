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
 * A source of flow that is consumed by a [FlowConsumer].
 *
 * Implementations of this interface should be considered stateful and must be assumed not to be re-usable
 * (concurrently) for multiple [FlowConsumer]s, unless explicitly said otherwise.
 */
public interface FlowSource {
    /**
     * This method is invoked when the source is started.
     *
     * @param conn The connection between the source and consumer.
     * @param now The virtual timestamp in milliseconds at which the provider finished.
     */
    public fun onStart(conn: FlowConnection, now: Long) {}

    /**
     * This method is invoked when the source is finished.
     *
     * @param conn The connection between the source and consumer.
     * @param now The virtual timestamp in milliseconds at which the source finished.
     * @param delta The virtual duration between this call and the last call to [onPull] in milliseconds.
     */
    public fun onStop(conn: FlowConnection, now: Long, delta: Long) {}

    /**
     * This method is invoked when the source is pulled.
     *
     * @param conn The connection between the source and consumer.
     * @param now The virtual timestamp in milliseconds at which the pull is occurring.
     * @param delta The virtual duration between this call and the last call to [onPull] in milliseconds.
     * @return The duration after which the resource consumer should be pulled again.
     */
    public fun onPull(conn: FlowConnection, now: Long, delta: Long): Long

    /**
     * This method is invoked when the flow graph has converged into a steady-state system.
     *
     * Make sure to enable [FlowConnection.shouldSourceConverge] if you need this callback. By default, this method
     * will not be invoked.
     *
     * @param conn The connection between the source and consumer.
     * @param now The virtual timestamp in milliseconds at which the system converged.
     * @param delta The virtual duration between this call and the last call to [onConverge] in milliseconds.
     */
    public fun onConverge(conn: FlowConnection, now: Long, delta: Long) {}
}
