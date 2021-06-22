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

package org.opendc.simulator.compute.device

import org.opendc.simulator.compute.power.PowerDriver
import org.opendc.simulator.power.SimPowerInlet
import org.opendc.simulator.resources.SimResourceCommand
import org.opendc.simulator.resources.SimResourceConsumer
import org.opendc.simulator.resources.SimResourceContext
import org.opendc.simulator.resources.SimResourceEvent
import java.util.*

/**
 * A power supply of a [SimBareMetalMachine].
 *
 * @param ratedOutputPower The rated output power of the PSU.
 * @param energyEfficiency The energy efficiency of the PSU for various power draws.
 */
public class SimPsu(
    private val ratedOutputPower: Double,
    energyEfficiency: Map<Double, Double>,
) : SimPowerInlet() {
    /**
     * The power draw of the machine at this instant.
     */
    public val powerDraw: Double
        get() = _powerDraw
    private var _powerDraw = 0.0

    /**
     * The energy efficiency of the PSU at various power draws.
     */
    private val energyEfficiency = TreeMap(energyEfficiency)

    /**
     * The consumer context.
     */
    private var _ctx: SimResourceContext? = null

    /**
     * The driver that is connected to the PSU.
     */
    private var _driver: PowerDriver.Logic? = null

    init {
        require(energyEfficiency.isNotEmpty()) { "Must specify at least one entry for energy efficiency of PSU" }
    }

    /**
     * Update the power draw of the PSU.
     */
    public fun update() {
        _ctx?.interrupt()
    }

    /**
     * Connect the specified [PowerDriver.Logic] to this PSU.
     */
    public fun connect(driver: PowerDriver.Logic) {
        check(_driver == null) { "PSU already connected" }
        _driver = driver
        update()
    }

    override fun createConsumer(): SimResourceConsumer = object : SimResourceConsumer {
        override fun onNext(ctx: SimResourceContext): SimResourceCommand {
            val powerDraw = computePowerDraw(_driver?.computePower() ?: 0.0)

            return if (powerDraw > 0.0)
                SimResourceCommand.Consume(Double.POSITIVE_INFINITY, powerDraw, Long.MAX_VALUE)
            else
                SimResourceCommand.Idle()
        }

        override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
            when (event) {
                SimResourceEvent.Start -> _ctx = ctx
                SimResourceEvent.Run -> _powerDraw = ctx.speed
                SimResourceEvent.Exit -> _ctx = null
                else -> {}
            }
        }
    }

    /**
     * Compute the power draw of the PSU including the power loss.
     */
    private fun computePowerDraw(load: Double): Double {
        val loadPercentage = (load / ratedOutputPower).coerceIn(0.0, 1.0)
        val efficiency = energyEfficiency.ceilingEntry(loadPercentage)?.value ?: 1.0
        return load / efficiency
    }

    override fun toString(): String = "SimPsu[draw=$_powerDraw]"
}
