/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.compute.topology.specs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.common.units.DataRate
import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.common.units.Power
import org.opendc.simulator.compute.power.batteries.BatteryAggregator
import org.opendc.simulator.compute.power.batteries.SimBattery
import org.opendc.simulator.compute.power.batteries.policy.BatteryPolicy
import org.opendc.simulator.compute.power.batteries.policy.DoubleThresholdBatteryPolicy
import org.opendc.simulator.compute.power.batteries.policy.RunningMeanBatteryPolicy
import org.opendc.simulator.compute.power.batteries.policy.RunningMeanPlusBatteryPolicy
import org.opendc.simulator.compute.power.batteries.policy.SingleThresholdBatteryPolicy
import org.opendc.simulator.compute.virtualization.VirtualizationOverheadModelFactory.VirtualizationOverheadModelEnum
import org.opendc.simulator.engine.engine.FlowEngine
import org.opendc.simulator.engine.graph.distributionPolicies.FlowDistributorFactory.DistributionPolicy

/**
 * Definition of a Topology modeled in the simulation.
 *
 * @param clusters List of the clusters in this topology
 */
@Serializable
public data class TopologySpec(
    val clusters: List<ClusterJSONSpec>,
)

/**
 * Definition of a compute cluster modeled in the simulation.
 *
 * @param name The name of the cluster.
 * @param hosts List of the different hosts (nodes) available in this cluster
 */
@Serializable
public data class ClusterJSONSpec(
    val name: String = "Cluster",
    val count: Int = 1,
    val hosts: List<HostJSONSpec>,
    val powerSource: PowerSourceJSONSpec = PowerSourceJSONSpec.DFLT,
    val battery: BatteryJSONSpec? = null,
)

/**
 * Definition of a compute host modeled in the simulation.
 *
 * @param name The name of the host.
 * @param cpu The CPU available in this cluster
 * @param memory The amount of RAM memory available in Byte
 * @param powerModel The power model used to determine the power draw of a host
 * @param count The power model used to determine the power draw of a host
 */
@Serializable
public data class HostJSONSpec(
    val name: String = "Host",
    val cpu: CPUJSONSpec,
    val count: Int = 1,
    val memory: MemoryJSONSpec,
    val gpu: GPUJSONSpec? = null,
    val cpuPowerModel: PowerModelSpec = PowerModelSpec.DFLT,
    val gpuPowerModel: PowerModelSpec = PowerModelSpec.DFLT,
    val cpuDistributionPolicy: DistributionPolicySpec = MaxMinFairnessDistributionPolicySpec(),
    val gpuDistributionPolicy: DistributionPolicySpec = MaxMinFairnessDistributionPolicySpec(),
)

/**
 * Definition of a compute CPU modeled in the simulation.
 *
 * @param modelName The model name of the device.
 * @param vendor The vendor of the storage device.
 * @param arch The micro-architecture of the processor node.
 * @param count The number of CPUs of this type in the host.
 * @param coreCount The number of cores in the CPU.
 * @param coreSpeed The speed of the cores.
 */
@Serializable
public data class CPUJSONSpec(
    val modelName: String = "unknown",
    val vendor: String = "unknown",
    val arch: String = "unknown",
    val count: Int = 1,
    val coreCount: Int,
    val coreSpeed: Frequency,
)

/**
 * Definition of a compute Memory modeled in the simulation.
 *
 * @param modelName The model name of the device.
 * @param vendor The vendor of the storage device.
 * @param arch The micro-architecture of the processor node.
 * @param memorySize The size of the memory Unit
 * @param memorySpeed The speed of the cores
 */
@Serializable
public data class MemoryJSONSpec(
    val modelName: String = "unknown",
    val vendor: String = "unknown",
    val arch: String = "unknown",
    val memorySize: DataSize,
    val memorySpeed: Frequency = Frequency.ofMHz(-1),
)

@Serializable
public data class GPUJSONSpec(
    val count: Int = 1,
    val coreCount: Int,
    val coreSpeed: Frequency,
    val memorySize: DataSize = DataSize.ofMiB(-1),
    val memoryBandwidth: DataRate = DataRate.ofKibps(-1),
    val vendor: String = "unknown",
    val modelName: String = "unknown",
    val architecture: String = "unknown",
    val virtualizationOverHeadModel: VirtualizationOverheadModelSpec = NoVirtualizationOverheadModelSpec(),
)

