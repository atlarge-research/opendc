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

package org.opendc.simulator.power

/**
 * An abstract outlet that provides a source of electricity for datacenter components.
 */
public abstract class SimPowerOutlet {
    /**
     * A flag to indicate that the inlet is currently connected to an outlet.
     */
    public val isConnected: Boolean
        get() = _inlet != null

    /**
     * The inlet that is connected to this outlet currently.
     */
    public val inlet: SimPowerInlet?
        get() = _inlet
    private var _inlet: SimPowerInlet? = null

    /**
     * Connect the specified power [inlet] to this outlet.
     *
     * @param inlet The inlet to connect to the outlet.
     */
    public fun connect(inlet: SimPowerInlet) {
        check(!isConnected) { "Outlet already connected" }
        check(!inlet.isConnected) { "Inlet already connected" }

        _inlet = inlet
        inlet._outlet = this

        onConnect(inlet)
    }

    /**
     * Disconnect the connected power outlet from this inlet
     */
    public fun disconnect() {
        val inlet = _inlet
        if (inlet != null) {
            _inlet = null
            assert(inlet._outlet == this) { "Inlet state incorrect" }
            inlet._outlet = null

            onDisconnect(inlet)
        }
    }

    /**
     * This method is invoked when an inlet is connected to the outlet.
     */
    protected abstract fun onConnect(inlet: SimPowerInlet)

    /**
     * This method is invoked when an inlet is disconnected from the outlet.
     */
    protected abstract fun onDisconnect(inlet: SimPowerInlet)
}
