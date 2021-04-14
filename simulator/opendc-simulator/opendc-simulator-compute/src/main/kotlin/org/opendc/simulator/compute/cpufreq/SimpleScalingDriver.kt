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
import org.opendc.simulator.compute.SimProcessingUnit
import org.opendc.simulator.compute.power.PowerModel

/**
 * A [ScalingDriver] that ignores the instructions of the [ScalingGovernor] and directly computes the power consumption
 * based on the specified [power model][model].
 */
public class SimpleScalingDriver(private val model: PowerModel) : ScalingDriver {
    override fun createLogic(machine: SimMachine): ScalingDriver.Logic = object : ScalingDriver.Logic {
        override fun createContext(cpu: SimProcessingUnit): ScalingContext {
            return object : ScalingContext {
                override val machine: SimMachine = machine

                override val cpu: SimProcessingUnit = cpu

                override fun setTarget(freq: Double) {}
            }
        }

        override fun computePower(): Double = model.computePower(machine.usage.value)

        override fun toString(): String = "SimpleScalingDriver.Logic"
    }
}
