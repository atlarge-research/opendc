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

package org.opendc.simulator.flow2.util;

import org.opendc.simulator.flow2.*;
import org.opendc.simulator.flow2.sink.FlowSink;
import org.opendc.simulator.flow2.source.FlowSource;

/**
 * Helper class to transform flow from outlet to inlet.
 */
public final class FlowTransformer implements FlowStageLogic, FlowSource, FlowSink {
    private final FlowStage stage;
    private final InPort input;
    private final OutPort output;

    /**
     * Construct a new {@link FlowTransformer}.
     */
    public FlowTransformer(FlowGraph graph, FlowTransform transform) {
        this.stage = graph.newStage(this);
        this.input = stage.getInlet("in");
        this.output = stage.getOutlet("out");

        this.input.setHandler(new ForwardInHandler(output, transform));
        this.input.setMask(true);
        this.output.setHandler(new ForwardOutHandler(input, transform));
        this.output.setMask(true);
    }

    /**
     * Return the {@link Outlet} of the transformer.
     */
    @Override
    public Outlet getOutput() {
        return output;
    }

    /**
     * Return the {@link Inlet} of the transformer.
     */
    @Override
    public Inlet getInput() {
        return input;
    }

    /**
     * Close the transformer.
     */
    void close() {
        stage.close();
    }

    @Override
    public long onUpdate(FlowStage ctx, long now) {
        return Long.MAX_VALUE;
    }

    private static class ForwardInHandler implements InHandler {
        private final OutPort output;
        private final FlowTransform transform;

        ForwardInHandler(OutPort output, FlowTransform transform) {
            this.output = output;
            this.transform = transform;
        }

        @Override
        public float getRate(InPort port) {
            return transform.applyInverse(output.getRate());
        }

        @Override
        public void onPush(InPort port, float demand) {
            float rate = transform.apply(demand);
            output.push(rate);
        }

        @Override
        public void onUpstreamFinish(InPort port, Throwable cause) {
            output.fail(cause);
        }
    }

    private static class ForwardOutHandler implements OutHandler {
        private final InPort input;
        private final FlowTransform transform;

        ForwardOutHandler(InPort input, FlowTransform transform) {
            this.input = input;
            this.transform = transform;
        }

        @Override
        public void onPull(OutPort port, float capacity) {
            input.pull(transform.applyInverse(capacity));
        }

        @Override
        public void onDownstreamFinish(OutPort port, Throwable cause) {
            input.cancel(cause);
        }
    }
}
