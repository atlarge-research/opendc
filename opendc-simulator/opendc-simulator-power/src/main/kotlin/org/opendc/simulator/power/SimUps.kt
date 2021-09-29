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

import org.opendc.simulator.resources.*

/**
 * A model of an Uninterruptible Power Supply (UPS).
 *
 * This model aggregates multiple power sources into a single source in order to ensure that power is always available.
 *
 * @param interpreter The underlying [SimResourceInterpreter] to drive the simulation under the hood.
 * @param idlePower The idle power consumption of the UPS independent of the load.
 * @param lossCoefficient The coefficient for the power loss of the UPS proportional to the load.
 */
public class SimUps(
    interpreter: SimResourceInterpreter,
    private val idlePower: Double = 0.0,
    private val lossCoefficient: Double = 0.0,
) : SimPowerOutlet() {
    /**
     * The resource aggregator used to combine the input sources.
     */
    private val switch = SimResourceSwitchMaxMin(interpreter)

    /**
     * The [SimResourceProvider] that represents the output of the UPS.
     */
    private val provider = switch.newOutput()

    /**
     * Create a new UPS outlet.
     */
    public fun newInlet(): SimPowerInlet {
        val forward = SimResourceForwarder(isCoupled = true)
        switch.addInput(forward)
        return Inlet(forward)
    }

    override fun onConnect(inlet: SimPowerInlet) {
        val consumer = inlet.createConsumer()
        provider.startConsumer(object : SimResourceConsumer by consumer {
            override fun onNext(ctx: SimResourceContext, now: Long, delta: Long): Long {
                val duration = consumer.onNext(ctx, now, delta)
                val loss = computePowerLoss(ctx.demand)
                val newLimit = ctx.demand + loss

                ctx.push(newLimit)
                return duration
            }
        })
    }

    override fun onDisconnect(inlet: SimPowerInlet) {
        provider.cancel()
    }

    /**
     * Compute the power loss that occurs in the UPS.
     */
    private fun computePowerLoss(load: Double): Double {
        // See https://download.schneider-electric.com/files?p_Doc_Ref=SPD_NRAN-66CK3D_EN
        return idlePower + lossCoefficient * load
    }

    /**
     * A UPS inlet.
     */
    public inner class Inlet(private val forwarder: SimResourceForwarder) : SimPowerInlet(), AutoCloseable {
        override fun createConsumer(): SimResourceConsumer = forwarder

        /**
         * Remove the inlet from the PSU.
         */
        override fun close() {
            forwarder.close()
        }

        override fun toString(): String = "SimPsu.Inlet"
    }
}
