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
 * A flow source that contains a fixed amount and is pushed with a given utilization.
 */
public final class SimpleFlowSource implements FlowSource, FlowStageLogic {
    private final float utilization;
    private float remainingAmount;
    private long lastPull;

    private final FlowStage stage;
    private final OutPort output;
    private final Consumer<SimpleFlowSource> completionHandler;

    /**
     * Construct a new {@link SimpleFlowSource}.
     *
     * @param graph The {@link FlowGraph} to which this source belongs.
     * @param amount The amount to transfer via the outlet.
     * @param utilization The utilization of the capacity of the outlet.
     * @param completionHandler A callback invoked when the source completes.
     */
    public SimpleFlowSource(
            FlowGraph graph, float amount, float utilization, Consumer<SimpleFlowSource> completionHandler) {
        if (amount < 0.0) {
            throw new IllegalArgumentException("Amount must be non-negative");
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
                completionHandler.accept(SimpleFlowSource.this);
            }
        });
        this.completionHandler = completionHandler;
        this.utilization = utilization;
        this.remainingAmount = amount;
        this.lastPull = graph.getEngine().getClock().millis();
    }

    /**
     * Construct a new {@link SimpleFlowSource}.
     *
     * @param graph The {@link FlowGraph} to which this source belongs.
     * @param amount The amount to transfer via the outlet.
     * @param utilization The utilization of the capacity of the outlet.
     */
    public SimpleFlowSource(FlowGraph graph, float amount, float utilization) {
        this(graph, amount, utilization, SimpleFlowSource::close);
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
        float consumed = output.getRate() * delta / 1000.f;
        float limit = output.getCapacity() * utilization;

        float remainingAmount = this.remainingAmount - consumed;
        this.remainingAmount = remainingAmount;

        long duration = (long) Math.ceil(remainingAmount / limit * 1000);

        if (duration <= 0) {
            completionHandler.accept(this);
            return Long.MAX_VALUE;
        }

        output.push(limit);
        return now + duration;
    }
}
