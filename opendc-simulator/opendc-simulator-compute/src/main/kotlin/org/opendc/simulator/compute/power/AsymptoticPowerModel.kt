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

import kotlin.math.E
import kotlin.math.pow

/**
 * The asymptotic power model partially adapted from GreenCloud.
 *
 * @param maxPower The maximum power draw of the server in W.
 * @param idlePower The power draw of the server at its lowest utilization level in W.
 * @param asymUtil A utilization level at which the server attains asymptotic,
 *              i.e., close to linear power consumption versus the offered load.
 *              For most of the CPUs,a is in [0.2, 0.5].
 * @param isDvfsEnabled A flag indicates whether DVFS is enabled.
 */
public class AsymptoticPowerModel(
    private val maxPower: Double,
    private val idlePower: Double,
    private val asymUtil: Double,
    private val isDvfsEnabled: Boolean,
) : PowerModel {
    private val factor: Double = (maxPower - idlePower) / 100

    public override fun computePower(utilization: Double): Double =
        if (isDvfsEnabled)
            idlePower + (factor * 100) / 2 * (1 + utilization.pow(3) - E.pow(-utilization.pow(3) / asymUtil))
        else
            idlePower + (factor * 100) / 2 * (1 + utilization - E.pow(-utilization / asymUtil))

    override fun toString(): String = "AsymptoticPowerModel[max=$maxPower,idle=$idlePower,asymptotic=$asymUtil]"
}