@Serializable
public data class PowerModelSpec(
    val modelType: String,
    val power: Power = Power.ofWatts(400),
    val maxPower: Power,
    val idlePower: Power,
    val calibrationFactor: Double = 1.0,
    val asymUtil: Double = 0.0,
    val dvfs: Boolean = true,
) {
    init {
        require(maxPower >= idlePower) { "The max power of a power model can not be less than the idle power" }
    }

    public companion object {
        public val DFLT: PowerModelSpec =
            PowerModelSpec(
                modelType = "linear",
                power = Power.ofWatts(350),
                maxPower = Power.ofWatts(400.0),
                idlePower = Power.ofWatts(200.0),
                calibrationFactor = 1.0,
                asymUtil = 0.0,
                dvfs = true,
            )
    }
}

@Serializable
public sealed interface DistributionPolicySpec {
    public val type: DistributionPolicy
}

@Serializable
@SerialName("BEST_EFFORT")
public data class BestEffortDistributionPolicySpec(
    override val type: DistributionPolicy = DistributionPolicy.BEST_EFFORT,
    val updateIntervalLength: Long = 1000L,
) : DistributionPolicySpec

@Serializable
@SerialName("EQUAL_SHARE")
public data class EqualShareDistributionPolicySpec(
    override val type: DistributionPolicy = DistributionPolicy.EQUAL_SHARE,
) : DistributionPolicySpec

@Serializable
@SerialName("FIRST_FIT")
public data class FirstFitDistributionPolicySpec(
    override val type: DistributionPolicy = DistributionPolicy.FIRST_FIT,
) : DistributionPolicySpec

@Serializable
@SerialName("FIXED_SHARE")
public data class FixedShareDistributionPolicySpec(
    override val type: DistributionPolicy = DistributionPolicy.FIXED_SHARE,
    val shareRatio: Double = 1.0,
) : DistributionPolicySpec

public fun DistributionPolicySpec.toDistributionPolicy(): DistributionPolicy {
    return when (this) {
        is BestEffortDistributionPolicySpec ->
            DistributionPolicy.BEST_EFFORT.apply {
                setProperty("updateIntervalLength", updateIntervalLength)
            }
        is EqualShareDistributionPolicySpec -> DistributionPolicy.EQUAL_SHARE
        is FixedShareDistributionPolicySpec ->
            DistributionPolicy.FIXED_SHARE.apply {
                setProperty("shareRatio", shareRatio)
            }
        is FirstFitDistributionPolicySpec -> DistributionPolicy.FIRST_FIT
        is MaxMinFairnessDistributionPolicySpec -> DistributionPolicy.MAX_MIN_FAIRNESS
    }
}

@Serializable
@SerialName("MAX_MIN_FAIRNESS")
public data class MaxMinFairnessDistributionPolicySpec(
    override val type: DistributionPolicy = DistributionPolicy.MAX_MIN_FAIRNESS,
) : DistributionPolicySpec

@Serializable
public sealed interface VirtualizationOverheadModelSpec {
    public val type: VirtualizationOverheadModelEnum
}

@Serializable
@SerialName("NONE")
public data class NoVirtualizationOverheadModelSpec(
    override val type: VirtualizationOverheadModelEnum =
        VirtualizationOverheadModelEnum.NONE,
) : VirtualizationOverheadModelSpec

@Serializable
@SerialName("CONSTANT")
public data class ConstantVirtualizationOverheadModelSpec(
    override val type: VirtualizationOverheadModelEnum = VirtualizationOverheadModelEnum.CONSTANT,
    val percentageOverhead: Double? = -1.0,
) : VirtualizationOverheadModelSpec

@Serializable
@SerialName("SHARE_BASED")
public data class ShareBasedVirtualizationOverheadModelSpec(
    override val type: VirtualizationOverheadModelEnum = VirtualizationOverheadModelEnum.SHARE_BASED,
) : VirtualizationOverheadModelSpec

