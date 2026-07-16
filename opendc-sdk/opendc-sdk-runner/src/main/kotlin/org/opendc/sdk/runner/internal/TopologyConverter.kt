/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.runner.internal

import org.opendc.compute.topology.specs.BatteryJSONSpec
import org.opendc.compute.topology.specs.BatteryPolicyJSONSpec
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.compute.topology.specs.DoubleBatteryPolicyJSONSpec
import org.opendc.compute.topology.specs.HostSpec
import org.opendc.compute.topology.specs.PowerSourceSpec
import org.opendc.compute.topology.specs.RunningMeanPlusPolicyJSONSpec
import org.opendc.compute.topology.specs.RunningMeanPolicyJSONSpec
import org.opendc.compute.topology.specs.RunningMedianPolicyJSONSpec
import org.opendc.compute.topology.specs.RunningQuartilesPolicyJSONSpec
import org.opendc.compute.topology.specs.SingleBatteryPolicyJSONSpec
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.model.topology.BatterySpec
import org.opendc.sdk.model.topology.BestEffort
import org.opendc.sdk.model.topology.ConstantVirtualizationOverhead
import org.opendc.sdk.model.topology.DoubleThresholdPolicy
import org.opendc.sdk.model.topology.EqualShare
import org.opendc.sdk.model.topology.FirstFit
import org.opendc.sdk.model.topology.FixedShare
import org.opendc.sdk.model.topology.GpuSpec
import org.opendc.sdk.model.topology.MaxMinFairness
import org.opendc.sdk.model.topology.NoVirtualizationOverhead
import org.opendc.sdk.model.topology.PowerModelType
import org.opendc.sdk.model.topology.RunningMeanPlusPolicy
import org.opendc.sdk.model.topology.RunningMeanPolicy
import org.opendc.sdk.model.topology.RunningMedianPolicy
import org.opendc.sdk.model.topology.RunningQuartilesPolicy
import org.opendc.sdk.model.topology.ShareBasedVirtualizationOverhead
import org.opendc.sdk.model.topology.SingleThresholdPolicy
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.simulator.compute.models.CpuModel
import org.opendc.simulator.compute.models.GpuModel
import org.opendc.simulator.compute.models.MachineModel
import org.opendc.simulator.compute.models.MemoryUnit
import org.opendc.simulator.compute.power.getPowerModel
import org.opendc.simulator.compute.virtualization.VirtualizationOverheadModelFactory.VirtualizationOverheadModelEnum
import java.nio.file.Path
import org.opendc.sdk.model.topology.BatteryPolicy as SdkBatteryPolicy
import org.opendc.sdk.model.topology.ClusterSpec as SdkClusterSpec
import org.opendc.sdk.model.topology.DistributionPolicy as SdkDistributionPolicy
import org.opendc.sdk.model.topology.HostSpec as SdkHostSpec
import org.opendc.sdk.model.topology.PowerSourceSpec as SdkPowerSourceSpec
import org.opendc.sdk.model.topology.PowerSpec as SdkPowerModel
import org.opendc.sdk.model.topology.VirtualizationOverhead as SdkVirtualizationOverhead
import org.opendc.simulator.engine.graph.distributionPolicies.FlowDistributorFactory.DistributionPolicy as EngineDistributionPolicy

/**
 * Converts an SDK [TopologySpec] into the engine's [ClusterSpec] list consumed by
 * `setupHosts`. External carbon traces are materialized through [resolve].
 */
internal fun TopologySpec.toClusterSpecs(resolve: (ResourceReference) -> Path): List<ClusterSpec> {
    val naming = TopologyNaming()
    return clusters.flatMap { cluster -> List(cluster.count) { cluster.toClusterSpec(naming, resolve) } }
}

private fun SdkClusterSpec.toClusterSpec(
    naming: TopologyNaming,
    resolve: (ResourceReference) -> Path,
): ClusterSpec {
    val clusterName = naming.cluster(name)
    val hostSpecs = hosts.flatMap { host -> List(host.count) { host.toHostSpec(clusterName, naming) } }
    return ClusterSpec(clusterName, hostSpecs, powerSource.toSpec(naming, resolve), battery?.toSpec(naming))
}

private fun SdkHostSpec.toHostSpec(
    clusterName: String,
    naming: TopologyNaming,
): HostSpec {
    val cpus =
        List(cpu.count) {
            CpuModel(naming.nextCpuId(), cpu.coreCount, cpu.coreSpeed.toMHz(), cpu.vendor, cpu.modelName, cpu.architecture)
        }
    val memoryUnit = MemoryUnit(memory.vendor, memory.modelName, memory.speed.toMHz(), memory.size.toMiB().toLong())
    val gpus = List(gpu?.count ?: 0) { gpu!!.toGpuModel(naming.nextGpuId()) }
    val cpuPolicy = cpuDistribution.toEngine()
    val gpuPolicy = gpuDistribution.toEngine()

    val machineModel = MachineModel(cpus, memoryUnit, gpus, cpuPolicy, gpuPolicy)
    return HostSpec(
        naming.host(name),
        name,
        clusterName,
        machineModel,
        cpuPowerModel.toEngine(),
        if (gpus.isEmpty()) null else gpuPowerModel.toEngine(),
        cpuDistributionPolicy = cpuPolicy,
        gpuDistributionPolicy = gpuPolicy,
    )
}

