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

package org.opendc.simulator.compute.cpufreq

import org.opendc.simulator.compute.SimMachine
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.PowerModel
import org.opendc.simulator.resources.SimResourceSource
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * A [ScalingDriver] that scales the frequency of the processor based on a discrete set of frequencies.
 *
 * @param states A map describing the states of the driver.
 */
public class PStateScalingDriver(states: Map<Double, PowerModel>) : ScalingDriver {
    /**
     * The P-States defined by the user and ordered by key.
     */
    private val states = TreeMap(states)

    override fun createLogic(machine: SimMachine): ScalingDriver.Logic = object : ScalingDriver.Logic {
        /**
         * The scaling contexts.
         */
        private val contexts = mutableListOf<ScalingContextImpl>()

        override fun createContext(cpu: ProcessingUnit, resource: SimResourceSource): ScalingContext {
            val ctx = ScalingContextImpl(machine, cpu, resource)
            contexts.add(ctx)
            return ctx
        }

        override fun computePower(): Double {
            var targetFreq = 0.0
            var totalSpeed = 0.0

            for (ctx in contexts) {
                targetFreq = max(ctx.target, targetFreq)
                totalSpeed += ctx.resource.speed
            }

            val maxFreq = states.lastKey()
            val (actualFreq, model) = states.ceilingEntry(min(maxFreq, targetFreq))
            val utilization = totalSpeed / (actualFreq * contexts.size)
            return model.computePower(utilization)
        }

        override fun toString(): String = "PStateScalingDriver.Logic"
    }

    private class ScalingContextImpl(
        override val machine: SimMachine,
        override val cpu: ProcessingUnit,
        override val resource: SimResourceSource
    ) : ScalingContext {
        var target = cpu.frequency
            private set

        override fun setTarget(freq: Double) {
            target = freq
        }

        override fun toString(): String = "PStateScalingDriver.Context"
    }
}
