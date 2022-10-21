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

import java.util.function.Consumer;
import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.FlowStage;
import org.opendc.simulator.flow2.FlowStageLogic;
import org.opendc.simulator.flow2.OutHandler;
import org.opendc.simulator.flow2.OutPort;
import org.opendc.simulator.flow2.Outlet;

/**
 * A {@link FlowSource} that ensures a flow is emitted for a specified amount of time at some utilization.
 */
public class RuntimeFlowSource implements FlowSource, FlowStageLogic {
    private final float utilization;

    private final FlowStage stage;
    private final OutPort output;
    private final Consumer<RuntimeFlowSource> completionHandler;

    private long duration;
    private long lastPull;

    /**
     * Construct a {@link RuntimeFlowSource} instance.
     *
     * @param graph The {@link FlowGraph} to which this source belongs.
     * @param duration The duration of the source.
     * @param utilization The utilization of the capacity of the outlet.
     * @param completionHandler A callback invoked when the source completes.
     */
    public RuntimeFlowSource(
            FlowGraph graph, long duration, float utilization, Consumer<RuntimeFlowSource> completionHandler) {
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive and non-zero");
        }

        if (utilization <= 0.0) {
            throw new IllegalArgumentException("Utilization must be positive and non-zero");
        }

        this.stage = graph.newStage(this);
        this.output = stage.getOutlet("out");
        this.output.setHandler(new OutHandler() {
            @Override
            public void onPull(OutPort port, float capacity) {}

            @Override
            public void onDownstreamFinish(OutPort port, Throwable cause) {
                // Source cannot complete without re-connecting to another sink, so mark the source as completed
                completionHandler.accept(RuntimeFlowSource.this);
            }
        });
        this.duration = duration;
        this.utilization = utilization;
        this.completionHandler = completionHandler;
        this.lastPull = graph.getEngine().getClock().millis();
    }

    /**
     * Construct a new {@link RuntimeFlowSource}.
     *
     * @param graph The {@link FlowGraph} to which this source belongs.
     * @param duration The duration of the source.
     * @param utilization The utilization of the capacity of the outlet.
     */
    public RuntimeFlowSource(FlowGraph graph, long duration, float utilization) {
        this(graph, duration, utilization, RuntimeFlowSource::close);
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
        long lastPull = this.lastPull;
        this.lastPull = now;

        long delta = Math.max(0, now - lastPull);

        OutPort output = this.output;
        float limit = output.getCapacity() * utilization;
        long duration = this.duration - delta;

        if (duration <= 0) {
            completionHandler.accept(this);
            return Long.MAX_VALUE;
        }

        this.duration = duration;
        output.push(limit);
        return now + duration;
    }
}
