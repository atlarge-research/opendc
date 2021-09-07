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

import kotlin.math.ceil
import kotlin.math.floor

/**
 * A [PowerModel] that interpolates the active power of machine over an array of power values based on the current load.
 *
 * This model can be used in conjunction with values from the [SPECpower benchmark](http://www.spec.org/power_ssj2008).
 *
 * @param values A [DoubleArray] of the average active power in W of a machine at the load percentage of the index.
 * @see <a href="http://www.spec.org/power_ssj2008/results/res2011q1/">Machines used in the SPEC benchmark</a>
 */
public class InterpolationPowerModel(public val values: DoubleArray) : PowerModel {
    init {
        require(values.isNotEmpty()) { "No power values given" }
    }

    public override fun computePower(utilization: Double): Double {
        val powerValues = values
        val clampedUtilization = utilization.coerceIn(0.0, 1.0)
        val target = clampedUtilization * (powerValues.size - 1)

        val left = floor(target).toInt()
        val right = ceil(target).toInt()
        val t = target - left

        return (1 - t) * powerValues[left] + t * powerValues[right]
    }

    override fun toString(): String = "InterpolationPowerModel[values=${values.size}]"
}
