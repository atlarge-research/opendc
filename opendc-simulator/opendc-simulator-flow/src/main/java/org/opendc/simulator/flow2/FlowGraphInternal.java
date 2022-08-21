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

package org.opendc.simulator.flow2;

/**
 * Interface implemented by {@link FlowGraph} implementations.
 */
interface FlowGraphInternal extends FlowGraph {
    /**
     * Internal method to remove the specified {@link FlowStage} from the graph.
     */
    void detach(FlowStage stage);

    /**
     * Helper method to connect an outlet to an inlet.
     */
    static void connect(FlowGraph graph, Outlet outlet, Inlet inlet) {
        if (!(outlet instanceof OutPort) || !(inlet instanceof InPort)) {
            throw new IllegalArgumentException("Invalid outlet or inlet passed to graph");
        }

        InPort inPort = (InPort) inlet;
        OutPort outPort = (OutPort) outlet;

        if (!graph.equals(outPort.getGraph()) || !graph.equals(inPort.getGraph())) {
            throw new IllegalArgumentException("Outlet or inlet does not belong to graph");
        } else if (outPort.input != null || inPort.output != null) {
            throw new IllegalStateException("Inlet or outlet already connected");
        }

        outPort.input = inPort;
        inPort.output = outPort;

        inPort.connect();
        outPort.connect();
    }

    /**
     * Helper method to disconnect an outlet.
     */
    static void disconnect(FlowGraph graph, Outlet outlet) {
        if (!(outlet instanceof OutPort)) {
            throw new IllegalArgumentException("Invalid outlet passed to graph");
        }

        OutPort outPort = (OutPort) outlet;

        if (!graph.equals(outPort.getGraph())) {
            throw new IllegalArgumentException("Outlet or inlet does not belong to graph");
        }

        outPort.cancel(null);
        outPort.complete();
    }

    /**
     * Helper method to disconnect an inlet.
     */
    static void disconnect(FlowGraph graph, Inlet inlet) {
        if (!(inlet instanceof InPort)) {
            throw new IllegalArgumentException("Invalid outlet passed to graph");
        }

        InPort inPort = (InPort) inlet;

        if (!graph.equals(inPort.getGraph())) {
            throw new IllegalArgumentException("Outlet or inlet does not belong to graph");
        }

        inPort.finish(null);
        inPort.cancel(null);
    }
}
