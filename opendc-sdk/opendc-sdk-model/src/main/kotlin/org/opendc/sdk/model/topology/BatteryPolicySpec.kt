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
