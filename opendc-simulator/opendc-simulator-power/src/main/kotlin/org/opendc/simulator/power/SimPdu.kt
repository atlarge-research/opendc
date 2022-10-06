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

import org.opendc.simulator.flow.FlowConsumer
import org.opendc.simulator.flow.FlowEngine
import org.opendc.simulator.flow.FlowMapper
import org.opendc.simulator.flow.FlowSource
import org.opendc.simulator.flow.mux.FlowMultiplexer
import org.opendc.simulator.flow.mux.MaxMinFlowMultiplexer

/**
 * A model of a Power Distribution Unit (PDU).
 *
 * @param engine The underlying [FlowEngine] to drive the simulation under the hood.
 * @param idlePower The idle power consumption of the PDU independent of the load on the PDU.
 * @param lossCoefficient The coefficient for the power loss of the PDU proportional to the square load.
 */
public class SimPdu(
    engine: FlowEngine,
    private val idlePower: Double = 0.0,
    private val lossCoefficient: Double = 0.0,
) : SimPowerInlet() {
    /**
     * The [FlowMultiplexer] that distributes the electricity over the PDU outlets.
     */
    private val mux = MaxMinFlowMultiplexer(engine)

    /**
     * The [FlowForwarder] that represents the input of the PDU.
     */
    private val output = mux.newOutput()

    /**
     * Create a new PDU outlet.
     */
    public fun newOutlet(): Outlet = Outlet(mux, mux.newInput())

    override fun createSource(): FlowSource = FlowMapper(output) { _, rate ->
        val loss = computePowerLoss(rate)
        rate + loss
    }

    override fun toString(): String = "SimPdu"

    /**
     * Compute the power loss that occurs in the PDU.
     */
    private fun computePowerLoss(load: Double): Double {
        // See https://download.schneider-electric.com/files?p_Doc_Ref=SPD_NRAN-66CK3D_EN
        return idlePower + lossCoefficient * (load * load)
    }

    /**
     * A PDU outlet.
     */
    public class Outlet(private val switch: FlowMultiplexer, private val provider: FlowConsumer) : SimPowerOutlet(), AutoCloseable {
        override fun onConnect(inlet: SimPowerInlet) {
            provider.startConsumer(inlet.createSource())
        }

        override fun onDisconnect(inlet: SimPowerInlet) {
            provider.cancel()
        }

        /**
         * Remove the outlet from the PDU.
         */
        override fun close() {
            switch.removeInput(provider)
        }

        override fun toString(): String = "SimPdu.Outlet"
    }
}