public fun VirtualizationOverheadModelSpec.toVirtualizationOverheadModel(): VirtualizationOverheadModelEnum {
    return when (this) {
        is NoVirtualizationOverheadModelSpec -> VirtualizationOverheadModelEnum.NONE
        is ConstantVirtualizationOverheadModelSpec ->
            VirtualizationOverheadModelEnum.CONSTANT.apply {
                if (percentageOverhead != null) {
                    // -1.0 is used to indicate that no percentage overhead is specified
                    if (percentageOverhead != -1.0 && (percentageOverhead < 0.0 || percentageOverhead > 1.0)) {
                        throw IllegalArgumentException("Percentage overhead must be between 0.0 and 1.0")
                    }
                    setProperty("percentageOverhead", percentageOverhead)
                }
            }
        is ShareBasedVirtualizationOverheadModelSpec -> VirtualizationOverheadModelEnum.SHARE_BASED
    }
}

/**
 * Definition of a power source used for JSON input.
 *
 * @property maxPower in Watt
 */
@Serializable
public data class PowerSourceJSONSpec(
    val name: String = "PowerSource",
    val maxPower: Long = Long.MAX_VALUE,
    val carbonTracePath: String? = null,
) {
    public companion object {
        public val DFLT: PowerSourceJSONSpec =
            PowerSourceJSONSpec()
    }
}

/**
 * Definition of a battery used for JSON input.
 *
 * @property name The name of the battery
 * @property capacity The capacity of the battery in kWh
 * @property chargingSpeed The charging speed of the battery in W
 * @property initialCharge The initial charge in the battery
 * @property batteryPolicy The policy used to decide when the battery charges and discharges
 * @property embodiedCarbon The embodied carbon needed to create the battery in gram
 * @property expectedLifetime The expected lifetime of the battery in years
 *
 */
@Serializable
public data class BatteryJSONSpec(
    val name: String = "Battery",
    var capacity: Double,
    val chargingSpeed: Double,
    var initialCharge: Double = 0.0,
    val batteryPolicy: BatteryPolicyJSONSpec,
    var embodiedCarbon: Double = 0.0,
    var expectedLifetime: Double = 0.0,
)

@Serializable
public sealed interface BatteryPolicyJSONSpec

@Serializable
@SerialName("single")
public data class SingleBatteryPolicyJSONSpec(
    val carbonThreshold: Double,
) : BatteryPolicyJSONSpec

@Serializable
@SerialName("double")
public data class DoubleBatteryPolicyJSONSpec(
    val lowerThreshold: Double,
    val upperThreshold: Double,
) : BatteryPolicyJSONSpec

@Serializable
@SerialName("runningMean")
public data class RunningMeanPolicyJSONSpec(
    val startingThreshold: Double,
    val windowSize: Int,
) : BatteryPolicyJSONSpec

@Serializable
@SerialName("runningMeanPlus")
public data class RunningMeanPlusPolicyJSONSpec(
    val startingThreshold: Double,
    val windowSize: Int,
) : BatteryPolicyJSONSpec

@Serializable
@SerialName("runningMedian")
public data class RunningMedianPolicyJSONSpec(
    val startingThreshold: Double,
    val windowSize: Int,
) : BatteryPolicyJSONSpec

@Serializable
@SerialName("runningQuartiles")
public data class RunningQuartilesPolicyJSONSpec(
    val startingThreshold: Double,
    val windowSize: Int,
) : BatteryPolicyJSONSpec

public fun createSimBatteryPolicy(
    batterySpec: BatteryPolicyJSONSpec,
    engine: FlowEngine,
    battery: SimBattery,
    batteryAggregator: BatteryAggregator,
): BatteryPolicy {
    return when (batterySpec) {
        is SingleBatteryPolicyJSONSpec ->
            SingleThresholdBatteryPolicy(
                engine,
                battery,
                batteryAggregator,
                batterySpec.carbonThreshold,
            )
        is DoubleBatteryPolicyJSONSpec ->
            DoubleThresholdBatteryPolicy(
                engine,
                battery,
                batteryAggregator,
                batterySpec.lowerThreshold,
                batterySpec.upperThreshold,
            )
        is RunningMeanPolicyJSONSpec ->
            RunningMeanBatteryPolicy(
                engine,
                battery,
                batteryAggregator,
                batterySpec.startingThreshold,
                batterySpec.windowSize,
            )
        is RunningMeanPlusPolicyJSONSpec ->
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
