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

package org.opendc.simulator.power;

import org.jetbrains.annotations.NotNull;
import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.Inlet;
import org.opendc.simulator.flow2.Outlet;
import org.opendc.simulator.flow2.mux.FlowMultiplexer;
import org.opendc.simulator.flow2.mux.MaxMinFlowMultiplexer;
import org.opendc.simulator.flow2.util.FlowTransform;
import org.opendc.simulator.flow2.util.FlowTransformer;

/**
 * A model of a Power Distribution Unit (PDU).
 */
public final class SimPdu extends SimPowerInlet {
    /**
     * The {@link FlowMultiplexer} that distributes the electricity over the PDU outlets.
     */
    private final MaxMinFlowMultiplexer mux;

    /**
     * A {@link FlowTransformer} that applies the power loss to the PDU's power inlet.
     */
    private final FlowTransformer transformer;

    /**
     * Construct a {@link SimPdu} instance.
     *
     * @param graph The underlying {@link FlowGraph} to which the PDU belongs.
     * @param idlePower The idle power consumption of the PDU independent of the load on the PDU.
     * @param lossCoefficient The coefficient for the power loss of the PDU proportional to the square load.
     */
    public SimPdu(FlowGraph graph, float idlePower, float lossCoefficient) {
        this.mux = new MaxMinFlowMultiplexer(graph);
        this.transformer = new FlowTransformer(graph, new FlowTransform() {
            @Override
            public float apply(float value) {
                // See https://download.schneider-electric.com/files?p_Doc_Ref=SPD_NRAN-66CK3D_EN
                return value * (lossCoefficient * value + 1) + idlePower;
            }

            @Override
            public float applyInverse(float value) {
                float c = lossCoefficient;
                if (c != 0.f) {
                    return (float) (1 + Math.sqrt(4 * value * c - 4 * idlePower * c + 1)) / (2 * c);
                } else {
                    return value - idlePower;
                }
            }
        });

        graph.connect(mux.newOutput(), transformer.getInput());
    }

    /**
     * Construct a {@link SimPdu} instance without any loss.
     *
     * @param graph The underlying {@link FlowGraph} to which the PDU belongs.
     */
    public SimPdu(FlowGraph graph) {
        this(graph, 0.f, 0.f);
    }

    /**
     * Create a new PDU outlet.
     */
    public PowerOutlet newOutlet() {
        return new PowerOutlet(mux);
    }

    @NotNull
    @Override
    public Outlet getFlowOutlet() {
        return transformer.getOutput();
    }

    @Override
    public String toString() {
        return "SimPdu";
    }

    /**
     * A PDU outlet.
     */
    public static final class PowerOutlet extends SimPowerOutlet implements AutoCloseable {
        private final FlowMultiplexer mux;
        private final Inlet inlet;
        private boolean isClosed;

        private PowerOutlet(FlowMultiplexer mux) {
            this.mux = mux;
            this.inlet = mux.newInput();
        }

        /**
         * Remove the outlet from the PDU.
         */
        @Override
        public void close() {
            isClosed = true;
            mux.releaseInput(inlet);
        }

        @Override
        public String toString() {
            return "SimPdu.Outlet";
        }

        @NotNull
        @Override
        protected Inlet getFlowInlet() {
            if (isClosed) {
                throw new IllegalStateException("Outlet is closed");
            }
            return inlet;
        }
    }
}
