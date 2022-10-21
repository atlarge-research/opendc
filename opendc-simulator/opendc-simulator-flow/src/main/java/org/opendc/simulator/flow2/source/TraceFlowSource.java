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
 * A flow source that replays a sequence of fragments, each indicating the flow rate for some period of time.
 */
public final class TraceFlowSource implements FlowSource, FlowStageLogic {
    private final OutPort output;
    private final long[] deadlines;
    private final float[] usages;
    private final int size;
    private int index;

    private final FlowStage stage;
    private final Consumer<TraceFlowSource> completionHandler;

    /**
     * Construct a {@link TraceFlowSource}.
     *
     * @param graph The {@link FlowGraph} to which the source belongs.
     * @param trace The {@link Trace} to replay.
     * @param completionHandler The completion handler to invoke when the source finishes.
     */
    public TraceFlowSource(FlowGraph graph, Trace trace, Consumer<TraceFlowSource> completionHandler) {
        this.stage = graph.newStage(this);
        this.output = stage.getOutlet("out");
        this.output.setHandler(new OutHandler() {
            @Override
            public void onPull(OutPort port, float capacity) {}

            @Override
            public void onDownstreamFinish(OutPort port, Throwable cause) {
                // Source cannot complete without re-connecting to another sink, so mark the source as completed
                completionHandler.accept(TraceFlowSource.this);
            }
        });
        this.deadlines = trace.deadlines;
        this.usages = trace.usages;
        this.size = trace.size;
        this.completionHandler = completionHandler;
    }

    /**
     * Construct a {@link TraceFlowSource}.
     *
     * @param graph The {@link FlowGraph} to which the source belongs.
     * @param trace The {@link Trace} to replay.
     */
    public TraceFlowSource(FlowGraph graph, Trace trace) {
        this(graph, trace, TraceFlowSource::close);
    }

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
        int size = this.size;
        int index = this.index;
        long[] deadlines = this.deadlines;
        long deadline;

        do {
            deadline = deadlines[index];
        } while (deadline <= now && ++index < size);

        if (index >= size) {
            output.push(0.0f);
            completionHandler.accept(this);
            return Long.MAX_VALUE;
        }

        this.index = index;
        float usage = usages[index];
        output.push(usage);

        return deadline;
    }

    /**
     * A trace describes the workload over time.
     */
    public static final class Trace {
        private final long[] deadlines;
        private final float[] usages;
        private final int size;

        /**
         * Construct a {@link Trace}.
         *
         * @param deadlines The deadlines of the trace fragments.
         * @param usages The usages of the trace fragments.
         * @param size The size of the trace.
         */
        public Trace(long[] deadlines, float[] usages, int size) {
            this.deadlines = deadlines;
            this.usages = usages;
            this.size = size;
        }

        public long[] getDeadlines() {
            return deadlines;
        }

        public float[] getUsages() {
            return usages;
        }

        public int getSize() {
            return size;
        }
    }
}
