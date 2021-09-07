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

@file:JvmName("RiskCostModels")
package org.opendc.telemetry.risk.cost

import org.apache.commons.math3.distribution.RealDistribution
import org.opendc.telemetry.risk.RiskFactor
import org.opendc.telemetry.risk.indicator.RiskIndicatorPoint
import kotlin.math.max

/**
 * A cost model where each violation has the same cost.
 */
public fun constantCostModel(cost: Double): RiskCostModel {
    return object : RiskCostModel {
        override fun getCost(factor: RiskFactor, point: RiskIndicatorPoint): Double = cost

        override fun toString(): String = "ConstantRiskCostModel[$cost]"
    }
}

/**
 * A cost model where a violation has a stochastic cost.
 */
public fun stochasticCostModel(dist: RealDistribution): RiskCostModel {
    return object : RiskCostModel {
        override fun getCost(factor: RiskFactor, point: RiskIndicatorPoint): Double {
            val cost = dist.sample()
            return max(0.0, cost)
        }

        override fun toString(): String = "StochasticRiskCostModel[$dist]"
    }
}

/**
 * A cost model where a violation has a linear cost.
 */
public fun linearCostModel(a: Double, b: Double = 0.0): RiskCostModel {
    return object : RiskCostModel {
        override fun getCost(factor: RiskFactor, point: RiskIndicatorPoint): Double {
            return max(0.0, a * point.value + b)
        }

        override fun toString(): String = "LinearRiskCostModel[a=$a,b=$b]"
    }
}
