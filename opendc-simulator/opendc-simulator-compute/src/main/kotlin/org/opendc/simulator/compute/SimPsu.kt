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

package org.opendc.simulator.compute

import org.opendc.simulator.power.SimPowerInlet
import org.opendc.simulator.resources.SimResourceCommand
import org.opendc.simulator.resources.SimResourceConsumer
import org.opendc.simulator.resources.SimResourceContext
import org.opendc.simulator.resources.SimResourceEvent

/**
 * A power supply of a [SimBareMetalMachine].
 */
public abstract class SimPsu : SimPowerInlet() {
    /**
     * The power draw of the machine at this instant.
     */
    public val powerDraw: Double
        get() = _powerDraw
    private var _powerDraw = 0.0

    /**
     * The consumer context.
     */
    private var _ctx: SimResourceContext? = null

    override fun createConsumer(): SimResourceConsumer = object : SimResourceConsumer {
        override fun onNext(ctx: SimResourceContext): SimResourceCommand {
            val powerDraw = _powerDraw
            return if (powerDraw > 0.0)
                SimResourceCommand.Consume(Double.POSITIVE_INFINITY, powerDraw, Long.MAX_VALUE)
            else
                SimResourceCommand.Idle()
        }

        override fun onEvent(ctx: SimResourceContext, event: SimResourceEvent) {
            when (event) {
                SimResourceEvent.Start -> _ctx = ctx
                SimResourceEvent.Exit -> _ctx = null
                else -> {}
            }
        }
    }

    /**
     * Update the power draw of the PSU.
     */
    public fun update() {
        _powerDraw = computePower()
        _ctx?.interrupt()
    }

    /**
     * Compute the power draw of the PSU.
     */
    protected abstract fun computePower(): Double

    override fun toString(): String = "SimPsu[draw=$_powerDraw]"
}
