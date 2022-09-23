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

package org.opendc.simulator.flow.mux

import org.opendc.simulator.flow.FlowConsumer
import org.opendc.simulator.flow.FlowCounters
import org.opendc.simulator.flow.FlowSource

/**
 * A [FlowMultiplexer] enables multiplexing multiple [FlowSource]s over possibly multiple [FlowConsumer]s.
 */
public interface FlowMultiplexer {
    /**
     * The maximum number of inputs supported by the multiplexer.
     */
    public val maxInputs: Int

    /**
     * The maximum number of outputs supported by the multiplexer.
     */
    public val maxOutputs: Int

    /**
     * The inputs of the multiplexer that can be used to consume sources.
     */
    public val inputs: Set<FlowConsumer>

    /**
     * The outputs of the multiplexer over which the flows will be distributed.
     */
    public val outputs: Set<FlowSource>

    /**
     * The actual processing rate of the multiplexer.
     */
    public val rate: Double

    /**
     * The demanded processing rate of the input.
     */
    public val demand: Double

    /**
     * The capacity of the outputs.
     */
    public val capacity: Double

    /**
     * The flow counters to track the flow metrics of all multiplexer inputs.
     */
    public val counters: FlowCounters

    /**
     * Create a new input on this multiplexer with a coupled capacity.
     */
    public fun newInput(): FlowConsumer

    /**
     * Create a new input on this multiplexer with the specified [capacity].
     *
     * @param capacity The capacity of the input.
     */
    public fun newInput(capacity: Double): FlowConsumer

    /**
     * Remove [input] from this multiplexer.
     */
    public fun removeInput(input: FlowConsumer)

    /**
     * Create a new output on this multiplexer.
     */
    public fun newOutput(): FlowSource

    /**
     * Remove [output] from this multiplexer.
     */
    public fun removeOutput(output: FlowSource)

    /**
     * Clear all inputs and outputs from the multiplexer.
     */
    public fun clear()

    /**
     * Clear the inputs of the multiplexer.
     */
    public fun clearInputs()

    /**
     * Clear the outputs of the multiplexer.
     */
    public fun clearOutputs()

    /**
     * Flush the counters of the multiplexer.
     */
    public fun flushCounters()

    /**
     * Flush the counters of the specified [input].
     */
    public fun flushCounters(input: FlowConsumer)
}
