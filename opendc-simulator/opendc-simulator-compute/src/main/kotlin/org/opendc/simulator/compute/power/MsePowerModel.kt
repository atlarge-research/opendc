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

import kotlin.math.pow

/**
 * The power model that minimizes the mean squared error (MSE)
 * to the actual power measurement by tuning the calibration parameter.
 * @see <a href="https://dl.acm.org/doi/abs/10.1145/1273440.1250665">
 *     Fan et al., Power provisioning for a warehouse-sized computer, ACM SIGARCH'07</a>
 *
 * @param maxPower The maximum power draw of the server in W.
 * @param idlePower The power draw of the server at its lowest utilization level in W.
 * @param calibrationParam The parameter set to minimize the MSE.
 */
public class MsePowerModel(
    private val maxPower: Double,
    private val idlePower: Double,
    private val calibrationParam: Double
) : PowerModel {
    private val factor: Double = (maxPower - idlePower) / 100

    public override fun computePower(utilization: Double): Double {
        return idlePower + factor * (2 * utilization - utilization.pow(calibrationParam)) * 100
    }

    override fun toString(): String = "MsePowerModel[max=$maxPower,idle=$idlePower,MSE_param=$calibrationParam]"
}
