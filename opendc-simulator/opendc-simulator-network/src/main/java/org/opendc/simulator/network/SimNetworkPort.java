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

import org.opendc.simulator.flow2.Inlet;
import org.opendc.simulator.flow2.Outlet;

/**
 * A network port allows network devices to be connected to network through links.
 */
public abstract class SimNetworkPort {
    SimNetworkLink link;

    /**
     * Determine whether the network port is connected to another port.
     *
     * @return <code>true</code> if the network port is connected, <code>false</code> otherwise.
     */
    public boolean isConnected() {
        return link != null;
    }

    /**
     * Return network link which connects this port to another port.
     */
    public SimNetworkLink getLink() {
        return link;
    }

    /**
     * Connect this port to the specified <code>port</code>.
     */
    public void connect(SimNetworkPort port) {
        if (port == this) {
            throw new IllegalArgumentException("Circular reference");
        }
        if (isConnected()) {
            throw new IllegalStateException("Port already connected");
        }
        if (port.isConnected()) {
            throw new IllegalStateException("Target port already connected");
        }

        final SimNetworkLink link = new SimNetworkLink(this, port);
        this.link = link;
        port.link = link;

        // Start bidirectional flow channel between the two ports
        final Outlet outlet = getOutlet();
        final Inlet inlet = getInlet();

        outlet.getGraph().connect(outlet, port.getInlet());
        inlet.getGraph().connect(port.getOutlet(), inlet);
    }

    /**
     * Disconnect the current network link if it exists.
     */
    public void disconnect() {
        final SimNetworkLink link = this.link;
        if (link == null) {
            return;
        }

        final SimNetworkPort opposite = link.opposite(this);
        this.link = null;
        opposite.link = null;

        final Outlet outlet = getOutlet();
        final Inlet inlet = getInlet();

        outlet.getGraph().disconnect(outlet);
        inlet.getGraph().disconnect(inlet);
    }

    /**
     * Return the {@link Outlet} representing the outgoing traffic of this port.
     */
    protected abstract Outlet getOutlet();

    /**
     * An [Inlet] representing the ingoing traffic of this port.
     */
    protected abstract Inlet getInlet();

    @Override
    public String toString() {
        return "SimNetworkPort[isConnected=" + isConnected() + "]";
    }
}
