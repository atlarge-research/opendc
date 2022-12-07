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

import java.util.Arrays;
import java.util.BitSet;
import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.FlowStage;
import org.opendc.simulator.flow2.FlowStageLogic;
import org.opendc.simulator.flow2.InHandler;
import org.opendc.simulator.flow2.InPort;
import org.opendc.simulator.flow2.Inlet;
import org.opendc.simulator.flow2.OutHandler;
import org.opendc.simulator.flow2.OutPort;
import org.opendc.simulator.flow2.Outlet;

/**
 * A {@link FlowMultiplexer} implementation that allocates inputs to the outputs of the multiplexer exclusively.
 * This means that a single input is directly connected to an output and that the multiplexer can only support as many
 * inputs as outputs.
 */
public final class ForwardingFlowMultiplexer implements FlowMultiplexer, FlowStageLogic {
    /**
     * Factory implementation for this implementation.
     */
    static FlowMultiplexerFactory FACTORY = ForwardingFlowMultiplexer::new;

    public final IdleInHandler IDLE_IN_HANDLER = new IdleInHandler();
    public final IdleOutHandler IDLE_OUT_HANDLER = new IdleOutHandler();

    private final FlowStage stage;

    private InPort[] inlets;
    private OutPort[] outlets;
    private final BitSet activeInputs;
    private final BitSet activeOutputs;
    private final BitSet availableOutputs;

    private float capacity = 0.f;
    private float demand = 0.f;

    public ForwardingFlowMultiplexer(FlowGraph graph) {
        this.stage = graph.newStage(this);

        this.inlets = new InPort[4];
        this.activeInputs = new BitSet();
        this.outlets = new OutPort[4];
        this.activeOutputs = new BitSet();
        this.availableOutputs = new BitSet();
    }

    @Override
    public float getCapacity() {
        return capacity;
    }

    @Override
    public float getDemand() {
        return demand;
    }

    @Override
    public float getRate() {
        final BitSet activeOutputs = this.activeOutputs;
        final OutPort[] outlets = this.outlets;
        float rate = 0.f;
        for (int i = activeOutputs.nextSetBit(0); i != -1; i = activeOutputs.nextSetBit(i + 1)) {
            rate += outlets[i].getRate();
        }
        return rate;
    }

    @Override
    public int getMaxInputs() {
        return getOutputCount();
    }

    @Override
    public int getMaxOutputs() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getInputCount() {
        return activeInputs.length();
    }

    @Override
    public Inlet newInput() {
        final BitSet activeInputs = this.activeInputs;
        int slot = activeInputs.nextClearBit(0);

        InPort inPort = stage.getInlet("in" + slot);
        inPort.setMask(true);

        InPort[] inlets = this.inlets;
        if (slot >= inlets.length) {
            int newLength = inlets.length + (inlets.length >> 1);
            inlets = Arrays.copyOf(inlets, newLength);
            this.inlets = inlets;
        }

        final BitSet availableOutputs = this.availableOutputs;
        int outSlot = availableOutputs.nextSetBit(0);

        if (outSlot < 0) {
            throw new IllegalStateException("No capacity available for a new input");
        }

        inlets[slot] = inPort;
        activeInputs.set(slot);

        OutPort outPort = outlets[outSlot];
        availableOutputs.clear(outSlot);

        inPort.setHandler(new ForwardingInHandler(outPort));
        outPort.setHandler(new ForwardingOutHandler(inPort));

        inPort.pull(outPort.getCapacity());

        return inPort;
    }

    @Override
    public void releaseInput(Inlet inlet) {
        InPort port = (InPort) inlet;
        int slot = port.getId();

        final BitSet activeInputs = this.activeInputs;

        if (!activeInputs.get(slot)) {
            return;
        }

        port.cancel(null);
        activeInputs.clear(slot);

        ForwardingInHandler inHandler = (ForwardingInHandler) port.getHandler();
        availableOutputs.set(inHandler.output.getId());

        port.setHandler(IDLE_IN_HANDLER);
    }

    @Override
    public int getOutputCount() {
        return activeOutputs.length();
    }

    @Override
    public Outlet newOutput() {
        final BitSet activeOutputs = this.activeOutputs;
        int slot = activeOutputs.nextClearBit(0);

        OutPort port = stage.getOutlet("out" + slot);
        OutPort[] outlets = this.outlets;
        if (slot >= outlets.length) {
            int newLength = outlets.length + (outlets.length >> 1);
            outlets = Arrays.copyOf(outlets, newLength);
            this.outlets = outlets;
        }
        outlets[slot] = port;

        activeOutputs.set(slot);
        availableOutputs.set(slot);

        port.setHandler(IDLE_OUT_HANDLER);

        return port;
    }

    @Override
    public void releaseOutput(Outlet outlet) {
        OutPort port = (OutPort) outlet;
        int slot = port.getId();
        activeInputs.clear(slot);
        availableOutputs.clear(slot);
        port.complete();

        port.setHandler(IDLE_OUT_HANDLER);
    }

    @Override
    public long onUpdate(FlowStage ctx, long now) {
        return Long.MAX_VALUE;
    }

    class ForwardingInHandler implements InHandler {
        final OutPort output;

        ForwardingInHandler(OutPort output) {
            this.output = output;
        }

        @Override
        public float getRate(InPort port) {
            return output.getRate();
        }

        @Override
        public void onPush(InPort port, float rate) {
            ForwardingFlowMultiplexer.this.demand += -port.getDemand() + rate;

            output.push(rate);
        }

        @Override
        public void onUpstreamFinish(InPort port, Throwable cause) {
            ForwardingFlowMultiplexer.this.demand -= port.getDemand();

            final OutPort output = this.output;
            output.push(0.f);

            releaseInput(port);
        }
    }

    private class ForwardingOutHandler implements OutHandler {
        private final InPort input;

        ForwardingOutHandler(InPort input) {
            this.input = input;
        }

        @Override
        public void onPull(OutPort port, float capacity) {
            ForwardingFlowMultiplexer.this.capacity += -port.getCapacity() + capacity;

            input.pull(capacity);
        }

        @Override
        public void onDownstreamFinish(OutPort port, Throwable cause) {
            ForwardingFlowMultiplexer.this.capacity -= port.getCapacity();

            input.cancel(cause);

            releaseOutput(port);
        }
    }

    private static class IdleInHandler implements InHandler {
        @Override
        public float getRate(InPort port) {
            return 0.f;
        }

        @Override
        public void onPush(InPort port, float rate) {
            port.cancel(new IllegalStateException("Inlet is not allocated"));
        }

        @Override
        public void onUpstreamFinish(InPort port, Throwable cause) {}
    }

    private class IdleOutHandler implements OutHandler {
        @Override
        public void onPull(OutPort port, float capacity) {
            ForwardingFlowMultiplexer.this.capacity += -port.getCapacity() + capacity;
        }

        @Override
        public void onDownstreamFinish(OutPort port, Throwable cause) {
            ForwardingFlowMultiplexer.this.capacity -= port.getCapacity();
        }
    }
}
