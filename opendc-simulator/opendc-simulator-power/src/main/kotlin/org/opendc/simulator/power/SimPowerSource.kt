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

import org.opendc.simulator.flow.FlowEngine
import org.opendc.simulator.flow.FlowSink

/**
 * A [SimPowerOutlet] that represents a source of electricity.
 *
 * @param engine The underlying [FlowEngine] to drive the simulation under the hood.
 */
public class SimPowerSource(engine: FlowEngine, public val capacity: Double) : SimPowerOutlet() {
    /**
     * The resource source that drives this power source.
     */
    private val source = FlowSink(engine, capacity)

    /**
     * The power draw at this instant.
     */
    public val powerDraw: Double
        get() = source.rate

    override fun onConnect(inlet: SimPowerInlet) {
        source.startConsumer(inlet.createSource())
    }

    override fun onDisconnect(inlet: SimPowerInlet) {
        source.cancel()
    }

    override fun toString(): String = "SimPowerSource"
}
