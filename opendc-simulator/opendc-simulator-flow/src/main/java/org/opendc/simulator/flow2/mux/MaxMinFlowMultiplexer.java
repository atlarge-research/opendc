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
 * A {@link FlowMultiplexer} implementation that distributes the available capacity of the outputs over the inputs
 * using max-min fair sharing.
 * <p>
 * The max-min fair sharing algorithm of this multiplexer ensures that each input receives a fair share of the combined
 * output capacity, but allows individual inputs to use more capacity if there is still capacity left.
 */
public final class MaxMinFlowMultiplexer implements FlowMultiplexer, FlowStageLogic {
    /**
     * Factory implementation for this implementation.
     */
    static FlowMultiplexerFactory FACTORY = MaxMinFlowMultiplexer::new;

    private final FlowStage stage;
    private final BitSet activeInputs;
    private final BitSet activeOutputs;

    private float capacity = 0.f;
    private float demand = 0.f;
    private float rate = 0.f;

    private InPort[] inlets;
    private long[] inputs;
    private float[] rates;
    private OutPort[] outlets;

    private final MultiplexerInHandler inHandler = new MultiplexerInHandler();
    private final MultiplexerOutHandler outHandler = new MultiplexerOutHandler();

    /**
     * Construct a {@link MaxMinFlowMultiplexer} instance.
     *
     * @param graph The {@link FlowGraph} to add the multiplexer to.
     */
    public MaxMinFlowMultiplexer(FlowGraph graph) {
        this.stage = graph.newStage(this);
        this.activeInputs = new BitSet();
        this.activeOutputs = new BitSet();

        this.inlets = new InPort[4];
        this.inputs = new long[4];
        this.rates = new float[4];
        this.outlets = new OutPort[4];
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
        return rate;
    }

    @Override
    public int getMaxInputs() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxOutputs() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long onUpdate(FlowStage ctx, long now) {
        float capacity = this.capacity;
        float demand = this.demand;
        float rate = demand;

        if (demand > capacity) {
            rate = redistributeCapacity(inlets, inputs, rates, capacity);
        }

        if (this.rate != rate) {
            // Only update the outputs if the output rate has changed
            this.rate = rate;

            changeRate(activeOutputs, outlets, capacity, rate);
        }

        return Long.MAX_VALUE;
    }

    @Override
    public int getInputCount() {
        return activeInputs.length();
    }

    @Override
    public Inlet newInput() {
        final BitSet activeInputs = this.activeInputs;
        int slot = activeInputs.nextClearBit(0);

        InPort port = stage.getInlet("in" + slot);
        port.setHandler(inHandler);
        port.pull(this.capacity);

        InPort[] inlets = this.inlets;
        if (slot >= inlets.length) {
            int newLength = inlets.length + (inlets.length >> 1);
            inlets = Arrays.copyOf(inlets, newLength);
            inputs = Arrays.copyOf(inputs, newLength);
            rates = Arrays.copyOf(rates, newLength);
            this.inlets = inlets;
        }
        inlets[slot] = port;

        activeInputs.set(slot);
        return port;
    }

    @Override
    public void releaseInput(Inlet inlet) {
        InPort port = (InPort) inlet;

        activeInputs.clear(port.getId());
        port.cancel(null);
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
        port.setHandler(outHandler);

        OutPort[] outlets = this.outlets;
        if (slot >= outlets.length) {
            int newLength = outlets.length + (outlets.length >> 1);
            outlets = Arrays.copyOf(outlets, newLength);
            this.outlets = outlets;
        }
        outlets[slot] = port;

        activeOutputs.set(slot);
        return port;
    }

    @Override
    public void releaseOutput(Outlet outlet) {
        OutPort port = (OutPort) outlet;
        activeInputs.clear(port.getId());
        port.complete();
    }

    /**
     * Helper function to redistribute the specified capacity across the inlets.
     */
    private static float redistributeCapacity(InPort[] inlets, long[] inputs, float[] rates, float capacity) {
        // If the demand is higher than the capacity, we need use max-min fair sharing to distribute the
        // constrained capacity across the inputs.
        for (int i = 0; i < inputs.length; i++) {
            InPort inlet = inlets[i];
            if (inlet == null) {
                break;
            }

            inputs[i] = ((long) Float.floatToRawIntBits(inlet.getDemand()) << 32) | (i & 0xFFFFFFFFL);
        }
        Arrays.sort(inputs);

        float availableCapacity = capacity;
        int inputSize = inputs.length;

        // Divide the available output capacity fairly over the inputs using max-min fair sharing
        for (int i = 0; i < inputs.length; i++) {
            long v = inputs[i];
            int slot = (int) v;
            float d = Float.intBitsToFloat((int) (v >> 32));

            if (d == 0.0) {
                continue;
            }

            float availableShare = availableCapacity / (inputSize - i);
            float r = Math.min(d, availableShare);

            rates[slot] = r;
            availableCapacity -= r;
        }

        return capacity - availableCapacity;
    }

    /**
     * Helper method to change the rate of the outlets.
     */
    private static void changeRate(BitSet activeOutputs, OutPort[] outlets, float capacity, float rate) {
        // Divide the requests over the available capacity of the input resources fairly
        for (int i = activeOutputs.nextSetBit(0); i != -1; i = activeOutputs.nextSetBit(i + 1)) {
            OutPort outlet = outlets[i];
            float fraction = outlet.getCapacity() / capacity;
            outlet.push(rate * fraction);
        }
    }

    /**
     * A {@link InHandler} implementation for the multiplexer inputs.
     */
    private class MultiplexerInHandler implements InHandler {
        @Override
        public float getRate(InPort port) {
            return rates[port.getId()];
        }

        @Override
        public void onPush(InPort port, float demand) {
            MaxMinFlowMultiplexer.this.demand += -port.getDemand() + demand;
            rates[port.getId()] = demand;
        }

        @Override
        public void onUpstreamFinish(InPort port, Throwable cause) {
            MaxMinFlowMultiplexer.this.demand -= port.getDemand();
            releaseInput(port);
            rates[port.getId()] = 0.f;
        }
    }

    /**
     * A {@link OutHandler} implementation for the multiplexer outputs.
     */
    private class MultiplexerOutHandler implements OutHandler {
        @Override
        public void onPull(OutPort port, float capacity) {
            float newCapacity = MaxMinFlowMultiplexer.this.capacity - port.getCapacity() + capacity;
            MaxMinFlowMultiplexer.this.capacity = newCapacity;
            changeInletCapacity(newCapacity);
        }

        @Override
        public void onDownstreamFinish(OutPort port, Throwable cause) {
            float newCapacity = MaxMinFlowMultiplexer.this.capacity - port.getCapacity();
            MaxMinFlowMultiplexer.this.capacity = newCapacity;
            releaseOutput(port);
            changeInletCapacity(newCapacity);
        }

        private void changeInletCapacity(float capacity) {
            BitSet activeInputs = MaxMinFlowMultiplexer.this.activeInputs;
            InPort[] inlets = MaxMinFlowMultiplexer.this.inlets;

            for (int i = activeInputs.nextSetBit(0); i != -1; i = activeInputs.nextSetBit(i + 1)) {
                inlets[i].pull(capacity);
            }
        }
    }
}
