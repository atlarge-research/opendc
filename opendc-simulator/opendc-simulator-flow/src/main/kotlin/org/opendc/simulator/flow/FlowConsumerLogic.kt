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
 * A collection of callbacks associated with a [FlowConsumer].
 */
public interface FlowConsumerLogic {
    /**
     * This method is invoked when a [FlowSource] changes the rate of flow to this consumer.
     *
     * @param ctx The context in which the provider runs.
     * @param now The virtual timestamp in milliseconds at which the update is occurring.
     * @param delta The virtual duration between this call and the last call to [onPush] in milliseconds.
     * @param rate The requested processing rate of the source.
     */
    public fun onPush(ctx: FlowConsumerContext, now: Long, delta: Long, rate: Double) {}

    /**
     * This method is invoked when the flow graph has converged into a steady-state system.
     *
     * @param ctx The context in which the provider runs.
     * @param now The virtual timestamp in milliseconds at which the system converged.
     * @param delta The virtual duration between this call and the last call to [onConverge] in milliseconds.
     */
    public fun onConverge(ctx: FlowConsumerContext, now: Long, delta: Long) {}

    /**
     * This method is invoked when the [FlowSource] is completed.
     *
     * @param ctx The context in which the provider runs.
     * @param now The virtual timestamp in milliseconds at which the provider finished.
     * @param delta The virtual duration between this call and the last call to [onPush] in milliseconds.
     */
    public fun onFinish(ctx: FlowConsumerContext, now: Long, delta: Long) {}
}
