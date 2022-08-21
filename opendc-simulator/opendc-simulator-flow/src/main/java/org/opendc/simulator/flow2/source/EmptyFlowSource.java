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

package org.opendc.simulator.flow2.source;

import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.FlowStage;
import org.opendc.simulator.flow2.FlowStageLogic;
import org.opendc.simulator.flow2.OutPort;
import org.opendc.simulator.flow2.Outlet;

/**
 * An empty {@link FlowSource}.
 */
public final class EmptyFlowSource implements FlowSource, FlowStageLogic {
    private final FlowStage stage;
    private final OutPort output;

    /**
     * Construct a new {@link EmptyFlowSource}.
     */
    public EmptyFlowSource(FlowGraph graph) {
        this.stage = graph.newStage(this);
        this.output = stage.getOutlet("out");
    }

    /**
     * Return the {@link Outlet} of the source.
     */
    @Override
    public Outlet getOutput() {
        return output;
    }

    /**
     * Remove this node from the graph.
     */
    public void close() {
        stage.close();
    }

    @Override
    public long onUpdate(FlowStage ctx, long now) {
        return Long.MAX_VALUE;
    }
}
