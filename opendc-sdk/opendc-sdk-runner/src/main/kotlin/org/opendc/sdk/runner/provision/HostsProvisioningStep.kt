/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.sdk.runner.provision

import org.opendc.common.ResourceType
import org.opendc.compute.carbon.getCarbonFragments
import org.opendc.compute.simulator.cluster.SimCluster
import org.opendc.compute.simulator.infrastructure.SimHost
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.model.topology.BatterySpec
import org.opendc.sdk.model.topology.ConstantVirtualizationOverheadSpec
import org.opendc.sdk.model.topology.DataCenterSpec
import org.opendc.sdk.model.topology.GpuSpec
import org.opendc.sdk.model.topology.HostSpec
import org.opendc.sdk.model.topology.NoVirtualizationOverheadSpec
import org.opendc.sdk.model.topology.PowerModelSpec
import org.opendc.sdk.model.topology.PowerModelType
import org.opendc.sdk.model.topology.PowerSourceSpec
import org.opendc.sdk.model.topology.ShareBasedVirtualizationOverheadSpec
import org.opendc.sdk.model.topology.TopologySpec
import org.opendc.sdk.model.topology.VirtualizationOverheadSpec
import org.opendc.sdk.model.topology.createSimBatteryPolicy
import org.opendc.sdk.runner.factory.toEngine
import org.opendc.simulator.compute.carbon.CarbonModel
import org.opendc.simulator.compute.models.CpuModel
import org.opendc.simulator.compute.models.GpuModel
import org.opendc.simulator.compute.models.MachineModel
import org.opendc.simulator.compute.models.MemoryUnit
import org.opendc.simulator.compute.power.ClusterDistributor
import org.opendc.simulator.compute.power.SimPowerSource
import org.opendc.simulator.compute.power.batteries.BatteryAggregator
import org.opendc.simulator.compute.power.batteries.SimBattery
import org.opendc.simulator.compute.power.getPowerModel
import org.opendc.simulator.compute.virtualization.VirtualizationOverheadModelFactory.VirtualizationOverheadModelEnum
import org.opendc.simulator.engine.engine.FlowEngine
import org.opendc.simulator.engine.graph.FlowDistributor
import org.opendc.simulator.engine.graph.FlowEdge
import org.opendc.simulator.engine.graph.distributionPolicies.FlowDistributorFactory
import org.opendc.simulator.engine.graph.distributionPolicies.FlowDistributorFactory.DistributionPolicy
import java.nio.file.Path

/**
 * A [ProvisioningStep] that provisions a list of hosts for a [ComputeService].
 *
 * @param serviceDomain The domain name under which the compute service is registered.
 * @param topologySpec A list of [HostSpec] objects describing the simulated hosts to provision.
 * @param startTime The absolute start time of the simulation. Used to determine the carbon trace offset.
 */
