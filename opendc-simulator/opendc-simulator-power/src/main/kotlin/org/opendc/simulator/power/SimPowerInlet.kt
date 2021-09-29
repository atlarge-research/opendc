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

import org.opendc.simulator.flow.FlowSource

/**
 * An abstract inlet that consumes electricity from a power outlet.
 */
public abstract class SimPowerInlet {
    /**
     * A flag to indicate that the inlet is currently connected to an outlet.
     */
    public val isConnected: Boolean
        get() = _outlet != null

    /**
     * The [SimPowerOutlet] to which the inlet is connected.
     */
    public val outlet: SimPowerOutlet?
        get() = _outlet
    internal var _outlet: SimPowerOutlet? = null

    /**
     * Create a [FlowSource] which represents the consumption of electricity from the power outlet.
     */
    public abstract fun createConsumer(): FlowSource
}
