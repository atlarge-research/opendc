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

import org.opendc.simulator.flow2.Inlet;
import org.opendc.simulator.flow2.Outlet;

/**
 * An abstract outlet that provides a source of electricity for datacenter components.
 */
public abstract class SimPowerOutlet {
    private SimPowerInlet inlet;

    /**
     * Determine whether the outlet is connected to a {@link SimPowerInlet}.
     *
     * @return <code>true</code> if the outlet is connected to an inlet, <code>false</code> otherwise.
     */
    public boolean isConnected() {
        return inlet != null;
    }

    /**
     * Return the {@link SimPowerInlet} to which the outlet is connected.
     */
    public SimPowerInlet getInlet() {
        return inlet;
    }

    /**
     * Connect the specified power [inlet] to this outlet.
     *
     * @param inlet The inlet to connect to the outlet.
     */
    public void connect(SimPowerInlet inlet) {
        if (isConnected()) {
            throw new IllegalStateException("Outlet already connected");
        }
        if (inlet.isConnected()) {
            throw new IllegalStateException("Inlet already connected");
        }

        this.inlet = inlet;
        this.inlet.outlet = this;

        final Inlet flowInlet = getFlowInlet();
        final Outlet flowOutlet = inlet.getFlowOutlet();

        flowInlet.getGraph().connect(flowOutlet, flowInlet);
    }

    /**
     * Disconnect the connected power outlet from this inlet
     */
    public void disconnect() {
        SimPowerInlet inlet = this.inlet;
        if (inlet != null) {
            this.inlet = null;
            assert inlet.outlet == this : "Inlet state incorrect";
            inlet.outlet = null;

            final Inlet flowInlet = getFlowInlet();
            flowInlet.getGraph().disconnect(flowInlet);
        }
    }

    /**
     * Return the flow {@link Inlet} that models the consumption of a power outlet as flow input.
     */
    protected abstract Inlet getFlowInlet();
}
