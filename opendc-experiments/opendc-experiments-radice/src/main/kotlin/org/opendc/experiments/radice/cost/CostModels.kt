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

@file:JvmName("CostModels")
package org.opendc.experiments.radice.cost

import org.opendc.telemetry.risk.RiskFactor
import org.opendc.telemetry.risk.cost.RiskCostModel
import org.opendc.telemetry.risk.indicator.RiskIndicatorPoint
import org.opendc.telemetry.risk.indicator.ServerKey
import java.util.*

/**
 * Construct a [RiskCostModel] for modelling the penalties in the EC2 SLAs.
 */
fun ec2CostModel(costPerCoreH: Double, credit: Map<Double, Double>): RiskCostModel {
    return object : RiskCostModel {
        private val credit = TreeMap(credit)

        override fun getCost(factor: RiskFactor, point: RiskIndicatorPoint): Double {
            val key = point.key
            if (key !is ServerKey || point !is RiskIndicatorPoint.Ratio) {
                return 0.0
            }

            val penalty = this.credit.ceilingEntry(point.value)?.value ?: 0.0
            val totalTimeH = point.total / (60 * 60 * 1000.0)
            val cores = key.server.cpuCount
            return (totalTimeH * cores * costPerCoreH * penalty)
        }
    }
}

/**
 * Construct a [RiskCostModel] for refunding the scheduling latency of a VM.
 */
fun latencyCostModel(costPerCoreH: Double, penalty: Double = 1.0): RiskCostModel {
    return object : RiskCostModel {
        override fun getCost(factor: RiskFactor, point: RiskIndicatorPoint): Double {
            val key = point.key
            if (key !is ServerKey) {
                return 0.0
            }

            val totalTimeH = point.value / (60 * 60 * 1000.0) // Convert from ms to h
            val cores = key.server.cpuCount
            return totalTimeH * cores * costPerCoreH * penalty
        }
    }
}

/**
 * Construct a [RiskCostModel] for penalties due to QoS.
 */
fun qosCostModel(costPerCoreH: Double, credit: Map<Double, Double>): RiskCostModel {
    return object : RiskCostModel {
        private val credit = TreeMap(credit)

        override fun getCost(factor: RiskFactor, point: RiskIndicatorPoint): Double {
            val key = point.key
            if (key !is ServerKey || point !is RiskIndicatorPoint.Ratio) {
                return 0.0
            }

            val penalty = this.credit.floorEntry(point.value)?.value ?: 0.0
            val totalTimeH = point.total / (60 * 60) // Convert from CPU-seconds to CPU-hours
            return totalTimeH * costPerCoreH * penalty
        }
    }
}
