/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.simulator.network

import org.opendc.simulator.resources.SimResourceConsumer
import org.opendc.simulator.resources.SimResourceProvider

/**
 * A network port allows network devices to be connected to network through links.
 */
public abstract class SimNetworkPort {
    /**
     * A flag to indicate that the network port is connected to another port.
     */
    public val isConnected: Boolean
        get() = _link != null

    /**
     * The network link which connects this port to another port.
     */
    public val link: SimNetworkLink?
        get() = _link
    private var _link: SimNetworkLink? = null

    /**
     * Connect this port to the specified [port].
     */
    public fun connect(port: SimNetworkPort) {
        require(port !== this) { "Circular reference" }
        check(!isConnected) { "Port already connected" }
        check(!port.isConnected) { "Target port already connected" }

        val link = SimNetworkLink(this, port)
        _link = link
        port._link = link

        // Start bi-directional flow channel between the two ports
        provider.startConsumer(port.createConsumer())
        port.provider.startConsumer(createConsumer())
    }

    /**
     * Disconnect the current network link if it exists.
     */
    public fun disconnect() {
        val link = _link ?: return
        val opposite = link.opposite(this)
        _link = null
        opposite._link = null

        provider.cancel()
        opposite.provider.cancel()
    }

    /**
     * Create a [SimResourceConsumer] which generates the outgoing traffic of this port.
     */
    protected abstract fun createConsumer(): SimResourceConsumer

    /**
     * The [SimResourceProvider] which processes the ingoing traffic of this port.
     */
    protected abstract val provider: SimResourceProvider

    override fun toString(): String = "SimNetworkPort[isConnected=$isConnected]"
}
