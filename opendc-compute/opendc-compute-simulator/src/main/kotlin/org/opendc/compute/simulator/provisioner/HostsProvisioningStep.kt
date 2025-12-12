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

package org.opendc.compute.simulator.provisioner

import org.opendc.common.ResourceType
import org.opendc.compute.carbon.getCarbonFragments
import org.opendc.compute.costmodel.getEnergyCostFragments
import org.opendc.compute.simulator.host.SimHost
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.compute.topology.specs.HostSpec
import org.opendc.compute.topology.specs.createSimBatteryPolicy
import org.opendc.simulator.compute.costmodel.CostModel
import org.opendc.simulator.compute.power.CarbonModel
import org.opendc.simulator.compute.power.SimPowerSource
import org.opendc.simulator.compute.power.batteries.BatteryAggregator
import org.opendc.simulator.compute.power.batteries.SimBattery
import org.opendc.simulator.engine.engine.FlowEngine
import org.opendc.simulator.engine.graph.FlowEdge
import org.opendc.simulator.engine.graph.distributionPolicies.FlowDistributorFactory
import org.opendc.simulator.engine.graph.distributionPolicies.FlowDistributorFactory.DistributionPolicy

/**
 * A [ProvisioningStep] that provisions a list of hosts for a [ComputeService].
 *
 * @param serviceDomain The domain name under which the compute service is registered.
 * @param clusterSpecs A list of [HostSpec] objects describing the simulated hosts to provision.
 * @param startTime The absolute start time of the simulation. Used to determine the carbon trace offset.
 */
public class HostsProvisioningStep internal constructor(
    private val serviceDomain: String,
    private val clusterSpecs: List<ClusterSpec>,
    private val startTime: Long = 0L,
) : ProvisioningStep {
    override fun apply(ctx: ProvisioningContext): AutoCloseable {
        val service =
            requireNotNull(
                ctx.registry.resolve(serviceDomain, ComputeService::class.java),
            ) { "Compute service $serviceDomain does not exist" }
        val simHosts = mutableSetOf<SimHost>()
        val simPowerSources = mutableListOf<SimPowerSource>()
        val simCostModels = mutableListOf<CostModel>()

        val engine = FlowEngine.create(ctx.dispatcher)

        for (cluster in clusterSpecs) {
            if (cluster.clusterCostModel?.energyCostTracePath == null) {
                // do something?
                kotlin.system.exitProcess(0)
            }
            val energyCostFragments = getEnergyCostFragments(cluster.clusterCostModel?.energyCostTracePath)

            val costModel = CostModel(engine, energyCostFragments, startTime,
                cluster.clusterCostModel?.monthlySalaries?:0.0,
                cluster.clusterCostModel?.generalUtilities?:0.0)
            simCostModels.add(costModel)
            service.addCostModel(costModel)

            // Create Power Source
            val simPowerSource = SimPowerSource(engine, cluster.powerSource.totalPower.toDouble(), cluster.powerSource.name, cluster.name)
            simPowerSources.add(simPowerSource)
            service.addPowerSource(simPowerSource)
            simPowerSource.addReceiver(costModel)

            val powerDistributor =
                FlowDistributorFactory.getFlowDistributor(
                    engine,
                    DistributionPolicy.MAX_MIN_FAIRNESS,
                    cluster.hostSpecs.size,
                    1,
                )

            val carbonFragments = getCarbonFragments(cluster.powerSource.carbonTracePath)

            var carbonModel: CarbonModel? = null
            // Create Carbon Model
            if (carbonFragments != null) {
                carbonModel = CarbonModel(engine, carbonFragments, startTime)
                carbonModel.addReceiver(simPowerSource)
                ctx.registry.register(serviceDomain, CarbonModel::class.java, carbonModel)
            }

            if (cluster.battery != null) {
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
                        cluster.battery!!.capacity,
                        cluster.battery!!.chargingSpeed,
                        cluster.battery!!.initialCharge,
                        cluster.battery!!.name,
                        cluster.name,
                        cluster.battery!!.embodiedCarbon,
                        cluster.battery!!.expectedLifetime,
                    )
                FlowEdge(battery, batteryDistributor)

                // Create Aggregator
                val batteryAggregator = BatteryAggregator(engine, battery, batteryDistributor)

                val batteryPolicy =
                    createSimBatteryPolicy(
                        cluster.battery!!.batteryPolicy,
                        engine,
                        battery,
                        batteryAggregator,
                    )

                carbonModel?.addReceiver(batteryPolicy)

                FlowEdge(powerDistributor, batteryAggregator, ResourceType.POWER)

                service.addBattery(battery)
            } else {
                FlowEdge(powerDistributor, simPowerSource, ResourceType.POWER)
            }

            // Create hosts, they are connected to the powerMux when SimMachine is created
            for (hostSpec in cluster.hostSpecs) {
                val simHost =
                    SimHost(
                        hostSpec.name,
                        hostSpec.type,
                        cluster.name,
                        ctx.dispatcher.timeSource,
                        engine,
                        hostSpec.model,
                        hostSpec.cpuPowerModel,
                        hostSpec.gpuPowerModel,
                        hostSpec.embodiedCarbon,
                        hostSpec.expectedLifetime,
                        powerDistributor,
                    )
                require(simHosts.add(simHost)) { "Host with name ${hostSpec.name} already exists" }
                service.addHost(simHost)
                simHost.getSimMachine()?.addReceiver(costModel)
            }

            costModel.updateCounters() // Init the cost model, needed to calculate the hardware values before anything runs
        }

        return AutoCloseable {
            for (simHost in simHosts) {
                simHost.close()
            }

            for (simPowerSource in simPowerSources) {
                simPowerSource.close()
            }

            for (costModel in simCostModels) {
                costModel.close()
            }
        }
    }
}
