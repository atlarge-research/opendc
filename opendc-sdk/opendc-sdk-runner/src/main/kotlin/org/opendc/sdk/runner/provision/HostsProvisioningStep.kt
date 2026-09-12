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
import org.opendc.compute.simulator.host.SimHost
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.topology.specs.BatteryJSONSpec
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.compute.topology.specs.HostSpec
import org.opendc.compute.topology.specs.PowerSourceSpec
import org.opendc.compute.topology.specs.createSimBatteryPolicy
import org.opendc.simulator.compute.carbon.CarbonModel
import org.opendc.simulator.compute.power.SimPowerSource
import org.opendc.simulator.compute.power.batteries.BatteryAggregator
import org.opendc.simulator.compute.power.batteries.SimBattery
import org.opendc.simulator.engine.engine.FlowEngine
import org.opendc.simulator.engine.graph.FlowDistributor
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
public class HostsProvisioningStep(
    private val serviceDomain: String,
    private val clusterSpecs: List<ClusterSpec>,
    private val startTime: Long = 0L,
) : ProvisioningStep {

    private val simHosts = mutableSetOf<SimHost>()
    private val simPowerSources = mutableListOf<SimPowerSource>()

    override fun apply(ctx: ProvisioningContext): AutoCloseable {
        val service =
            requireNotNull(
                ctx.registry.resolve(serviceDomain, ComputeService::class.java),
            ) { "Compute service $serviceDomain does not exist" }

        val engine = FlowEngine.create(ctx.dispatcher)

        for ((clusterName, hostSpecs, powerSourceSpec, batterySpec) in clusterSpecs) {

            // Create the Power Source to which hosts are connected
            val (simPowerSource, powerDistributor) = this.createSimPowerSource(service, engine, powerSourceSpec, clusterName, hostSpecs.size)

            // Create the carbonmodel if provided
            val carbonModel: CarbonModel? = createCarbonModel(ctx, engine, powerSourceSpec, simPowerSource)

            // Create a battery and connect it to the powerSource
            this.addBattery(engine, service, batterySpec, simPowerSource, powerDistributor, clusterName, carbonModel)

            // Create hosts, they are connected to the powerMux when SimMachine is created
            for ((name, type, _, model, cpuPowerModel, gpuPowerModel, embodiedCarbon, expectedLifetime) in hostSpecs) {
                val simHost =
                    SimHost(
                        name,
                        type,
                        clusterName,
                        ctx.dispatcher.timeSource,
                        engine,
                        model,
                        cpuPowerModel,
                        gpuPowerModel,
                        embodiedCarbon,
                        expectedLifetime,
                        powerDistributor,
                    )

                carbonModel?.addReceiver(simHost.simMachine?.psu)

                require(simHosts.add(simHost)) { "Host with name $name already exists" }
                service.addHost(simHost)
            }
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
        val simPowerSource = SimPowerSource(engine, powerSourceSpec.totalPower.toDouble(), powerSourceSpec.name, clusterName)
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
        simPowerSource: SimPowerSource
    ): CarbonModel? {
        val carbonFragments = getCarbonFragments(powerSourceSpec.carbonTracePath)
        var carbonModel: CarbonModel? = null
        // Create Carbon Model
        if (carbonFragments != null) {
            carbonModel = CarbonModel(engine, carbonFragments, startTime)
            carbonModel.addReceiver(simPowerSource)
            ctx.registry.register(serviceDomain, CarbonModel::class.java, carbonModel)
        }

        return carbonModel;
    }

    private fun addBattery(
        engine: FlowEngine,
        service: ComputeService,
        batterySpec: BatteryJSONSpec?,
        simPowerSource: SimPowerSource,
        powerDistributor: FlowDistributor,
        clusterName: String,
        carbonModel: CarbonModel?
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
                batterySpec.batteryPolicy,
                engine,
                battery,
                batteryAggregator,
            )

        carbonModel?.addReceiver(batteryPolicy)

        FlowEdge(powerDistributor, batteryAggregator, ResourceType.POWER)

        service.addBattery(battery)
    }

}
