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

package org.opendc.simulator.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.Inlet;
import org.opendc.simulator.flow2.Outlet;
import org.opendc.simulator.flow2.mux.FlowMultiplexer;
import org.opendc.simulator.flow2.mux.MaxMinFlowMultiplexer;

/**
 * A {@link SimNetworkSwitch} that can support new networking ports on demand.
 */
public final class SimNetworkSwitchVirtual implements SimNetworkSwitch {
    private final List<Port> ports = new ArrayList<>();

    /**
     * The {@link MaxMinFlowMultiplexer} to actually perform the switching.
     */
    private final MaxMinFlowMultiplexer mux;

    /**
     * Construct a {@link SimNetworkSwitchVirtual} instance.
     *
     * @param graph The {@link FlowGraph} to drive the simulation.
     */
    public SimNetworkSwitchVirtual(FlowGraph graph) {
        this.mux = new MaxMinFlowMultiplexer(graph);
    }

    /**
     * Open a new port on the switch.
     */
    public Port newPort() {
        final Port port = new Port(mux);
        ports.add(port);
        return port;
    }

    @Override
    public List<SimNetworkPort> getPorts() {
        return Collections.unmodifiableList(ports);
    }

    /**
     * A port on the network switch.
     */
    public class Port extends SimNetworkPort implements AutoCloseable {
        private final FlowMultiplexer mux;
        private final Inlet inlet;
        private final Outlet outlet;
        private boolean isClosed;

        private Port(FlowMultiplexer mux) {
            this.mux = mux;
            this.inlet = mux.newInput();
            this.outlet = mux.newOutput();
        }

        @Override
        protected Outlet getOutlet() {
            if (isClosed) {
                throw new IllegalStateException("Port is closed");
            }
            return outlet;
        }

        @Override
        protected Inlet getInlet() {
            if (isClosed) {
                throw new IllegalStateException("Port is closed");
            }
            return inlet;
        }

        @Override
        public void close() {
            isClosed = true;
            mux.releaseInput(inlet);
            mux.releaseOutput(outlet);
            ports.remove(this);
        }
    }
}
