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

import org.opendc.simulator.flow.FlowConsumer
import org.opendc.simulator.flow.FlowEngine
import org.opendc.simulator.flow.FlowSource
import org.opendc.simulator.flow.mux.MaxMinFlowMultiplexer

/**
 * A [SimNetworkSwitch] that can support new networking ports on demand.
 */
public class SimNetworkSwitchVirtual(private val engine: FlowEngine) : SimNetworkSwitch {
    /**
     * The ports of this switch.
     */
    override val ports: List<Port>
        get() = _ports
    private val _ports = mutableListOf<Port>()

    /**
     * The [MaxMinFlowMultiplexer] to actually perform the switching.
     */
    private val mux = MaxMinFlowMultiplexer(engine)

    /**
     * Open a new port on the switch.
     */
    public fun newPort(): Port {
        val port = Port()
        _ports.add(port)
        return port
    }

    /**
     * A port on the network switch.
     */
    public inner class Port : SimNetworkPort(), AutoCloseable {
        /**
         * A flag to indicate that this virtual port was removed from the switch.
         */
        private var isClosed: Boolean = false

        override val provider: FlowConsumer
            get() = _provider
        private val _provider = mux.newInput()

        private val _source = mux.newOutput()

        override fun createConsumer(): FlowSource = _source

        override fun close() {
            isClosed = true
            mux.removeInput(_provider)
            _ports.remove(this)
        }
    }
}
