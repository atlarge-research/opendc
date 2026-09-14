/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.sdk.model.topology

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.simulator.compute.power.batteries.BatteryAggregator
import org.opendc.simulator.compute.power.batteries.SimBattery
import org.opendc.simulator.compute.power.batteries.policy.BatteryPolicy
import org.opendc.simulator.compute.power.batteries.policy.DoubleThresholdBatteryPolicy
import org.opendc.simulator.compute.power.batteries.policy.RunningMeanBatteryPolicy
import org.opendc.simulator.compute.power.batteries.policy.RunningMeanPlusBatteryPolicy
import org.opendc.simulator.compute.power.batteries.policy.SingleThresholdBatteryPolicy
import org.opendc.simulator.engine.engine.FlowEngine

@Serializable
public sealed interface BatteryPolicySpec

@Serializable
@SerialName("single")
public data class SingleBatteryPolicySpec(
    val carbonThreshold: Double,
) : BatteryPolicySpec

@Serializable
@SerialName("double")
public data class DoubleBatteryPolicySpec(
    val lowerThreshold: Double,
    val upperThreshold: Double,
) : BatteryPolicySpec

@Serializable
@SerialName("runningMean")
public data class RunningMeanPolicySpec(
    val startingThreshold: Double,
    val windowSize: Int,
) : BatteryPolicySpec

@Serializable
@SerialName("runningMeanPlus")
public data class RunningMeanPlusPolicySpec(
    val startingThreshold: Double,
    val windowSize: Int,
) : BatteryPolicySpec

@Serializable
@SerialName("runningMedian")
public data class RunningMedianPolicySpec(
    val startingThreshold: Double,
    val windowSize: Int,
) : BatteryPolicySpec

@Serializable
@SerialName("runningQuartiles")
public data class RunningQuartilesPolicySpec(
    val startingThreshold: Double,
    val windowSize: Int,
) : BatteryPolicySpec

public fun createSimBatteryPolicy(
    batterySpec: BatteryPolicySpec,
    engine: FlowEngine,
    battery: SimBattery,
    batteryAggregator: BatteryAggregator,
): BatteryPolicy {
    return when (batterySpec) {
        is SingleBatteryPolicySpec ->
            SingleThresholdBatteryPolicy(
                engine,
                battery,
                batteryAggregator,
                batterySpec.carbonThreshold,
            )
        is DoubleBatteryPolicySpec ->
            DoubleThresholdBatteryPolicy(
                engine,
                battery,
                batteryAggregator,
                batterySpec.lowerThreshold,
                batterySpec.upperThreshold,
            )
        is RunningMeanPolicySpec ->
            RunningMeanBatteryPolicy(
                engine,
                battery,
                batteryAggregator,
                batterySpec.startingThreshold,
                batterySpec.windowSize,
            )
        is RunningMeanPlusPolicySpec ->
            RunningMeanPlusBatteryPolicy(
                engine,
                battery,
                batteryAggregator,
                batterySpec.startingThreshold,
                batterySpec.windowSize,
            )
        else -> throw IllegalArgumentException("Unknown battery policy")
    }
}
