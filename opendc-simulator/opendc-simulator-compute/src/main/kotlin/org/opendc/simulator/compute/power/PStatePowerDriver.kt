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

package org.opendc.simulator.compute.power

import org.opendc.simulator.compute.SimMachine
import org.opendc.simulator.compute.SimProcessingUnit
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * A [PowerDriver] that computes the power draw using multiple [PowerModel]s based on multiple frequency states.
 *
 * @param states A map describing the states of the driver.
 */
public class PStatePowerDriver(states: Map<Double, PowerModel>) : PowerDriver {
    /**
     * The P-States defined by the user and ordered by key.
     */
    private val states: TreeMap<Double, PowerModel> = TreeMap(states)

    override fun createLogic(machine: SimMachine, cpus: List<SimProcessingUnit>): PowerDriver.Logic = object : PowerDriver.Logic {
        override fun computePower(): Double {
            var targetFreq = 0.0
            var totalSpeed = 0.0

            for (cpu in cpus) {
                targetFreq = max(cpu.capacity, targetFreq)
                totalSpeed += cpu.rate
            }

            val maxFreq = states.lastKey()
            val (actualFreq, model) = states.ceilingEntry(min(maxFreq, targetFreq))
            val utilization = totalSpeed / (actualFreq * cpus.size)
            return model.computePower(utilization)
        }
    }

    override fun toString(): String = "PStatePowerDriver[states=$states]"
}
