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

package org.opendc.simulator.network;

import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.Inlet;
import org.opendc.simulator.flow2.Outlet;
import org.opendc.simulator.flow2.sink.SimpleFlowSink;
import org.opendc.simulator.flow2.source.EmptyFlowSource;

/**
 * A network sink which discards all received traffic and does not generate any traffic itself.
 */
public final class SimNetworkSink extends SimNetworkPort {
    private final EmptyFlowSource source;
    private final SimpleFlowSink sink;

    /**
     * Construct a {@link SimNetworkSink} instance.
     *
     * @param graph The {@link FlowGraph} to which the sink belongs.
     * @param capacity The capacity of the sink in terms of processed data.
     */
    public SimNetworkSink(FlowGraph graph, float capacity) {
        this.source = new EmptyFlowSource(graph);
        this.sink = new SimpleFlowSink(graph, capacity);
    }

    /**
     * Return the capacity of the sink.
     */
    public float getCapacity() {
        return sink.getCapacity();
    }

    @Override
    protected Outlet getOutlet() {
        return source.getOutput();
    }

    @Override
    protected Inlet getInlet() {
        return sink.getInput();
    }

    @Override
    public String toString() {
        return "SimNetworkSink[capacity=" + getCapacity() + "]";
    }
}
