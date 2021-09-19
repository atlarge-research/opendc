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
import kotlin.math.max
import kotlin.math.min

/**
 * The linear interpolation power model partially adapted from CloudSim.
 * This model is developed to adopt the <a href="http://www.spec.org/power_ssj2008/">SPECpower benchmark</a>.
 *
 * @param powerValues A [List] of average active power measured by the power analyzer(s) and accumulated by the
 * PTDaemon (Power and Temperature Daemon) for this measurement interval, displayed as watts (W).
 * @see <a href="http://www.spec.org/power_ssj2008/results/res2011q1/">Machines used in the SPEC benchmark</a>
 */
public class InterpolationPowerModel(private val powerValues: List<Double>) : PowerModel {
    public override fun computePower(utilization: Double): Double {
        val clampedUtilization = min(1.0, max(0.0, utilization))
        val utilizationFlr = floor(clampedUtilization * 10).toInt()
        val utilizationCil = ceil(clampedUtilization * 10).toInt()
        val powerFlr: Double = getAveragePowerValue(utilizationFlr)
        val powerCil: Double = getAveragePowerValue(utilizationCil)
        val delta = (powerCil - powerFlr) / 10

        return if (utilization % 0.1 == 0.0)
            getAveragePowerValue((clampedUtilization * 10).toInt())
        else
            powerFlr + delta * (clampedUtilization - utilizationFlr.toDouble() / 10) * 100
    }

    override fun toString(): String = "InterpolationPowerModel[entries=${powerValues.size}]"

    /**
     * Gets the power consumption for a given utilization percentage.
     *
     * @param index the utilization percentage in the scale from [0 to 10],
     * where 10 means 100% of utilization.
     * @return the power consumption for the given utilization percentage
     */
    private fun getAveragePowerValue(index: Int): Double = powerValues[index]
}