private fun GpuSpec.toGpuModel(id: Int): GpuModel =
    GpuModel(
        id, coreCount, coreSpeed.toMHz(), memoryBandwidth.toKibps(), memory.toMiB().toLong(),
        vendor, modelName, architecture, virtualizationOverhead.toEngine(),
    )

private fun SdkPowerSourceSpec.toSpec(
    naming: TopologyNaming,
    resolve: (ResourceReference) -> Path,
): PowerSourceSpec =
    PowerSourceSpec(
        naming.powerSource(name),
        totalPower = maxPower.toWatts().toLong(),
        carbonTracePath = carbon?.let { resolve(it).toString() },
    )

private fun BatterySpec.toSpec(naming: TopologyNaming): BatteryJSONSpec =
    BatteryJSONSpec(naming.battery(name), capacity, chargingSpeed, initialCharge, policy.toSpec(), embodiedCarbon, expectedLifetime)

private fun SdkPowerModel.toEngine() =
    getPowerModel(type.modelType, power.toWatts(), maxPower.toWatts(), idlePower.toWatts(), calibrationFactor, asymUtil, dvfs)

private val PowerModelType.modelType: String
    get() =
        when (this) {
            PowerModelType.CONSTANT -> "constant"
            PowerModelType.LINEAR -> "linear"
            PowerModelType.SQUARE -> "square"
            PowerModelType.CUBIC -> "cubic"
            PowerModelType.SQRT -> "sqrt"
            PowerModelType.MSE -> "mse"
            PowerModelType.ASYMPTOTIC -> "asymptotic"
        }

private fun SdkDistributionPolicy.toEngine(): EngineDistributionPolicy =
    when (this) {
        MaxMinFairness -> EngineDistributionPolicy.MAX_MIN_FAIRNESS
        EqualShare -> EngineDistributionPolicy.EQUAL_SHARE
        FirstFit -> EngineDistributionPolicy.FIRST_FIT
        is BestEffort -> EngineDistributionPolicy.BEST_EFFORT.apply { setProperty("updateIntervalLength", updateIntervalMs) }
        is FixedShare -> EngineDistributionPolicy.FIXED_SHARE.apply { setProperty("shareRatio", shareRatio) }
    }

private fun SdkVirtualizationOverhead.toEngine(): VirtualizationOverheadModelEnum =
    when (this) {
        NoVirtualizationOverhead -> VirtualizationOverheadModelEnum.NONE
        ShareBasedVirtualizationOverhead -> VirtualizationOverheadModelEnum.SHARE_BASED
        is ConstantVirtualizationOverhead ->
            VirtualizationOverheadModelEnum.CONSTANT.apply {
                setProperty("percentageOverhead", percentageOverhead ?: -1.0)
            }
    }

private fun SdkBatteryPolicy.toSpec(): BatteryPolicyJSONSpec =
    when (this) {
        is SingleThresholdPolicy -> SingleBatteryPolicyJSONSpec(carbonThreshold)
        is DoubleThresholdPolicy -> DoubleBatteryPolicyJSONSpec(lowerThreshold, upperThreshold)
        is RunningMeanPolicy -> RunningMeanPolicyJSONSpec(startingThreshold, windowSize)
        is RunningMeanPlusPolicy -> RunningMeanPlusPolicyJSONSpec(startingThreshold, windowSize)
        is RunningMedianPolicy -> RunningMedianPolicyJSONSpec(startingThreshold, windowSize)
        is RunningQuartilesPolicy -> RunningQuartilesPolicyJSONSpec(startingThreshold, windowSize)
    }

/** Per-conversion registry producing unique names and monotonic device ids. */
private class TopologyNaming {
    private val clusters = HashMap<String, Int>()
    private val hosts = HashMap<String, Int>()
    private val powerSources = HashMap<String, Int>()
    private val batteries = HashMap<String, Int>()
    private var cpuId = 0
    private var gpuId = 0

    fun cluster(name: String): String = unique(name, clusters)

    fun host(name: String): String = unique(name, hosts)

    fun powerSource(name: String): String = unique(name, powerSources)

    fun battery(name: String): String = unique(name, batteries)

    fun nextCpuId(): Int = cpuId++

    fun nextGpuId(): Int = gpuId++

    private fun unique(
        name: String,
        seen: MutableMap<String, Int>,
    ): String {
        val count =
            seen[name] ?: run {
                seen[name] = 0
                return name
            }
        seen[name] = count + 1
        return "$name-$count"
    }
}
