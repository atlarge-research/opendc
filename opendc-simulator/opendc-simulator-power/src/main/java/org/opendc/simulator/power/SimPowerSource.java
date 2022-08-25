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

package org.opendc.simulator.power;

import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.Inlet;
import org.opendc.simulator.flow2.sink.SimpleFlowSink;

/**
 * A {@link SimPowerOutlet} that represents a source of electricity with a maximum capacity.
 */
public final class SimPowerSource extends SimPowerOutlet {
    /**
     * The resource source that drives this power source.
     */
    private final SimpleFlowSink sink;

    /**
     * Construct a {@link SimPowerSource} instance.
     *
     * @param graph The underlying {@link FlowGraph} to which the power source belongs.
     * @param capacity The maximum amount of power provided by the source.
     */
    public SimPowerSource(FlowGraph graph, float capacity) {
        this.sink = new SimpleFlowSink(graph, capacity);
    }

    /**
     * Return the capacity of the power source.
     */
    public float getCapacity() {
        return sink.getCapacity();
    }

    /**
     * Return the power draw at this instant.
     */
    public float getPowerDraw() {
        return sink.getRate();
    }

    @Override
    protected Inlet getFlowInlet() {
        return sink.getInput();
    }

    @Override
    public String toString() {
        return "SimPowerSource";
    }
}