public class HostsProvisioningStep(
    private val serviceDomain: String,
    private val topologySpec: TopologySpec,
    private val startTime: Long = 0L,
    private val resolve: (ResourceReference) -> Path,
) : ProvisioningStep {
    private val simHosts = mutableSetOf<SimHost>()
    private val simPowerSources = mutableListOf<SimPowerSource>()
    private val naming = TopologyNaming()

    override fun apply(ctx: ProvisioningContext): AutoCloseable {
        val service =
            requireNotNull(
                ctx.registry.resolve(serviceDomain, ComputeService::class.java),
            ) { "Compute service $serviceDomain does not exist" }

        val engine = FlowEngine.create(ctx.dispatcher)

        for (dataCenterSpec in topologySpec.datacenters!!) {
            this.createDataCenter(ctx, engine, service, dataCenterSpec)
        }

        return AutoCloseable {
            for (simHost in simHosts) {
                simHost.close()
            }

            for (simPowerSource in simPowerSources) {
                simPowerSource.close()
            }
        }
    }

    private fun createDataCenter(
        ctx: ProvisioningContext,
        engine: FlowEngine,
        service: ComputeService,
        dataCenterSpec: DataCenterSpec,
    ) {
        val (dcPowerSource, dcPowerDistributor) =
            this.createSimPowerSource(
                service,
                engine,
                dataCenterSpec.powerSource,
                dataCenterSpec.name,
                dataCenterSpec.clusters.size,
            )

        // Create the carbonmodel if provided
        val carbonModel: CarbonModel? = createCarbonModel(ctx, engine, dataCenterSpec.powerSource, dcPowerSource)

        // Create a battery and connect it to the powerSource
        this.addBattery(
            engine,
            service,
            dataCenterSpec.battery,
            dcPowerSource,
            dcPowerDistributor,
            dataCenterSpec.name,
            carbonModel,
        )

        for ((clusterName, count, hostSpecs) in dataCenterSpec.clusters) {
            repeat(count) {
                val numHosts: Int = hostSpecs.sumOf { it.count }
                // Create the Power Source to which hosts are connected

                val clusterPowerDistributor =
                    ClusterDistributor(
                        engine,
                        numHosts,
                        1,
                    )

                val simCluster =
                    SimCluster(
                        clusterName,
                        dataCenterName = dataCenterSpec.name,
                        engine.clock,
                        clusterPowerDistributor,
                    )

                FlowEdge(clusterPowerDistributor, dcPowerDistributor, ResourceType.POWER)

                // Create hosts, they are connected to the powerMux when SimMachine is created
                for (hostSpec in hostSpecs) {
                    this.createHosts(ctx, engine, service, hostSpec, simCluster, clusterPowerDistributor, carbonModel)
                }
            }
        }
    }

    private data class PowerSourceFlows(
        val simPowerSource: SimPowerSource,
        val powerDistributor: FlowDistributor,
    )

    private fun createSimPowerSource(
        service: ComputeService,
        engine: FlowEngine,
        powerSourceSpec: PowerSourceSpec,
        clusterName: String,
        numHosts: Int,
    ): PowerSourceFlows {
        val simPowerSource =
            SimPowerSource(
                engine,
                powerSourceSpec.maxPower.toWatts(),
                naming.powerSource(powerSourceSpec.name),
                clusterName,
            )
        simPowerSources.add(simPowerSource)
        service.addPowerSource(simPowerSource)

        val powerDistributor =
            FlowDistributorFactory.getFlowDistributor(
                engine,
                DistributionPolicy.MAX_MIN_FAIRNESS,
                numHosts,
                1,
            )

        return PowerSourceFlows(simPowerSource, powerDistributor)
    }

    private fun createCarbonModel(
        ctx: ProvisioningContext,
        engine: FlowEngine,
        powerSourceSpec: PowerSourceSpec,
        simPowerSource: SimPowerSource,
    ): CarbonModel? {
        val pathToFile = powerSourceSpec.carbon?.let { resolve(it).toString() }

        val carbonFragments = getCarbonFragments(pathToFile)
        var carbonModel: CarbonModel? = null
        // Create Carbon Model
        if (carbonFragments != null) {
            carbonModel = CarbonModel(engine, carbonFragments, startTime)
            carbonModel.addReceiver(simPowerSource)
            ctx.registry.register(serviceDomain, CarbonModel::class.java, carbonModel)
        }

        return carbonModel
    }

    private fun addBattery(
        engine: FlowEngine,
        service: ComputeService,
        batterySpec: BatterySpec?,
        simPowerSource: SimPowerSource,
        powerDistributor: FlowDistributor,
        clusterName: String,
        carbonModel: CarbonModel?,
    ) {
        if (batterySpec == null) {
            FlowEdge(powerDistributor, simPowerSource, ResourceType.POWER)
            return
        }

        // Create Battery Distributor
        val batteryDistributor =
            FlowDistributorFactory.getFlowDistributor(
                engine,
                DistributionPolicy.MAX_MIN_FAIRNESS,
                2,
                1,
            )
        FlowEdge(batteryDistributor, simPowerSource)

        // Create Battery
        val battery =
            SimBattery(
                engine,
                batterySpec.capacity,
                batterySpec.chargingSpeed,
                batterySpec.initialCharge,
                batterySpec.name,
                clusterName,
                batterySpec.embodiedCarbon,
                batterySpec.expectedLifetime,
            )
        FlowEdge(battery, batteryDistributor)

        // Create Aggregator
        val batteryAggregator = BatteryAggregator(engine, battery, batteryDistributor)

        val batteryPolicy =
            createSimBatteryPolicy(
                batterySpec.policy,
                engine,
                battery,
                batteryAggregator,
            )

        carbonModel?.addReceiver(batteryPolicy)

        FlowEdge(powerDistributor, batteryAggregator, ResourceType.POWER)

        service.addBattery(battery)
    }

    private fun createHosts(
        ctx: ProvisioningContext,
        engine: FlowEngine,
        service: ComputeService,
        hostSpec: HostSpec,
        simCluster: SimCluster,
        powerDistributor: FlowDistributor,
        carbonModel: CarbonModel?,
    ) {
        repeat(hostSpec.count) {
            val cpus =
                List(hostSpec.cpu.count) {
                    CpuModel(
                        naming.nextCpuId(),
                        hostSpec.cpu.coreCount,
                        hostSpec.cpu.coreSpeed.toMHz(),
                        hostSpec.cpu.vendor,
                        hostSpec.cpu.modelName,
                        hostSpec.cpu.architecture,
                    )
                }
            val memoryUnit =
                MemoryUnit(
                    hostSpec.memory.vendor,
                    hostSpec.memory.modelName,
                    hostSpec.memory.speed.toMHz(),
                    hostSpec.memory.size.toMiB().toLong(),
                )
            val gpus = List(hostSpec.gpu?.count ?: 0) { hostSpec.gpu!!.toGpuModel(naming.nextGpuId()) }
            val cpuPolicy = hostSpec.cpuDistribution.toEngine()
            val gpuPolicy = hostSpec.gpuDistribution.toEngine()

            val machineModel =
                MachineModel(
                    cpus,
                    memoryUnit,
                    gpus,
                    cpuPolicy,
                    gpuPolicy,
                )

            // TODO: Connect to spec
            val embodiedCarbon = 1000.0
            val expectedLifetime = 5.0

            val simHost =
                SimHost(
                    naming.host(hostSpec.name),
                    simCluster.getName(),
                    ctx.dispatcher.timeSource,
                    engine,
                    machineModel = machineModel,
                    hostSpec.cpuPowerModel.toEngine(),
                    hostSpec.gpuPowerModel.toEngine(),
                    embodiedCarbon,
                    expectedLifetime,
                    powerDistributor,
                )

            carbonModel?.addReceiver(simHost.simMachine?.psu)

            require(simHosts.add(simHost)) { "Error when making Host $simHost" }
            service.addHost(simHost)

            simCluster.addHost(simHost)
        }
    }

    private fun GpuSpec.toGpuModel(id: Int): GpuModel =
        GpuModel(
            id, coreCount, coreSpeed.toMHz(), memoryBandwidth.toKibps(), memory.toMiB().toLong(),
            vendor, modelName, architecture, virtualizationOverhead.toEngine(),
        )

    private fun PowerModelSpec.toEngine() =
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

    private fun VirtualizationOverheadSpec.toEngine(): VirtualizationOverheadModelEnum =
        when (this) {
            NoVirtualizationOverheadSpec -> VirtualizationOverheadModelEnum.NONE
            ShareBasedVirtualizationOverheadSpec -> VirtualizationOverheadModelEnum.SHARE_BASED
            is ConstantVirtualizationOverheadSpec ->
                VirtualizationOverheadModelEnum.CONSTANT.apply {
                    setProperty("percentageOverhead", percentageOverhead ?: -1.0)
                }
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
}
