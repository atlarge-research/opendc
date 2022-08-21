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

package org.opendc.simulator.flow2.sink;

import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.FlowStage;
import org.opendc.simulator.flow2.FlowStageLogic;
import org.opendc.simulator.flow2.InHandler;
import org.opendc.simulator.flow2.InPort;
import org.opendc.simulator.flow2.Inlet;

/**
 * A sink with a fixed capacity.
 */
public final class SimpleFlowSink implements FlowSink, FlowStageLogic {
    private final FlowStage stage;
    private final InPort input;
    private final Handler handler;

    /**
     * Construct a new {@link SimpleFlowSink} with the specified initial capacity.
     *
     * @param graph The graph to add the sink to.
     * @param initialCapacity The initial capacity of the sink.
     */
    public SimpleFlowSink(FlowGraph graph, float initialCapacity) {
        this.stage = graph.newStage(this);
        this.handler = new Handler();
        this.input = stage.getInlet("in");
        this.input.pull(initialCapacity);
        this.input.setMask(true);
        this.input.setHandler(handler);
    }

    /**
     * Return the {@link Inlet} of this sink.
     */
    @Override
    public Inlet getInput() {
        return input;
    }

    /**
     * Return the capacity of the sink.
     */
    public float getCapacity() {
        return input.getCapacity();
    }

    /**
     * Update the capacity of the sink.
     *
     * @param capacity The new capacity to update the sink to.
     */
    public void setCapacity(float capacity) {
        input.pull(capacity);
        stage.invalidate();
    }

    /**
     * Return the flow rate of the sink.
     */
    public float getRate() {
        return input.getRate();
    }

    /**
     * Remove this node from the graph.
     */
    public void close() {
        stage.close();
    }

    @Override
    public long onUpdate(FlowStage ctx, long now) {
        InPort input = this.input;
        handler.rate = Math.min(input.getDemand(), input.getCapacity());
        return Long.MAX_VALUE;
    }

    /**
     * The {@link InHandler} implementation for the sink.
     */
    private static final class Handler implements InHandler {
        float rate;

        @Override
        public float getRate(InPort port) {
            return rate;
        }

        @Override
        public void onPush(InPort port, float demand) {
            float capacity = port.getCapacity();
            rate = Math.min(demand, capacity);
        }

        @Override
        public void onUpstreamFinish(InPort port, Throwable cause) {
            rate = 0.f;
        }
    }
}
