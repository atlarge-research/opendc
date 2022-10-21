/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.simulator.flow2.mux;

import org.opendc.simulator.flow2.FlowStageLogic;
import org.opendc.simulator.flow2.Inlet;
import org.opendc.simulator.flow2.Outlet;

/**
 * A {@link FlowStageLogic} that multiplexes multiple inputs over (possibly) multiple outputs.
 */
public interface FlowMultiplexer {
    /**
     * Return maximum number of inputs supported by the multiplexer.
     */
    int getMaxInputs();

    /**
     * Return maximum number of outputs supported by the multiplexer.
     */
    int getMaxOutputs();

    /**
     * Return the number of active inputs on this multiplexer.
     */
    int getInputCount();

    /**
     * Allocate a new input on this multiplexer with the specified capacity..
     *
     * @return The identifier of the input for this stage.
     */
    Inlet newInput();

    /**
     * Release the input at the specified slot.
     *
     * @param inlet The inlet to release.
     */
    void releaseInput(Inlet inlet);

    /**
     * Return the number of active outputs on this multiplexer.
     */
    int getOutputCount();

    /**
     * Allocate a new output on this multiplexer.
     *
     * @return The outlet for this stage.
     */
    Outlet newOutput();

    /**
     * Release the output at the specified slot.
     *
     * @param outlet The outlet to release.
     */
    void releaseOutput(Outlet outlet);

    /**
     * Return the total input capacity of the {@link FlowMultiplexer}.
     */
    float getCapacity();

    /**
     * Return the total input demand for the {@link FlowMultiplexer}.
     */
    float getDemand();

    /**
     * Return the total input rate for the {@link FlowMultiplexer}.
     */
    float getRate();
}
